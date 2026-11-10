package com.dentalclinic.dto;

public class PaymentRequestDto {
    private Long appointmentId;
    private Double amount;
    private String orderInfo;
    private String bankCode;
    private String paymentMethod;
    private String returnUrl;

    public PaymentRequestDto() {}

    public PaymentRequestDto(Long appointmentId, Double amount, String orderInfo, String bankCode) {
        this.appointmentId = appointmentId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.bankCode = bankCode;
    }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getOrderInfo() { return orderInfo; }
    public void setOrderInfo(String orderInfo) { this.orderInfo = orderInfo; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
}
