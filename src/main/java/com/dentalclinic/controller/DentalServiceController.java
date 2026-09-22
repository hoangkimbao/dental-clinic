package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.DentalServiceCatalog;
import com.dentalclinic.model.DentalServiceCategory;
import com.dentalclinic.service.DentalServiceCatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dental-services")
public class DentalServiceController {

    private final DentalServiceCatalogService serviceCatalogService;

    public DentalServiceController(DentalServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ApiResponse<List<DentalServiceCatalog>> getServices(
            @RequestParam(required = false) DentalServiceCategory category,
            @RequestParam(required = false, defaultValue = "false") boolean featured) {
        if (featured) {
            return ApiResponse.success(serviceCatalogService.getFeaturedServices());
        }
        if (category != null) {
            return ApiResponse.success(serviceCatalogService.getServicesByCategory(category));
        }
        return ApiResponse.success(serviceCatalogService.getAllServices());
    }

    @GetMapping("/{code}")
    public ApiResponse<DentalServiceCatalog> getServiceByCode(@PathVariable String code) {
        return ApiResponse.success(serviceCatalogService.getServiceByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ApiResponse<DentalServiceCatalog> createService(@RequestBody DentalServiceCatalog service) {
        return ApiResponse.success("Tạo dịch vụ thành công", serviceCatalogService.saveService(service));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ApiResponse<String> deleteService(@PathVariable Long id) {
        serviceCatalogService.deleteService(id);
        return ApiResponse.success("Đã xóa dịch vụ thành công", "DELETED");
    }
}
