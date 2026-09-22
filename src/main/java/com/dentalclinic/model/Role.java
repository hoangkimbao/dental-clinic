package com.dentalclinic.model;

public enum Role {
    ROLE_OWNER,        // Nha sĩ chủ / Chủ phòng khám (Toàn quyền)
    ROLE_ADMIN,        // IT Administrator / Quản trị viên hệ thống IT
    ROLE_RECEPTIONIST, // Lễ tân (Thua chủ 1 bậc: Quản lý lịch hẹn, cọc giữ chỗ, phân ca)
    ROLE_DENTIST,      // Nha sĩ thường (Bệnh án EMR, phác đồ chỉnh nha, lịch khám)
    ROLE_ASSISTANT,    // Phụ tá (Lịch phụ khám, chuẩn bị dụng cụ)
    ROLE_CLEANER,      // Tạp vụ / Vệ sinh (Checklist vô trùng, lịch làm)
    ROLE_PATIENT       // Bệnh nhân / Khách hàng (Đặt lịch, cọc giữ chỗ, xem lộ trình)
}
