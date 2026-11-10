#!/usr/bin/env bash
# ==============================================================================
# DentalCare Luxury Clinic — Automated Zero-Downtime VPS Sync & Deploy Watchdog
# ==============================================================================
# This script continuously synchronizes the VPS deployment with upstream git:
# 1. Enforces concurrency locking via /tmp/dentalcare-sync.lock
# 2. Detects new commits on upstream branch without dirtying local changes
# 3. Pre-builds new container image before restarting (zero-downtime)
# 4. Recreates the application container
# 5. Polls Spring Boot Actuator health endpoint
# 6. Automatically rolls back to the previous stable commit if healthcheck fails
# 7. Cleans up dangling Docker images to preserve disk space
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

LOCK_FILE="/tmp/dentalcare-sync.lock"
LOG_FILE="/var/log/dentalcare-sync.log"

# Fallback to local log if /var/log is not writable (non-root execution)
if ! touch "$LOG_FILE" 2>/dev/null; then
    LOG_FILE="$SCRIPT_DIR/vps-sync.log"
fi

# Structured logging helper
log() {
    local LEVEL="$1"
    shift
    local MSG="$*"
    local TIMESTAMP
    TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"
    echo "[$TIMESTAMP] [$LEVEL] $MSG" | tee -a "$LOG_FILE"
}

# ------------------------------------------------------------------------------
# 1. Concurrency Lock Handling (Atomic Directory Lock)
# ------------------------------------------------------------------------------
LOCK_DIR="/tmp/dentalcare-sync.lock.d"
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    PID=$(cat "$LOCK_FILE" 2>/dev/null || true)
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
        log "INFO" "Sync operation already active (PID: $PID). Skipping this cycle."
        exit 0
    else
        log "WARN" "Found stale lock from dead process ($PID). Re-acquiring..."
        rm -rf "$LOCK_DIR" "$LOCK_FILE"
        mkdir -p "$LOCK_DIR"
    fi
fi

echo $$ > "$LOCK_FILE"
cleanup() {
    rm -rf "$LOCK_DIR" "$LOCK_FILE"
}
trap cleanup EXIT INT TERM

# ------------------------------------------------------------------------------
# 2. Command Line Arguments & Help
# ------------------------------------------------------------------------------
FORCE_SYNC=0
if [ "$1" = "--force" ] || [ "$1" = "-f" ]; then
    FORCE_SYNC=1
    log "INFO" "Forced sync mode requested."
elif [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "Usage: ./vps-sync.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -f, --force    Force container rebuild and health check even if git is up-to-date"
    echo "  -h, --help     Show this help message"
    echo ""
    echo "Crontab integration example (run every 5 minutes):"
    echo "  */5 * * * * /home/ubuntu/dental-clinic/vps-sync.sh >> /var/log/dentalcare-cron.log 2>&1"
    exit 0
fi

# ------------------------------------------------------------------------------
# 3. Determine Docker Compose CLI
# ------------------------------------------------------------------------------
COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        log "ERROR" "Docker Compose CLI not found. Please install Docker Compose."
        exit 1
    fi
fi

# ------------------------------------------------------------------------------
# 4. Git Repository Change Detection
# ------------------------------------------------------------------------------
if ! git rev-parse --is-inside-work-tree &>/dev/null; then
    log "ERROR" "Current directory ($SCRIPT_DIR) is not a git repository."
    exit 1
fi

BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "main")
log "INFO" "Checking remote updates for branch '$BRANCH'..."

# Fetch remote changes silently
git fetch origin "$BRANCH" > /dev/null 2>&1 || {
    log "WARN" "Unable to fetch origin/$BRANCH (network offline or auth required). Skipping pull."
    if [ $FORCE_SYNC -eq 0 ]; then
        exit 0
    fi
}

LOCAL_HASH=$(git rev-parse HEAD)
REMOTE_HASH=$(git rev-parse "origin/$BRANCH" 2>/dev/null || echo "$LOCAL_HASH")

if [ $FORCE_SYNC -eq 0 ] && [ "$LOCAL_HASH" = "$REMOTE_HASH" ]; then
    log "INFO" "Codebase is fully up-to-date ($LOCAL_HASH). No rebuild required."
    exit 0
fi

PREV_COMMIT="$LOCAL_HASH"

if [ "$LOCAL_HASH" != "$REMOTE_HASH" ]; then
    log "INFO" "New commits detected: $LOCAL_HASH -> $REMOTE_HASH. Pulling changes..."
    if ! git pull origin "$BRANCH"; then
        log "ERROR" "git pull origin $BRANCH failed! Aborting sync."
        exit 1
    fi
else
    log "INFO" "Executing forced rebuild on commit $LOCAL_HASH..."
fi

# ------------------------------------------------------------------------------
# 5. Zero-Downtime Container Rebuild
# ------------------------------------------------------------------------------
# Source .env if present
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    . "$SCRIPT_DIR/.env" 2>/dev/null || true
    set +a
fi

# Configure native Cloudflared tunnel command for distroless container
if [ -z "$CLOUDFLARE_TUNNEL_COMMAND" ]; then
    if [ -n "$CLOUDFLARE_TUNNEL_TOKEN" ] || [ -n "$TUNNEL_TOKEN" ]; then
        export CLOUDFLARE_TUNNEL_COMMAND="tunnel --no-autoupdate --metrics 0.0.0.0:2000 run"
    else
        export CLOUDFLARE_TUNNEL_COMMAND="tunnel --no-autoupdate --metrics 0.0.0.0:2000 --url http://app:8080"
    fi
fi

log "INFO" "Pre-building updated application container image..."
if ! $COMPOSE_CMD build --pull app; then
    log "ERROR" "Container build failed! Rolling back git to previous commit $PREV_COMMIT..."
    git reset --hard "$PREV_COMMIT"
    exit 1
fi

log "INFO" "Applying container update (zero-downtime reload)..."
if ! $COMPOSE_CMD up -d --no-deps app; then
    log "ERROR" "Failed to recreate app container! Initiating rollback..."
    git reset --hard "$PREV_COMMIT"
    $COMPOSE_CMD build app
    $COMPOSE_CMD up -d --no-deps app
    exit 1
fi

# ------------------------------------------------------------------------------
# 6. Actuator Health Check Validation
# ------------------------------------------------------------------------------
log "INFO" "Validating application health on http://127.0.0.1:8080/actuator/health..."
RETRY=0
MAX_RETRIES=25
HEALTHY=0

while [ $RETRY -lt $MAX_RETRIES ]; do
    RETRY=$((RETRY + 1))
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/actuator/health 2>/dev/null || true)
    if [ "$HTTP_STATUS" = "200" ]; then
        HEALTHY=1
        break
    fi
    log "DEBUG" "Healthcheck poll attempt $RETRY/$MAX_RETRIES returned HTTP $HTTP_STATUS, retrying in 3s..."
    sleep 3
done

# ------------------------------------------------------------------------------
# 7. Verification & Automated Rollback
# ------------------------------------------------------------------------------
if [ $HEALTHY -eq 1 ]; then
    NEW_COMMIT=$(git rev-parse --short HEAD)
    log "SUCCESS" "Application deployment verified healthy! Active commit: $NEW_COMMIT"
    
    # Ensure tunnel container is also running and healthy
    $COMPOSE_CMD up -d --no-deps tunnel > /dev/null 2>&1 || true
    
    # Prune obsolete container layers to conserve VPS disk storage
    docker image prune -f > /dev/null 2>&1 || true
    log "INFO" "Dangling docker images pruned. VPS synchronization completed successfully."
    exit 0
else
    log "CRITICAL" "New deployment failed health check after $MAX_RETRIES attempts! Triggering automated rollback..."
    git reset --hard "$PREV_COMMIT"
    log "INFO" "Rebuilding previous stable commit ($PREV_COMMIT)..."
    $COMPOSE_CMD build app
    $COMPOSE_CMD up -d --no-deps app
    
    # Verify rollback health
    ROLLBACK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/actuator/health 2>/dev/null || true)
    if [ "$ROLLBACK_STATUS" = "200" ]; then
        log "WARN" "Rollback to $PREV_COMMIT succeeded. Service availability restored."
    else
        log "ERROR" "Rollback container also failed healthcheck ($ROLLBACK_STATUS). Immediate operator review required!"
    fi
    exit 1
fi
