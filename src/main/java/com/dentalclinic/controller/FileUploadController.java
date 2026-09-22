package com.dentalclinic.controller;

import com.dentalclinic.model.DentalImageAttachment;
import com.dentalclinic.model.DentalImageType;
import com.dentalclinic.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emr/images")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "medicalRecordId", required = false) Long medicalRecordId,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "patientName", required = false) String patientName,
            @RequestParam(value = "imageType", required = false) String imageType,
            @RequestParam(value = "notes", required = false) String notes) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn file hình ảnh hoặc phim X-Quang."));
        }

        DentalImageType type = DentalImageType.PANORAMA;
        if (imageType != null && !imageType.trim().isEmpty()) {
            try {
                type = DentalImageType.valueOf(imageType.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        try {
            DentalImageAttachment attachment = fileUploadService.uploadDentalImage(
                    file, medicalRecordId, patientId, patientName, type, notes
            );
            return ResponseEntity.ok(attachment);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Không thể lưu file: " + e.getMessage()));
        }
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<List<DentalImageAttachment>> getByMedicalRecord(@PathVariable Long recordId) {
        return ResponseEntity.ok(fileUploadService.getImagesByMedicalRecord(recordId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DentalImageAttachment>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(fileUploadService.getImagesByPatient(patientId));
    }
}
