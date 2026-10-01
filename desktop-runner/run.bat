@echo off
setlocal
cd /d "%~dp0"

set "TARGET_URL=%~1"
if "%TARGET_URL%"=="" (
    if defined DENTAL_APP_URL (
        set "TARGET_URL=%DENTAL_APP_URL%"
    ) else (
        set "TARGET_URL=http://localhost:8080"
    )
)

echo ==================================================================
echo   DentalCare Luxury Clinic — PC Command Center Desktop Launcher
echo ==================================================================
echo [*] Target URL: %TARGET_URL%

python --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    python runner.py "%TARGET_URL%"
    if %ERRORLEVEL% EQU 0 goto :EOF
)

echo [WARN] Python runner not available or exited. Launching Microsoft Edge App Mode fallback...
start msedge --app="%TARGET_URL%" --window-size=1440,900 --app-id=DentalCareCommandCenter
