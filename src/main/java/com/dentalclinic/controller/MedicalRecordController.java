package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.MedicalRecord;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.MedicalRecordRepository;
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
@RequestMapping("/api/medical-records")
@CrossOrigin(origins = "*")
@Tag(name = "4. Medical Records (EMR)", description = "Hồ sơ bệnh án điện tử")
public class MedicalRecordController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;

    public MedicalRecordController(MedicalRecordRepository medicalRecordRepository, UserRepository userRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
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
    @Operation(summary = "Tra cứu bệnh án EMR (Lọc theo SĐT hoặc Toàn bộ)")
    public ResponseEntity<ApiResponse<List<MedicalRecord>>> getMedicalRecords(
            @RequestParam(required = false) String phone,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = resolveCurrentUser(userDetails);
        if (currentUser == null) {
            throw new AccessDeniedException("Yêu cầu xác thực để truy cập hồ sơ bệnh án!");
        }

        Role role = currentUser.getRole();
        if (role == Role.ROLE_PATIENT) {
            String patientPhone = currentUser.getPhone();
            if (phone != null && !phone.isBlank() && !phone.trim().equals(patientPhone)) {
                throw new AccessDeniedException("Bệnh nhân không có quyền tra cứu hồ sơ của người khác!");
            }
            if (patientPhone != null && !patientPhone.isBlank()) {
                return ResponseEntity.ok(ApiResponse.success(
                        medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(patientPhone.trim())));
            }
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        if (role == Role.ROLE_OWNER || role == Role.ROLE_DENTIST || role == Role.ROLE_ADMIN) {
            if (phone != null && !phone.isBlank()) {
                return ResponseEntity.ok(ApiResponse.success(
                        medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(phone.trim())));
            }
            return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findAllByOrderByRecordDateDesc()));
        }

        throw new AccessDeniedException("Không có quyền truy cập hồ sơ bệnh án điện tử!");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'DENTIST', 'ADMIN', 'PATIENT')")
    @Operation(summary = "Xem chi tiết bệnh án theo ID")
    public ResponseEntity<ApiResponse<MedicalRecord>> getRecordById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh án #" + id));

        User currentUser = resolveCurrentUser(userDetails);
        if (currentUser != null && currentUser.getRole() == Role.ROLE_PATIENT) {
            String patientPhone = currentUser.getPhone();
            boolean isOwner = (record.getPatient() != null && record.getPatient().getId().equals(currentUser.getId()))
                    || (record.getPatientPhone() != null && record.getPatientPhone().equals(patientPhone));
            if (!isOwner) {
                throw new AccessDeniedException("Bệnh nhân không có quyền xem hồ sơ bệnh án này!");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'DENTIST')")
    @Operation(summary = "Thêm/Cập nhật bệnh án (Chủ & Bác sĩ)")
    public ResponseEntity<ApiResponse<MedicalRecord>> createRecord(@RequestBody MedicalRecord record) {
        MedicalRecord saved = medicalRecordRepository.save(record);
        return ResponseEntity.ok(ApiResponse.success("Lưu bệnh án thành công!", saved));
    }
}
