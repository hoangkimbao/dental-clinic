package com.dentalclinic.service;

import com.dentalclinic.config.VnPayConfig;
import com.dentalclinic.dto.DentalImageAiAnalysisDto;
import com.dentalclinic.dto.PaymentRequestDto;
import com.dentalclinic.dto.PaymentResponseDto;
import com.dentalclinic.dto.StaffCreateRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.itteam.service.NineRouterAiClient;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentStatus;
import com.dentalclinic.model.DentalImageAttachment;
import com.dentalclinic.model.DentalImageType;
import com.dentalclinic.model.DentalMaterial;
import com.dentalclinic.model.Payment;
import com.dentalclinic.model.PaymentStatus;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.StaffShift;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.CouponRepository;
import com.dentalclinic.repository.DentalImageRepository;
import com.dentalclinic.repository.DentalMaterialRepository;
import com.dentalclinic.repository.PaymentRepository;
import com.dentalclinic.repository.StaffShiftRepository;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreServicesUnitTest {

    @Mock
    private DentalMaterialRepository dentalMaterialRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StaffShiftRepository staffShiftRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmailService emailService;
    @Mock
    private DentalImageRepository dentalImageRepository;
    @Mock
    private NineRouterAiClient nineRouterAiClient;

    private DentalMaterialService dentalMaterialService;
    private AppointmentService appointmentService;
    private StaffService staffService;
    private PaymentService paymentService;
    private DentalImageService dentalImageService;

    @BeforeEach
    void setUp() {
        dentalMaterialService = new DentalMaterialService(dentalMaterialRepository);
        appointmentService = new AppointmentService(
                appointmentRepository,
                userRepository,
                paymentRepository,
                notificationService,
                couponRepository,
                passwordEncoder,
                jwtTokenProvider,
                emailService,
                100000.0
        );
        appointmentService.setDentalMaterialService(dentalMaterialService);
        staffService = new StaffService(userRepository, staffShiftRepository, passwordEncoder);
        VnPayConfig vnPayConfig = new VnPayConfig();
        paymentService = new PaymentService(paymentRepository, appointmentRepository, vnPayConfig, notificationService);
        dentalImageService = new DentalImageService(dentalImageRepository, nineRouterAiClient);
    }

    @Test
    @DisplayName("Kịch bản 1: Khấu trừ tồn kho vật tư khi hoàn tất ca điều trị & kiểm tra cảnh báo tồn kho thấp")
    void testDeductStockAndCompleteTreatmentWithMaterials() {
        DentalMaterial composite = new DentalMaterial(
                "Composite 3M Filtek Z350", "Ống", 20, 10, LocalDate.now().plusMonths(6), 850000.0
        );
        composite.setId(1L);

        Appointment appt = new Appointment();
        appt.setId(100L);
        appt.setPatientName("Nguyễn Văn A");
        appt.setStatus(AppointmentStatus.CONFIRMED);

        when(dentalMaterialRepository.findById(1L)).thenReturn(Optional.of(composite));
        when(dentalMaterialRepository.save(any(DentalMaterial.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment completed = appointmentService.completeTreatmentWithMaterials(100L, 1L, 5);

        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
        assertEquals(15, composite.getStockQuantity());
        verify(dentalMaterialRepository, times(1)).save(composite);
        verify(appointmentRepository, times(1)).save(appt);
    }

    @Test
    @DisplayName("Kịch bản 1: Báo lỗi khi số lượng vật tư tiêu hao vượt quá số lượng tồn kho")
    void testDeductStockThrowsWhenInsufficient() {
        DentalMaterial lidocaine = new DentalMaterial(
                "Thuốc tê Lidocaine 2%", "Hộp", 3, 5, LocalDate.now().plusMonths(2), 450000.0
        );
        lidocaine.setId(2L);

        when(dentalMaterialRepository.findById(2L)).thenReturn(Optional.of(lidocaine));

        assertThrows(IllegalArgumentException.class,
                () -> dentalMaterialService.deductStockForTreatment(2L, 10));
    }

    @Test
    @DisplayName("Kịch bản 2: Tạo URL thanh toán VNPay có chữ ký HMAC-SHA512 và xử lý Callback PAID")
    void testCreateVnPayPaymentAndHandleCallback() {
        Appointment appt = new Appointment();
        appt.setId(202L);
        appt.setPatientName("Trần Thị B");
        appt.setDepositAmount(500000.0);
        appt.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(202L)).thenReturn(Optional.of(appt));
        when(paymentRepository.findByAppointment(appt)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        PaymentRequestDto request = new PaymentRequestDto(202L, 500000.0, "Dat coc ca Implant #202", "NCB");
        PaymentResponseDto createRes = paymentService.createVnPayPayment(request);

        assertNotNull(createRes.getPaymentUrl());
        assertTrue(createRes.getPaymentUrl().contains("vnp_SecureHash="));
        assertEquals(PaymentStatus.PENDING, createRes.getStatus());

        Payment savedPayment = new Payment();
        savedPayment.setId(99L);
        savedPayment.setAppointment(appt);
        savedPayment.setAmount(500000.0);
        savedPayment.setTransactionCode(createRes.getTransactionCode());
        savedPayment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByTransactionCode(createRes.getTransactionCode()))
                .thenReturn(Optional.of(savedPayment));

        PaymentResponseDto callbackRes = paymentService.handleVnPayCallback(Map.of(
                "vnp_TxnRef", createRes.getTransactionCode(),
                "vnp_ResponseCode", "00"
        ));

        assertEquals(PaymentStatus.PAID, callbackRes.getStatus());
        assertEquals(PaymentStatus.PAID, savedPayment.getStatus());
        assertEquals(AppointmentStatus.DEPOSIT_PAID, appt.getStatus());
    }

    @Test
    @DisplayName("Kịch bản 3: Trợ lý AI Phân tích ảnh X-quang nha khoa qua NineRouterAiClient")
    void testAnalyzeXRayImageWithAiClient() {
        DentalImageAttachment xray = DentalImageAttachment.builder()
                .id(55L)
                .patientName("Lê Hoàng C")
                .imageType(DentalImageType.PANORAMA)
                .fileUrl("/uploads/xray-panorama-01.png")
                .notes("Bệnh nhân đau vùng răng hàm dưới phải")
                .build();

        when(dentalImageRepository.findById(55L)).thenReturn(Optional.of(xray));
        when(nineRouterAiClient.askDirect(anyString(), anyString()))
                .thenReturn("Phát hiện sâu mặt nhai R46 độ II và răng khôn R48 mọc lệch gần 45 độ.");
        when(dentalImageRepository.save(any(DentalImageAttachment.class))).thenAnswer(inv -> inv.getArgument(0));

        DentalImageAiAnalysisDto analysis = dentalImageService.analyzeXRayImage(55L);

        assertEquals(55L, analysis.getImageId());
        assertEquals("PANORAMA", analysis.getImageType());
        assertTrue(analysis.getFindings().contains("R46"));
        assertTrue(xray.getNotes().contains("[AI X-Ray Analysis]"));
        assertNotNull(analysis.getDisclaimer());
        verify(dentalImageRepository, times(1)).save(xray);
    }

    @Test
    @DisplayName("Kịch bản 4: Kiểm thử StaffService tạo tài khoản Bác sĩ và tự động phân ca trực ban đầu")
    void testStaffServiceCreateDentistAndDuplicateCheck() {
        StaffCreateRequest req = new StaffCreateRequest();
        req.setUsername("dr.baostu");
        req.setPassword("123456");
        req.setFullName("BS. CKI Hoàng Kim Bảo");
        req.setPhone("0909123456");
        req.setRole(Role.ROLE_DENTIST);

        when(userRepository.findByUsername("dr.baostu")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(staffShiftRepository.save(any(StaffShift.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = staffService.createStaff(req);

        assertEquals("dr.baostu", created.getUsername());
        assertEquals(Role.ROLE_DENTIST, created.getRole());
        verify(staffShiftRepository, times(1)).save(any(StaffShift.class));

        when(userRepository.findByUsername("dr.baostu")).thenReturn(Optional.of(created));
        assertThrows(BadRequestException.class, () -> staffService.createStaff(req));
    }
}
