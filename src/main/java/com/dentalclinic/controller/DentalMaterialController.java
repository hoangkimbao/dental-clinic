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
@RequestMapping({"/api/dental-materials", "/api/materials"})
@CrossOrigin(origins = "*")
@Tag(name = "Dental Materials (Inventory)", description = "Quản lý tồn kho vật tư nha khoa, cảnh báo sắp hết & cận hạn sử dụng")
public class DentalMaterialController {

    private final DentalMaterialService materialService;

    public DentalMaterialController(DentalMaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    @Operation(summary = "Tra cứu tồn kho vật tư nha khoa trung tâm")
    public ResponseEntity<ApiResponse<List<DentalMaterial>>> getMaterials(
            @RequestParam(required = false) MaterialCategory category) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getAllMaterials(category)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết vật tư nha khoa theo ID")
    public ResponseEntity<ApiResponse<DentalMaterial>> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getMaterialById(id)));
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST', 'DENTIST')")
    @Operation(summary = "Điều chỉnh số lượng tồn kho vật tư (Nhập/Xuất)")
    public ResponseEntity<ApiResponse<DentalMaterial>> updateStock(
            @PathVariable Long id,
            @RequestBody UpdateStockRequest req) {
        DentalMaterial updated = materialService.updateStock(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tồn kho thành công!", updated));
    }

    @PostMapping("/{id}/consume")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DENTIST', 'RECEPTIONIST')")
    @Operation(summary = "Khấu trừ vật tư tiêu hao sử dụng trong ca điều trị")
    public ResponseEntity<ApiResponse<DentalMaterial>> consumeMaterialForTreatment(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int quantity) {
        DentalMaterial updated = materialService.deductStockForTreatment(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã trừ tồn kho vật tư điều trị thành công!", updated));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Thêm mới vật tư nha khoa vào kho trung tâm")
    public ResponseEntity<ApiResponse<DentalMaterial>> createMaterial(@RequestBody DentalMaterial material) {
        DentalMaterial saved = materialService.saveMaterial(material);
        return ResponseEntity.ok(ApiResponse.success("Thêm mới vật tư thành công!", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Cập nhật thông tin vật tư nha khoa")
    public ResponseEntity<ApiResponse<DentalMaterial>> updateMaterial(
            @PathVariable Long id,
            @RequestBody DentalMaterial material) {
        DentalMaterial updated = materialService.updateMaterial(id, material);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin vật tư thành công!", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Ngừng kích hoạt / Xóa vật tư nha khoa")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success("Đã ngừng kích hoạt vật tư thành công!", null));
    }

    @GetMapping({"/alerts/low-stock", "/low-stock"})
    @Operation(summary = "Danh sách vật tư nha khoa chạm ngưỡng tồn kho tối thiểu")
    public ResponseEntity<ApiResponse<List<DentalMaterial>>> getLowStockAlerts() {
        return ResponseEntity.ok(ApiResponse.success(materialService.getLowStockMaterials()));
    }

    @GetMapping({"/alerts/expiring", "/expiring"})
    @Operation(summary = "Danh sách vật tư nha khoa sắp hết hạn sử dụng")
    public ResponseEntity<ApiResponse<List<DentalMaterial>>> getExpiringMaterials(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getExpiringMaterials(daysAhead)));
    }
}
