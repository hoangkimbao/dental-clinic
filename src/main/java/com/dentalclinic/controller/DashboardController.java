package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.DashboardStatsDto;
import com.dentalclinic.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@Tag(name = "6. Dashboard & Analytics", description = "Thống kê doanh thu & báo cáo phân tích")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy dữ liệu thống kê Dashboard (Chỉ Chủ & Lễ tân)")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getStats() {
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
