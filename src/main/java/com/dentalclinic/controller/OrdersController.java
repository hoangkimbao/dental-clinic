package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.DentalOrder;
import com.dentalclinic.security.CustomUserDetails;
import com.dentalclinic.service.DentalOrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final DentalOrderService orderService;

    public OrdersController(DentalOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/my-orders")
    public ApiResponse<List<DentalOrder>> getMyOrders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String phone = "";
        if (userDetails != null && userDetails.getUser() != null) {
            phone = userDetails.getUser().getPhone();
        }
        return ApiResponse.success(orderService.getOrdersByPhone(phone));
    }
}
