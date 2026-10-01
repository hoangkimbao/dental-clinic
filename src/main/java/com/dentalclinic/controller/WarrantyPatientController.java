package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.WarrantyLookupResponseDto;
import com.dentalclinic.model.PorcelainCrownWarranty;
import com.dentalclinic.security.CustomUserDetails;
import com.dentalclinic.service.LoyaltyService;
import com.dentalclinic.service.PorcelainCrownWarrantyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Warranty controller at /api/warranty (singular) — used by mobile/QR scan flows.
 * Test uses both /api/warranties/** (admin/public lookup) and /api/warranty/** (patient-facing).
 */
@RestController
@RequestMapping("/api/warranty")
public class WarrantyPatientController {

    private final PorcelainCrownWarrantyService warrantyService;
    private final LoyaltyService loyaltyService;

    public WarrantyPatientController(PorcelainCrownWarrantyService warrantyService,
                                     LoyaltyService loyaltyService) {
        this.warrantyService = warrantyService;
        this.loyaltyService = loyaltyService;
    }

    /**
     * T1-WRN-02: Public warranty lookup by QR security token in path
     * GET /api/warranty/verify/{qrToken}
     */
    @GetMapping("/verify/{qrToken}")
    public ApiResponse<WarrantyLookupResponseDto> verifyByPathToken(@PathVariable String qrToken) {
        WarrantyLookupResponseDto dto = warrantyService.lookupByCodeOrQr(qrToken);
        return ApiResponse.success("Xác thực thẻ bảo hành thành công", dto);
    }

    /**
     * T1-WRN-04: Patient queries personal list of porcelain crown warranties
     * GET /api/warranty/my-warranties
     */
    @GetMapping("/my-warranties")
    public ApiResponse<List<PorcelainCrownWarranty>> getMyWarranties(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String phone) {
        String resolvedPhone = phone;
        if (resolvedPhone == null && userDetails != null && userDetails.getUser() != null) {
            resolvedPhone = userDetails.getUser().getPhone();
        }
        if (resolvedPhone == null) resolvedPhone = "";
        return ApiResponse.success(warrantyService.getWarrantiesByPhone(resolvedPhone));
    }

    /**
     * T1-WRN-05: Patient claims loyalty reward points by scanning warranty QR
     * POST /api/warranty/scan-claim
     */
    @PostMapping("/scan-claim")
    public ApiResponse<Map<String, Object>> scanAndClaimPoints(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> request) {

        String qrCode = request.get("qrCode");
        // Verify warranty first
        WarrantyLookupResponseDto dto = warrantyService.lookupByCodeOrQr(qrCode);

        // Award loyalty points for scanning warranty
        String phone = (userDetails != null && userDetails.getUser() != null
                && userDetails.getUser().getPhone() != null)
                ? userDetails.getUser().getPhone()
                : dto.getPatientPhoneMasked();

        // Use patient phone or fallback — add 50 bonus points for QR scan
        if (userDetails != null && userDetails.getUser() != null
                && userDetails.getUser().getPhone() != null) {
            loyaltyService.addPoints(userDetails.getUser().getPhone(), 50,
                    "Quét thẻ bảo hành sứ Lava Plus");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("warrantyCode", dto.getWarrantyCode());
        result.put("pointsEarned", 50);
        result.put("message", "Chúc mừng! Bạn đã tích được 50 điểm từ thẻ bảo hành răng sứ.");
        return ApiResponse.success("Tích điểm thành công!", result);
    }
}
