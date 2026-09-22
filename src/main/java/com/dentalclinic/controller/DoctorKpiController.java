package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.DoctorKpiResponseDto;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.DoctorKpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff-kpi")
@CrossOrigin(origins = "*")
@Tag(name = "Staff & Doctor KPIs", description = "Đánh giá năng suất, ca khám, chuyển đổi điều trị & doanh số bác sĩ")
public class DoctorKpiController {

    private final DoctorKpiService kpiService;
    private final UserRepository userRepository;

    public DoctorKpiController(DoctorKpiService kpiService, UserRepository userRepository) {
        this.kpiService = kpiService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @GetMapping("/doctor/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Xem chỉ số KPI & tỷ lệ chuyển đổi của bác sĩ theo ID")
    public ResponseEntity<ApiResponse<DoctorKpiResponseDto>> getDoctorKpi(
            @PathVariable Long id,
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(ApiResponse.success(kpiService.getDoctorKpi(id, month)));
    }

    @GetMapping("/my-kpi")
    @Operation(summary = "Bác sĩ xem chỉ số KPI cá nhân trong tháng")
    public ResponseEntity<ApiResponse<DoctorKpiResponseDto>> getMyKpi() {
        return ResponseEntity.ok(ApiResponse.success(kpiService.getMyKpi(getCurrentUser())));
    }

    @GetMapping("/rankings")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Bảng xếp hạng năng suất và KPI bác sĩ toàn phòng khám")
    public ResponseEntity<ApiResponse<List<DoctorKpiResponseDto>>> getClinicDoctorRankings(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(ApiResponse.success(kpiService.getClinicRankings(month)));
    }
}
