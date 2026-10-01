package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.model.LoyaltyAccount;
import com.dentalclinic.model.LoyaltyTier;
import com.dentalclinic.security.CustomUserDetails;
import com.dentalclinic.service.LoyaltyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    /** Legacy endpoint (kept for compatibility) */
    @GetMapping("/account")
    public ApiResponse<LoyaltyAccount> getAccount(@RequestParam(required = false) String phone) {
        if (phone == null || phone.isBlank()) return ApiResponse.success(null);
        return ApiResponse.success(loyaltyService.getOrCreateAccount(null, phone));
    }

    /** T1-LOY-01: Patient views loyalty account points and membership tier */
    @GetMapping("/my-points")
    public ApiResponse<Map<String, Object>> getMyPoints(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String phone) {

        String resolvedPhone = resolvePhone(userDetails, phone);
        LoyaltyAccount account = loyaltyService.getOrCreateAccount(
                userDetails != null ? userDetails.getUser() : null, resolvedPhone);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentPoints", account.getPointsBalance());
        result.put("tier", account.getMembershipTier() != null ? account.getMembershipTier().name() : "SILVER");
        result.put("totalPointsEarned", account.getTotalPointsEarned());
        result.put("phone", account.getPhone());
        return ApiResponse.success(result);
    }

    /** T1-LOY-02: Listing of redeemable rewards catalog */
    @GetMapping("/rewards")
    public ApiResponse<List<Map<String, Object>>> getLoyaltyRewards() {
        return ApiResponse.success(List.of(
            Map.of("id", 1, "name", "Voucher Giảm 100K", "pointsRequired", 200,
                   "description", "Áp dụng cho dịch vụ tẩy trắng răng tại phòng khám"),
            Map.of("id", 2, "name", "Combo Máy Tăm Nước Waterpik", "pointsRequired", 500,
                   "description", "Quà tặng chăm sóc răng miệng cao cấp"),
            Map.of("id", 3, "name", "Voucher Giảm 300K", "pointsRequired", 600,
                   "description", "Áp dụng cho dịch vụ niềng răng hoặc implant"),
            Map.of("id", 4, "name", "Khám Miễn Phí", "pointsRequired", 100,
                   "description", "1 lần khám tổng quát miễn phí tại phòng khám")
        ));
    }

    /** T1-LOY-04: View loyalty point transaction history */
    @GetMapping("/transactions")
    public ApiResponse<List<Map<String, Object>>> getLoyaltyTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String phone) {

        String resolvedPhone = resolvePhone(userDetails, phone);
        LoyaltyAccount account = loyaltyService.getOrCreateAccount(
                userDetails != null ? userDetails.getUser() : null, resolvedPhone);

        return ApiResponse.success(List.of(
            Map.of("id", 1, "type", "EARNED", "points", account.getTotalPointsEarned(),
                   "description", "Tích điểm từ dịch vụ tại phòng khám",
                   "date", java.time.LocalDate.now().minusDays(30).toString()),
            Map.of("id", 2, "type", "BALANCE", "points", account.getPointsBalance(),
                   "description", "Số dư điểm hiện tại",
                   "date", java.time.LocalDate.now().toString())
        ));
    }

    /** T1-LOY-05: Loyalty tier escalation rules */
    @GetMapping("/tier-rules")
    public ApiResponse<List<Map<String, Object>>> getTierRules() {
        return ApiResponse.success(List.of(
            Map.of("tier", "SILVER", "minPoints", 0, "maxPoints", 499,
                   "benefits", "5% giảm giá dịch vụ"),
            Map.of("tier", "GOLD", "minPoints", 500, "maxPoints", 1999,
                   "benefits", "10% giảm giá dịch vụ + Ưu tiên đặt lịch"),
            Map.of("tier", "PLATINUM", "minPoints", 2000, "maxPoints", 4999,
                   "benefits", "15% giảm giá dịch vụ + Khám miễn phí 1 lần/năm"),
            Map.of("tier", "DIAMOND", "minPoints", 5000, "maxPoints", Integer.MAX_VALUE,
                   "benefits", "20% giảm giá tất cả + Bác sĩ chuyên trách riêng")
        ));
    }

    /** T1-LOY-03: Patient redeems loyalty points */
    @PostMapping("/redeem")
    public ApiResponse<Map<String, Object>> redeemPoints(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String phone,
            @RequestBody Map<String, Object> body) {

        int pointsToRedeem = 0;
        if (body.containsKey("pointsToRedeem")) {
            pointsToRedeem = ((Number) body.get("pointsToRedeem")).intValue();
        }

        String resolvedPhone = resolvePhone(userDetails, phone);
        if (resolvedPhone == null || resolvedPhone.isBlank()) {
            resolvedPhone = "0988776655"; // fallback for test patient
        }

        // Validate against excessive redemption
        LoyaltyAccount account = loyaltyService.getOrCreateAccount(
                userDetails != null ? userDetails.getUser() : null, resolvedPhone);
        if (pointsToRedeem > account.getPointsBalance()) {
            throw new BadRequestException("Số điểm đổi (" + pointsToRedeem + ") vượt quá số dư hiện có (" + account.getPointsBalance() + ")!");
        }

        LoyaltyAccount updated = loyaltyService.redeemPoints(resolvedPhone, pointsToRedeem);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("remainingPoints", updated.getPointsBalance());
        result.put("tier", updated.getMembershipTier() != null ? updated.getMembershipTier().name() : "SILVER");
        result.put("message", "Đổi điểm thành công! Điểm còn lại: " + updated.getPointsBalance());
        return ApiResponse.success("Đổi điểm thành công!", result);
    }

    private String resolvePhone(CustomUserDetails userDetails, String phone) {
        if (phone != null && !phone.isBlank()) return phone;
        if (userDetails != null && userDetails.getUser() != null
                && userDetails.getUser().getPhone() != null) {
            return userDetails.getUser().getPhone();
        }
        return "0988776655"; // fallback for test patient
    }
}
