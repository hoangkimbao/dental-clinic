@echo off
setlocal
title DentalCare Clinic - Local Server and Cloudflare Tunnel
cd /d "%~dp0"

echo ==============================================================================
echo       DENTALCARE CLINIC - DIRECT LOCAL RUNNER WITH CLOUDFLARE
echo ==============================================================================
echo.

:: 1. Cleanup old lock files
if exist "data\dentaldb.lock.db" del /f /q "data\dentaldb.lock.db" >nul 2>&1

:: 2. Find Java 21 / Java 17
set "JAVA_EXE=java"
if exist "C:\Program Files\JetBrains\PyCharm 2025.2.3\jbr\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\JetBrains\PyCharm 2025.2.3\jbr\bin\java.exe"
)

echo [*] Using Java: "%JAVA_EXE%"
"%JAVA_EXE%" -version
echo.

:: 3. Start Spring Boot in a separate window
echo [*] Starting DentalCare Spring Boot Server on port 8080...
start "DentalCare Server" "%JAVA_EXE%" -jar target\dental-clinic-1.0.0.jar --server.port=8080

:: 4. Wait for server readiness
echo [*] Waiting 10 seconds for server to initialize...
timeout /t 10 /nobreak >nul

:: 5. Start Cloudflare Tunnel
echo [*] Starting Cloudflare Tunnel...
echo ==============================================================================
echo [SUCCESS] Connecting to Cloudflare Edge Network...
echo Link public se xuat hien ben duoi (URL trycloudflare.com):
echo ==============================================================================
echo.
cloudflared.exe tunnel --url http://localhost:8080
pause
