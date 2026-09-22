package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.DentalProduct;
import com.dentalclinic.model.DentalProductCategory;
import com.dentalclinic.service.DentalProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dental-products")
public class DentalProductController {

    private final DentalProductService productService;

    public DentalProductController(DentalProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<DentalProduct>> getProducts(@RequestParam(required = false) DentalProductCategory category) {
        if (category != null) {
            return ApiResponse.success(productService.getProductsByCategory(category));
        }
        return ApiResponse.success(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ApiResponse<DentalProduct> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ApiResponse<DentalProduct> createProduct(@RequestBody DentalProduct product) {
        return ApiResponse.success("Tạo sản phẩm thành công", productService.saveProduct(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ApiResponse<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success("Đã xóa sản phẩm thành công", "DELETED");
    }
}
