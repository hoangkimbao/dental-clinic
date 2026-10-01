#!/usr/bin/env bash
# ==============================================================================
# DentalCare Luxury Clinic — Unified Deployment & Command Center Launcher (Unix/macOS/WSL)
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================================================="
echo "      DENTALCARE LUXURY CLINIC — OMNICHANNEL ENTERPRISE DEPLOYMENT"
echo "                Web Portal | Mobile PWA | PC Desktop Runner"
echo "=============================================================================="
echo ""

# 1. Verify Docker Engine
echo "[*] Step 1/5: Checking Docker runtime environment..."
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker CLI not found. Please install Docker."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "[ERROR] Docker daemon is not running. Please start Docker."
    exit 1
fi

COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
fi
echo "[*] Using Compose CLI: $COMPOSE_CMD"
echo ""

# Ensure PWA icons
if command -v python3 &> /dev/null && [ -f "desktop-runner/runner.py" ]; then
    python3 desktop-runner/runner.py --check > /dev/null 2>&1 || true
elif command -v python &> /dev/null && [ -f "desktop-runner/runner.py" ]; then
    python desktop-runner/runner.py --check > /dev/null 2>&1 || true
fi

# 2. Build & Start Stack
echo "[*] Step 2/5: Building and starting container stack & Cloudflare tunnel..."
$COMPOSE_CMD up -d --build
echo "[OK] Containers started."
echo ""

# 3. Wait for Local Backend Health
echo "[*] Step 3/5: Waiting for backend to become ready on http://localhost:8080..."
APP_RETRIES=0
MAX_APP_RETRIES=30
while [ $APP_RETRIES -lt $MAX_APP_RETRIES ]; do
    APP_RETRIES=$((APP_RETRIES + 1))
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ || true)
    if [ "$HTTP_STATUS" = "200" ]; then
        echo "[OK] Backend is healthy (HTTP 200 OK)."
        break
    fi
    echo "    - Attempt $APP_RETRIES/$MAX_APP_RETRIES: Initializing, waiting 2s..."
    sleep 2
done
echo ""

# 4. Poll Cloudflared Container Logs for Public Tunnel URL
echo "[*] Step 4/5: Polling Cloudflare tunnel logs for public URL..."
TUNNEL_RETRIES=0
MAX_TUNNEL_RETRIES=20
TUNNEL_URL=""

while [ $TUNNEL_RETRIES -lt $MAX_TUNNEL_RETRIES ]; do
    TUNNEL_RETRIES=$((TUNNEL_RETRIES + 1))
    TUNNEL_URL=$($COMPOSE_CMD logs tunnel 2>&1 | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | head -n 1 || true)
    if [ -n "$TUNNEL_URL" ]; then
        break
    fi
    echo "    - Attempt $TUNNEL_RETRIES/$MAX_TUNNEL_RETRIES: Waiting for tunnel URL, waiting 2s..."
    sleep 2
done

if [ -n "$TUNNEL_URL" ]; then
    TARGET_URL="$TUNNEL_URL"
    echo ""
    echo "=============================================================================="
    echo "  [SUCCESS] CLOUDFLARE PUBLIC TUNNEL ESTABLISHED!"
    echo "=============================================================================="
    echo "  Public Web Portal & Mobile PWA URL : $TUNNEL_URL"
    echo "  Local Direct Backend URL           : http://localhost:8080"
    echo "=============================================================================="
else
    echo ""
    echo "=============================================================================="
    echo "  [WARNING] CLOUDFLARE TUNNEL RETRIEVAL TIMED OUT"
    echo "=============================================================================="
    echo "  Falling back to local backend URL: http://localhost:8080"
    echo "=============================================================================="
    TARGET_URL="http://localhost:8080"
fi
echo ""

# 5. Launch Desktop Runner
echo "[*] Step 5/5: Launching Desktop Runner pointed to $TARGET_URL..."
if command -v python3 &> /dev/null && [ -f "desktop-runner/runner.py" ]; then
    python3 desktop-runner/runner.py "$TARGET_URL" &
elif command -v python &> /dev/null && [ -f "desktop-runner/runner.py" ]; then
    python desktop-runner/runner.py "$TARGET_URL" &
elif command -v xdg-open &> /dev/null; then
    xdg-open "$TARGET_URL"
elif command -v open &> /dev/null; then
    open "$TARGET_URL"
else
    echo "[INFO] Open $TARGET_URL in your browser."
fi

echo ""
echo "[*] Stack is running. To stop: ./stop-all.sh (or $COMPOSE_CMD down)"
