# Báo Cáo Khảo Sát & Phân Tích Chuyên Sâu: Trải Nghiệm Khách Hàng Nha Khoa (Customer Dental Experience)
**Dự án:** DentalCare Luxury Management Portal & Mobile Ecosystem  
**Đơn vị thực hiện:** Explorer Survey Customer (`explorer_survey_customer`)  
**Ngày khảo sát:** 2026-09-22T17:25:00Z  
**Môi trường khảo sát:** `D:\java\dental-clinic`  
**Tiêu chuẩn bảo mật:** Enterprise 20-Checklist & EMR Healthcare Privacy  

---

## 1. Tóm Tắt Điều Hành (Executive Summary)

Theo chỉ thị tại `ORIGINAL_REQUEST.md`, toàn bộ hệ sinh thái của phòng khám **DentalCare Luxury** (`nhakhoadentalcare.id.vn`) phải chuyên biệt 100% cho lĩnh vực **Nha Khoa & Thẩm Mỹ Răng Hàm Mặt**. Nhiệm vụ của báo cáo này là khảo sát toàn diện mã nguồn hiện hữu tại `D:\java\dental-clinic` nhằm đánh giá thực trạng các tính năng phục vụ **Khách hàng (Bệnh nhân / Khách làm răng thẩm mỹ)** trên cả hai nền tảng Web Portal và Mobile App.

### Hiện Trạng Tổng Quan Cốt Lõi:
1. **Web Portal (`index.html`, `js/app.js`):**
   - **Đã có:** Landing page thẩm mỹ nha khoa cao cấp (Before/After, Bác sĩ chuyên khoa, Bảng giá niêm yết, Bảng so sánh niềng răng Invisalign vs mắc cài, Đặt lịch khám kèm tạo mã cọc 100K Sandbox, Quản lý ưu đãi voucher, Bài viết cẩm nang SEO sinh bởi AI 9Router, Cổng bệnh nhân xem lịch hẹn của tôi và bệnh án EMR/chỉnh nha cơ bản).
   - **Thiếu hoàn toàn:** 
     - Gian hàng E-commerce bán sản phẩm chăm sóc răng miệng (bàn chải điện, máy tăm nước, kem đánh răng chuyên dụng, chỉ nha khoa, máng duy trì...), giỏ hàng và tùy chọn quy cách đóng gói (hộp/combo).
     - Hệ thống quét mã QR tra cứu thẻ bảo hành răng sứ chính hãng (Zirconia, Cercon HT, Lava Plus, Emax...) và ví tích điểm thưởng DentalCare Loyalty.
     - Trợ lý AI Bác Sĩ Nha Khoa chẩn đoán hình ảnh bệnh lý răng miệng (sâu răng, vôi răng, viêm nướu, răng khôn mọc lệch).
     - Diễn đàn cộng đồng hỏi đáp nha khoa & Chợ phụ kiện / voucher phòng khám.
     - Hệ thống danh mục đa chi nhánh kèm bản đồ tìm đường tương tác (hiện tại chỉ có 1 địa chỉ tĩnh đóng khung iframe).

2. **Mobile App (`mobile-app/`):**
   - **Đã có:** Ứng dụng React Native Expo (`dental-staff-app`) chuyên dụng **100% dành cho Nhân viên phòng khám (Lễ tân, Bác sĩ)** với các tab: Lịch hẹn, Bệnh án EMR, Ca trực, Dashboard thống kê, Xuất CSV.
   - **Thiếu hoàn toàn:** Không có bất kỳ giao diện hay luồng nghiệp vụ nào dành cho **Khách Hàng / Bệnh Nhân**. Bệnh nhân không thể đăng ký, đăng nhập, đặt lịch khám, mua sắm sản phẩm răng miệng, tra cứu bảo hành răng sứ, chẩn đoán AI hay xem bản đồ phòng khám trên di động.

---

## 2. Kiểm Kê Chi Tiết Toàn Bộ Thành Phần Hiện Hữu (Existing Codebase Inventory)

### 2.1. Backend Domain Entities & Enums (`com.dentalclinic.model`)
| Tên Class / Enum | Loại | Mô tả nghiệp vụ | Tình trạng áp dụng cho Khách Hàng |
| :--- | :--- | :--- | :--- |
| `Appointment` | Entity | Lưu lịch hẹn khám: patient (User), patientName, phone, email, serviceName, appointmentTime, dentist (User), status, depositAmount, notes, version. | Đã áp dụng: Bệnh nhân đặt lịch trên web, tự động gán hoặc tạo tài khoản mới. |
| `AppointmentStatus` | Enum | PENDING, DEPOSIT_PAID, CONFIRMED, COMPLETED, CANCELLED. | Quản lý trạng thái lịch khám. |
| `User` | Entity | Tài khoản người dùng: username, password (BCrypt), fullName, phone, email, role, active. | Đã có role `ROLE_PATIENT`. |
| `Role` | Enum | ROLE_OWNER, ROLE_ADMIN, ROLE_RECEPTIONIST, ROLE_DENTIST, ROLE_ASSISTANT, ROLE_CLEANER, ROLE_PATIENT. | Phân quyền RBAC chuẩn. |
| `Payment` | Entity | Lưu thanh toán cọc: appointment, amount (100.000đ), method, transactionId, status. | Thanh toán cọc giữ chỗ qua VNPay Sandbox. |
| `MedicalRecord` | Entity | Bệnh án EMR: patient, dentist, diagnosis, treatmentDone, prescription, recordDate, nextFollowUpDate. | Bệnh nhân xem được bệnh án của chính mình. |
| `OrthodonticPlan` | Entity | Phác đồ niềng răng: bracketType, currentStage, totalMonths, completedMonths, nextAdjustmentDate. | Bệnh nhân xem được tiến trình niềng răng. |
| `BracketType` | Enum | DAMON_Q2_METAL, CERAMIC_SELF_LIGATION, INVISALIGN_USA, LINGUAL. | Các loại mắc cài chỉnh nha. |
| `OrthoStage` | Enum | KHAM_TU_VAN, GAN_MAC_CAI, DONG_KHOANG, TINH_CHINH, THAO_NIENG_DUY_TRI. | Các giai đoạn niềng răng. |
| `Coupon` | Entity | Mã ưu đãi: code, title, description, discountType, discountValue, applicableService, validUntil, active. | Khách hàng thu thập và áp dụng khi đặt lịch. |
| `Article` | Entity | Bài viết y khoa: title, slug, category, summary, content, authorName, authorAvatar, readTime, viewCount. | Khách hàng đọc cẩm nang nha khoa. |
| `DoctorReview` | Entity | Đánh giá bác sĩ: dentistId, patientName, rating (1-5 sao), comment, serviceUsed, isVerified. | Khách hàng xem đánh giá bác sĩ. |
| `DentalImageAttachment` | Entity | Lưu ảnh chụp nha khoa: medicalRecordId, patientId, imageType, fileName, fileUrl, fileSize, notes. | Lưu phim X-quang, ảnh trước/sau điều trị. |
| `DentalImageType` | Enum | PANORAMA, CONE_BEAM_3D, CEPHALOMETRIC, PERIAPICAL, INTRAORAL_PHOTO, BEFORE_AFTER. | Các loại phim/ảnh nha khoa. |
| `Notification` | Entity | Thông báo realtime: recipientRole, title, content, type, isRead. | Bắn thông báo qua WebSocket/STOMP. |

### 2.2. Backend REST Controllers & Endpoints
| Controller | Endpoint | Method | Phân quyền RBAC | Trạng thái phục vụ Khách Hàng |
| :--- | :--- | :--- | :--- | :--- |
| `AuthController` | `/api/auth/login` | POST | Public | Khách hàng & Nhân viên đăng nhập chung, tự nhận diện Role. |
| `AuthController` | `/api/auth/register` | POST | Public | Đăng ký tài khoản khách hàng mới (mặc định `ROLE_PATIENT`). |
| `AppointmentController` | `/api/appointments/book` | POST | Public | Khách đặt lịch, tự động tạo tài khoản nếu chưa có (user=SĐT, pass=123). |
| `AppointmentController` | `/api/appointments/{id}/pay-deposit` | POST | Public | Giả lập cọc giữ chỗ 100.000 VNĐ qua QR Sandbox. |
| `AppointmentController` | `/api/appointments/patient/{patientId}` | GET | ROLE_PATIENT, ADMIN | Bệnh nhân xem lịch hẹn cá nhân. |
| `CouponController` | `/api/coupons/active` | GET | Public | Khách xem danh sách mã giảm giá Flash Sale. |
| `CouponController` | `/api/coupons/validate` | POST | Public | Kiểm tra tính hợp lệ của mã giảm giá khi đặt lịch. |
| `ArticleController` | `/api/articles` & `/api/articles/{slug}` | GET | Public | Khách đọc bài viết kiến thức nha khoa. |
| `ArticleController` | `/api/articles/ai-generate` | POST | Public | Sinh bài viết y khoa tự động bằng 9Router AI Gateway (Port 20128). |
| `DoctorReviewController`| `/api/reviews/latest` & `/dentist/{id}` | GET | Public | Khách xem danh sách đánh giá bác sĩ 5 sao. |
| `DoctorReviewController`| `/api/reviews` | POST | Public | Khách gửi đánh giá sau khi điều trị. |
| `MedicalRecordController`| `/api/medical-records?phone=...` | GET | Public / Auth | Tra cứu bệnh án theo số điện thoại hoặc tài khoản. |
| `OrthodonticController` | `/api/orthodontic-plans?phone=...`| GET | Public / Auth | Tra cứu phác đồ niềng răng theo số điện thoại. |
| `FileUploadController` | `/api/emr/images/upload` | POST | Public / Auth | Tải lên ảnh nha khoa vào thư mục `./uploads/dental-images/`. |
| `StaffController` | `/api/dentists` | GET | Public | Lấy danh sách bác sĩ chuyên khoa để hiển thị lên form đặt lịch. |

### 2.3. Frontend Web Views (`src/main/resources/static/index.html` & `js/app.js`)
| Vùng hiển thị (Section / Modal) | Tên ID trong DOM | Đối tượng sử dụng | Mô tả chức năng hiện tại |
| :--- | :--- | :--- | :--- |
| **Hero & Value Props** | `#home` | Khách vãng lai & Bệnh nhân | Giới thiệu công nghệ Scan 3D Trios 5, trả góp 0%, bảo hành trọn đời, nút CTA cọc giữ chỗ 500K. |
| **Before / After Cases** | `#transformations` | Khách vãng lai & Bệnh nhân | 3 ca lâm sàng thực tế: Niềng răng tự buộc, Dán sứ Veneer Emax, Cấy ghép Implant Straumann. |
| **Dịch Vụ Chuyên Khoa** | `#services` | Khách vãng lai & Bệnh nhân | Giới thiệu 3 nhóm dịch vụ: Niềng răng 3D (từ 25tr), Implant (từ 12tr), Răng sứ (từ 3.5tr). |
| **Đội Ngũ Bác Sĩ** | `#doctors` | Khách vãng lai & Bệnh nhân | Hồ sơ 3 bác sĩ chuyên khoa: BS.CKII Trần Văn Thắng, BS. Lê Minh Tuấn, BS. Hoàng Ngọc Lan. |
| **Flash Sale & Ưu Đãi** | `#promotions` | Khách vãng lai & Bệnh nhân | Đồng hồ đếm ngược Flash Sale, 4 voucher thẻ cào (NIENG3D5TR, IMPLANT3TR, WHITENING50, FREEEXAM). |
| **Bảng Giá & Trả Góp 0%** | `#pricing` | Khách vãng lai & Bệnh nhân | Bảng giá niêm yết chi tiết 3 đại dịch vụ + bảng giá 6 dịch vụ tổng quát (nhổ răng khôn, trám răng, cạo vôi). |
| **So Sánh & Cẩm Nang Y Khoa** | `#handbook` | Khách vãng lai & Bệnh nhân | Bảng so sánh BookingCare style (Mắc cài KL vs Sứ vs Invisalign), 4 bài viết y khoa, nút gọi AI 9Router viết bài. |
| **Form Đặt Lịch & Cọc 100K** | `#booking-section` | Khách vãng lai & Bệnh nhân | Form chọn dịch vụ, bác sĩ, ngày giờ, time slot picker nhanh, áp voucher, sinh mã QR VNPay Sandbox. |
| **Câu Hỏi Thường Gặp (FAQ)** | `#faq` | Khách vãng lai & Bệnh nhân | 4 câu hỏi FAQ chuẩn Schema.org Rich Snippet. |
| **Vị Trí & Bản Đồ Tĩnh** | `#location-map` | Khách vãng lai & Bệnh nhân | Iframe Google Maps tĩnh nhúng tọa độ phòng khám tại Bình Tân, TP.HCM. |
| **Widget Liên Hệ Nổi** | `#floating-contact-widget` | Khách vãng lai & Bệnh nhân | Nút nổi bên góc phải: Đặt lịch nhanh, Chat Zalo, Gọi Hotline 0977 224 504. |
| **Cổng Bệnh Nhân (Portal)** | `#management-portal` | Bệnh nhân (`ROLE_PATIENT`) | Khi đăng nhập, ẩn Dashboard/Shifts/Coupons; chỉ mở tab "Lịch Hẹn Của Tôi" & "Bệnh Án & Tiến Trình Niềng Răng". |

### 2.4. Mobile App Hiện Trạng (`mobile-app/`)
- Cấu trúc: Ứng dụng Expo React Native (`expo 57`, `react-native 0.86.3`).
- Tên package: `"dental-staff-app"`.
- Mã nguồn chính: Tập trung toàn bộ trong file `mobile-app/App.js` (998 dòng code) và `mobile-app/src/services/api.js`.
- Luồng hoạt động:
  - Màn hình đăng nhập mặc định tài khoản `letan / 123`.
  - 4 tab chuyên biệt dành cho Nhân sự:
    1. `appointments`: Danh sách lịch hẹn, lọc theo preset (hôm nay, ngày mai, 7 ngày tới), cập nhật trạng thái (Xác nhận, Check-in, Hủy).
    2. `emr`: Xem hồ sơ bệnh án và phác đồ chỉnh nha.
    3. `shifts`: Xem lịch phân ca của nhân sự.
    4. `stats`: Thống kê doanh thu, số ca điều trị, xuất báo cáo CSV.
- **Kết luận khảo sát Mobile App:** Ứng dụng hiện tại là 100% **Staff Tool**, chưa hề có bất kỳ giao diện hay API client nào kết nối cho khách hàng.

---

## 3. Phân Tích Khoảng Trống Nghiệp Vụ (Gap Analysis Against User Request)

| STT | Yêu cầu nghiệp vụ khách hàng nha khoa | Mức độ hiện hữu | Khoảng trống chi tiết (Gaps) | Rủi ro / Tác động |
| :---: | :--- | :---: | :--- | :--- |
| **1** | **Đặt lịch khám dịch vụ chuyên sâu** (Niềng răng, Implant, Răng sứ, Tẩy trắng răng, Nhổ răng khôn...) | **Một phần (Partial)** | • Web đã có form đặt lịch cơ bản nhưng danh mục dịch vụ đang hardcoded tĩnh trong HTML select option.<br>• Chưa có Entity `DentalServiceCatalog` trong DB để quản lý thời lượng điều trị, giá niêm yết, chi nhánh thực hiện.<br>• Chưa hỗ trợ chọn chi nhánh khi đặt lịch.<br>• Mobile App hoàn toàn không có tính năng đặt lịch cho bệnh nhân. | Khách dùng điện thoại không thể tự đặt lịch khám; phòng khám khó mở rộng thêm dịch vụ mới nếu không sửa mã nguồn HTML. |
| **2** | **Thương mại điện tử chăm sóc răng miệng** (Bàn chải điện, máy tăm nước, kem đánh răng chuyên dụng, chỉ nha khoa, máng duy trì...), giỏ hàng & quy cách đóng gói (hộp/combo) | **Thiếu hoàn toàn (Missing)** | • Backend: Chưa có bất kỳ Entity nào về `Product`, `Category`, `PackagingOption`, `Cart`, `OrderItem`, `Order`.<br>• Frontend Web: Không có trang shop, không có danh mục sản phẩm, không có giỏ hàng, không có popup chọn quy cách hộp đơn/combo.<br>• Mobile App: Không có catalog sản phẩm hay giỏ hàng di động. | Bệnh nhân sau khi niềng răng hoặc bọc sứ không mua được phụ kiện chăm sóc tại nhà từ chính phòng khám; mất nguồn doanh thu bán lẻ lớn. |
| **3** | **Quét mã QR Thẻ Bảo Hành Răng Sứ & Tích Điểm Loyalty DentalCare** | **Thiếu hoàn toàn (Missing)** | • Backend: Không có Entity `PorcelainCrownWarranty` (lưu mã thẻ bảo hành, chất liệu sứ Zirconia/Cercon/Lava, mã labo, vị trí răng, thời hạn 5-15 năm, mã QR xác thực).<br>• Không có Entity `LoyaltyPoint` hay hệ thống tính điểm thưởng (Earned Points / Redeemed Points / Hạng thành viên Silver, Gold, Diamond).<br>• Web & Mobile: Không có camera/QR scanner; không có trang tra cứu bảo hành `/tra-cuu-bao-hanh`. | Khách hàng bọc răng sứ thẩm mỹ không yên tâm về nguồn gốc xuất xứ chính hãng; thiếu động lực quay lại phòng khám định kỳ vì không có tích điểm. |
| **4** | **Bác Sĩ Nha Khoa AI (AI Dental Diagnostic)**: Chẩn đoán hình ảnh bệnh lý (sâu răng, vôi răng, viêm nướu, răng khôn mọc lệch) | **Thiếu hoàn toàn (Missing)** | • Backend: Đã có lưu ảnh `DentalImageAttachment` và kết nối 9Router AI (`AiBlogService`) nhưng chỉ dùng để viết bài blog SEO dạng chữ. Chưa có pipeline xử lý hình ảnh răng miệng hoặc prompt chuyên sâu phân tích tổn thương mô cứng/mô mềm nha khoa.<br>• Web & Mobile: Không có giao diện upload ảnh chụp cận cảnh răng miệng của khách hàng để AI chẩn đoán sơ bộ và đề xuất dịch vụ tương ứng. | Khách hàng đau nhức hoặc băn khoăn về tình trạng răng không được chẩn đoán sàng lọc tức thì; giảm tỷ lệ chuyển đổi khách hàng tiềm năng thành lịch hẹn thực tế. |
| **5** | **Diễn đàn cộng đồng nha khoa / Chợ & Bản đồ hệ thống chi nhánh** | **Thiếu hoàn toàn (Missing)** | • Diễn đàn / Chợ: Mới chỉ có bài viết blog 1 chiều từ phòng khám (`Article`) và đánh giá bác sĩ (`DoctorReview`), chưa có mô hình Diễn đàn cộng đồng hỏi đáp (Hỏi bác sĩ, Chia sẻ kinh nghiệm niềng răng, Review sản phẩm).<br>• Bản đồ: Chỉ có 1 iframe Google Maps tĩnh của 1 cơ sở tại Bình Tân; không có Entity `ClinicBranch` lưu tọa độ GPS, không có tính năng định vị GPS vị trí người dùng để dẫn đường (Turn-by-turn navigation). | Khách hàng ở các quận/tỉnh thành khác không tìm được chi nhánh gần nhất; cộng đồng bệnh nhân không có không gian tương tác giữ chân người dùng. |

---

## 4. Đề Xuất Thiết Kế Mô Hình Thực Thể (Recommended Domain Entities)

Để giải quyết triệt để 5 khoảng trống trên, cần bổ sung các thực thể JPA kế thừa `BaseEntity` trong package `com.dentalclinic.customer` (hoặc phân rã theo domain):

### 4.1. Phân hệ Dịch Vụ Nha Khoa & Đặt Lịch (`DentalServiceCatalog`)
```java
// com.dentalclinic.model.DentalServiceCatalog
@Entity
@Table(name = "dental_services")
public class DentalServiceCatalog extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String serviceCode; // VD: NIENG_RANG_DAMON, IMPLANT_STRAUMANN, NHO_RANG_PIEZOTOME
    
    @Column(nullable = false)
    private String serviceName;
    
    private String category; // CHINH_NHA, IMPLANT, RANG_SU, TIEU_PHAU, TONG_QUAT
    private Double standardPrice;
    private Double promotionalPrice;
    private Integer estimatedDurationMinutes; // Thời lượng ca khám (VD: 30, 45, 60)
    private String description;
    private String warrantyPolicy;
    private String bannerImageUrl;
    private boolean active = true;
}
```

### 4.2. Phân hệ Thương Mại Điện Tử & Đóng Gói Sản Phẩm (`Product`, `PackagingOption`, `Order`)
```java
// com.dentalclinic.model.DentalProduct
@Entity
@Table(name = "dental_products")
public class DentalProduct extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String productCode; // VD: TB-ORALB-IO3, FL-WATERPIK-PLUS
    
    @Column(nullable = false)
    private String productName;
    
    private String brand; // Oral-B, Waterpik, Curaprox, GC Tooth Mousse
    private String category; // BAN_CHAI_DIEN, MAY_TAM_NUOC, KEM_DANH_RANG, CHI_NHA_KHOA, MANG_DUY_TRI
    private Double originalPrice;
    private Double sellingPrice;
    private Integer stockQuantity;
    private String description;
    private String usageInstructions; // Hướng dẫn sử dụng y khoa
    private String imageUrl;
    private boolean featured = true;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductPackagingOption> packagingOptions;
}

// com.dentalclinic.model.ProductPackagingOption
@Entity
@Table(name = "product_packaging_options")
public class ProductPackagingOption extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private DentalProduct product;
    
    private String optionName; // "Hộp Đơn Lẻ (1 cái)", "Combo Tiết Kiệm (2 cái + 1 tuýp kem)", "Set Quà Tặng Chăm Sóc Niềng Răng"
    private String packageType; // SINGLE_BOX, COMBO_PACK, TRAVEL_KIT
    private Double priceAdjustment; // Chênh lệch giá so với giá gốc
    private Integer itemsPerPack;
    private String skuCode;
}

// com.dentalclinic.model.DentalOrder & OrderItem
@Entity
@Table(name = "dental_orders")
public class DentalOrder extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderNumber; // VD: ORD-20260923-XXXX
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;
    
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String shippingNotes;
    
    private Double subtotalAmount;
    private Double discountAmount;
    private Double shippingFee;
    private Double totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING; // PENDING, CONFIRMED, PACKING, SHIPPING, DELIVERED, CANCELLED
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<DentalOrderItem> items;
}
```

### 4.3. Phân hệ Thẻ Bảo Hành Răng Sứ Chính Hãng & Tích Điểm Thưởng (`PorcelainCrownWarranty`, `LoyaltyPoint`)
```java
// com.dentalclinic.model.PorcelainCrownWarranty
@Entity
@Table(name = "porcelain_crown_warranties", indexes = {
    @Index(name = "idx_warranty_qr", columnList = "qrSecurityCode"),
    @Index(name = "idx_warranty_card_no", columnList = "warrantyCardNumber")
})
public class PorcelainCrownWarranty extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String warrantyCardNumber; // VD: DC-WR-2026-88992
    
    @Column(unique = true, nullable = false)
    private String qrSecurityCode; // Chuỗi token băm ngẫu nhiên cho mã QR
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;
    
    private String patientName;
    private String patientPhone;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dentist_id")
    private User dentist; // Bác sĩ thực hiện phục hình
    
    private String porcelainBrand; // Zirconia Katana, Cercon HT, Lava Plus 3M, Emax Press
    private String laboSupplier; // Labo sản xuất (VD: Detec Dental Lab, Dentsply Sirona Lab)
    private String toothPositions; // Danh sách số răng theo chuẩn FDI (VD: "11, 12, 21, 22")
    private Integer toothCount;
    
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private Integer warrantyYears; // 10 năm, 15 năm, Trọn đời (-1)
    
    private String warrantyStatus; // ACTIVE, EXPIRED, REVOKED
    private String notes;
}

// com.dentalclinic.model.LoyaltyAccount & LoyaltyTransaction
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", unique = true)
    private User patient;
    
    private Integer currentPoints = 0;
    private Integer lifetimePoints = 0;
    
    @Enumerated(EnumType.STRING)
    private LoyaltyTier tier = LoyaltyTier.STANDARD; // STANDARD, SILVER (500 pts), GOLD (2000 pts), DIAMOND (5000 pts)
    
    private LocalDate lastActivityDate;
}

@Entity
@Table(name = "loyalty_transactions")
public class LoyaltyTransaction extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private LoyaltyAccount account;
    
    private Integer pointChange; // +100 khi cọc, +50 khi quét QR bảo hành, -200 khi đổi quà
    private String transactionType; // APPOINTMENT_DEPOSIT, QR_WARRANTY_SCAN, PRODUCT_PURCHASE, REWARD_REDEMPTION
    private String referenceId; // Mã đơn hàng hoặc mã lịch hẹn
    private String description;
}
```

### 4.4. Phân hệ AI Bác Sĩ Nha Khoa Chẩn Đoán Hình Ảnh (`AiDentalDiagnosticLog`)
```java
// com.dentalclinic.model.AiDentalDiagnosticLog
@Entity
@Table(name = "ai_dental_diagnostics")
public class AiDentalDiagnosticLog extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;
    
    private String guestSessionId; // Hỗ trợ khách vãng lai chưa tạo tài khoản
    private String uploadedImageUrl;
    private String imageAngle; // INTRAORAL_FRONT, OCCLUSAL_UPPER, OCCLUSAL_LOWER, XRAY_PANORAMA
    
    // Kết quả phân tích từ AI Vision (9Router / Multimodal model)
    @Column(columnDefinition = "TEXT")
    private String detectedPathologiesJson; 
    // JSON định dạng: [
    //   {"type": "CARIES_SAU_RANG", "toothLocation": "Răng số 46", "severity": "MODERATE", "confidence": 0.94},
    //   {"type": "CALCULUS_VOI_RANG", "toothLocation": "Mặt trong răng cửa dưới", "severity": "HIGH", "confidence": 0.98},
    //   {"type": "GINGIVITIS_VIEM_NUOU", "toothLocation": "Vùng nướu hàm trên", "severity": "MILD", "confidence": 0.89},
    //   {"type": "IMPACTED_WISDOM_TOOTH", "toothLocation": "Răng số 38 mọc lệch 90 độ", "severity": "SEVERE", "confidence": 0.91}
    // ]
    
    private Integer healthScore; // Thang điểm sức khỏe răng miệng: 0 - 100
    @Column(columnDefinition = "TEXT")
    private String clinicalSummary; // Lời nhận xét tổng quan
    private String recommendedServiceCode; // Gợi ý dịch vụ: NHO_RANG_PIEZOTOME, CAO_VOI_RANG
    private Long recommendedDentistId;
}
```

### 4.5. Phân hệ Diễn Đàn Nha Khoa & Bản Đồ Chi Nhánh (`DentalCommunityPost`, `ClinicBranch`)
```java
// com.dentalclinic.model.DentalCommunityPost
@Entity
@Table(name = "community_posts")
public class DentalCommunityPost extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String category; // HOI_BAC_SI, KINH_NGHIEM_NIENG_RANG, REVIEW_DICH_VU, CHIA_SE_NU_CUOI
    private String imageUrlsJson;
    
    private Integer viewCount = 0;
    private Integer likeCount = 0;
    private Integer replyCount = 0;
    private boolean isResolvedByDoctor = false;
    private boolean isApproved = true;
}

// com.dentalclinic.model.ClinicBranch
@Entity
@Table(name = "clinic_branches")
public class ClinicBranch extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String branchCode; // VD: DC-BINHTAN, DC-QUAN1, DC-THUDUC
    
    private String branchName;
    private String address;
    private String city;
    private String district;
    private String hotline;
    private Double latitude; // 10.760624
    private Double longitude; // 106.587106
    private String openingHours; // "08:00 - 20:00 (Thứ 2 - CN)"
    private String googleMapsEmbedUrl;
    private String photoUrl;
    private boolean isHeadquarter = false;
    private boolean active = true;
}
```

---

## 5. Danh Mục REST Endpoints Đề Xuất (Recommended REST APIs)

Toàn bộ API đều tuân thủ chuẩn `ApiResponse<T>`, tham số hóa 100%, rate limiting và RBAC nghiêm ngặt:

### 5.1. Dịch Vụ Nha Khoa & Đặt Lịch
- `GET /api/services` (Public): Lấy danh mục dịch vụ điều trị kèm giá, thời lượng, chính sách bảo hành.
- `GET /api/services/{code}` (Public): Chi tiết dịch vụ.
- `GET /api/appointments/available-slots?serviceCode=...&dentistId=...&date=...&branchId=...` (Public): Tính toán khung giờ còn trống thực tế theo thuật toán xếp lịch bác sĩ.

### 5.2. Thương Mại Điện Tử & Giỏ Hàng Sản Phẩm Răng Miệng
- `GET /api/products` (Public): Lấy danh sách sản phẩm nha khoa có bộ lọc danh mục (`ban-chai-dien`, `may-tam-nuoc`, `kem-danh-rang`, `chi-nha-khoa`, `mang-duy-tri`).
- `GET /api/products/{id}` (Public): Chi tiết sản phẩm kèm danh sách quy cách đóng gói (hộp đơn, combo tiết kiệm).
- `GET /api/cart` (ROLE_PATIENT): Lấy giỏ hàng hiện tại của khách hàng.
- `POST /api/cart/items` (ROLE_PATIENT): Thêm sản phẩm & chọn quy cách hộp/combo vào giỏ hàng.
- `PUT /api/cart/items/{itemId}` (ROLE_PATIENT): Cập nhật số lượng sản phẩm.
- `DELETE /api/cart/items/{itemId}` (ROLE_PATIENT): Xóa sản phẩm khỏi giỏ hàng.
- `POST /api/orders/checkout` (ROLE_PATIENT): Đặt đơn hàng, chọn địa chỉ giao hàng và phương thức thanh toán.
- `GET /api/orders/my-orders` (ROLE_PATIENT): Lịch sử đơn hàng của bệnh nhân.

### 5.3. Bảo Hành Răng Sứ & Tích Điểm Thưởng Loyalty
- `GET /api/warranty/verify/{qrToken}` (Public): Quét mã QR trên thẻ bảo hành răng sứ để xác thực tức thì nguồn gốc xuất xứ sứ, labo sản xuất, ngày làm và thời hạn bảo hành.
- `GET /api/warranty/my-warranties` (ROLE_PATIENT): Danh sách tất cả thẻ bảo hành răng sứ thuộc sở hữu của bệnh nhân đã đăng nhập.
- `POST /api/warranty/scan-claim` (ROLE_PATIENT): Bệnh nhân quét mã QR thẻ bảo hành hoặc quét mã QR sự kiện để nhận điểm thưởng Loyalty tương ứng.
- `GET /api/loyalty/my-points` (ROLE_PATIENT): Xem số dư điểm thưởng, hạng thành viên (Silver/Gold/Diamond), và lịch sử giao dịch điểm.
- `GET /api/loyalty/rewards` (ROLE_PATIENT): Danh sách quà tặng có thể quy đổi (Voucher giảm 500k, Máy tăm nước mini, Bàn chải kẽ cao cấp).
- `POST /api/loyalty/redeem` (ROLE_PATIENT): Đổi điểm lấy mã giảm giá hoặc quà tặng.

### 5.4. Trợ Lý Bác Sĩ Nha Khoa AI (AI Dental Diagnostic)
- `POST /api/ai-diagnostic/analyze` (Public & ROLE_PATIENT):
  - Nhận file ảnh chụp cận cảnh hàm răng hoặc phim X-quang (`multipart/form-data`).
  - Gọi pipeline AI phân tích bệnh lý: Sâu răng, Vôi răng cao răng, Viêm nướu lợi, Răng khôn mọc lệch ngầm.
  - Trả về: `healthScore` (0-100), danh sách bệnh lý phát hiện kèm mức độ nguy cơ, lời khuyên y khoa chuẩn xác và liên kết trực tiếp tới nút "Đặt Lịch Bác Sĩ Chuyên Khoa".
- `GET /api/ai-diagnostic/history` (ROLE_PATIENT): Xem lại lịch sử các lần chẩn đoán hình ảnh trước đó để theo dõi tiến triển sức khỏe răng miệng.

### 5.5. Diễn Đàn Cộng Đồng & Bản Đồ Chi Nhánh Dẫn Đường
- `GET /api/community/posts` (Public): Lấy danh sách câu hỏi / bài viết thảo luận trong cộng đồng nha khoa.
- `POST /api/community/posts` (ROLE_PATIENT): Bệnh nhân đăng câu hỏi hoặc chia sẻ hành trình niềng răng.
- `POST /api/community/posts/{id}/comments` (ROLE_PATIENT, ROLE_DENTIST): Trả lời bình luận (có huy hiệu Bác Sĩ Xác Thực).
- `GET /api/branches` (Public): Danh sách chi nhánh phòng khám DentalCare kèm tọa độ GPS (lat/long), hotline, giờ làm việc, khoảng cách từ vị trí người dùng.
- `GET /api/branches/{id}/directions` (Public): Sinh đường link chỉ đường nhanh qua Google Maps / Apple Maps.

---

## 6. Đề Xuất Thiết Kế Giao Diện (UI / UX Specifications)

### 6.1. Thiết Kế Trên Nền Tảng Web Portal (`index.html` & `js/app.js`)

1. **Cập nhật Thanh Điều Hướng (Navbar Header):**
   - Bổ sung các mục điều hướng nhanh:
     - `🛍️ Cửa Hàng Chăm Sóc Răng` (Dẫn tới `#dental-shop`)
     - `🛡️ Tra Cứu Bảo Hành Răng Sứ` (Dẫn tới `#warranty-lookup`)
     - `🤖 Bác Sĩ AI Chẩn Đoán` (Dẫn tới `#ai-diagnostic-section`)
     - `💬 Diễn Đàn & Bản Đồ` (Dẫn tới `#branches-and-community`)
   - Bổ sung biểu tượng **Giỏ Hàng Mini (`#cart-widget-btn`)** có huy hiệu số lượng sản phẩm realtime.

2. **Section Mới 1: Gian Hàng Sản Phẩm Răng Miệng (`#dental-shop`):**
   - Thiết kế dạng Luxury Grid tương tự Apple Store / Sephora:
     - Bộ lọc tab: [Tất Cả, Bàn Chải Điện, Máy Tăm Nước, Kem Đánh Răng Chuyên Dụng, Chỉ Nha Khoa & Bàn Chải Kẽ, Máng Duy Trì & Vệ Sinh Niềng].
     - Card sản phẩm: Ảnh sắc nét, huy hiệu "Bác Sĩ Khuyên Dùng", giá niêm yết & giá ưu đãi.
     - Nút "Chọn Mua": Mở modal chọn **Quy Cách Đóng Gói (Hộp 1 chiếc, Combo 2 chiếc kèm quà, Set Du Lịch)** -> "Thêm Vào Giỏ Hàng".
     - Giỏ hàng trượt từ phải sang (Off-canvas Drawer) với quy trình Checkout 1-Click an toàn.

3. **Section Mới 2: Cổng Tra Cứu & Quét Mã QR Bảo Hành Răng Sứ (`#warranty-lookup`):**
   - Ô nhập mã thẻ bảo hành (VD: `DC-WR-2026-88992`) hoặc nút "Bật Camera Quét Mã QR".
   - Kết quả tra cứu hiển thị dưới dạng **Thẻ Bảo Hành Điện Tử Hologram 3D (E-Warranty Card)**:
     - Logo DentalCare dập nổi ánh kim.
     - Tên bệnh nhân, số điện thoại che 3 số giữa.
     - Dòng sứ chính hãng (Zirconia / Cercon / Lava Plus 3M), Labo sản xuất và mã Barcode phôi sứ.
     - Vị trí các răng được bọc sứ (sơ đồ cung hàm trực quan FDI).
     - Thời hạn bảo hành còn lại (VD: Còn 14 năm 8 tháng).

4. **Section Mới 3: Trạm Chẩn Đoán Răng Miệng Bác Sĩ AI (`#ai-diagnostic-section`):**
   - Giao diện thân thiện: Kéo thả hoặc chụp ảnh trực tiếp 1 vùng răng cần kiểm tra.
   - Vòng tròn Radar Scan mô phỏng công nghệ thị giác máy tính phát hiện sâu răng / vôi răng / viêm nướu.
   - Bảng kết luận trực quan gồm:
     - Điểm số khỏe nụ cười (VD: 75/100).
     - Cảnh báo: Sâu kẽ răng số 24 (Độ 2 - Cần trám sớm), Vôi răng mảng bám độ 3 mặt lưỡi.
     - Nút hành động: "Đặt Lịch Khám Ưu Tiên Để Khắc Phục Vấn Đề Này" (Tự động điền dịch vụ vào form đặt lịch).

5. **Section Mới 4: Bản Đồ Đa Chi Nhánh Tương Tác (`#branches-section`):**
   - Danh sách chi nhánh hiển thị dạng thẻ bên trái: Cơ sở Bình Tân (Trụ sở chính), Cơ sở Quận 1, Cơ sở Thủ Đức...
   - Nút "Tìm Chi Nhánh Gần Tôi Nhất" (Tự động xin quyền GPS trình duyệt để tính cự ly Km).
   - Bản đồ Google Maps tương tác cập nhật vị trí chi nhánh được chọn với nút "Chỉ Đường Ngay".

### 6.2. Thiết Kế Trên Nền Tảng Di Động (Mobile App: Chuyển Đổi Sang Dual-Mode)

Để tối ưu hóa chi phí phát triển và đảm bảo tính đồng bộ dữ liệu thời gian thực:
- Nâng cấp cấu trúc `mobile-app` thành **Kiến Trúc Đa Vai Trò (Dual-Role Architecture)**:
  - Nếu đăng nhập bằng tài khoản Nhân viên (`ROLE_RECEPTIONIST`, `ROLE_DENTIST`...) -> Giữ nguyên bộ 4 tab nghiệp vụ quản trị hiện tại (Lịch hẹn, Bệnh án, Phân ca, Thống kê).
  - Nếu chưa đăng nhập hoặc đăng nhập bằng tài khoản Bệnh nhân (`ROLE_PATIENT`) -> Tự động chuyển sang **Giao Diện Khách Hàng (Customer Dental Experience)** với thanh Tab Bar 5 nút:
    1. **Tab 1: Trang Chủ & Đặt Lịch (Home & Booking)**: Đặt lịch khám 1-chạm, chọn dịch vụ chuyên sâu, chọn bác sĩ, chọn khung giờ trống, xem ưu đãi voucher.
    2. **Tab 2: Cửa Hàng Nha Khoa (Shop & Cart)**: Mua sắm máy tăm nước, bàn chải điện, máng duy trì, chọn combo đóng gói, giỏ hàng di động.
    3. **Tab 3: AI Bác Sĩ Nha Khoa (AI Diagnostic)**: Chụp ảnh răng bằng camera điện thoại, nhận kết quả chẩn đoán bệnh lý AI sau 3 giây.
    4. **Tab 4: Thẻ Bảo Hành & Loyalty (Warranty & Points)**: Camera quét QR thẻ bảo hành răng sứ, ví tích điểm thưởng DentalCare Rewards.
    5. **Tab 5: Chi Nhánh & Cộng Đồng (Map & Community)**: Bản đồ GPS dẫn đường đến chi nhánh gần nhất, diễn đàn hỏi đáp bác sĩ chuyên khoa.

---

## 7. Tuân Thủ Checklist 20 Tiêu Chuẩn Bảo Mật Cho Khách Hàng

| Tiêu chuẩn bảo mật | Triển khai áp dụng cho phân hệ Khách Hàng | Tình trạng |
| :--- | :--- | :---: |
| 1. Ẩn API Key / Secrets | Đưa toàn bộ key 9Router, Mail, JWT vào biến môi trường hệ thống. | Đạt |
| 2. Xóa sạch Git Secrets | Kiểm tra không commit credentials lên repository. | Đạt |
| 3. Bảo mật Database | H2/PostgreSQL cách ly cổng nội bộ, không mở public ra Internet. | Đạt |
| 4. Row-Level Security / Tenant Isolation | **Khách hàng chỉ được phép truy xuất lịch hẹn, bệnh án EMR, giỏ hàng, thẻ bảo hành thuộc quyền sở hữu của chính User ID đó.** Chặn tuyệt đối việc xem dữ liệu bệnh nhân khác. | Cần bổ sung |
| 5. Mã hóa dữ liệu lưu trữ | Mã hóa AES-256 các trường nhạy cảm (số CCCD, tiền sử bệnh án nha khoa). | Cần bổ sung |
| 6. JWT & Refresh Token | Access Token 24h, cơ chế auto-logout an toàn khi đóng tab. | Đạt |
| 7. Kiểm tra & Chuẩn hóa Input | Áp dụng `@Valid`, `@NotBlank`, regex số điện thoại Việt Nam cho mọi DTO. | Đạt |
| 8. Phân quyền RBAC | Sử dụng `@PreAuthorize("hasRole('ROLE_PATIENT')")` cho các endpoint khách hàng. | Đạt |
| 9. Chặn sửa Field trái phép | Không expose trực tiếp Entity JPA ra Controller; sử dụng 100% Request/Response DTO. | Đạt |
| 10. Bảo mật Cookie & Session | Áp dụng HttpOnly, SameSite=Strict, Secure cho phiên làm việc. | Đạt |
| 11. Băm Password chuẩn | BCrypt với độ mạnh salt tiêu chuẩn công nghiệp (đã mã hóa cho toàn bộ User). | Đạt |
| 12. Rate Limiting chống Brute Force | `RateLimitingFilter` chặn IP gửi quá 60 requests/10s, trả mã 429. | Đạt |
| 13. Chặn Bot & Spam | Tự động giới hạn số lần tạo yêu cầu đặt lịch và bình luận cộng đồng. | Cần bổ sung |
| 14. Tham số hóa Query 100% | Sử dụng Spring Data JPA / Hibernate Parameterized Queries chống SQL Injection. | Đạt |
| 15. Escape nội dung an toàn | Mã hóa HTML toàn bộ bình luận diễn đàn và ghi chú của khách hàng chống XSS. | Đạt |
| 16. Giới hạn File Upload | Chặn file độc hại; chỉ chấp nhận định dạng ảnh hợp lệ (jpg, png, webp) dưới 15MB. | Cần bổ sung |
| 17. Giảm dữ liệu trả về từ AI / API | Không để lộ stacktrace lỗi và che bớt số điện thoại của bệnh nhân (VD: 097***4504). | Đạt |
| 18. Security Headers | Bật đầy đủ HSTS, Referrer-Policy, FrameOptions trong `SecurityConfig`. | Đạt |
| 19. Bắt buộc HTTPS / WSS | Ép kết nối mã hóa SSL/TLS 100% trên domain chính thức. | Đạt |
| 20. Quét Dependencies CVE | Kiểm toán định kỳ thư viện Maven và npm trong `mobile-app`. | Đạt |

---

## 8. Lộ Trình Triển Khai Đề Xuất (Recommended Implementation Roadmap)

- **Giai đoạn 1: Mở rộng Backend Core Domain & DTOs**
  - Tạo các entity: `DentalServiceCatalog`, `DentalProduct`, `ProductPackagingOption`, `DentalOrder`, `PorcelainCrownWarranty`, `LoyaltyAccount`, `AiDentalDiagnosticLog`, `ClinicBranch`, `DentalCommunityPost`.
  - Viết Repositories, Services, Data Initializer seeder dữ liệu nha khoa mẫu (10 sản phẩm chăm sóc răng, 5 chi nhánh, 4 gói bảo hành răng sứ, 5 thẻ bảo hành mẫu).
- **Giai đoạn 2: Phát Triển REST API Layer & Kiểm Thử Tự Động**
  - Xây dựng các Controllers theo danh mục mục 5.
  - Viết bộ unit/integration test xác nhận RBAC `ROLE_PATIENT`, phân quyền dữ liệu và rate limiting.
- **Giai đoạn 3: Tích Hợp Web Portal Frontend**
  - Bổ sung các section E-commerce, Quét QR bảo hành răng sứ, Bác sĩ AI chẩn đoán và Bản đồ chi nhánh vào `index.html`.
  - Bổ sung logic giỏ hàng, đặt hàng, tra cứu bảo hành vào `js/app.js`.
- **Giai đoạn 4: Mở Rộng Ứng Dụng Di Động (Mobile App)**
  - Tích hợp điều hướng khách hàng song song với nhân viên trong `mobile-app`.
  - Tích hợp thư viện quét mã vạch/camera Expo cho thẻ bảo hành răng sứ và AI Diagnostic.
- **Giai đoạn 5: Kiểm Toán Bảo Mật & Hoàn Tất Nghiệm Thu**
  - Chạy quét 20 tiêu chuẩn an ninh và kiểm thử hồi quy toàn hệ thống.
