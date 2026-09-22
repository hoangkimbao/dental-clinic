package com.dentalclinic.model;

public enum AppointmentStatus {
    PENDING,        // Chờ cọc
    DEPOSIT_PAID,   // Đã cọc 100K
    CONFIRMED,      // Đã xác nhận / Check-in
    COMPLETED,      // Khám hoàn tất
    CANCELLED       // Đã hủy
}
