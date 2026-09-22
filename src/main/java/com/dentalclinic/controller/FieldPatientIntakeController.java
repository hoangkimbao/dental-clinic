package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.FieldIntakeRequest;
import com.dentalclinic.dto.UpdateLeadStatusRequest;
import com.dentalclinic.model.FieldPatientIntake;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.FieldPatientIntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/field-intake")
@CrossOrigin(origins = "*")
@Tag(name = "Field Patient Intake", description = "Tiếp nhận bệnh nhân tại hiện trường (khám học đường, hội nghị)")
public class FieldPatientIntakeController {

    private final FieldPatientIntakeService intakeService;
    private final UserRepository userRepository;

    public FieldPatientIntakeController(FieldPatientIntakeService intakeService, UserRepository userRepository) {
        this.intakeService = intakeService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @PostMapping
    @Operation(summary = "Tiếp nhận hồ sơ khám sơ bộ tại sự kiện nha khoa học đường / hiện trường")
    public ResponseEntity<ApiResponse<FieldPatientIntake>> captureLead(@RequestBody FieldIntakeRequest req) {
        FieldPatientIntake lead = intakeService.captureLead(req, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Tiếp nhận hồ sơ hiện trường thành công!", lead));
    }

    @GetMapping
    @Operation(summary = "Tra cứu danh sách hồ sơ tiếp nhận hiện trường")
    public ResponseEntity<ApiResponse<List<FieldPatientIntake>>> getLeads(
            @RequestParam(required = false) String eventName) {
        return ResponseEntity.ok(ApiResponse.success(intakeService.getAllLeads(eventName)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết hồ sơ tiếp nhận theo ID")
    public ResponseEntity<ApiResponse<FieldPatientIntake>> getLeadById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(intakeService.getLeadById(id)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Cập nhật trạng thái liên hệ / chuyển đổi thành lịch hẹn khám")
    public ResponseEntity<ApiResponse<FieldPatientIntake>> updateLeadStatus(
            @PathVariable Long id,
            @RequestBody UpdateLeadStatusRequest req) {
        FieldPatientIntake updated = intakeService.updateLeadStatus(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!", updated));
    }

    @PostMapping("/batch-sync")
    @Operation(summary = "Đồng bộ hàng loạt hồ sơ ngoại tuyến từ máy tính bảng hiện trường")
    public ResponseEntity<ApiResponse<List<FieldPatientIntake>>> batchSyncLeads(
            @RequestBody List<FieldIntakeRequest> requests) {
        List<FieldPatientIntake> synced = intakeService.batchSyncLeads(requests, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ dữ liệu hiện trường thành công!", synced));
    }
}
