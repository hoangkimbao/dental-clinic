package com.dentalclinic.controller;

import com.dentalclinic.dto.PaymentRequestDto;
import com.dentalclinic.dto.PaymentResponseDto;
import com.dentalclinic.model.Payment;
import com.dentalclinic.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payments & VNPay Gateway", description = "API tích hợp cổng thanh toán trực tuyến VNPay / MoMo và xử lý IPN/Callback")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách toàn bộ giao dịch thanh toán")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/create-vnpay")
    @Operation(summary = "Khởi tạo giao dịch thanh toán VNPay và trả về URL thanh toán có chữ ký HMAC-SHA512")
    public ResponseEntity<?> createVnPayPayment(@RequestBody PaymentRequestDto request) {
        try {
            PaymentResponseDto response = paymentService.createVnPayPayment(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/vnpay-callback")
    @Operation(summary = "Xử lý Return URL / Webhook Callback từ cổng thanh toán VNPay (GET)")
    public ResponseEntity<?> handleVnPayCallbackGet(@RequestParam Map<String, String> params) {
        try {
            return ResponseEntity.ok(paymentService.handleVnPayCallback(params));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/vnpay-callback")
    @Operation(summary = "Xử lý Webhook IPN Callback từ cổng thanh toán VNPay (POST)")
    public ResponseEntity<?> handleVnPayCallbackPost(@RequestBody Map<String, String> params) {
        try {
            return ResponseEntity.ok(paymentService.handleVnPayCallback(params));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
