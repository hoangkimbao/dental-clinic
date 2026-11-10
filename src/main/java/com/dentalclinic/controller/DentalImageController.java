package com.dentalclinic.controller;

import com.dentalclinic.dto.DentalImageAiAnalysisDto;
import com.dentalclinic.model.DentalImageAttachment;
import com.dentalclinic.service.DentalImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dental-images")
@CrossOrigin(origins = "*")
@Tag(name = "Dental X-Ray AI Analysis", description = "API quản lý ảnh X-quang nha khoa và Trợ lý AI phân tích bất thường lâm sàng")
public class DentalImageController {

    private final DentalImageService dentalImageService;

    public DentalImageController(DentalImageService dentalImageService) {
        this.dentalImageService = dentalImageService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách toàn bộ ảnh X-quang và hình ảnh lâm sàng của bệnh nhân")
    public ResponseEntity<List<DentalImageAttachment>> getAllImages() {
        return ResponseEntity.ok(dentalImageService.getAllImages());
    }

    @PostMapping
    @Operation(summary = "Tải lên / lưu thông tin ảnh X-quang nha khoa mới")
    public ResponseEntity<DentalImageAttachment> createImage(@RequestBody DentalImageAttachment image) {
        return ResponseEntity.ok(dentalImageService.saveImage(image));
    }

    @PostMapping("/{id}/ai-analyze")
    @Operation(summary = "Kích hoạt AI phân tích ảnh X-quang nha khoa theo ID (POST)")
    public ResponseEntity<?> analyzeXRayPost(@PathVariable Long id) {
        try {
            DentalImageAiAnalysisDto result = dentalImageService.analyzeXRayImage(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}/ai-analyze")
    @Operation(summary = "Kích hoạt AI phân tích ảnh X-quang nha khoa theo ID (GET)")
    public ResponseEntity<?> analyzeXRayGet(@PathVariable Long id) {
        try {
            DentalImageAiAnalysisDto result = dentalImageService.analyzeXRayImage(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
