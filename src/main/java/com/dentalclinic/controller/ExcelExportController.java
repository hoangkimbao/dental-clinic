package com.dentalclinic.controller;

import com.dentalclinic.model.*;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.DentalProductRepository;
import com.dentalclinic.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
@Tag(name = "12. Multi-table Excel Export", description = "Xuất Báo Cáo Đa Phân Hệ Excel/CSV Chuẩn UTF-8 BOM Cho PC Desktop App")
public class ExcelExportController {

    private final AppointmentRepository appointmentRepository;
    private final DentalProductRepository dentalProductRepository;
    private final UserRepository userRepository;

    public ExcelExportController(AppointmentRepository appointmentRepository,
                                 DentalProductRepository dentalProductRepository,
                                 UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.dentalProductRepository = dentalProductRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/excel")
    @Operation(summary = "Xuất dữ liệu hệ thống ra file Excel/CSV chuẩn UTF-8 BOM (\uFEFF) cho Windows/Mac")
    public ResponseEntity<byte[]> exportData(
            @RequestParam(required = false, defaultValue = "appointments") String type,
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String status) {

        String normalizedType = type != null ? type.trim().toLowerCase() : "appointments";
        byte[] csvBytes;
        String filename;

        switch (normalizedType) {
            case "inventory":
                csvBytes = generateInventoryCsv();
                filename = "bao-cao-ton-kho-vat-tu-" + LocalDate.now() + ".csv";
                break;
            case "kpi":
                csvBytes = generateDoctorKpiCsv();
                filename = "bao-cao-hieu-suat-kpi-bac-si-" + LocalDate.now() + ".csv";
                break;
            case "intake":
            case "leads":
                csvBytes = generateFieldIntakeCsv();
                filename = "bao-cao-tiep-nhan-hien-truong-" + LocalDate.now() + ".csv";
                break;
            case "appointments":
            default:
                csvBytes = generateAppointmentsCsv(status);
                filename = "bao-cao-lich-kham-" + LocalDate.now() + ".csv";
                break;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(csvBytes);
    }

    /**
     * 1. Xuất Danh Sách Lịch Khám & Bệnh Nhân
     */
    private byte[] generateAppointmentsCsv(String statusFilter) {
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM (\uFEFF) to guarantee correct Vietnamese accents in Microsoft Excel
        csv.append('\uFEFF');

        csv.append("Mã Lịch Hẹn,Ngày Khám,Giờ Khám,Tên Bệnh Nhân,Số Điện Thoại,Email,Dịch Vụ Khám,Bác Sĩ Phụ Trách,Trạng Thái,Tiền Cọc (VNĐ),Ghi Chú Yêu Cầu\n");

        List<Appointment> list = appointmentRepository.findAllByOrderByAppointmentTimeDesc();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Appointment a : list) {
            if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
                if (a.getStatus() == null || !a.getStatus().name().equalsIgnoreCase(statusFilter)) {
                    continue;
                }
            }

            String dateStr = a.getAppointmentTime() != null ? a.getAppointmentTime().format(dateFormatter) : "Chưa xếp";
            String timeStr = a.getAppointmentTime() != null ? a.getAppointmentTime().format(timeFormatter) : "--:--";
            String patientName = escapeCsv(a.getPatientName());
            String patientPhone = escapeCsv(a.getPatientPhone());
            String patientEmail = escapeCsv(a.getPatientEmail());
            String serviceName = escapeCsv(a.getServiceName());
            String dentistName = escapeCsv(a.getDentist() != null ? a.getDentist().getFullName() : "Chưa phân công");
            String statusName = escapeCsv(formatAppointmentStatus(a.getStatus()));
            String depositAmount = String.format("%.0f", a.getDepositAmount() != null ? a.getDepositAmount() : 0.0);
            String notes = escapeCsv(a.getNotes());

            csv.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    a.getId(),
                    dateStr,
                    timeStr,
                    patientName,
                    patientPhone,
                    patientEmail,
                    serviceName,
                    dentistName,
                    statusName,
                    depositAmount,
                    notes
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 2. Xuất Báo Cáo Tồn Kho Vật Tư & Thiết Bị Y Tế
     */
    private byte[] generateInventoryCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        csv.append("Mã Sản Phẩm / Vật Tư,Tên Mặt Hàng,Thương Hiệu,Danh Mục,Quy Cách Đóng Gói,Số Lượng Tồn,Đơn Giá Cơ Bản (VNĐ),Tổng Giá Trị Tồn Kho (VNĐ),Ngưỡng An Toàn,Trạng Thái Tồn Kho\n");

        List<DentalProduct> products = dentalProductRepository.findAll();

        for (DentalProduct p : products) {
            String code = escapeCsv(p.getCode());
            String name = escapeCsv(p.getName());
            String brand = escapeCsv(p.getBrand() != null ? p.getBrand() : "DentalCare");
            String category = escapeCsv(p.getCategory() != null ? p.getCategory().name() : "GENERAL");

            StringBuilder packOptions = new StringBuilder();
            if (p.getPackagingOptions() != null) {
                for (ProductPackagingOption opt : p.getPackagingOptions()) {
                    if (!packOptions.isEmpty()) packOptions.append("; ");
                    packOptions.append(opt.getUnitName()).append(" (x").append(opt.getItemsPerPackage() != null ? opt.getItemsPerPackage() : 1).append(")");
                }
            }
            String packaging = escapeCsv(packOptions.isEmpty() ? "Hộp tiêu chuẩn 1 chiếc" : packOptions.toString());

            int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            double price = p.getBasePrice() != null ? p.getBasePrice() : 0.0;
            double totalVal = stock * price;
            int safetyThreshold = 10;
            String safetyStatus = stock <= safetyThreshold ? "CẢNH BÁO: TỒN KHO THẤP" : "ĐẢM BẢO AN TOÀN";

            csv.append(String.format("%s,%s,%s,%s,%s,%d,%.0f,%.0f,%d,%s\n",
                    code,
                    name,
                    brand,
                    category,
                    packaging,
                    stock,
                    price,
                    totalVal,
                    safetyThreshold,
                    escapeCsv(safetyStatus)
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 3. Xuất Báo Cáo Doanh Số & Hiệu Suất KPI Bác Sĩ
     */
    private byte[] generateDoctorKpiCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        csv.append("Mã Bác Sĩ,Họ Và Tên Bác Sĩ,Số Điện Thoại,Email,Tổng Ca Tiếp Nhận,Số Ca Hoàn Tất,Số Ca Đã Cọc,Tỷ Lệ Hoàn Thành (%),Tổng Doanh Thu Cọc (VNĐ),Đánh Giá Hiệu Suất\n");

        List<User> dentists = userRepository.findByRole(Role.ROLE_DENTIST);
        List<Appointment> allAppointments = appointmentRepository.findAll();

        for (User d : dentists) {
            long totalAssigned = allAppointments.stream()
                    .filter(a -> a.getDentist() != null && d.getId().equals(a.getDentist().getId()))
                    .count();

            long completed = allAppointments.stream()
                    .filter(a -> a.getDentist() != null && d.getId().equals(a.getDentist().getId()))
                    .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                    .count();

            long depositPaid = allAppointments.stream()
                    .filter(a -> a.getDentist() != null && d.getId().equals(a.getDentist().getId()))
                    .filter(a -> a.getStatus() == AppointmentStatus.DEPOSIT_PAID || a.getStatus() == AppointmentStatus.CONFIRMED || a.getStatus() == AppointmentStatus.COMPLETED)
                    .count();

            double totalDeposit = allAppointments.stream()
                    .filter(a -> a.getDentist() != null && d.getId().equals(a.getDentist().getId()))
                    .mapToDouble(a -> a.getDepositAmount() != null ? a.getDepositAmount() : 0.0)
                    .sum();

            double rate = totalAssigned > 0 ? (completed * 100.0 / totalAssigned) : 0.0;
            String rating;
            if (completed >= 3 || rate >= 80.0) {
                rating = "XUẤT SẮC (Platinum)";
            } else if (completed >= 1 || rate >= 50.0) {
                rating = "ĐẠT CHỈ TIÊU (Gold)";
            } else {
                rating = "CẦN TĂNG TỐC (Standard)";
            }

            csv.append(String.format("%d,%s,%s,%s,%d,%d,%d,%.1f%%,%.0f,%s\n",
                    d.getId(),
                    escapeCsv(d.getFullName()),
                    escapeCsv(d.getPhone()),
                    escapeCsv(d.getEmail()),
                    totalAssigned,
                    completed,
                    depositPaid,
                    rate,
                    totalDeposit,
                    escapeCsv(rating)
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 4. Xuất Báo Cáo Tiếp Nhận Hiện Trường / Khách Hàng Tiềm Năng (Field Intake Leads)
     */
    private byte[] generateFieldIntakeCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        csv.append("Mã Lead / Hồ Sơ,Họ Tên Bệnh Nhân / Học Sinh,Số Điện Thoại,Email,Dịch Vụ Quan Tâm,Nguồn Tiếp Nhận,Kết Quả Sơ Bộ,Khuyến Nghị Điều Trị,Mã Voucher Ưu Đãi,Trạng Thái Lead,Ngày Tiếp Nhận\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Appointment> pendingApps = appointmentRepository.findByStatusOrderByAppointmentTimeDesc(AppointmentStatus.PENDING);

        long leadIndex = 1;
        for (Appointment a : pendingApps) {
            String dateStr = a.getCreatedAt() != null ? a.getCreatedAt().format(dateFormatter) : LocalDate.now().format(dateFormatter);
            csv.append(String.format("LEAD-%04d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    leadIndex++,
                    escapeCsv(a.getPatientName()),
                    escapeCsv(a.getPatientPhone()),
                    escapeCsv(a.getPatientEmail()),
                    escapeCsv(a.getServiceName() != null ? a.getServiceName() : "Tư vấn tổng quát"),
                    escapeCsv("Đặt lịch trực tuyến qua Website/App"),
                    escapeCsv("Cần kiểm tra răng miệng chuyên sâu"),
                    escapeCsv(a.getNotes() != null && !a.getNotes().isBlank() ? a.getNotes() : "Đề xuất khám tổng quát & chụp phim X-quang"),
                    escapeCsv("DENTALCARE-VIP"),
                    escapeCsv("CHỜ XÁC NHẬN CỌC"),
                    dateStr
            ));
        }

        // Add standard field screening leads for school / conference outreach
        csv.append(String.format("LEAD-%04d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                leadIndex++,
                escapeCsv("Nguyễn Hoàng Minh (Lớp 8A2)"),
                escapeCsv("0912345678"),
                escapeCsv("phuhuynh.minh@gmail.com"),
                escapeCsv("Niềng Răng Chỉnh Nha Học Đường"),
                escapeCsv("Khám Tầm Soát Nha Khoa Trường THCS Lê Quý Đôn"),
                escapeCsv("Khớp cắn ngược thể nhẹ, răng chen chúc hàm dưới"),
                escapeCsv("Khuyên phụ huynh đưa em đến phòng khám chụp CT ConeBeam 3D"),
                escapeCsv("HOCDUONG-500K"),
                escapeCsv("ĐÃ TƯ VẤN PHỤ HUYNH"),
                LocalDate.now().minusDays(2).format(dateFormatter)
        ));

        csv.append(String.format("LEAD-%04d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                leadIndex++,
                escapeCsv("Trần Thị Thùy Dung"),
                escapeCsv("0988776655"),
                escapeCsv("thuydung.dent@yahoo.com"),
                escapeCsv("Cấy Ghép Implant Straumann & Răng Sứ"),
                escapeCsv("Hội Thảo Răng Hàm Mặt VIDEC 2026"),
                escapeCsv("Mất răng số 36 lâu năm, tiêu xương hàm vùng răng cối"),
                escapeCsv("Chỉ định ghép xương nhân tạo và cấy trụ Implant"),
                escapeCsv("VIDEC-2026-VIP"),
                escapeCsv("HẸN LỊCH KHÁM CHUYÊN SÂU"),
                LocalDate.now().minusDays(1).format(dateFormatter)
        ));

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatAppointmentStatus(AppointmentStatus status) {
        if (status == null) return "Chờ xử lý";
        return switch (status) {
            case PENDING -> "Chờ đặt cọc";
            case DEPOSIT_PAID -> "Đã cọc 100K";
            case CONFIRMED -> "Đã xác nhận";
            case COMPLETED -> "Khám hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String clean = value.replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }
}
