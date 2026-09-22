package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.UpdateStockRequest;
import com.dentalclinic.model.DentalMaterial;
import com.dentalclinic.model.MaterialCategory;
import com.dentalclinic.service.DentalMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dental-materials")
@CrossOrigin(origins = "*")
@Tag(name = "Dental Materials", description = "Quản lý tồn kho vật tư, khí cụ nha khoa & cấy ghép")
public class DentalMaterialController {

    private final DentalMaterialService materialService;

    public DentalMaterialController(DentalMaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    @Operation(summary = "Tra cứu tồn kho vật tư trung tâm")
    public ResponseEntity<ApiResponse<List<DentalMaterial>>> getMaterials(
            @RequestParam(required = false) MaterialCategory category) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getAllMaterials(category)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết vật tư theo ID")
    public ResponseEntity<ApiResponse<DentalMaterial>> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getMaterialById(id)));
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Điều chỉnh số lượng tồn kho vật tư")
    public ResponseEntity<ApiResponse<DentalMaterial>> updateStock(
            @PathVariable Long id,
            @RequestBody UpdateStockRequest req) {
        DentalMaterial updated = materialService.updateStock(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tồn kho thành công!", updated));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Thêm mới vật tư nha khoa vào kho trung tâm")
    public ResponseEntity<ApiResponse<DentalMaterial>> createMaterial(@RequestBody DentalMaterial material) {
        DentalMaterial saved = materialService.saveMaterial(material);
        return ResponseEntity.ok(ApiResponse.success("Thêm mới vật tư thành công!", saved));
    }

    @GetMapping("/alerts/low-stock")
    @Operation(summary = "Danh sách vật tư dưới ngưỡng an toàn")
    public ResponseEntity<ApiResponse<List<DentalMaterial>>> getLowStockAlerts() {
        return ResponseEntity.ok(ApiResponse.success(materialService.getLowStockAlerts()));
    }
}
