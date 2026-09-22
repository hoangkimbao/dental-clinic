package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.AiDiagnosticRequestDto;
import com.dentalclinic.dto.AiDiagnosticResponseDto;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.model.AiDentalDiagnosticLog;
import com.dentalclinic.security.CustomUserDetails;
import com.dentalclinic.service.AiDentalDiagnosticService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/dental-ai", "/api/ai-diagnostic"})
public class AiDentalDiagnosticController {

    private final AiDentalDiagnosticService diagnosticService;

    public AiDentalDiagnosticController(AiDentalDiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @PostMapping(value = "/diagnose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AiDiagnosticResponseDto> diagnoseMultipart(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageAngle", required = false) String imageAngle,
            @RequestParam(value = "symptoms", required = false) String symptoms,
            @RequestParam(value = "patientName", required = false) String patientName,
            @RequestParam(value = "patientPhone", required = false) String patientPhone,
            @RequestParam(value = "forceFallback", required = false) Boolean forceFallback) {

        MultipartFile uploadFile = image != null ? image : file;
        if (uploadFile == null || uploadFile.isEmpty() || uploadFile.getSize() == 0) {
            throw new BadRequestException("Vui lòng tải lên hình ảnh chụp răng hoặc phim X-quang.");
        }

        AiDiagnosticRequestDto request = new AiDiagnosticRequestDto();
        request.setPatientName(patientName != null ? patientName : "Khách thăm khám");
        request.setPatientPhone(patientPhone != null ? patientPhone : "0900000000");
        request.setImageUrl("/uploads/dental-ai/" + uploadFile.getOriginalFilename());
        request.setSymptoms((symptoms != null ? symptoms : "") + (imageAngle != null ? " " + imageAngle : ""));
        request.setForceFallback(forceFallback != null && forceFallback);

        return ApiResponse.success("Chẩn đoán hình ảnh AI hoàn tất", diagnosticService.diagnose(request));
    }

    @PostMapping(value = "/diagnose", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AiDiagnosticResponseDto> diagnoseJson(@RequestBody AiDiagnosticRequestDto request) {
        return ApiResponse.success("Chẩn đoán hình ảnh AI hoàn tất", diagnosticService.diagnose(request));
    }

    @GetMapping("/history")
    public ApiResponse<List<AiDentalDiagnosticLog>> getHistory(
            @RequestParam(required = false) String phone,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String queryPhone = phone;
        if (queryPhone == null || queryPhone.isBlank()) {
            if (userDetails != null && userDetails.getUser() != null) {
                queryPhone = userDetails.getUser().getPhone();
            }
        }
        if (queryPhone == null || queryPhone.isBlank()) {
            queryPhone = "0988776655";
        }
        return ApiResponse.success(diagnosticService.getHistoryByPhone(queryPhone));
    }
}
