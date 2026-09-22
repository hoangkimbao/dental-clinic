package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.OrthodonticPlan;
import com.dentalclinic.repository.OrthodonticPlanRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/orthodontic-plans")
@CrossOrigin(origins = "*")
@Tag(name = "5. Orthodontic Plans", description = "Kế hoạch & Lộ trình chỉnh nha niềng răng")
public class OrthodonticController {

    private final OrthodonticPlanRepository orthodonticPlanRepository;

    public OrthodonticController(OrthodonticPlanRepository orthodonticPlanRepository) {
        this.orthodonticPlanRepository = orthodonticPlanRepository;
    }

    @GetMapping
    @Operation(summary = "Xem lộ trình niềng răng (Lọc theo SĐT bệnh nhân)")
    public ResponseEntity<ApiResponse<List<OrthodonticPlan>>> getOrthoPlans(@RequestParam(required = false) String phone) {
        if (phone != null && !phone.isBlank()) {
            List<OrthodonticPlan> list = orthodonticPlanRepository.findByPatientPhone(phone.trim())
                    .map(List::of).orElseGet(Collections::emptyList);
            return ResponseEntity.ok(ApiResponse.success(list));
        }
        return ResponseEntity.ok(ApiResponse.success(orthodonticPlanRepository.findAllByOrderByNextAdjustmentDateAsc()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'DENTIST')")
    @Operation(summary = "Tạo/Cập nhật phác đồ chỉnh nha (Chủ & Bác sĩ)")
    public ResponseEntity<ApiResponse<OrthodonticPlan>> createOrUpdatePlan(@RequestBody OrthodonticPlan plan) {
        OrthodonticPlan saved = orthodonticPlanRepository.save(plan);
        return ResponseEntity.ok(ApiResponse.success("Lưu phác đồ chỉnh nha thành công!", saved));
    }
}
