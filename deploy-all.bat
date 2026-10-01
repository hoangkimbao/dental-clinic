@echo off
setlocal enabledelayedexpansion
title DentalCare Clinic — Unified Enterprise Deployment & Command Center Launcher
chcp 65001 >nul

cd /d "%~dp0"

echo ==============================================================================
echo       DENTALCARE LUXURY CLINIC — OMNICHANNEL ENTERPRISE DEPLOYMENT
echo                 Web Portal ^| Mobile PWA ^| PC Desktop Runner
echo ==============================================================================
echo.

:: 1. Verify Docker Availability
echo [*] Step 1/5: Checking Docker runtime environment...
docker --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker CLI not found. Please install Docker Desktop and ensure it is in your PATH.
    pause
    exit /b 1
)

docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker daemon is not running. Please start Docker Desktop and run this script again.
    pause
    exit /b 1
)

:: Determine docker compose command (v2 plugin vs v1 standalone)
set "COMPOSE_CMD=docker compose"
docker compose version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set "COMPOSE_CMD=docker-compose"
)
echo [*] Using Docker Compose CLI: %COMPOSE_CMD%
echo.

:: Ensure PWA icons are generated before container build
python --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    if exist "desktop-runner\runner.py" (
        python desktop-runner\runner.py --check >nul 2>&1
    )
)

:: 2. Build and Start Container Stack
echo [*] Step 2/5: Building and starting DentalCare container stack & Cloudflare tunnel...
%COMPOSE_CMD% up -d --build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to start Docker stack. Please check docker-compose.yml and logs.
    pause
    exit /b 1
)
echo [OK] Containers started successfully.
echo.

:: 3. Wait for Local Spring Boot Server Readiness
echo [*] Step 3/5: Waiting for Spring Boot backend to become ready on http://localhost:8080...
set /a APP_RETRY=0
set /a APP_MAX_RETRIES=30
set "APP_READY=0"

:CHECK_APP_LOOP
set /a APP_RETRY+=1
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if %ERRORLEVEL% EQU 0 (
    set "APP_READY=1"
    goto APP_READY_SUCCESS
)

if %APP_RETRY% GEQ %APP_MAX_RETRIES% (
    echo [WARN] Backend did not respond with 200 OK within 60s, proceeding to tunnel check...
    goto APP_READY_CONTINUE
)

echo     - Attempt %APP_RETRY%/%APP_MAX_RETRIES%: Backend initializing, waiting 2s...
timeout /t 2 /nobreak >nul
goto CHECK_APP_LOOP

:APP_READY_SUCCESS
echo [OK] Backend is healthy and responding on http://localhost:8080!
:APP_READY_CONTINUE
echo.

:: 4. Poll Cloudflared Container Logs for Public Tunnel URL
echo [*] Step 4/5: Polling Cloudflare Tunnel container logs for public URL...
set /a TUNNEL_RETRY=0
set /a TUNNEL_MAX_RETRIES=20
set "TUNNEL_URL="

:POLL_TUNNEL_LOOP
set /a TUNNEL_RETRY+=1

for /f "usebackq delims=" %%U in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$log = (%COMPOSE_CMD% logs tunnel 2>&1) -join \"`n\"; if ($log -match 'https://[a-zA-Z0-9\-]+\.trycloudflare\.com') { $matches[0] }"`) do (
    set "TUNNEL_URL=%%U"
)

if not "%TUNNEL_URL%"=="" (
    goto TUNNEL_FOUND
)

if %TUNNEL_RETRY% GEQ %TUNNEL_MAX_RETRIES% (
    goto TUNNEL_TIMEOUT
)

echo     - Attempt %TUNNEL_RETRY%/%TUNNEL_MAX_RETRIES%: Tunnel negotiating with Cloudflare Edge, waiting 2s...
timeout /t 2 /nobreak >nul
goto POLL_TUNNEL_LOOP

:TUNNEL_FOUND
set "TARGET_URL=%TUNNEL_URL%"
echo.
echo ==============================================================================
echo   [SUCCESS] CLOUDFLARE PUBLIC TUNNEL ESTABLISHED!
echo ==============================================================================
echo   Public Web Portal ^& Mobile PWA URL : %TUNNEL_URL%
echo   Local Direct Backend URL           : http://localhost:8080
echo   Mobile PWA Install                 : Open %TUNNEL_URL% on iOS/Android
echo ==============================================================================
goto LAUNCH_RUNNER

:TUNNEL_TIMEOUT
echo.
echo ==============================================================================
echo   [WARNING] CLOUDFLARE TUNNEL RETRIEVAL TIMED OUT (40s)
echo ==============================================================================
echo   Cloudflare tunnel took longer to generate or internet access is limited.
echo   Activating automatic local fallback: http://localhost:8080
echo ==============================================================================
set "TARGET_URL=http://localhost:8080"

:LAUNCH_RUNNER
echo.
:: 5. Launch PC Desktop App Runner
echo [*] Step 5/5: Launching PC Desktop Command Center Runner pointed to %TARGET_URL%...

python --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    if exist "desktop-runner\runner.py" (
        echo [*] Launching via Python Desktop Runner...
        start "DentalCare Command Center" python desktop-runner\runner.py "%TARGET_URL%"
        goto SCRIPT_END
    )
)

:: Fallback if Python is not installed or runner.py not found
echo [*] Launching directly via Microsoft Edge Native App Mode...
start msedge --app="%TARGET_URL%" --window-size=1440,900 --app-id=DentalCareCommandCenter

:SCRIPT_END
echo.
echo [*] Deployment launcher finished. DentalCare stack is running in background.
echo [*] To shut down the stack, run: stop-all.bat (or %COMPOSE_CMD% down)
echo.
pause
