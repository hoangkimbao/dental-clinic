package com.dentalclinic.dto;

import com.dentalclinic.model.PaymentStatus;
import java.time.LocalDateTime;

public class PaymentResponseDto {
    private Long paymentId;
    private Long appointmentId;
    private Double amount;
    private String transactionCode;
    private String paymentUrl;
    private PaymentStatus status;
    private String message;
    private LocalDateTime paymentTime;

    public PaymentResponseDto() {}

    public PaymentResponseDto(Long paymentId, Long appointmentId, Double amount,
                              String transactionCode, String paymentUrl,
                              PaymentStatus status, String message) {
        this.paymentId = paymentId;
        this.appointmentId = appointmentId;
        this.amount = amount;
        this.transactionCode = transactionCode;
        this.paymentUrl = paymentUrl;
        this.status = status;
        this.message = message;
        this.paymentTime = LocalDateTime.now();
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
}
