package com.dentalclinic.service;

import com.dentalclinic.config.VnPayConfig;
import com.dentalclinic.dto.PaymentRequestDto;
import com.dentalclinic.dto.PaymentResponseDto;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentStatus;
import com.dentalclinic.model.Payment;
import com.dentalclinic.model.PaymentMethod;
import com.dentalclinic.model.PaymentStatus;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final VnPayConfig vnPayConfig;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                          AppointmentRepository appointmentRepository,
                          VnPayConfig vnPayConfig,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
        this.vnPayConfig = vnPayConfig;
        this.notificationService = notificationService;
    }

    /**
     * Kịch bản 2: Tạo URL thanh toán VNPay với chữ ký HMAC-SHA512
     */
    @Transactional
    public PaymentResponseDto createVnPayPayment(PaymentRequestDto request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn ID: " + request.getAppointmentId()));

        double rawAmount = request.getAmount() != null && request.getAmount() > 0
                ? request.getAmount()
                : (appointment.getDepositAmount() != null && appointment.getDepositAmount() > 0
                ? appointment.getDepositAmount() : 200000.0);

        String txnRef = "VNP-" + appointment.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String orderInfo = request.getOrderInfo() != null && !request.getOrderInfo().isBlank()
                ? request.getOrderInfo()
                : "Thanh toan lich hen nha khoa #" + appointment.getId();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnpParams.put("vnp_Amount", String.valueOf((long) (rawAmount * 100)));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "health");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", request.getReturnUrl() != null && !request.getReturnUrl().isBlank()
                ? request.getReturnUrl() : vnPayConfig.getVnp_ReturnUrl());
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnpSecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        String paymentUrl = vnPayConfig.getVnp_Url() + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;

        Payment payment = paymentRepository.findByAppointment(appointment).orElseGet(Payment::new);
        payment.setAppointment(appointment);
        payment.setAmount(rawAmount);
        payment.setPaymentMethod(PaymentMethod.QR_VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionCode(txnRef);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(payment.getId());
        response.setAppointmentId(appointment.getId());
        response.setTransactionCode(txnRef);
        response.setPaymentUrl(paymentUrl);
        response.setStatus(PaymentStatus.PENDING);
        response.setAmount(rawAmount);
        response.setPaymentTime(payment.getPaymentTime());
        response.setMessage("Khởi tạo giao dịch VNPay thành công");
        return response;
    }

    /**
     * Kịch bản 2: Xử lý Webhook / Callback từ VNPay để cập nhật trạng thái PAID
     */
    @Transactional
    public PaymentResponseDto handleVnPayCallback(Map<String, String> params) {
        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");

        Optional<Payment> paymentOpt = paymentRepository.findByTransactionCode(txnRef);
        Payment payment = paymentOpt.orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy giao dịch với mã: " + txnRef));

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(payment.getId());
        response.setTransactionCode(txnRef);
        response.setAmount(payment.getAmount() != null ? payment.getAmount() : 0.0);

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);

            Appointment appointment = payment.getAppointment();
            if (appointment != null) {
                appointment.setStatus(AppointmentStatus.DEPOSIT_PAID);
                appointmentRepository.save(appointment);
                response.setAppointmentId(appointment.getId());
                if (notificationService != null) {
                    notificationService.sendNotification(
                            "ROLE_RECEPTIONIST",
                            "Thanh toán VNPay thành công",
                            "Khách hàng " + appointment.getPatientName() + " đã thanh toán mã GD: " + txnRef,
                            "PAYMENT"
                    );
                }
            }
            response.setStatus(PaymentStatus.PAID);
            response.setPaymentTime(payment.getPaymentTime());
            response.setMessage("Thanh toán VNPay thành công (PAID)");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            if (payment.getAppointment() != null) {
                response.setAppointmentId(payment.getAppointment().getId());
            }
            response.setStatus(PaymentStatus.FAILED);
            response.setPaymentTime(LocalDateTime.now());
            response.setMessage("Giao dịch VNPay thất bại với mã lỗi: " + responseCode);
        }

        return response;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
