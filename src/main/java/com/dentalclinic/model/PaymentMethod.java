package com.dentalclinic.model;

public enum PaymentMethod {
    QR_VNPAY,
    MOMO,
    BANK_TRANSFER,
    CASH;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static PaymentMethod fromValue(String value) {
        if (value == null) return null;
        if ("VNPAY_QR".equalsIgnoreCase(value) || "QR_VNPAY".equalsIgnoreCase(value)) {
            return QR_VNPAY;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CASH;
        }
    }
}
