package com.dentalclinic.model;

public enum MaterialOrderStatus {
    DRAFT,             // Bản nháp
    PENDING_APPROVAL,  // Chờ ban quản trị phê duyệt
    APPROVED,          // Đã duyệt & xuất kho
    DISPATCHED,        // Đang vận chuyển
    RECEIVED,          // Đại lý đã nhận hàng
    REJECTED           // Từ chối phê duyệt
}
