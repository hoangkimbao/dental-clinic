package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.BookingRequest;
import com.dentalclinic.dto.BookingResultDto;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentStatus;
import com.dentalclinic.model.Payment;
import com.dentalclinic.model.PaymentMethod;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.security.CustomUserDetails;
import com.dentalclinic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
@Tag(name = "2. Appointments", description = "Quản lý Đặt lịch khám & Tự động cấp tài khoản cho bệnh nhân")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }

    private User resolveCurrentUser(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            return userDetails.getUser();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            if (auth.getPrincipal() instanceof CustomUserDetails cud) {
                return cud.getUser();
            }
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @PostMapping("/book")
    @Operation(summary = "Đặt lịch khám mới (Tự động cấp tài khoản nếu chưa có)")
    public ResponseEntity<ApiResponse<BookingResultDto>> bookAppointment(@Valid @RequestBody BookingRequest request) {
        BookingResultDto result = appointmentService.createBooking(request);
        String msg = result.isNewAccountCreated() 
                ? "Đặt lịch thành công! Hệ thống đã tự động kích hoạt tài khoản cho bạn (Tên đăng nhập là SĐT của bạn)."
                : "Đặt lịch khám thành công!";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'ADMIN')")
    @Operation(summary = "Lấy tất cả lịch hẹn (Chủ phòng, Lễ tân & Quản trị viên)")
    public ResponseEntity<ApiResponse<List<Appointment>>> getAllAppointments() {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAllAppointments()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'ADMIN')")
    @Operation(summary = "Lấy lịch hẹn phân trang (Chủ phòng, Lễ tân & Quản trị viên)")
    public ResponseEntity<ApiResponse<Page<Appointment>>> getAppointmentsPaged(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsPaged(pageable)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Appointment>>> getByStatus(@PathVariable AppointmentStatus status) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsByStatus(status)));
    }

    @GetMapping("/dentist/{dentistId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'DENTIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Appointment>>> getForDentist(@PathVariable Long dentistId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForDentist(dentistId)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Appointment>>> getForPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = resolveCurrentUser(userDetails);
        if (currentUser != null && currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(patientId)) {
                throw new AccessDeniedException("Không có quyền xem lịch hẹn của bệnh nhân khác!");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForPatient(patientId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'DENTIST', 'PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Appointment>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        User currentUser = resolveCurrentUser(userDetails);
        if (currentUser != null && currentUser.getRole() == Role.ROLE_PATIENT) {
            boolean isOwner = (appointment.getPatient() != null && appointment.getPatient().getId().equals(currentUser.getId()))
                    || (appointment.getPatientPhone() != null && appointment.getPatientPhone().equals(currentUser.getPhone()));
            if (!isOwner) {
                throw new AccessDeniedException("Không có quyền xem chi tiết lịch hẹn này!");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(appointment));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'DENTIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Appointment>> updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!", appointmentService.updateStatus(id, status)));
    }

    @PostMapping("/{id}/pay-deposit")
    @Operation(summary = "Xác nhận cọc giữ chỗ 100K (Sandbox / Webhook)")
    public ResponseEntity<ApiResponse<Payment>> payDeposit(@PathVariable Long id,
                                                           @RequestParam(required = false) String transactionId,
                                                           @RequestParam(required = false) PaymentMethod method) {
        Payment payment = appointmentService.processDeposit(id, transactionId, method);
        return ResponseEntity.ok(ApiResponse.success("Thanh toán cọc 100.000đ thành công!", payment));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'DENTIST', 'ADMIN')")
    @Operation(summary = "Lọc danh sách lịch hẹn theo mốc thời gian (Hôm nay, Ngày mai, 7 ngày tới, Tương lai, Khoảng ngày)")
    public ResponseEntity<ApiResponse<List<Appointment>>> filterAppointments(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false) Long dentistId,
            @RequestParam(required = false) AppointmentStatus status) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.filterAppointments(preset, startDate, endDate, dentistId, status)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'DENTIST', 'ADMIN')")
    @Operation(summary = "Xuất danh sách lịch hẹn ra file Excel/CSV chuẩn UTF-8")
    public ResponseEntity<byte[]> exportAppointments(
            @RequestParam(defaultValue = "all") String preset,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false) Long dentistId,
            @RequestParam(required = false) AppointmentStatus status) {

        List<Appointment> list = appointmentService.filterAppointments(preset, startDate, endDate, dentistId, status);
        byte[] csvData = appointmentService.generateAppointmentsCsv(list);

        String filename = "danh-sach-lich-kham-" + preset + "-" + java.time.LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvData);
    }
}
