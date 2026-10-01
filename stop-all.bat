@echo off
setlocal
cd /d "%~dp0"

echo ==============================================================================
echo       DENTALCARE LUXURY CLINIC — SHUTTING DOWN ENTERPRISE STACK
echo ==============================================================================

set "COMPOSE_CMD=docker compose"
docker compose version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set "COMPOSE_CMD=docker-compose"
)

echo [*] Bringing down Docker containers and networks...
%COMPOSE_CMD% down

echo [OK] DentalCare stack successfully stopped.
pause
