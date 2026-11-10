#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "========================================================"
echo "Generating True Binary PNG Assets for DentalCare Mobile"
echo "========================================================"

if command -v node >/dev/null 2>&1; then
    echo "Using Node.js generator (generate_png_assets.js)..."
    node "${SCRIPT_DIR}/generate_png_assets.js"
    echo "[SUCCESS] Authentic binary PNG assets generated in ${SCRIPT_DIR}/assets"
else
    echo "[ERROR] Node.js is required to generate binary PNG assets on Linux/macOS."
    exit 1
fi
