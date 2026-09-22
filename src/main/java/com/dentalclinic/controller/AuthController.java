package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.*;
import com.dentalclinic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "1. Authentication", description = "Xác thực JWT, Đăng ký Bệnh nhân & Quản lý Mật khẩu")
public class AuthController {

    private final AuthService authService;

    // Constructor Injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống (Universal Login: Tự nhận diện Role)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản Khách Hàng / Bệnh Nhân (ROLE_PATIENT)")
    public ResponseEntity<ApiResponse<AuthResponse>> registerPatient(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký tài khoản thành công!", response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản cá nhân")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công!", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu khôi phục mật khẩu (Gửi mã OTP)")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String msg = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(msg, msg));
    }
}
