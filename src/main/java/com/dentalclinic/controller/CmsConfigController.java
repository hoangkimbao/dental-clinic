package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.CmsConfigDto;
import com.dentalclinic.service.CmsConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/config")
@CrossOrigin(origins = "*")
@Tag(name = "11. CMS Configuration", description = "Quản lý Cấu hình Menu & Footer Phòng Khám Nha Khoa")
public class CmsConfigController {

    private final CmsConfigService cmsConfigService;

    public CmsConfigController(CmsConfigService cmsConfigService) {
        this.cmsConfigService = cmsConfigService;
    }

    @GetMapping
    @Operation(summary = "Lấy cấu hình động Menu & Footer của phòng khám")
    public ResponseEntity<ApiResponse<CmsConfigDto>> getCmsConfig() {
        return ResponseEntity.ok(ApiResponse.success(cmsConfigService.getCmsConfig()));
    }

    @PutMapping
    @Operation(summary = "Cập nhật cấu hình Menu & Footer của phòng khám")
    public ResponseEntity<ApiResponse<CmsConfigDto>> updateCmsConfig(
            @RequestBody CmsConfigDto newConfig,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ADMIN_DESKTOP";
        CmsConfigDto updated = cmsConfigService.updateCmsConfig(newConfig, username);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình CMS thành công!", updated));
    }

    @PostMapping("/reset")
    @Operation(summary = "Khôi phục cấu hình CMS Menu & Footer về mặc định")
    public ResponseEntity<ApiResponse<CmsConfigDto>> resetCmsConfig() {
        CmsConfigDto reset = cmsConfigService.resetToDefault();
        return ResponseEntity.ok(ApiResponse.success("Đã khôi phục cấu hình mặc định!", reset));
    }
}
