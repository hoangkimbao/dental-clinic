package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.MedicalRecord;
import com.dentalclinic.repository.MedicalRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@CrossOrigin(origins = "*")
@Tag(name = "4. Medical Records (EMR)", description = "Hồ sơ bệnh án điện tử")
public class MedicalRecordController {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordController(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @GetMapping
    @Operation(summary = "Tra cứu bệnh án EMR (Lọc theo SĐT hoặc Toàn bộ)")
    public ResponseEntity<ApiResponse<List<MedicalRecord>>> getMedicalRecords(@RequestParam(required = false) String phone) {
        if (phone != null && !phone.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(phone.trim())));
        }
        return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findAllByOrderByRecordDateDesc()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'DENTIST')")
    @Operation(summary = "Thêm/Cập nhật bệnh án (Chủ & Bác sĩ)")
    public ResponseEntity<ApiResponse<MedicalRecord>> createRecord(@RequestBody MedicalRecord record) {
        MedicalRecord saved = medicalRecordRepository.save(record);
        return ResponseEntity.ok(ApiResponse.success("Lưu bệnh án thành công!", saved));
    }
}
