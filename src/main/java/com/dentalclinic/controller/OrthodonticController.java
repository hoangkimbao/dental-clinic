package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.OrthodonticPlan;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.OrthodonticPlanRepository;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/orthodontic-plans")
@CrossOrigin(origins = "*")
@Tag(name = "5. Orthodontic Plans", description = "Kế hoạch & Lộ trình chỉnh nha niềng răng")
public class OrthodonticController {

    private final OrthodonticPlanRepository orthodonticPlanRepository;
    private final UserRepository userRepository;

    public OrthodonticController(OrthodonticPlanRepository orthodonticPlanRepository, UserRepository userRepository) {
        this.orthodonticPlanRepository = orthodonticPlanRepository;
        this.userRepository = userRepository;
    }

    private User resolveCurrentUser(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            return userDetails.getUser();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            if (auth.getPrincipal() instanceof CustomUserDetails cud) {
                return cud.getUser();
            }
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "Xem lộ trình niềng răng (Bảo vệ IDOR)")
    public ResponseEntity<ApiResponse<List<OrthodonticPlan>>> getOrthoPlans(
            @RequestParam(required = false) String phone,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = resolveCurrentUser(userDetails);
        if (currentUser == null) {
            throw new AccessDeniedException("Yêu cầu xác thực để tra cứu lộ trình chỉnh nha!");
        }

        Role role = currentUser.getRole();
        if (role == Role.ROLE_PATIENT) {
            String patientPhone = currentUser.getPhone();
            if (phone != null && !phone.isBlank() && !phone.trim().equals(patientPhone)) {
                throw new AccessDeniedException("Không có quyền tra cứu lộ trình chỉnh nha của bệnh nhân khác!");
            }
            if (patientPhone != null && !patientPhone.isBlank()) {
                List<OrthodonticPlan> list = orthodonticPlanRepository.findByPatientPhone(patientPhone.trim())
                        .map(List::of).orElseGet(Collections::emptyList);
                return ResponseEntity.ok(ApiResponse.success(list));
            }
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        if (role == Role.ROLE_OWNER || role == Role.ROLE_DENTIST || role == Role.ROLE_ADMIN || role == Role.ROLE_RECEPTIONIST) {
            if (phone != null && !phone.isBlank()) {
                List<OrthodonticPlan> list = orthodonticPlanRepository.findByPatientPhone(phone.trim())
                        .map(List::of).orElseGet(Collections::emptyList);
                return ResponseEntity.ok(ApiResponse.success(list));
            }
            return ResponseEntity.ok(ApiResponse.success(orthodonticPlanRepository.findAllByOrderByNextAdjustmentDateAsc()));
        }

        throw new AccessDeniedException("Không có quyền truy cập lộ trình chỉnh nha!");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'DENTIST')")
    @Operation(summary = "Tạo/Cập nhật phác đồ chỉnh nha (Chủ & Bác sĩ)")
    public ResponseEntity<ApiResponse<OrthodonticPlan>> createOrUpdatePlan(@RequestBody OrthodonticPlan plan) {
        OrthodonticPlan saved = orthodonticPlanRepository.save(plan);
        return ResponseEntity.ok(ApiResponse.success("Lưu phác đồ chỉnh nha thành công!", saved));
    }
}
