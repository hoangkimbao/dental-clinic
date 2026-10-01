#!/usr/bin/env bash
# ==============================================================================
# DentalCare Luxury Clinic — Stop Enterprise Stack (Unix/macOS/WSL)
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
fi

echo "[*] Bringing down DentalCare Docker containers and networks..."
$COMPOSE_CMD down

echo "[OK] DentalCare stack successfully stopped."
