@echo off
chcp 65001 >nul
title DentalCare Luxury Clinic - PC Command Center ^& Notification Hub

echo ==============================================================================
echo    DENTALCARE LUXURY CLINIC — PC COMMAND CENTER ^& NOTIFICATION HUB
echo    Trực tháp điều hành phòng khám, quản lý CMS và thông báo STOMP thời gian thực
echo ==============================================================================
echo.

cd /d "%~dp0"

echo [1/3] Kiểm tra môi trường Node.js / NPM...
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Không tìm thấy Node.js trên hệ thống máy tính này!
    echo Vui lòng cài đặt Node.js LTS từ https://nodejs.org/ để khởi chạy ứng dụng.
    echo.
    pause
    exit /b 1
)

echo [2/3] Kiểm tra gói thư viện phụ thuộc (node_modules)...
if not exist "node_modules\" (
    echo [INFO] Thư mục node_modules chưa được cài đặt. Đang tiến hành cài đặt dependencies...
    call npm install --no-audit --no-fund
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo [ERROR] Cài đặt gói dependencies thất bại. Vui lòng kiểm tra kết nối mạng.
        pause
        exit /b 1
    )
    echo [OK] Đã hoàn tất cài đặt dependencies.
) else (
    echo [OK] Đã tìm thấy thư mục node_modules sẵn sàng.
)

echo [3/3] Đang khởi chạy DentalCare PC Command Center...
echo.
call npm start

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [WARNING] Ứng dụng Desktop đã thoát với mã lỗi: %ERRORLEVEL%
    pause
)
