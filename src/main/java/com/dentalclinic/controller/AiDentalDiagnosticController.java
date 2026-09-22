package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.AiDiagnosticRequestDto;
import com.dentalclinic.dto.AiDiagnosticResponseDto;
import com.dentalclinic.model.AiDentalDiagnosticLog;
import com.dentalclinic.service.AiDentalDiagnosticService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dental-ai")
public class AiDentalDiagnosticController {

    private final AiDentalDiagnosticService diagnosticService;

    public AiDentalDiagnosticController(AiDentalDiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @PostMapping("/diagnose")
    public ApiResponse<AiDiagnosticResponseDto> diagnose(@RequestBody AiDiagnosticRequestDto request) {
        return ApiResponse.success("Chẩn đoán hình ảnh AI hoàn tất", diagnosticService.diagnose(request));
    }

    @GetMapping("/history")
    public ApiResponse<List<AiDentalDiagnosticLog>> getHistory(@RequestParam String phone) {
        return ApiResponse.success(diagnosticService.getHistoryByPhone(phone));
    }
}
