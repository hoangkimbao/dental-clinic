package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.CreateMaterialOrderRequest;
import com.dentalclinic.dto.UpdateOrderStatusRequest;
import com.dentalclinic.model.MaterialOrder;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.MaterialOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-orders")
@CrossOrigin(origins = "*")
@Tag(name = "Material Orders", description = "Quy trình đề xuất, duyệt & xuất kho đơn hàng vật tư cho đại lý")
public class MaterialOrderController {

    private final MaterialOrderService orderService;
    private final UserRepository userRepository;

    public MaterialOrderController(MaterialOrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Tạo đơn đặt hàng vật tư từ đại lý cấp 2 / chi nhánh vệ tinh")
    public ResponseEntity<ApiResponse<MaterialOrder>> createOrder(@RequestBody CreateMaterialOrderRequest req) {
        MaterialOrder order = orderService.createOrder(req, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Tạo đơn đặt vật tư thành công!", order));
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả đơn đặt hàng vật tư")
    public ResponseEntity<ApiResponse<List<MaterialOrder>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết đơn hàng vật tư theo ID")
    public ResponseEntity<ApiResponse<MaterialOrder>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Phê duyệt hoặc từ chối đơn đặt hàng vật tư (Chủ phòng & Admin)")
    public ResponseEntity<ApiResponse<MaterialOrder>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest req) {
        MaterialOrder updated = orderService.updateOrderStatus(id, req, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công!", updated));
    }
}
