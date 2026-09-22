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
    private final DentalServiceCatalogRepository serviceCatalogRepository;
    private final DentalProductRepository productRepository;
    private final ProductPackagingOptionRepository packagingOptionRepository;
    private final PorcelainCrownWarrantyRepository warrantyRepository;
    private final ClinicBranchRepository branchRepository;
    private final DentalCommunityPostRepository communityPostRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final Tier2AgentRepository tier2AgentRepository;
    private final DentalMaterialRepository dentalMaterialRepository;
    private final MaterialOrderRepository materialOrderRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final DoctorKpiRecordRepository doctorKpiRecordRepository;
    private final FieldPatientIntakeRepository fieldPatientIntakeRepository;
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
                           DentalServiceCatalogRepository serviceCatalogRepository,
                           DentalProductRepository productRepository,
                           ProductPackagingOptionRepository packagingOptionRepository,
                           PorcelainCrownWarrantyRepository warrantyRepository,
                           ClinicBranchRepository branchRepository,
                           DentalCommunityPostRepository communityPostRepository,
                           LoyaltyAccountRepository loyaltyAccountRepository,
                           Tier2AgentRepository tier2AgentRepository,
                           DentalMaterialRepository dentalMaterialRepository,
                           MaterialOrderRepository materialOrderRepository,
                           StaffAttendanceRepository staffAttendanceRepository,
                           DoctorKpiRecordRepository doctorKpiRecordRepository,
                           FieldPatientIntakeRepository fieldPatientIntakeRepository,
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
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.productRepository = productRepository;
        this.packagingOptionRepository = packagingOptionRepository;
        this.warrantyRepository = warrantyRepository;
        this.branchRepository = branchRepository;
        this.communityPostRepository = communityPostRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.tier2AgentRepository = tier2AgentRepository;
        this.dentalMaterialRepository = dentalMaterialRepository;
        this.materialOrderRepository = materialOrderRepository;
        this.staffAttendanceRepository = staffAttendanceRepository;
        this.doctorKpiRecordRepository = doctorKpiRecordRepository;
        this.fieldPatientIntakeRepository = fieldPatientIntakeRepository;
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

        // 10. Tạo Danh Mục Dịch Vụ Nha Khoa Chuyên Sâu (DentalServiceCatalog)
        if (serviceCatalogRepository.count() == 0) {
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "INVISALIGN", "Niềng Răng Trong Suốt Invisalign Hoa Kỳ",
                    DentalServiceCategory.ORTHODONTICS,
                    "Chỉnh nha kỹ thuật số vô hình với khay niềng thông minh SmartTrack, phác đồ 3D ClinCheck biết trước kết quả.",
                    65000000.0, 45, true
            ));
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "IMPLANT_STRAUMANN", "Cấy Ghép Implant Straumann Thụy Sĩ",
                    DentalServiceCategory.IMPLANT,
                    "Trụ Implant số 1 thế giới tích hợp xương tức thì SLActive, bảo tồn xương hàm tuyệt đối, bảo hành trọn đời.",
                    28000000.0, 60, true
            ));
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "CROWN_LAVA_PLUS", "Bọc Răng Sứ Thẩm Mỹ Lava Plus 3M",
                    DentalServiceCategory.PORCELAIN_CROWNS,
                    "Toàn sứ cao cấp 3M Lava Plus Hoa Kỳ, độ trong mờ tự nhiên, chịu lực 1400 MPa, bảo hành chính hãng 15 năm.",
                    7500000.0, 60, true
            ));
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "LASER_WHITENING", "Tẩy Trắng Răng Laser Whitening Hoa Kỳ",
                    DentalServiceCategory.WHITENING,
                    "Bật 3-5 tone sau 45 phút bằng ánh sáng Laser lạnh không ê buốt, an toàn tuyệt đối cho men răng.",
                    1800000.0, 45, true
            ));
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "PIEZOTOME_WISDOM", "Nhổ Răng Khôn Sóng Siêu Âm Piezotome",
                    DentalServiceCategory.WISDOM_TEETH,
                    "Công nghệ bóc tách mô cứng bằng sóng siêu âm cao tần, không tổn thương dây thần kinh, hạn chế sưng đau đến 80%.",
                    2500000.0, 30, true
            ));
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "SCALING_POLISH", "Cạo Vôi Răng Siêu Âm & Đánh Bóng",
                    DentalServiceCategory.GENERAL,
                    "Lấy sạch mảng bám vôi răng dưới nướu không ê buốt, làm sạch vi khuẩn và đánh bóng bề mặt men răng.",
                    300000.0, 30, false
            ));
            serviceCatalogRepository.save(new DentalServiceCatalog(
                    "COMPOSITE_FILLING", "Trám Răng Thẩm Mỹ Laser Composite",
                    DentalServiceCategory.GENERAL,
                    "Phục hồi hình thể răng sâu, nứt mẻ với vật liệu Composite nano trùng màu răng tự nhiên.",
                    450000.0, 30, false
            ));
        }

        // 11. Tạo Sản Phẩm Chăm Sóc Răng Miệng & Quy Cách Đóng Gói (DentalProduct & Packaging)
        if (productRepository.count() == 0) {
            DentalProduct p1 = new DentalProduct(
                    "TB-ORALB-IO9", "Bàn Chải Điện Oral-B iO Series 9",
                    DentalProductCategory.BRUSH, "Oral-B",
                    "Công nghệ từ tính iO mang lại cảm giác chải êm ái, màn hình màu tương tác và cảm biến áp lực bảo vệ nướu.",
                    4500000.0, 50, "https://images.unsplash.com/photo-1559591937-e62fb330bc1f?w=500&auto=format&fit=crop&q=80"
            );
            p1.addPackagingOption(new ProductPackagingOption(p1, PackagingType.BOX, "Hộp Đơn Chuẩn (1 máy + 1 đầu chải + sạc)", 1, 0.0, 4500000.0));
            p1.addPackagingOption(new ProductPackagingOption(p1, PackagingType.COMBO, "Combo Tiết Kiệm (1 máy + 4 đầu Gentle Care thay thế)", 5, 15.0, 4800000.0));
            productRepository.save(p1);

            DentalProduct p2 = new DentalProduct(
                    "FL-WATERPIK-WP560", "Máy Tăm Nước Cầm Tay Waterpik Cordless Advanced",
                    DentalProductCategory.FLOSSER, "Waterpik",
                    "Được chứng nhận bởi Hiệp hội Nha khoa Hoa Kỳ (ADA), loại bỏ 99.9% mảng bám, chống nước IPX7.",
                    2100000.0, 40, "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=80"
            );
            p2.addPackagingOption(new ProductPackagingOption(p2, PackagingType.BOX, "Hộp Đơn (1 máy + 4 đầu phun)", 1, 0.0, 2100000.0));
            p2.addPackagingOption(new ProductPackagingOption(p2, PackagingType.COMBO, "Combo Toàn Diện (1 máy + set 6 đầu phun + 2 chai súc miệng)", 9, 10.0, 2450000.0));
            productRepository.save(p2);

            DentalProduct p3 = new DentalProduct(
                    "TP-SENSODYNE-RAPID", "Kem Đánh Răng Sensodyne Rapid Relief 100g",
                    DentalProductCategory.TOOTHPASTE, "Sensodyne",
                    "Giảm ê buốt chỉ sau 60 giây, bảo vệ men răng nhạy cảm cả ngày.",
                    120000.0, 200, "https://images.unsplash.com/photo-1559591938-16e03fb8a43f?w=500&auto=format&fit=crop&q=80"
            );
            p3.addPackagingOption(new ProductPackagingOption(p3, PackagingType.BOX, "Hộp 1 Tuýp 100g", 1, 0.0, 120000.0));
            p3.addPackagingOption(new ProductPackagingOption(p3, PackagingType.COMBO, "Combo 3 Tuýp + Tặng Bàn Chải Kẽ Chuyên Dụng", 4, 15.0, 310000.0));
            productRepository.save(p3);

            DentalProduct p4 = new DentalProduct(
                    "TP-MARVIS-WHITE", "Kem Đánh Răng Thượng Lưu Marvis Whitening Mint 85ml",
                    DentalProductCategory.TOOTHPASTE, "Marvis",
                    "Hương bạc hà băng giá xứ Florence, Ý. Loại bỏ vết ố do cà phê và thuốc lá, bảo vệ men răng bóng sáng.",
                    320000.0, 100, "https://images.unsplash.com/photo-1563178406-4cdc2923acbc?w=500&auto=format&fit=crop&q=80"
            );
            p4.addPackagingOption(new ProductPackagingOption(p4, PackagingType.BOX, "Hộp 1 Tuýp 85ml", 1, 0.0, 320000.0));
            p4.addPackagingOption(new ProductPackagingOption(p4, PackagingType.COMBO, "Combo 2 Tuýp + Cây Kẹp Kem Bạc Kim Loại", 3, 12.0, 590000.0));
            productRepository.save(p4);

            DentalProduct p5 = new DentalProduct(
                    "FL-ORALB-50M", "Chỉ Nha Khoa Kháng Khuẩn Oral-B Essential Floss 50m",
                    DentalProductCategory.FLOSS, "Oral-B",
                    "Sợi tơ trượt nhẹ nhàng qua kẽ răng hẹp, tráng sáp hương bạc hà sảng khoái.",
                    65000.0, 300, "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=500&auto=format&fit=crop&q=80"
            );
            p5.addPackagingOption(new ProductPackagingOption(p5, PackagingType.BOX, "Hộp Đơn 1 Cuộn 50m", 1, 0.0, 65000.0));
            p5.addPackagingOption(new ProductPackagingOption(p5, PackagingType.COMBO, "Combo Lốc 3 Cuộn Tiết Kiệm", 3, 15.0, 165000.0));
            productRepository.save(p5);

            DentalProduct p6 = new DentalProduct(
                    "RET-VIVERA-PAIR", "Máng Duy Trì Chỉnh Nha Vivera Trong Suốt",
                    DentalProductCategory.RETAINER, "Align Technology",
                    "Máng duy trì cao cấp đúc khuôn theo dữ liệu Scan 3D Trios 5, độ bền cao gấp 2 lần máng thông thường.",
                    3500000.0, 30, "https://images.unsplash.com/photo-1606811971618-4486d14f3f99?w=500&auto=format&fit=crop&q=80"
            );
            p6.addPackagingOption(new ProductPackagingOption(p6, PackagingType.BOX, "Bộ Máng Tiêu Chuẩn (1 cặp trên + dưới kèm hộp đựng)", 1, 0.0, 3500000.0));
            p6.addPackagingOption(new ProductPackagingOption(p6, PackagingType.COMBO, "Combo 2 Cặp Máng Vivera + Hộp Vệ Sinh Khử Khuẩn UV", 3, 20.0, 5900000.0));
            productRepository.save(p6);
        }

        // 12. Tạo Thẻ Bảo Hành Răng Sứ Mẫu (PorcelainCrownWarranty)
        if (warrantyRepository.count() == 0) {
            warrantyRepository.save(new PorcelainCrownWarranty(
                    "DC-WR-2026-88992", "Vũ Hoàng Nam", "0988776655",
                    CrownType.LAVA_PLUS, "11, 21, 12, 22", "Detec Dental Lab (Chính Hãng 3M)",
                    15, LocalDate.now().minusMonths(6), LocalDate.now().plusYears(15).minusMonths(6),
                    "DENTALCARE-LAVA-88992", WarrantyStatus.ACTIVE
            ));
            warrantyRepository.save(new PorcelainCrownWarranty(
                    "DC-WR-2026-77123", "Đặng Thị Mai", "0977112233",
                    CrownType.CERCON_HT, "16, 26, 36, 46", "Dentsply Sirona Lab",
                    10, LocalDate.now().minusMonths(3), LocalDate.now().plusYears(10).minusMonths(3),
                    "DENTALCARE-CERCON-77123", WarrantyStatus.ACTIVE
            ));
            warrantyRepository.save(new PorcelainCrownWarranty(
                    "DC-WR-2026-66554", "Trần Bảo Ngọc", "0912345678",
                    CrownType.EMAX, "13, 23", "Ivoclar Vivadent Lab",
                    10, LocalDate.now().minusMonths(1), LocalDate.now().plusYears(10).minusMonths(1),
                    "DENTALCARE-EMAX-66554", WarrantyStatus.ACTIVE
            ));
        }

        // 13. Tạo Danh Mục Chi Nhánh Đa Cơ Sở (ClinicBranch)
        if (branchRepository.count() == 0) {
            branchRepository.save(new ClinicBranch(
                    "DC-HCMC-1", "Nha Khoa DentalCare Luxury Bình Tân (Trụ Sở Chính)",
                    "36/9/12/7 Nguyễn Triệu Luật, Khu Phố 3", "Phường Bình Tân", "TP. Hồ Chí Minh",
                    "028 3877 6655", "0977 224 504", 10.760624, 106.587106,
                    "08:00 - 20:00 (Thứ 2 - CN)",
                    "Đầy đủ tất cả dịch vụ cấy ghép Implant, Chỉnh nha 3D, Răng sứ thẩm mỹ, Phẫu thuật răng khôn"
            ));
            branchRepository.save(new ClinicBranch(
                    "DC-HCMC-2", "Nha Khoa DentalCare Luxury Quận 1",
                    "182 Hai Bà Trưng, Phường Đa Kao", "Quận 1", "TP. Hồ Chí Minh",
                    "028 3911 2233", "0977 224 504", 10.78761, 106.69742,
                    "08:00 - 20:00 (Thứ 2 - CN)",
                    "Thẩm Mỹ Răng Sứ, Dán Sứ Veneer Nano, Chỉnh Nha Trong Suốt Invisalign"
            ));
            branchRepository.save(new ClinicBranch(
                    "DC-HANOI-1", "Nha Khoa DentalCare Luxury Hoàn Kiếm",
                    "88 Phố Lý Thường Kiệt, Phường Cửa Nam", "Quận Hoàn Kiếm", "Hà Nội",
                    "024 3822 5588", "0977 224 504", 21.02534, 105.84912,
                    "08:00 - 20:00 (Thứ 2 - CN)",
                    "Cấy Ghép Implant Thụy Sĩ, Niềng Răng Mắc Cài Tự Buộc, Tẩy Trắng Laser"
            ));
            branchRepository.save(new ClinicBranch(
                    "DC-HANOI-2", "Nha Khoa DentalCare Luxury Cầu Giấy",
                    "126 Trần Duy Hưng, Phường Trung Hòa", "Quận Cầu Giấy", "Hà Nội",
                    "024 3788 9922", "0977 224 504", 21.00684, 105.79815,
                    "08:00 - 20:00 (Thứ 2 - CN)",
                    "Chỉnh Nha Trẻ Em & Người Lớn, Nhổ Răng Khôn Piezotome, Nha Khoa Tổng Quát"
            ));
        }

        // 14. Tạo Bài Thảo Luận Diễn Đàn Cộng Đồng (DentalCommunityPost)
        if (communityPostRepository.count() == 0) {
            communityPostRepository.save(new DentalCommunityPost(
                    null, "Lê Hoàng Yến", "0988112233",
                    "Kinh nghiệm niềng răng Invisalign sau 6 tháng: Có đau như lời đồn?",
                    "Chào mọi người, mình vừa hoàn thành khay niềng thứ 14 tại DentalCare. Khay rất ôm sát, đeo cả ngày không ai nhận ra. Tuần đầu hơi cộm một chút nhưng sang tuần thứ 2 là quen hoàn toàn...",
                    CommunityPostCategory.EXPERIENCE, 42, true
            ));
            communityPostRepository.save(new DentalCommunityPost(
                    null, "Trần Minh Quang", "0977334455",
                    "Mẹo giảm sưng sau khi nhổ răng khôn bằng sóng siêu âm Piezotome",
                    "Mình vừa nhổ cùng lúc 2 răng khôn hàm dưới bên BS Lan. Bí quyết là chườm lạnh 24h đầu, cắn gạc chặt trong 60 phút và ăn cháo nguội. Sang ngày thứ 2 hầu như không sưng và không cần uống thuốc giảm đau...",
                    CommunityPostCategory.RECOVERY_TIPS, 35, true
            ));
            communityPostRepository.save(new DentalCommunityPost(
                    null, "Nguyễn Thu Thảo", "0911556677",
                    "Bọc răng sứ Lava Plus ăn đồ cứng có sợ mẻ không bác sĩ?",
                    "Em định bọc 4 răng cửa hàm trên dòng Lava Plus 3M vì răng em bị xỉn màu và hơi thưa nhẹ. Nhờ các bác sĩ tư vấn về độ cứng và chế độ ăn nhai sau khi gắn sứ ạ...",
                    CommunityPostCategory.DENTAL_QA, 19, true
            ));
        }

        // 15. Khởi tạo tài khoản Loyalty cho bệnh nhân mẫu
        if (loyaltyAccountRepository.count() == 0) {
            User patient = userRepository.findByUsername("benhnhan").orElse(null);
            loyaltyAccountRepository.save(new LoyaltyAccount(patient, "0988776655", 850, LoyaltyTier.GOLD, 850));
        }

        // 16. Khởi tạo Đại Lý Cấp 2 & Chi Nhánh Vệ Tinh (Tier2Agent)
        Tier2Agent agent1 = null;
        if (tier2AgentRepository.count() == 0) {
            agent1 = tier2AgentRepository.save(new Tier2Agent(
                    "AGT-SEED-01", "Nha Khoa DentalCare Vệ Tinh Bình Tân", AgentType.SATELLITE_CLINIC,
                    "BS. Trần Thanh Tùng", "02837512345", "binhtan@dentalcare.vn",
                    "55 Đường Tên Lửa, Phường An Lạc A", "Quận Bình Tân", "Hồ Chí Minh", "TP. Hồ Chí Minh",
                    LocalDate.now().minusMonths(6), 300000000.0, 15.0
            ));
            tier2AgentRepository.save(new Tier2Agent(
                    "AGT-DIST-01", "Công Ty Thiết Bị & Vật Tư Nha Khoa Sài Gòn", AgentType.DISTRIBUTOR,
                    "Nguyễn Hải Long", "02839301234", "dist@saigondental.vn",
                    "120 Nguyễn Đình Chiểu, Phường Võ Thị Sáu", "Quận 3", "Hồ Chí Minh", "TP. Hồ Chí Minh",
                    LocalDate.now().minusYears(1), 500000000.0, 20.0
            ));
            tier2AgentRepository.save(new Tier2Agent(
                    "AGT-FRAN-01", "Nha Khoa DentalCare Vệ Tinh Đà Nẵng", AgentType.FRANCHISE_PARTNER,
                    "BS. Đặng Quốc Huy", "02363889977", "danang@dentalcare.vn",
                    "234 Nguyễn Văn Linh, Phường Thạc Gián", "Quận Thanh Khê", "Đà Nẵng", "Đà Nẵng",
                    LocalDate.now().minusMonths(3), 250000000.0, 18.0
            ));
        } else {
            agent1 = tier2AgentRepository.findAll().get(0);
        }

        // 17. Khởi tạo Kho Vật Tư Nha Khoa Trung Tâm (DentalMaterial)
        DentalMaterial mat1 = null;
        DentalMaterial mat2 = null;
        if (dentalMaterialRepository.count() == 0) {
            mat1 = dentalMaterialRepository.save(new DentalMaterial(
                    "IMP-STRAUMANN-BLX", "Trụ Cấy Ghép Implant Straumann BLX Roxolid Thụy Sĩ", MaterialCategory.IMPLANT_POST,
                    "Straumann Group (Thụy Sĩ)", "Trụ", 150, 20, 3500000.0, "LOT-STR-991",
                    LocalDate.now().plusYears(3), "Hộp 1 trụ SLA vô trùng kèm vít lành thương"
            ));
            mat2 = dentalMaterialRepository.save(new DentalMaterial(
                    "BRK-DAMON-Q2", "Bộ Mắc Cài Kim Loại Tự Buộc Damon Q2 Ormco", MaterialCategory.BRACKET,
                    "Ormco Corp (Hoa Kỳ)", "Bộ", 80, 15, 250000.0, "LOT-ORM-881",
                    LocalDate.now().plusYears(4), "Vỉ 20 mắc cài 2 hàm (Hệ số torque tiêu chuẩn)"
            ));
            dentalMaterialRepository.save(new DentalMaterial(
                    "CONS-GLOVE-NITRILE", "Găng Tay Y Tế Nitrile Không Bột Vglove", MaterialCategory.CONSUMABLE,
                    "Khải Hoàn Vglove", "Hộp", 120, 25, 95000.0, "LOT-GLV-2026",
                    LocalDate.now().plusYears(2), "Hộp 100 chiếc size S/M chuẩn y tế"
            ));
            dentalMaterialRepository.save(new DentalMaterial(
                    "ANES-SEPT-100", "Thuốc Tê Septanest 1/100.000 Articaine 4%", MaterialCategory.ANESTHETIC,
                    "Septodont (Pháp)", "Hộp", 50, 10, 750000.0, "LOT-SEP-44",
                    LocalDate.now().plusYears(2), "Hộp 50 ống thủy tinh 1.7ml"
            ));
            dentalMaterialRepository.save(new DentalMaterial(
                    "WIRE-NITI-016", "Dây Cung NiTi Kích Hoạt Nhiệt 0.016 Upper/Lower", MaterialCategory.ORTHO_WIRE,
                    "Dentsply Sirona", "Sợi", 300, 50, 85000.0, "LOT-NITI-11",
                    LocalDate.now().plusYears(5), "Gói 10 sợi dây cung chỉnh nha"
            ));
        } else {
            List<DentalMaterial> mats = dentalMaterialRepository.findAll();
            if (!mats.isEmpty()) mat1 = mats.get(0);
            if (mats.size() > 1) mat2 = mats.get(1);
        }

        // 18. Khởi tạo Đơn Đặt Hàng Vật Tư Mẫu (MaterialOrder & MaterialOrderItem)
        if (materialOrderRepository.count() == 0 && agent1 != null && mat1 != null) {
            User receptionist = userRepository.findByUsername("letan").orElse(null);
            MaterialOrder order1 = new MaterialOrder("ORD-MAT-2026-0001", agent1, receptionist, "Đơn đặt hàng mắc cài và trụ implant đợt 1");
            order1.setStatus(MaterialOrderStatus.PENDING_APPROVAL);
            order1.setTotalAmount(7000000.0);
            MaterialOrderItem item1 = new MaterialOrderItem(order1, mat1, 2, 3500000.0);
            order1.addItem(item1);
            materialOrderRepository.save(order1);
        }

        // 19. Khởi tạo Lịch Sử Chấm Công Nhân Sự (StaffAttendance)
        if (staffAttendanceRepository.count() == 0) {
            User dentist1User = userRepository.findByUsername("bs_tuan").orElse(null);
            if (dentist1User != null) {
                StaffShift sPast = new StaffShift(dentist1User, LocalDate.now().minusDays(1), ShiftType.CA_SANG_8H_12H, "Nha sĩ chính", "Phòng khám số 1");
                sPast.setStatus(ShiftStatus.COMPLETED);
                staffShiftRepository.save(sPast);

                StaffAttendance atnPast = new StaffAttendance(
                        dentist1User, sPast,
                        LocalDateTime.now().minusDays(1).withHour(8).withMinute(5),
                        "192.168.1.50", 10.760624, 106.587106, true,
                        AttendanceStatus.ON_TIME, "Chấm công đúng giờ ngày hôm qua"
                );
                atnPast.setCheckOutTime(LocalDateTime.now().minusDays(1).withHour(12).withMinute(2));
                staffAttendanceRepository.save(atnPast);
            }
        }

        // 20. Khởi tạo Chỉ Số KPI Bác Sĩ Nha Khoa (DoctorKpiRecord)
        if (doctorKpiRecordRepository.count() == 0) {
            String currentMonth = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
            User docTuan = userRepository.findByUsername("bs_tuan").orElse(null);
            if (docTuan != null) {
                doctorKpiRecordRepository.save(new DoctorKpiRecord(
                        docTuan, LocalDate.now(), currentMonth,
                        18, 10, 8, 4, 6, 220000000.0, 96.5
                ));
            }
            User docLan = userRepository.findByUsername("bs_lan").orElse(null);
            if (docLan != null) {
                doctorKpiRecordRepository.save(new DoctorKpiRecord(
                        docLan, LocalDate.now(), currentMonth,
                        24, 14, 2, 9, 13, 195000000.0, 92.0
                ));
            }
            User docOwner = userRepository.findByUsername("owner").orElse(null);
            if (docOwner != null) {
                doctorKpiRecordRepository.save(new DoctorKpiRecord(
                        docOwner, LocalDate.now(), currentMonth,
                        30, 15, 10, 12, 8, 380000000.0, 98.0
                ));
            }
        }

        // 21. Khởi tạo Hồ Sơ Tiếp Nhận Bệnh Nhân Hiện Trường (FieldPatientIntake)
        if (fieldPatientIntakeRepository.count() == 0) {
            FieldPatientIntake lead1 = new FieldPatientIntake(
                    "FLD-2026-0001", "Chương Trình Nụ Cười Học Đường 2026 - THCS Lê Quý Đôn",
                    FieldEventType.SCHOOL_SCREENING, "Nguyễn Hoàng Minh", "0987654321",
                    "Sâu răng hàm số 36, khớp cắn hở nhẹ hàm trên",
                    "Cần hàn răng sâu sớm, tái khám chỉnh nha học đường", "HOCDUONG100K"
            );
            lead1.setBirthYear(2013);
            lead1.setStudentClass("7A1");
            lead1.setParentName("Nguyễn Văn Tuấn");
            lead1.setParentPhone("0987654321");
            fieldPatientIntakeRepository.save(lead1);

            FieldPatientIntake lead2 = new FieldPatientIntake(
                    "FLD-2026-0002", "Hội Nghị Nha Khoa Quốc Tế VIDEC 2026",
                    FieldEventType.DENTAL_CONFERENCE, "Lê Thu Trang", "0912998877",
                    "Màu răng xỉn ố do nhiễm tetracycline nhẹ",
                    "Tư vấn dán sứ Veneer Emax hoặc tẩy trắng răng", "VIDEC2026"
            );
            fieldPatientIntakeRepository.save(lead2);
        }

        System.out.println("✅ Sample Dental Clinic Data, Articles, Products, Warranties, Staff & Operations Initialized successfully!");
    }
}
