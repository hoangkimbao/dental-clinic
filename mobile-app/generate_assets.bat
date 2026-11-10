@echo off
echo ========================================================
echo Generating True Binary PNG Assets for DentalCare Mobile
echo ========================================================

where node >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo Using Node.js generator (generate_png_assets.js)...
    node "%~dp0generate_png_assets.js"
    if %ERRORLEVEL% equ 0 goto success
)

echo Falling back to PowerShell GDI+ generator (generate_assets.ps1)...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0generate_assets.ps1"
if %ERRORLEVEL% equ 0 goto success

echo [ERROR] Failed to generate PNG assets. Ensure Node.js or PowerShell is installed.
exit /b 1

:success
echo.
echo [SUCCESS] Authentic binary PNG assets generated in mobile-app\assets\
