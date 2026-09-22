package com.dentalclinic.service;

import com.dentalclinic.dto.OrderItemRequestDto;
import com.dentalclinic.dto.OrderRequestDto;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class DentalOrderService {

    private final DentalOrderRepository orderRepository;
    private final DentalOrderItemRepository orderItemRepository;
    private final DentalProductRepository productRepository;
    private final ProductPackagingOptionRepository packagingOptionRepository;
    private final LoyaltyService loyaltyService;

    public DentalOrderService(DentalOrderRepository orderRepository,
                              DentalOrderItemRepository orderItemRepository,
                              DentalProductRepository productRepository,
                              ProductPackagingOptionRepository packagingOptionRepository,
                              LoyaltyService loyaltyService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.packagingOptionRepository = packagingOptionRepository;
        this.loyaltyService = loyaltyService;
    }

    public DentalOrder createOrder(OrderRequestDto request) {
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new BadRequestException("Tên khách hàng không được để trống!");
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().trim().isEmpty()) {
            throw new BadRequestException("Số điện thoại khách hàng không được để trống!");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new BadRequestException("Địa chỉ giao hàng không được để trống!");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng không có sản phẩm nào!");
        }

        String orderCode = "ORD-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));

        DentalOrder order = new DentalOrder(
                orderCode,
                request.getCustomerName().trim(),
                request.getCustomerPhone().trim(),
                request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null,
                request.getShippingAddress().trim(),
                0.0,
                request.getPackagingType() != null ? request.getPackagingType() : "BOX",
                request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD",
                DentalOrderStatus.PENDING,
                request.getNotes()
        );

        double totalAmount = 0.0;
        List<DentalOrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDto itemDto : request.getItems()) {
            if (itemDto.getProductId() == null) {
                throw new BadRequestException("ID sản phẩm không hợp lệ!");
            }
            int qty = (itemDto.getQuantity() != null && itemDto.getQuantity() > 0) ? itemDto.getQuantity() : 1;

            DentalProduct product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + itemDto.getProductId()));

            if (product.getStockQuantity() < qty) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' không đủ số lượng tồn kho (còn: " + product.getStockQuantity() + ")!");
            }

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - qty);
            productRepository.save(product);

            ProductPackagingOption packagingOption = null;
            double unitPrice = product.getBasePrice();

            if (itemDto.getPackagingOptionId() != null) {
                packagingOption = packagingOptionRepository.findById(itemDto.getPackagingOptionId())
                        .orElse(null);
                if (packagingOption != null) {
                    unitPrice = packagingOption.getPrice();
                }
            }

            double subtotal = unitPrice * qty;
            totalAmount += subtotal;

            DentalOrderItem item = new DentalOrderItem(order, product, packagingOption, qty, unitPrice, subtotal);
            orderItems.add(item);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        DentalOrder savedOrder = orderRepository.save(order);

        // Award loyalty points (1 point per 10,000 VND)
        int earnedPoints = (int) (totalAmount / 10000);
        if (earnedPoints > 0) {
            loyaltyService.addPoints(request.getCustomerPhone().trim(), earnedPoints, "Tích điểm đơn hàng " + orderCode);
        }

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public DentalOrder getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại với mã: " + orderCode));
    }

    @Transactional(readOnly = true)
    public List<DentalOrder> getOrdersByPhone(String phone) {
        return orderRepository.findByCustomerPhoneOrderByCreatedAtDesc(phone);
    }

    public DentalOrder updateOrderStatus(String orderCode, DentalOrderStatus newStatus) {
        DentalOrder order = getOrderByCode(orderCode);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}
