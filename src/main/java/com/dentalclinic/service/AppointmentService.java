package com.dentalclinic.service;

import com.dentalclinic.dto.AuthResponse;
import com.dentalclinic.dto.BookingRequest;
import com.dentalclinic.dto.BookingResultDto;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.*;
import com.dentalclinic.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class


AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final Double configuredDepositAmount;

    // 100% Constructor Injection
    public AppointmentService(AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              PaymentRepository paymentRepository,
                              NotificationService notificationService,
                              CouponRepository couponRepository,
                              PasswordEncoder passwordEncoder,
                              JwtTokenProvider jwtTokenProvider,
                              EmailService emailService,
                              @Value("${app.clinic.deposit-amount:100000.0}") Double configuredDepositAmount) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.couponRepository = couponRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.configuredDepositAmount = configuredDepositAmount;
    }

    @Transactional
    public BookingResultDto createBooking(BookingRequest request) {
        Appointment app = new Appointment();
        String cleanPhone = request.getPatientPhone().trim();
        String cleanName = request.getPatientName().trim();
        app.setPatientName(cleanName);
        app.setPatientPhone(cleanPhone);
        app.setPatientEmail(request.getPatientEmail());
        app.setServiceName(request.getServiceName().trim());
        app.setNotes(request.getNotes());
        app.setDepositAmount(configuredDepositAmount);

        // 1. Áp dụng mã Coupon nếu có
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            couponRepository.findByCodeIgnoreCaseAndActiveTrue(request.getCouponCode().trim()).ifPresent(coupon -> {
                String note = (app.getNotes() != null && !app.getNotes().isBlank()) ? app.getNotes() : "Yêu cầu khám";
                app.setNotes(note + " [VOUCHER ÁP DỤNG: " + coupon.getCode() + " - " + coupon.getTitle() + "]");
            });
        }

        // 2. TỰ ĐỘNG TẠO TÀI KHOẢN CHO KHÁCH HÀNG CHƯA ĐĂNG KÝ
        Optional<User> existingUser = userRepository.findAll().stream()
                .filter(u -> cleanPhone.equals(u.getPhone()) || cleanPhone.equals(u.getUsername()))
                .findFirst();

        User patient;
        boolean isNewAccount = false;
        AuthResponse authInfo = null;

        if (existingUser.isPresent()) {
            patient = existingUser.get();
        } else {
            // Tự động cấp tài khoản Bệnh nhân mới (Username = Số điện thoại, Mật khẩu = 123)
            patient = new User();
            patient.setUsername(cleanPhone);
            patient.setPassword(passwordEncoder.encode("123"));
            patient.setFullName(cleanName);
            patient.setPhone(cleanPhone);
            patient.setEmail(request.getPatientEmail());
            patient.setRole(Role.ROLE_PATIENT);
            patient = userRepository.save(patient);
            isNewAccount = true;

            // Tạo Token JWT tự động đăng nhập
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    cleanPhone,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(Role.ROLE_PATIENT.name()))
            );
            String token = jwtTokenProvider.generateToken(authentication, patient.getId(), patient.getFullName());
            authInfo = new AuthResponse(
                    token,
                    patient.getId(),
                    patient.getUsername(),
                    patient.getFullName(),
                    patient.getRole().name(),
                    patient.getPhone(),
                    patient.getEmail()
            );
        }

        app.setPatient(patient);

        LocalDateTime time = request.getAppointmentTime() != null 
                ? LocalDateTime.parse(request.getAppointmentTime()) 
                : LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        app.setAppointmentTime(time);

        if (request.getDentistId() != null) {
            userRepository.findById(request.getDentistId()).ifPresent(app::setDentist);
        }

        Appointment saved = appointmentRepository.save(app);

        // Bắn thông báo Realtime cho Lễ tân
        notificationService.sendNotification(
                "ROLE_RECEPTIONIST",
                "📅 LỊCH KHÁM MỚI CHỜ CỌC",
                "Bệnh nhân: " + saved.getPatientName() + " vừa đặt hẹn dịch vụ " + saved.getServiceName() + (isNewAccount ? " (Đã tự động tạo tài khoản)" : "") + ".",
                "BOOKING_PENDING"
        );

        // Tự động gửi Email xác nhận đặt lịch & hướng dẫn cọc 100k
        emailService.sendAppointmentBookingEmail(saved);

        return new BookingResultDto(
                saved,
                isNewAccount,
                isNewAccount ? cleanPhone : null,
                isNewAccount ? "123" : null,
                authInfo
        );
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentTimeDesc();
    }

    public Page<Appointment> getAppointmentsPaged(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatusOrderByAppointmentTimeDesc(status);
    }

    public List<Appointment> getAppointmentsForDentist(Long dentistId) {
        return appointmentRepository.findByDentistId(dentistId);
    }

    public List<Appointment> getAppointmentsForPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn #" + id));
    }

    @Transactional
    public Appointment updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment app = getAppointmentById(id);
        app.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(app);

        notificationService.sendNotification(
                "ROLE_RECEPTIONIST",
                "🔄 CẬP NHẬT TRẠNG THÁI LỊCH HẸN #" + id,
                "Lịch hẹn của bệnh nhân " + saved.getPatientName() + " chuyển sang: " + newStatus,
                "STATUS_UPDATED"
        );

        return saved;
    }

    @Transactional
    public Payment processDeposit(Long appointmentId, String transactionId, PaymentMethod method) {
        Appointment app = getAppointmentById(idFrom(appointmentId));
        app.setStatus(AppointmentStatus.DEPOSIT_PAID);
        appointmentRepository.save(app);

        Payment payment = new Payment(
                app,
                app.getDepositAmount(),
                method != null ? method : PaymentMethod.QR_VNPAY,
                transactionId != null ? transactionId : "TXN_" + System.currentTimeMillis()
        );

        Payment savedPayment = paymentRepository.save(payment);

        notificationService.sendNotification(
                "ROLE_RECEPTIONIST",
                "💰 ĐÃ NHẬN CỌC 100K #" + appointmentId,
                "Bệnh nhân " + app.getPatientName() + " đã cọc 100.000đ giữ chỗ thành công!",
                "PAYMENT_SUCCESS"
        );

        // Tự động gửi Email hóa đơn & xác nhận đã cọc thành công 100k
        emailService.sendDepositSuccessEmail(app, savedPayment.getTransactionCode());

        return savedPayment;
    }

    public List<Appointment> filterAppointments(String preset, java.time.LocalDate startDate, java.time.LocalDate endDate, Long dentistId, AppointmentStatus status) {
        List<Appointment> all = appointmentRepository.findAll();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDate today = now.toLocalDate();

        return all.stream()
                .filter(a -> {
                    if (a.getAppointmentTime() == null) return true;
                    java.time.LocalDateTime t = a.getAppointmentTime();
                    
                    if ("today".equalsIgnoreCase(preset)) {
                        return t.toLocalDate().isEqual(today);
                    } else if ("tomorrow".equalsIgnoreCase(preset)) {
                        return t.toLocalDate().isEqual(today.plusDays(1));
                    } else if ("next7days".equalsIgnoreCase(preset)) {
                        return !t.toLocalDate().isBefore(today) && !t.toLocalDate().isAfter(today.plusDays(7));
                    } else if ("future".equalsIgnoreCase(preset)) {
                        return !t.isBefore(now);
                    } else if (startDate != null || endDate != null) {
                        boolean matchStart = (startDate == null) || !t.toLocalDate().isBefore(startDate);
                        boolean matchEnd = (endDate == null) || !t.toLocalDate().isAfter(endDate);
                        return matchStart && matchEnd;
                    }
                    return true;
                })
                .filter(a -> dentistId == null || (a.getDentist() != null && dentistId.equals(a.getDentist().getId())))
                .filter(a -> status == null || status.equals(a.getStatus()))
                .sorted((a1, a2) -> {
                    if (a1.getAppointmentTime() == null) return 1;
                    if (a2.getAppointmentTime() == null) return -1;
                    return a1.getAppointmentTime().compareTo(a2.getAppointmentTime());
                })
                .toList();
    }

    public byte[] generateAppointmentsCsv(List<Appointment> appointments) {
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM to ensure Vietnamese displays accurately in Microsoft Excel
        csv.append('\uFEFF');

        // CSV Header
        csv.append("Mã Lịch Hẹn,Ngày Khám,Giờ Khám,Tên Bệnh Nhân,Số Điện Thoại,Email,Dịch Vụ Khám,Bác Sĩ Phụ Trách,Trạng Thái,Tiền Cọc (VNĐ),Ghi Chú Yêu Cầu\n");

        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        for (Appointment a : appointments) {
            String dateStr = a.getAppointmentTime() != null ? a.getAppointmentTime().format(dateFormatter) : "Chưa xếp";
            String timeStr = a.getAppointmentTime() != null ? a.getAppointmentTime().format(timeFormatter) : "--:--";
            String patientName = escapeCsv(a.getPatientName() != null ? a.getPatientName() : "");
            String patientPhone = escapeCsv(a.getPatientPhone() != null ? a.getPatientPhone() : "");
            String patientEmail = escapeCsv(a.getPatientEmail() != null ? a.getPatientEmail() : "");
            String serviceName = escapeCsv(a.getServiceName() != null ? a.getServiceName() : "");
            String dentistName = escapeCsv(a.getDentist() != null ? a.getDentist().getFullName() : "Chưa chỉ định");
            String statusName = escapeCsv(mapStatusToVietnamese(a.getStatus()));
            String depositAmount = String.format("%.0f", a.getDepositAmount() != null ? a.getDepositAmount() : 0.0);
            String notes = escapeCsv(a.getNotes() != null ? a.getNotes() : "");

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

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String mapStatusToVietnamese(AppointmentStatus status) {
        if (status == null) return "Chờ xử lý";
        return switch (status) {
            case PENDING -> "Chờ đặt cọc";
            case DEPOSIT_PAID -> "Đã cọc 100K (Chờ xác nhận)";
            case CONFIRMED -> "Đã xác nhận / Chuẩn bị tiếp đón";
            case COMPLETED -> "Khám hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String clean = value.replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }

    private Long idFrom(Long id) { return id; }
}
