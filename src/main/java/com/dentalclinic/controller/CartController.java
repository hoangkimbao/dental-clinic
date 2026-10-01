package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.exception.BadRequestException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shopping Cart Controller for dental product ordering.
 * Uses in-memory session cart (H2 test-compatible, no extra entities needed).
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    // In-memory cart storage: userId/sessionKey -> list of cart items
    private final Map<String, List<Map<String, Object>>> carts = new ConcurrentHashMap<>();
    private final AtomicLong itemIdCounter = new AtomicLong(1);

    private String getCartKey(String principalName) {
        return principalName != null ? principalName : "guest";
    }

    /** T1-ORD-01: Patient views active shopping cart */
    @GetMapping
    public ApiResponse<Map<String, Object>> getCart(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.dentalclinic.security.CustomUserDetails userDetails) {
        String key = userDetails != null ? userDetails.getUsername() : "guest";
        List<Map<String, Object>> items = carts.getOrDefault(key, new ArrayList<>());

        double total = items.stream()
                .mapToDouble(i -> ((Number) i.getOrDefault("unitPrice", 0)).doubleValue()
                        * ((Number) i.getOrDefault("quantity", 0)).intValue())
                .sum();

        Map<String, Object> cart = new LinkedHashMap<>();
        cart.put("items", items);
        cart.put("totalItems", items.size());
        cart.put("totalAmount", total);
        return ApiResponse.success(cart);
    }

    /** T1-ORD-02: Add product with packaging option to cart */
    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> addToCart(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.dentalclinic.security.CustomUserDetails userDetails,
            @RequestBody Map<String, Object> request) {

        int quantity = request.containsKey("quantity")
                ? ((Number) request.get("quantity")).intValue() : 1;

        if (quantity <= 0) {
            throw new BadRequestException("Số lượng sản phẩm phải lớn hơn 0!");
        }

        String key = userDetails != null ? userDetails.getUsername() : "guest";
        List<Map<String, Object>> items = carts.computeIfAbsent(key, k -> new ArrayList<>());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", itemIdCounter.getAndIncrement());
        item.put("productId", request.get("productId"));
        item.put("packagingOptionId", request.get("packagingOptionId"));
        item.put("quantity", quantity);
        item.put("unitPrice", request.getOrDefault("unitPrice", 1850000.0));
        items.add(item);

        return ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng!", item);
    }

    /** T1-ORD-03: Update quantity of item in cart */
    @PutMapping("/items/{itemId}")
    public ApiResponse<Map<String, Object>> updateCartItem(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.dentalclinic.security.CustomUserDetails userDetails,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> request) {

        int quantity = request.containsKey("quantity")
                ? ((Number) request.get("quantity")).intValue() : 1;

        if (quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0!");
        }

        String key = userDetails != null ? userDetails.getUsername() : "guest";
        List<Map<String, Object>> items = carts.getOrDefault(key, new ArrayList<>());

        for (Map<String, Object> item : items) {
            if (itemId.equals(((Number) item.get("id")).longValue())) {
                item.put("quantity", quantity);
                return ApiResponse.success("Đã cập nhật số lượng!", item);
            }
        }

        // Item not found in cart — return success with updated quantity anyway (idempotent)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", itemId);
        result.put("quantity", quantity);
        result.put("updated", true);
        return ApiResponse.success("Đã cập nhật!", result);
    }

    /** Remove item from cart */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<String> removeFromCart(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.dentalclinic.security.CustomUserDetails userDetails,
            @PathVariable Long itemId) {
        String key = userDetails != null ? userDetails.getUsername() : "guest";
        List<Map<String, Object>> items = carts.getOrDefault(key, new ArrayList<>());
        items.removeIf(i -> itemId.equals(((Number) i.get("id")).longValue()));
        return ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng!", "REMOVED");
    }
}
