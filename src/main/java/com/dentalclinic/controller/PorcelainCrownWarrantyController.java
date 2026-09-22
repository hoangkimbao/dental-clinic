package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.WarrantyLookupResponseDto;
import com.dentalclinic.model.PorcelainCrownWarranty;
import com.dentalclinic.service.PorcelainCrownWarrantyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warranties")
public class PorcelainCrownWarrantyController {

    private final PorcelainCrownWarrantyService warrantyService;

    public PorcelainCrownWarrantyController(PorcelainCrownWarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @GetMapping("/verify")
    public ApiResponse<WarrantyLookupResponseDto> verifyWarranty(@RequestParam String code) {
        return ApiResponse.success("Xác thực thẻ bảo hành thành công", warrantyService.lookupByCodeOrQr(code));
    }

    @GetMapping("/patient")
    public ApiResponse<List<PorcelainCrownWarranty>> getPatientWarranties(@RequestParam String phone) {
        return ApiResponse.success(warrantyService.getWarrantiesByPhone(phone));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'DENTIST')")
    public ApiResponse<PorcelainCrownWarranty> registerWarranty(@RequestBody PorcelainCrownWarranty warranty) {
        return ApiResponse.success("Đăng ký thẻ bảo hành thành công", warrantyService.registerWarranty(warranty));
    }
}
