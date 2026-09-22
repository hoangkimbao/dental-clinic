package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.OrderRequestDto;
import com.dentalclinic.model.DentalOrder;
import com.dentalclinic.model.DentalOrderStatus;
import com.dentalclinic.service.DentalOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dental-orders")
public class DentalOrderController {

    private final DentalOrderService orderService;

    public DentalOrderController(DentalOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<DentalOrder> placeOrder(@RequestBody OrderRequestDto request) {
        DentalOrder order = orderService.createOrder(request);
        return ApiResponse.success("Đặt hàng thành công! Mã đơn: " + order.getOrderCode(), order);
    }

    @GetMapping("/{orderCode}")
    public ApiResponse<DentalOrder> getOrder(@PathVariable String orderCode) {
        return ApiResponse.success(orderService.getOrderByCode(orderCode));
    }

    @GetMapping("/my-orders")
    public ApiResponse<List<DentalOrder>> getMyOrders(@RequestParam String phone) {
        return ApiResponse.success(orderService.getOrdersByPhone(phone));
    }

    @PatchMapping("/{orderCode}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'RECEPTIONIST')")
    public ApiResponse<DentalOrder> updateStatus(@PathVariable String orderCode, @RequestParam DentalOrderStatus status) {
        return ApiResponse.success("Cập nhật trạng thái thành công", orderService.updateOrderStatus(orderCode, status));
    }
}
