#!/usr/bin/env bash
# ==============================================================================
# DentalCare Clinic — Oracle Cloud VPS One-Click Auto-Deploy & Provisioning Script
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================================================="
echo "    DENTALCARE CLINIC — ORACLE CLOUD ALWAYS FREE AUTOMATED DEPLOYMENT"
echo "=============================================================================="
echo ""

# 1. Check or install Docker Engine
if ! command -v docker &> /dev/null; then
    echo "[*] Docker not found. Installing Docker Engine..."
    curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
    sudo sh /tmp/get-docker.sh
    sudo usermod -aG docker "$USER" || true
    rm -f /tmp/get-docker.sh
    echo "[OK] Docker installed successfully."
else
    echo "[OK] Docker is already installed."
fi

# 2. Determine Compose command
COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
fi

# 3. Build & start containers
echo "[*] Building and starting DentalCare containers with Cloudflare Tunnel..."

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

$COMPOSE_CMD up -d --build

# 4. Wait for application readiness on loopback interface
echo "[*] Waiting for backend readiness on http://127.0.0.1:8080..."
RETRY=0
MAX_RETRIES=30
while [ $RETRY -lt $MAX_RETRIES ]; do
    RETRY=$((RETRY + 1))
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/actuator/health 2>/dev/null || curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/ 2>/dev/null || true)
    if [ "$STATUS" = "200" ]; then
        echo "[OK] DentalCare backend is online and healthy!"
        break
    fi
    echo "    - Waiting for Spring Boot initialization ($RETRY/$MAX_RETRIES)..."
    sleep 2
done

# 5. Extract Cloudflare Tunnel URL (tail -n 1 for latest)
echo "[*] Extracting Cloudflare public tunnel URL..."
sleep 3
CUSTOM_DOMAIN="${CLOUDFLARE_CUSTOM_DOMAIN:-nhakhoadentalcare.id.vn}"
TUNNEL_URL=""

if [ -n "$CLOUDFLARE_TUNNEL_TOKEN" ] || [ -n "$TUNNEL_TOKEN" ]; then
    if $COMPOSE_CMD logs tunnel 2>&1 | grep -qiE '(Registered tunnel connection|Connection [a-f0-9-]+ registered)'; then
        TUNNEL_URL="https://$CUSTOM_DOMAIN"
    fi
fi

if [ -z "$TUNNEL_URL" ]; then
    TUNNEL_URL=$($COMPOSE_CMD logs tunnel 2>&1 | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | tail -n 1 || true)
fi

echo ""
echo "=============================================================================="
if [ -n "$TUNNEL_URL" ]; then
    echo "  [SUCCESS] DENTALCARE IS DEPLOYED AND RUNNING 24/7!"
    echo "  Public URL: $TUNNEL_URL"
    echo "  Internal Loopback: http://127.0.0.1:8080"
else
    echo "  [INFO] App is running locally on http://127.0.0.1:8080"
    echo "  Check tunnel logs: $COMPOSE_CMD logs -f tunnel"
fi
echo "=============================================================================="
echo ""
echo "[*] To enable automated continuous synchronization via git & healthcheck watchdog:"
echo "    chmod +x vps-sync.sh"
echo "    Add to crontab: (crontab -l 2>/dev/null; echo '*/5 * * * * $SCRIPT_DIR/vps-sync.sh >> /var/log/dentalcare-sync.log 2>&1') | crontab -"
echo ""
