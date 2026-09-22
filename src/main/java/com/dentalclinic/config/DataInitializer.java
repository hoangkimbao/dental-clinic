package com.dentalclinic.config;

import com.dentalclinic.model.*;
import com.dentalclinic.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final OrthodonticPlanRepository orthodonticPlanRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final CouponRepository couponRepository;
    private final ArticleRepository articleRepository;
    private final DoctorReviewRepository doctorReviewRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AppointmentRepository appointmentRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           OrthodonticPlanRepository orthodonticPlanRepository,
                           StaffShiftRepository staffShiftRepository,
                           PaymentRepository paymentRepository,
                           NotificationRepository notificationRepository,
                           CouponRepository couponRepository,
                           ArticleRepository articleRepository,
                           DoctorReviewRepository doctorReviewRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.orthodonticPlanRepository = orthodonticPlanRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.couponRepository = couponRepository;
        this.articleRepository = articleRepository;
        this.doctorReviewRepository = doctorReviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🌱 Checking Sample Dental Clinic Initial Data...");

        String hash123 = passwordEncoder.encode("123");

        User owner = null;
        User dentist1 = null;
        User dentist2 = null;

        if (userRepository.count() == 0) {
            System.out.println("🌱 Initializing Sample Dental Clinic Users & Appointments...");
            owner = userRepository.save(new User("owner", hash123, "BS.CKII Trần Văn Thắng (Chủ Phòng Khám)", "0901234567", "owner@dental.vn", Role.ROLE_OWNER));
            User admin = userRepository.save(new User("admin", hash123, "IT Administrator (Quản Trị Hệ Thống)", "0909998888", "admin@dental.vn", Role.ROLE_ADMIN));
            User receptionist = userRepository.save(new User("letan", hash123, "Nguyễn Thu Hà (Trưởng Lễ Tân)", "0902345678", "letan@dental.vn", Role.ROLE_RECEPTIONIST));
            dentist1 = userRepository.save(new User("bs_tuan", hash123, "BS. Lê Minh Tuấn (Chuyên Gia Chỉnh Nha)", "0903456789", "tuan@dental.vn", Role.ROLE_DENTIST));
            dentist2 = userRepository.save(new User("bs_lan", hash123, "BS. Hoàng Ngọc Lan (Nha Khoa Tổng Quát)", "0904567890", "lan@dental.vn", Role.ROLE_DENTIST));
            User assistant = userRepository.save(new User("phuta", hash123, "Phạm Hải Đăng (Phụ Tá Nha Khoa)", "0905678901", "dang@dental.vn", Role.ROLE_ASSISTANT));
            User cleaner = userRepository.save(new User("tapvu", hash123, "Cô Mai Thị Cúc (Vệ Sinh & Vô Trùng)", "0906789012", "cuc@dental.vn", Role.ROLE_CLEANER));
            User patient = userRepository.save(new User("benhnhan", hash123, "Vũ Hoàng Nam (Bệnh Nhân)", "0988776655", "nam@gmail.com", Role.ROLE_PATIENT));

            // 2. Lịch Hẹn Mẫu
            Appointment app1 = new Appointment();
            app1.setPatient(patient);
            app1.setDentist(dentist1);
            app1.setServiceName("Niềng Răng Mắc Cài Kim Loại Tự Buộc");
            app1.setAppointmentTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            app1.setStatus(AppointmentStatus.DEPOSIT_PAID);
            app1.setDepositAmount(100000.0);
            app1.setNotes("Khách hẹn gắn mắc cài hàm trên");
            Appointment savedApp1 = appointmentRepository.save(app1);

            Payment pay1 = new Payment(savedApp1, 100000.0, PaymentMethod.QR_VNPAY, "TXN_INIT_100K");
            paymentRepository.save(pay1);

            Appointment app2 = new Appointment();
            app2.setPatientName("Đặng Thị Mai");
            app2.setPatientPhone("0977112233");
            app2.setPatientEmail("mai@gmail.com");
            app2.setDentist(dentist2);
            app2.setServiceName("Nhổ Răng Khôn Hàm Dưới (Piezotome)");
            app2.setAppointmentTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(30));
            app2.setStatus(AppointmentStatus.PENDING);
            app2.setDepositAmount(100000.0);
            app2.setNotes("Răng khôn mọc lệch 90 độ, sợ đau");
            appointmentRepository.save(app2);

            // 3. Hồ sơ Bệnh án EMR Mẫu
            MedicalRecord med1 = new MedicalRecord();
            med1.setPatient(patient);
            med1.setDentist(dentist1);
            med1.setDiagnosis("Răng khấp khểnh độ 2, khớp cắn sâu nhẹ");
            med1.setTreatmentDone("Đã lấy dấu hàm, chụp phim Panorama, vệ sinh cạo vôi răng vô trùng.");
            med1.setPrescription("Nước súc miệng Chlorohexidine 0.12%, Gel giảm ê buốt");
            med1.setRecordDate(LocalDate.now().minusDays(10));
            med1.setNextFollowUpDate(LocalDate.now().plusDays(5));
            med1.setNotes("Chuẩn bị gắn mắc cài.");
            medicalRecordRepository.save(med1);

            // 4. Phác đồ Chỉnh nha Mẫu
            OrthodonticPlan ortho = new OrthodonticPlan();
            ortho.setPatient(patient);
            ortho.setDentist(dentist1);
            ortho.setBracketType(BracketType.DAMON_Q2_METAL);
            ortho.setCurrentStage(OrthoStage.GAN_MAC_CAI);
            ortho.setTotalEstimatedMonths(24);
            ortho.setCompletedMonths(2);
            ortho.setStartDate(LocalDate.now().minusMonths(2));
            ortho.setNextAdjustmentDate(LocalDate.now().plusDays(10));
            ortho.setDoctorNotes("Tiến triển khớp cắn tốt. Lần hẹn tới: Thay dây cung Niti 014 lên Niti 016.");
            orthodonticPlanRepository.save(ortho);

            // 5. Phân ca Lịch làm việc
            StaffShift s1 = new StaffShift(dentist1, LocalDate.now(), ShiftType.CA_SANG_8H_12H, "Nha sĩ chính", "Phòng khám số 1");
            staffShiftRepository.save(s1);

            StaffShift s2 = new StaffShift(assistant, LocalDate.now(), ShiftType.CA_SANG_8H_12H, "Phụ tá", "Hỗ trợ BS Tuấn phòng khám số 1");
            staffShiftRepository.save(s2);

            StaffShift s3 = new StaffShift(cleaner, LocalDate.now(), ShiftType.CA_FULL_NGAY, "Dọn vệ sinh & Vô trùng", "Quy trình hấp sấy vô trùng dụng cụ autoclave");
            staffShiftRepository.save(s3);

            // 6. Thông báo ban đầu
            notificationRepository.save(new Notification("ROLE_RECEPTIONIST", "🌟 Chào Mừng Hệ Thống Mới", "Hệ thống Quản lý Nha Khoa DentalClinic v2.0 Enterprise đã sẵn sàng phục vụ!", "ANNOUNCEMENT"));
            notificationRepository.save(new Notification("ROLE_DENTIST", "📅 Lịch Hẹn Hôm Nay", "Bác sĩ Tuấn có 3 lịch hẹn tái khám chỉnh nha trong ngày hôm nay.", "SCHEDULE"));
        } else {
            dentist1 = userRepository.findByUsername("bs_tuan").orElse(null);
            dentist2 = userRepository.findByUsername("bs_lan").orElse(null);
            owner = userRepository.findByUsername("owner").orElse(null);
            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(new User("admin", hash123, "IT Administrator (Quản Trị Hệ Thống)", "0909998888", "admin@dental.vn", Role.ROLE_ADMIN));
            }
        }

        // 7. Tạo Mã Ưu Đãi Flash Sale Mẫu
        if (couponRepository.count() == 0) {
            couponRepository.save(new Coupon("NIENG3D5TR", "Đại Tiệc Chỉnh Nha Kỹ Thuật Số", "Giảm ngay 5.000.000đ khi niềng răng mắc cài tự buộc Damon Q2 hoặc Invisalign + Tặng máy tăm nước 1.8tr", "FIXED_AMOUNT", 5000000.0, "NIENG_RANG", LocalDate.now().plusMonths(3)));
            couponRepository.save(new Coupon("IMPLANT3TR", "Trồng Răng Thụy Sĩ Chuẩn Quốc Tế", "Tặng ngay 3.000.000đ + Miễn phí Abutment Titan chính hãng khi cấy ghép Implant Straumann", "FIXED_AMOUNT", 3000000.0, "IMPLANT", LocalDate.now().plusMonths(3)));
            couponRepository.save(new Coupon("WHITENING50", "Tuần Lễ Răng Trắng Tinh Khiết", "Giảm sốc 50% Gói Tẩy Trắng Răng Laser Whitening Hoa Kỳ (Chỉ còn 900.000đ)", "PERCENTAGE", 50.0, "TAY_TRANG", LocalDate.now().plusMonths(1)));
            couponRepository.save(new Coupon("FREEEXAM", "Khám & Chụp Phim 3D 0 Đồng", "Miễn phí 100% gói Khám tổng quát & Chụp phim CT Cone Beam 3D (Tiết kiệm 1.500.000đ)", "FIXED_AMOUNT", 1500000.0, "ALL", LocalDate.now().plusMonths(6)));
        }

        // 8. Tạo Cẩm Nang Y Khoa Chuẩn CMS (Articles)
        if (articleRepository.count() == 0) {
            articleRepository.save(new Article(
                    "Bảng Giá Niềng Răng 2026: Chi Tiết Từng Loại & Lộ Trình Trả Góp 0%",
                    "bang-gia-nieng-rang-2026-tra-gop-0-phan-tram",
                    "NIENG_RANG",
                    "Tổng hợp chi phí niềng răng mắc cài kim loại, mắc cài sứ và Invisalign. Lộ trình thanh toán linh hoạt cho học sinh, sinh viên...",
                    "Niềng răng (chỉnh nha) là giải pháp hàng đầu giúp khắc phục tình trạng răng hô, móm, khấp khểnh, thưa hoặc sai lệch khớp cắn, mang lại nụ cười chuẩn tỉ lệ vàng và khuôn mặt cân đối.",
                    "BS. Lê Minh Tuấn",
                    "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?w=500&auto=format&fit=crop&q=80",
                    4
            ));

            articleRepository.save(new Article(
                    "Trồng Răng Implant Có Đau Không? Quy Trình 4 Bước Chuẩn Bộ Y Tế",
                    "trong-rang-implant-co-dau-khong-quy-trinh-chuan",
                    "IMPLANT",
                    "Giải đáp nỗi sợ trồng răng: Cấy ghép Implant kỹ thuật số với công nghệ định vị 3D không rạch nướu, thời gian hồi phục thần tốc...",
                    "Cấy ghép Implant là kỹ thuật phục hình răng mất hoàn hảo nhất hiện nay, thay thế cả chân răng và thân răng đã mất, ngăn ngừa tuyệt đối tình trạng tiêu xương hàm và hóp má.",
                    "BS.CKII Trần Văn Thắng",
                    "https://images.unsplash.com/photo-1606811841689-23dfddce3e95?w=500&auto=format&fit=crop&q=80",
                    5
            ));

            articleRepository.save(new Article(
                    "Nhổ Răng Khôn Bằng Sóng Siêu Âm Piezotome: Giảm Sưng Đau 80%",
                    "nho-rang-khon-song-sieu-am-piezotome-giam-dau",
                    "RANG_KHON",
                    "Vì sao nên nhổ răng khôn mọc lệch bằng công nghệ sóng siêu âm? Hướng dẫn cách chăm sóc vết nhổ để ăn uống bình thường sau 24h...",
                    "Răng khôn (răng số 8) mọc lệch, mọc ngầm là nguyên nhân hàng đầu gây sâu răng số 7, viêm lợi trùm, tiêu xương hàm và xô lệch toàn bộ hàm răng.",
                    "BS. Hoàng Ngọc Lan",
                    "https://images.unsplash.com/photo-1598256989800-fe5f95da9787?w=500&auto=format&fit=crop&q=80",
                    3
            ));

            articleRepository.save(new Article(
                    "Bọc Răng Sứ & Dán Sứ Veneer Không Mài Răng: Nên Chọn Loại Nào?",
                    "boc-rang-su-dan-su-veneer-khong-mai-rang",
                    "RANG_SU",
                    "So sánh chi tiết bọc sứ Zirconia/Cercon và dán sứ Veneer Emax siêu mỏng 0.2mm bảo tồn tối đa răng gốc tự nhiên...",
                    "Sở hữu hàm răng trắng sáng đều đặn giúp bạn tự tin tỏa sáng trong công việc và cuộc sống.",
                    "BS.CKII Trần Văn Thắng",
                    "https://images.unsplash.com/photo-1571772996211-2f02c9727629?w=500&auto=format&fit=crop&q=80",
                    4
            ));
        }

        // 9. Tạo Đánh Giá 5 Sao Bác Sĩ Mẫu
        if (doctorReviewRepository.count() == 0) {
            Long tId = dentist1 != null ? dentist1.getId() : 1L;
            Long lId = dentist2 != null ? dentist2.getId() : 2L;
            Long oId = owner != null ? owner.getId() : 3L;

            doctorReviewRepository.save(new DoctorReview(tId, "Nguyễn Thanh Trúc", 5, "Bác sĩ Tuấn tư vấn niềng răng cực kỳ có tâm, giải thích phác đồ 3D ClinCheck rất chi tiết. Phòng khám sạch sẽ, máy móc hiện đại!", "Niềng Răng Invisalign", true));
            doctorReviewRepository.save(new DoctorReview(tId, "Trần Hoàng Nam", 5, "Mình niềng mắc cài tự buộc ở đây được 6 tháng, răng chạy đều và nhanh thấy rõ. Chi phí trả góp hàng tháng rất nhẹ nhàng.", "Niềng Răng Damon Q2", true));
            doctorReviewRepository.save(new DoctorReview(lId, "Chị Vũ Thị Mai", 5, "Bác sĩ Lan nhổ răng khôn siêu âm êm ru, về nhà không hề bị sưng má, hôm sau ăn uống bình thường. Rất cảm ơn BS Lan!", "Nhổ Răng Khôn Piezotome", true));
            doctorReviewRepository.save(new DoctorReview(lId, "Phạm Đức Minh", 5, "Đưa bé 7 tuổi đến trám răng và cạo vôi, bác sĩ Lan rất nhẹ nhàng dỗ dành bé nên bé không sợ tí nào.", "Nha Khoa Trẻ Em", true));
            doctorReviewRepository.save(new DoctorReview(oId, "Chú Lê Văn Hùng (Việt Kiều Úc)", 5, "Tôi về nước trồng 2 trụ Implant Straumann chỗ Bác sĩ Thắng, làm cực kỳ nhanh và chuẩn xác. Giờ ăn nhai tốt như răng thật!", "Cấy Ghép Implant Straumann", true));
        }

        System.out.println("✅ Sample Dental Clinic Data, Articles & Doctor Reviews Initialized successfully!");
    }
}
