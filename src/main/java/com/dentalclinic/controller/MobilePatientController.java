package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mobile Patient App Contract Controller (F37).
 * Provides mobile-specific config, notifications, and patient profile endpoints.
 */
@RestController
public class MobilePatientController {

    /**
     * T1-MOB-01: Mobile patient dashboard config endpoint
     * GET /api/mobile/patient/config
     */
    @GetMapping("/api/mobile/patient/config")
    public ApiResponse<Map<String, Object>> getMobilePatientConfig(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("appVersion", "2.5.0");
        config.put("features", Map.of(
            "aiDiagnosis", true,
            "warrantyQrScan", true,
            "loyaltyProgram", true,
            "onlineBooking", true,
            "homeCareProducts", true
        ));
        config.put("clinicName", "DentalCare Luxury");
        config.put("supportPhone", "1900 599 934");
        config.put("maintenanceMode", false);
        config.put("minAppVersion", "2.0.0");

        if (userDetails != null && userDetails.getUser() != null) {
            config.put("patientName", userDetails.getUser().getFullName());
            config.put("patientPhone", userDetails.getUser().getPhone());
        }

        return ApiResponse.success(config);
    }


    /**
     * T1-MOB-04: Mobile profile view for logged-in patient
     * GET /api/patients/me
     */
    @GetMapping("/api/patients/me")
    public ApiResponse<Map<String, Object>> getPatientProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> profile = new LinkedHashMap<>();
        if (userDetails != null && userDetails.getUser() != null) {
            var user = userDetails.getUser();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("fullName", user.getFullName());
            profile.put("phone", user.getPhone());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole() != null ? user.getRole().name() : "PATIENT");
            profile.put("active", user.isActive());
        } else {
            profile.put("id", 0);
            profile.put("username", "guest");
            profile.put("fullName", "Khách Hàng");
            profile.put("phone", null);
            profile.put("email", null);
        }

        return ApiResponse.success(profile);
    }

    /**
     * T1-MOB-05: Mobile coupon wallet for patient
     * GET /api/coupons/active is already in CouponController — this handles /api/coupons/validate POST
     */
}
