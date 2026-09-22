package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.LoyaltyAccount;
import com.dentalclinic.service.LoyaltyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/account")
    public ApiResponse<LoyaltyAccount> getAccount(@RequestParam String phone) {
        return ApiResponse.success(loyaltyService.getOrCreateAccount(null, phone));
    }

    @PostMapping("/redeem")
    public ApiResponse<LoyaltyAccount> redeemPoints(@RequestParam String phone, @RequestParam int points) {
        return ApiResponse.success("Đổi điểm thành công!", loyaltyService.redeemPoints(phone, points));
    }
}
