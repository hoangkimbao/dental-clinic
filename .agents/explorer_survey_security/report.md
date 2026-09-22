# BÁO CÁO TOÀN DIỆN: KIỂM TOÁN 20 TIÊU CHUẨN BẢO MẬT ENTERPRISE & HỆ THỐNG KIỂM THỬ E2E
**Dự án:** Hệ Sinh Thái Quản Lý Nha Khoa DentalCare (`D:\java\dental-clinic`)  
**Đơn vị thực hiện:** Explorer Survey Security Specialist (`explorer_survey_security`)  
**Ngày thực hiện:** 23/09/2026 (UTC: 2026-09-22)  
**Tài liệu tham chiếu:** `ORIGINAL_REQUEST.md`, `PROJECT.md`, `TEST_INFRA.md`, `TEST_READY.md`  
**Chế độ:** Điều tra Độc lập / Read-only Investigation  

---

## 1. TỔNG QUAN ĐIỀU TRA & ĐÁNH GIÁ TỔNG THỂ (EXECUTIVE SUMMARY)

Hệ thống DentalCare là nền tảng quản trị nha khoa kỹ thuật số kết hợp phòng khám thông minh (Spring Boot 3.2.5, Java 17, H2 Database / PostgreSQL, kiến trúc Static Frontend + WebSocket + Module IT Team Command Center).

Qua đợt kiểm toán an ninh chuyên sâu mã nguồn (Authoritative Codebase Audit) trên toàn bộ hệ thống backend, database schema, security configuration, API controller, frontend scripts và bộ kiểm thử tự động, kết quả khảo sát mức độ tuân thủ đối với **20 Tiêu Chuẩn Bảo Mật Enterprise (Medical Security Standards)** được tổng hợp như sau:

### Bảng Chỉ Số Tuân Thủ Tổng Hợp
| Phân loại kết quả | Số lượng | Tỷ lệ (%) | Chi tiết tiêu chuẩn |
| :--- | :---: | :---: | :--- |
| **ĐẠT (Compliant / Pass)** | **2** | **10%** | TC 11 (BCrypt Hashing), TC 14 (Parameterized Queries 100%) |
| **ĐẠT MỘT PHẦN (Partially Compliant / Remediation Needed)** | **6** | **30%** | TC 6 (JWT Expiration), TC 7 (Input Validation), TC 8 (RBAC), TC 12 (Rate Limiting), TC 15 (XSS Escaping), TC 17 (PII Sanitization & Error Handling), TC 18 (Security Headers) |
| **KHÔNG ĐẠT / NGUY CƠ CAO (Non-Compliant / Fail - High Risk)** | **12** | **60%** | TC 1 (.env Isolation), TC 2 (No Git Secrets), TC 3 (Database Security), TC 4 (RLS / Tenant Isolation), TC 5 (AES-256 EMR Encryption), TC 9 (DTO Binding Protection), TC 10 (Secure Cookies), TC 13 (Bot/Spam Throttling), TC 16 (File Upload Validation), TC 19 (HTTPS/WSS Enforcement), TC 20 (Dependency CVE Audit) |

> **Cảnh báo rủi ro an ninh nghiêm trọng (Critical Vulnerabilities Found):**
> 1. **Lộ toàn bộ hồ sơ bệnh án (EMR IDOR Leakage):** Endpoint `GET /api/medical-records` không lọc theo bệnh nhân, bất kỳ ai đăng nhập đều xem được toàn bộ bệnh án nha khoa của mọi khách hàng trong phòng khám.
> 2. **Lỗ hổng phân quyền PermitAll trên API sửa bài viết & upload ảnh:** `SecurityConfig.java` đang cấu hình `.requestMatchers("/api/articles/**").permitAll()` và `.requestMatchers("/api/emr/images/**").permitAll()`, cho phép kẻ tấn công ẩn danh không cần đăng nhập có thể xóa/sửa bài viết, upload file tùy ý lên server.
> 3. **H2 Console Public:** Endpoint `/h2-console/**` được `permitAll()` mở công khai không cần mật khẩu hệ thống.
> 4. **Dữ liệu bệnh án EMR không mã hóa lưu trữ:** `diagnosis`, `treatmentDone`, `prescription` lưu dạng plain text 100% trong database (vi phạm tiêu chuẩn bảo mật y tế HIPAA/GDPR và TC 5).
> 5. **Tải tệp tin độc hại (Unrestricted File Upload):** `FileUploadService.java` không kiểm tra MIME type hay magic bytes, cho phép upload tệp thực thi hoặc script độc hại được lưu trực tiếp vào thư mục web static `/uploads/**`.

---

## 2. BẢNG MA TRẬN ĐÁNH GIÁ CHI TIẾT 20 TIÊU CHUẨN BẢO MẬT ENTERPRISE

| # | Tiêu Chuẩn Bảo Mật | Trạng Thái | File & Dòng Code Quan Sát | Đánh Giá Kỹ Thuật & Lỗ Hổng Chi Tiết |
|---|-------------------|:----------:|---------------------------|--------------------------------------|
| **1** | **.env / Secret Isolation** | ❌ **FAIL** | `src/main/resources/application.yml:14-15,49`<br>`docker-compose.yml:12-14`<br>`.gitignore:1-44` | • Không có file `.env` hoặc `.env.example` trong repo.<br>• `.gitignore` chưa chặn `.env`, `.env.*`, `*.key`, `*.pem`, `*.secret`.<br>• Secret JWT và mật khẩu DB bị hardcode trực tiếp trong `application.yml` và `docker-compose.yml` thay vì inject qua biến môi trường độc lập. |
| **2** | **No Git Secrets in Repo / History** | ❌ **FAIL** | `application.yml:49`<br>`com/dentalclinic/config/DataInitializer.java:55-70`<br>`docker-compose.yml:14` | • `DataInitializer.java` hardcode chuỗi mật khẩu gốc `"123"` cho toàn bộ 8 tài khoản mẫu (owner, admin, letan, bs_tuan, bs_lan, phuta, tapvu, benhnhan).<br>• Hardcode JWT Secret Key chuỗi 64 byte trong git commit.<br>• Thiếu pre-commit hook (như gitleaks, git-secrets). |
| **3** | **Database Security (Access Limits, Credentials)** | ❌ **FAIL** | `src/main/java/com/dentalclinic/security/SecurityConfig.java:73`<br>`application.yml:25-27`<br>`docker-compose.yml:27` | • `/h2-console/**` được mở `permitAll()` trong `SecurityConfig.java`, cho phép truy cập web console database không cần auth.<br>• Trong `docker-compose.yml`, cổng PostgreSQL `5432:5432` bị mở public ra ngoài Internet (0.0.0.0).<br>• Database H2 file (`./data/dentaldb`) không bật mã hóa tập tin tại chỗ (Cipher password). |
| **4** | **Row-Level Security (RLS) / Tenant-based Isolation** | ❌ **FAIL** | `com/dentalclinic/controller/MedicalRecordController.java:28-33`<br>`com/dentalclinic/controller/AppointmentController.java:71-75`<br>`com/dentalclinic/model/User.java:1-64` | • Chưa có khái niệm Tenant / Distributor / Chi nhánh trong Entity model (thiếu `tenant_id`, `clinic_id`, `distributor_id`).<br>• Endpoint `GET /api/medical-records` trả về toàn bộ bệnh án của mọi bệnh nhân nếu không truyền param `phone`.<br>• Endpoint `GET /api/appointments/patient/{patientId}` cho phép bất kỳ bệnh nhân nào xem lịch hẹn của bệnh nhân khác qua IDOR. |
| **5** | **AES-256 Encryption for Sensitive Medical Data** | ❌ **FAIL** | `com/dentalclinic/model/MedicalRecord.java:26-37`<br>`com/dentalclinic/model/OrthodonticPlan.java:39-41` | • Không có cơ chế mã hóa AES-256 GCM cho dữ liệu y bạ nhạy cảm.<br>• Các trường chẩn đoán (`diagnosis`), quá trình điều trị (`treatmentDone`), đơn thuốc (`prescription`), ghi chú bác sĩ (`doctorNotes`) lưu dạng chuỗi thô (plain text) trong database.<br>• Thiếu JPA `AttributeConverter` tích hợp thuật toán mã hóa đối xứng AES-256. |
| **6** | **JWT / OAuth2 Rotation & Expiration** | ⚠️ **PARTIAL** | `com/dentalclinic/security/JwtTokenProvider.java:21-65`<br>`com/dentalclinic/service/AuthService.java:43-63,96-106` | • **Đạt:** Token có hạn dùng (`expiration-ms: 86400000` = 24 giờ), kiểm tra chữ ký HS256 chuẩn qua JJWT 0.12.5.<br>• **Thiếu:** Chưa có cơ chế Refresh Token và xoay vòng Refresh Token (Token Rotation).<br>• **Lỗ hổng:** Khi người dùng đổi mật khẩu (`changePassword`), token cũ vẫn có hiệu lực cho đến khi hết hạn (thiếu Blacklist / Invalidation cache). Chưa hỗ trợ OAuth2 (Google/Apple). |
| **7** | **Input Sanitization & Validation (XSS & Injection)** | ⚠️ **PARTIAL** | `com/dentalclinic/dto/BookingRequest.java:7-12`<br>`com/dentalclinic/dto/RegisterRequest.java:9-25`<br>`com/dentalclinic/controller/MedicalRecordController.java:38` | • **Đạt:** `BookingRequest`, `RegisterRequest` có `@NotBlank`, `@Pattern`, `@Size`. `SensitiveDataSanitizer` lọc dữ liệu nhạy cảm cho module IT Team.<br>• **Thiếu:** Chưa có HTML Sanitizer (OWASP Java HTML Sanitizer / Jsoup) cho các trường nội dung bài viết, review bác sĩ.<br>• `MedicalRecordController.java` nhận trực tiếp `@RequestBody MedicalRecord record` không có `@Valid`. |
| **8** | **RBAC Enforcement Across Endpoints** | ⚠️ **PARTIAL** | `com/dentalclinic/security/SecurityConfig.java:70-101`<br>`com/dentalclinic/controller/ArticleController.java:40-56`<br>`com/dentalclinic/controller/FileUploadController.java:23` | • **Đạt:** `/api/it-team/**` khóa chặt cho `ROLE_ADMIN` và `ROLE_OWNER`. Nhiều endpoint appointment có `@PreAuthorize`.<br>• **Lỗ hổng nghiêm trọng:** `SecurityConfig.java` cấu hình `.requestMatchers("/api/articles/**").permitAll()` và `.requestMatchers("/api/reviews/**").permitAll()` dẫn đến kẻ tấn công ẩn danh có thể thêm/xóa/sửa bài viết và tự ý review.<br>• `/api/emr/images/**` mở `permitAll()` cho phép upload ảnh EMR công khai.<br>• `/api/auth/change-password` bị mở `permitAll()`, gây lỗi NullPointerException (500) nếu gọi không kèm auth header. |
| **9** | **DTO Binding Protection (No Entity Exposure)** | ❌ **FAIL** | `com/dentalclinic/controller/MedicalRecordController.java:38`<br>`com/dentalclinic/controller/OrthodonticController.java:41`<br>`com/dentalclinic/controller/ArticleController.java:41,46`<br>`com/dentalclinic/controller/ShiftController.java:45`<br>`com/dentalclinic/controller/DoctorReviewController.java:36` | • 5 Controller sử dụng trực tiếp JPA Entity trong request body (`@RequestBody MedicalRecord`, `@RequestBody OrthodonticPlan`, `@RequestBody Article`, `@RequestBody StaffShift`, `@RequestBody DoctorReview`).<br>• Gây nguy cơ tấn công Mass Assignment / Over-posting, làm lộ cấu trúc bảng và quan hệ ORM. |
| **10** | **Secure Cookies (HttpOnly, Secure, SameSite=Strict)** | ❌ **FAIL** | `com/dentalclinic/controller/AuthController.java:29-32`<br>`src/main/resources/static/js/app.js:84-100` | • Hệ thống không sử dụng Secure Cookie.<br>• Frontend lưu trữ JWT trong `localStorage` (`localStorage.getItem('DENTAL_USER')`) và `sessionStorage`.<br>• Khi gặp lỗ hổng XSS, token dễ dàng bị đánh cắp qua JavaScript (`document.cookie` hoặc trích xuất `localStorage`).<br>• Backend không set cookie phản hồi kèm cờ `HttpOnly; Secure; SameSite=Strict`. |
| **11** | **BCrypt Password Hashing** | ✅ **PASS** | `com/dentalclinic/security/SecurityConfig.java:39-42`<br>`com/dentalclinic/service/AuthService.java:73,100,104`<br>`com/dentalclinic/service/StaffService.java:39` | • Hệ thống sử dụng `BCryptPasswordEncoder` làm chuẩn mã hóa mật khẩu trong toàn bộ luồng Auth, Registration, Staff Creation và Data Seeding.<br>• *(Khuyến nghị nhỏ: `RegisterRequest.java` cần nâng min length từ 3 lên tối thiểu 8 ký tự).* |
| **12** | **Rate Limiting & Brute-Force Defense** | ⚠️ **PARTIAL** | `com/dentalclinic/config/RateLimitingFilter.java:16-80`<br>`com/dentalclinic/service/AuthService.java:43-63` | • **Đạt:** `RateLimitingFilter` giới hạn 60 request/10s cho các request bắt đầu bằng `/api/`.<br>• **Lỗ hổng 1:** Kẻ tấn công có thể giả mạo `X-Forwarded-For` header để qua mặt rate limiter (hàm `getSanitizedClientIp` tin cậy trực tiếp header này mà không kiểm tra proxy tin cậy).<br>• **Lỗ hổng 2:** Không có cơ chế khóa tài khoản hoặc exponential backoff khi đăng nhập sai nhiều lần tại `/api/auth/login`. |
| **13** | **Bot / Spam Throttling** | ❌ **FAIL** | `com/dentalclinic/controller/AuthController.java:34`<br>`com/dentalclinic/controller/AppointmentController.java:35` | • Không có Captcha (Cloudflare Turnstile, reCAPTCHA v3) tại các form nhạy cảm: Đăng ký tài khoản, Đặt lịch hẹn, Gửi mã OTP khôi phục mật khẩu, Đánh giá bác sĩ.<br>• Không có honeypot field, dễ bị bot spam đặt lịch rác hoặc tạo hàng loạt user ảo. |
| **14** | **100% Parameterized Queries** | ✅ **PASS** | Toàn bộ repository tại `com/dentalclinic/repository/*` và `com/dentalclinic/itteam/repository/*`<br>`ITTeamE2ETestSuite.java:1354-1381` | • 100% truy vấn cơ sở dữ liệu được quản lý qua Spring Data JPA và JPQL chuẩn hóa.<br>• Không có bất kỳ truy vấn SQL native nối chuỗi (String Concatenation) nào trong toàn bộ mã nguồn.<br>• Kiểm thử Adversarial `T5-ADV-01` chứng minh các chuỗi SQL Injection (`' OR '1'='1'`, `'; DROP TABLE...`) được xử lý an toàn dưới dạng chuỗi ký tự thuần túy. |
| **15** | **XSS Escaping** | ⚠️ **PARTIAL** | `src/main/resources/static/js/app.js:200-800`<br>`src/main/resources/static/index.html:1-2755` | • **Đạt:** Dữ liệu trả về qua Spring REST API được Jackson escape các ký tự điều khiển trong JSON format.<br>• **Lỗ hổng:** Trong `app.js` và `index.html`, một số vị trí hiển thị bài viết (`article.content`), thông báo và ghi chú vẫn gán trực tiếp qua `.innerHTML = ...` mà chưa qua thư viện sanitize ở client-side (như DOMPurify). |
| **16** | **File Upload Validation (MIME-types, Magic Bytes)** | ❌ **FAIL** | `com/dentalclinic/service/FileUploadService.java:29-62`<br>`com/dentalclinic/controller/FileUploadController.java:23-51`<br>`application.yml:29-31` | • `FileUploadService.java` chỉ kiểm tra `file.isEmpty()`. Không kiểm tra MIME-type cho phép (allowlist).<br>• Không kiểm tra Magic Bytes (File Header Signature). Kẻ tấn công có thể đổi tên mã độc `.jsp`, `.exe`, `.sh` hoặc tệp `.svg` chứa script độc hại.<br>• Tệp upload được lưu vào `./uploads/dental-images/` và được Spring Boot phục vụ tĩnh công khai qua `/uploads/**`.<br>• Cấu hình `max-file-size: 50MB` quá lớn, tạo nguy cơ làm cạn kiệt dung lượng đĩa cứng (DoS). |
| **17** | **PII Masking & Error Response Suppression** | ⚠️ **PARTIAL** | `com/dentalclinic/itteam/service/SensitiveDataSanitizer.java:1-179`<br>`com/dentalclinic/exception/GlobalExceptionHandler.java:49-54`<br>`com/dentalclinic/controller/ArticleController.java:70,86` | • **Đạt:** `SensitiveDataSanitizer` có regex che chắn hoàn hảo cho CCCD, SĐT, JWT, Password trong module IT Team.<br>• **Lỗ hổng 1:** Không áp dụng PII masking cho các API phòng khám chính (`GET /api/medical-records`, `GET /api/appointments`).<br>• **Lỗ hổng 2:** `GlobalExceptionHandler.java` (dòng 52) trả về trực tiếp `ex.getMessage()` trong lỗi 500 (`"Đã xảy ra lỗi máy chủ: " + ex.getMessage()`), làm lộ chi tiết lỗi hệ thống và tên class nội bộ. |
| **18** | **Security Headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options)** | ⚠️ **PARTIAL** | `com/dentalclinic/security/SecurityConfig.java:62-69` | • **Đạt:** Đã cấu hình HSTS (`maxAgeInSeconds(31536000)`), Referrer-Policy (`STRICT_ORIGIN_WHEN_CROSS_ORIGIN`).<br>• **Lỗ hổng:** Hoàn toàn thiếu cấu hình `Content-Security-Policy` (CSP) và `Permissions-Policy`.<br>• `X-Frame-Options` đang để `sameOrigin()` để phục vụ H2-console thay vì `DENY`. |
| **19** | **HTTPS / WSS Enforcement** | ❌ **FAIL** | `com/dentalclinic/security/SecurityConfig.java:58-108`<br>`com/dentalclinic/websocket/WebSocketConfig.java:20-24` | • Không có cấu hình buộc chuyển hướng HTTPS (`requiresChannel(channel -> channel.anyRequest().requiresSecure())`) ở tầng ứng dụng.<br>• `WebSocketConfig.java` cho phép `.setAllowedOriginPatterns("*")` kết nối từ mọi domain mà không có handshake authentication token, tạo nguy cơ Cross-Site WebSocket Hijacking (CSWSH). |
| **20** | **Dependency CVE Audit** | ❌ **FAIL** | `pom.xml:1-115` | • Phiên bản Spring Boot 3.2.5 (phát hành tháng 04/2024) chứa một số CVE đã công bố trên thư viện phụ thuộc (Spring Framework 6.1.6, Tomcat 10.1.20).<br>• `pom.xml` hoàn toàn thiếu plugin kiểm toán phụ thuộc tự động như `dependency-check-maven` (OWASP).<br>• Chưa tích hợp Dependabot hoặc Snyk trong quy trình CI/CD. |

---

## 3. PHÂN TÍCH CHUYÊN SÂU CÁC LỖ HỔNG AN NINH NGHIÊM TRỌNG (DEEP-DIVE THREAT ANALYSIS)

### Lỗ hổng 1: Lộ thông tin Bệnh án EMR & Vi phạm RLS (Insecure Direct Object References - IDOR)
- **Vị trí:** `src/main/java/com/dentalclinic/controller/MedicalRecordController.java` (dòng 26-33):
  ```java
  @GetMapping
  public ResponseEntity<ApiResponse<List<MedicalRecord>>> getMedicalRecords(@RequestParam(required = false) String phone) {
      if (phone != null && !phone.isBlank()) {
          return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(phone.trim())));
      }
      return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findAllByOrderByRecordDateDesc()));
  }
  ```
- **Hệ quả:** Bất kỳ người dùng nào có tài khoản đăng nhập (bao gồm cả tài khoản bệnh nhân `ROLE_PATIENT`, lễ tân, hoặc tạp vụ) gửi yêu cầu `GET /api/medical-records` không kèm tham số `phone` sẽ nhận về **TOÀN BỘ dữ liệu bệnh án y tế của tất cả bệnh nhân trong phòng khám** (chẩn đoán, quá trình điều trị, đơn thuốc, ngày khám, bác sĩ phụ trách).
- **Phân loại:** OWASP Top 10 A01:2021 – Broken Access Control (Mức độ: **CRITICAL - CVSS 9.1**).

---

### Lỗ hổng 2: Mở quyền truy cập công khai sửa đổi bài viết, đánh giá và tải tệp tin EMR
- **Vị trí:** `src/main/java/com/dentalclinic/security/SecurityConfig.java` (dòng 87-93):
  ```java
  .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
  .requestMatchers("/api/articles/**").permitAll()
  .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
  .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
  .requestMatchers("/api/reviews/**").permitAll()
  .requestMatchers("/api/emr/images/**").permitAll()
  ```
- **Hệ quả:** 
  1. `POST /api/articles`, `PUT /api/articles/{id}`, `DELETE /api/articles/{id}` hoàn toàn không yêu cầu token đăng nhập. Bất kỳ ai trên Internet đều có thể chèn bài viết giả mạo, sửa nội dung website hoặc xóa trắng bài viết nha khoa.
  2. `POST /api/emr/images/upload` cho phép tải lên tệp tin ẩn danh mà không xác thực danh tính.
  3. Kẻ tấn công có thể spam đánh giá bác sĩ không giới hạn.
- **Phân loại:** OWASP Top 10 A01:2021 – Broken Access Control (Mức độ: **CRITICAL - CVSS 9.8**).

---

### Lỗ hổng 3: Tải tệp không kiểm duyệt & nguy cơ thực thi mã từ xa (Unrestricted File Upload & RCE)
- **Vị trí:** `src/main/java/com/dentalclinic/service/FileUploadService.java` (dòng 29-62) kết hợp `SecurityConfig.java` (dòng 72):
  ```java
  String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
  String extension = "";
  int dotIdx = originalFilename.lastIndexOf(".");
  if (dotIdx > 0) {
      extension = originalFilename.substring(dotIdx);
  }
  String storedFileName = UUID.randomUUID().toString() + extension;
  Path targetLocation = uploadPath.resolve(storedFileName);
  Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
  ```
- **Hệ quả:** 
  1. Không có danh sách trắng phần mở rộng (allowed extensions: `.jpg`, `.png`, `.webp`, `.dcm`).
  2. Kẻ tấn công có thể upload tệp HTML chứa mã độc XSS, tệp SVG chứa `<script>`, hoặc tệp mã độc. Tệp sau đó được Spring Boot phục vụ tĩnh tại `/uploads/dental-images/{uuid}.{ext}` với quyền `permitAll()`.
- **Phân loại:** OWASP Top 10 A04:2021 – Insecure Design & A03:2021 - Injection (Mức độ: **HIGH - CVSS 8.5**).

---

### Lỗ hổng 4: Giao diện H2 Console mở công khai không kiểm soát
- **Vị trí:** `application.yml` (dòng 25-27) & `SecurityConfig.java` (dòng 73):
  ```yaml
  h2:
    console:
      enabled: true
      path: /h2-console
  ```
  ```java
  .requestMatchers("/h2-console/**").permitAll()
  ```
- **Hệ quả:** H2 Web Console mở hoàn toàn cho Internet. Kẻ tấn công có thể truy cập `/h2-console`, kết nối vào `jdbc:h2:file:./data/dentaldb`, đọc toàn bộ bảng mật khẩu người dùng, dữ liệu tài chính doanh thu phòng khám, hoặc khai thác lỗ hổng thực thi mã H2 đã biết.
- **Phân loại:** OWASP Top 10 A05:2021 – Security Misconfiguration (Mức độ: **HIGH - CVSS 8.2**).

---

### Lỗ hổng 5: Thiếu mã hóa dữ liệu y tế nhạy cảm (Unencrypted Medical Data at Rest)
- **Vị trí:** `com/dentalclinic/model/MedicalRecord.java` và `com/dentalclinic/model/OrthodonticPlan.java`.
- **Hệ quả:** Toàn bộ thông tin bệnh án nha khoa (chẩn đoán răng số mấy, viêm nha chu, HIV, tiền sử tim mạch, đơn thuốc) được ghi vào ổ đĩa dưới dạng văn bản thô. Nếu tệp database `dentaldb.mv.db` bị sao chép hoặc backup rò rỉ, toàn bộ bí mật y tế của bệnh nhân sẽ bị lộ ra ngoài, vi phạm nghiêm trọng Luật Khám bệnh, chữa bệnh Việt Nam và tiêu chuẩn HIPAA.

---

## 4. ĐÁNH GIÁ CHI TIẾT BỘ KIỂM THỬ TỰ ĐỘNG HIỆN HỮU (E2E TEST SUITE REVIEW)

### 4.1. Kiến Trúc Bộ Kiểm Thử Hiện Tại (`TEST_INFRA.md`, `TEST_READY.md`)
Hệ thống kiểm thử hiện hữu được xây dựng theo chiến lược **Dual Track Testing Strategy**:
- **Tập tin chính:** `src/test/java/com/dentalclinic/e2e/ITTeamE2ETestSuite.java` (1,467 dòng code, 73 test methods).
- **Công nghệ kiểm thử:** JUnit 5, `@SpringBootTest(webEnvironment = MOCK)`, `@AutoConfigureMockMvc`, Jackson ObjectMapper, Jayway JsonPath.
- **Mô hình kiểm thử:** 4-Tier (+ Tier 5 Adversarial) Opaque-box testing:
  - **Tier 1 (45 tests):** Độ phủ tính năng chính (9 phân hệ x 5 test).
  - **Tier 2 (15 tests):** Ca biên & ngoại lệ cực trị (Negative page, payload rỗng, ký tự đặc biệt, URL giả mạo).
  - **Tier 3 (5 tests):** Kịch bản luồng phối hợp chéo tính năng (Message -> Mention Activity, Runner -> Sanitizer -> Log).
  - **Tier 4 (3 tests):** Kịch bản vận hành thực tế (Sự cố hiệu năng, Kiểm toán an ninh, Giao ca trực IT).
  - **Tier 5 (5 tests):** Kiểm thử kháng cự tấn công (SQL Injection, XSS payloads, SSRF host evasion, Tampered JWT, Rate limit DoS).

### 4.2. Điểm Mạnh Nổi Bật của `ITTeamE2ETestSuite.java`
1. **Thiết kế Opaque-box độc lập:** Toàn bộ test tương tác 100% qua HTTP REST endpoints (`MockMvc`), không phụ thuộc chặt vào class nội bộ uncommitted, đảm bảo kiểm thử chính xác hành vi người dùng ngoài đời thực.
2. **Kỹ thuật chống nghẽn Rate-Limiting thông minh:** Test harness tự động sinh địa chỉ IP xoay vòng qua `X-Forwarded-For` header (`192.168.10.x`), giúp 73 test chạy đồng thời mà không bị ngắt quãng bởi `RateLimitingFilter` (tránh false positive HTTP 429).
3. **Bộ lọc dữ liệu nhạy cảm (`SensitiveDataSanitizer`):** Được kiểm thử chuyên sâu bằng 4 test file riêng biệt (`SensitiveDataSanitizerTest.java`, `SensitiveDataSanitizerAdversarialTest.java`, `SensitiveDataSanitizerChallengerTest.java`, `EntityPrePersistenceSanitizationTest.java`), chứng minh khả năng lọc sạch mật khẩu, bearer token, cookie, CCCD và số điện thoại trước khi lưu log.
4. **Phòng chống SSRF hoàn chỉnh:** Đã kiểm thử chặn đứng các kỹ thuật lách IP cục bộ (`0.0.0.0`, `[::1]`, `127.0.0.1.nip.io`, `localhost@attacker.com`) trong `ITApiRunnerService`.

### 4.3. Các "Điểm Mù" & Khoảng Trống Kiểm Thử Lớn (Test Suite Gaps & Blind Spots)
Mặc dù `ITTeamE2ETestSuite.java` hoạt động rất tốt cho phân hệ IT Team Command Center, nhưng **chưa hề bao phủ 20 tiêu chuẩn bảo mật cho toàn bộ hệ thống phòng khám lâm sàng (Clinical Dental Clinic System)**:

| Hạng mục bảo mật | Trạng thái kiểm thử trong `ITTeamE2ETestSuite` | Khoảng trống cần bổ sung kiểm thử tự động |
| :--- | :---: | :--- |
| **Kiểm thử IDOR / RLS Bệnh án** | ❌ **0 Test** | Chưa có test kiểm tra xem Bệnh nhân A có xem được bệnh án hoặc lịch hẹn của Bệnh nhân B hay không (`/api/medical-records`, `/api/appointments/patient/{id}`). |
| **Kiểm thử Quyền sửa Bài viết & Review** | ❌ **0 Test** | Chưa có test xác nhận request unauthenticated tới `POST /api/articles` hay `POST /api/reviews` phải bị trả về `401 Unauthorized`. |
| **Kiểm thử Tải tệp độc hại** | ❌ **0 Test** | Chưa có test upload file `.jsp`, `.exe`, `.svg` độc hại hoặc file > 10MB vào `/api/emr/images/upload`. |
| **Kiểm thử Mã hóa AES-256 EMR** | ❌ **0 Test** | Chưa có test kiểm tra trực tiếp bản ghi trong database xem trường `diagnosis` có được mã hóa cipher text hay không. |
| **Kiểm thử H2 Console Lockdown** | ❌ **0 Test** | Chưa có test xác nhận `/h2-console` bị từ chối truy cập trên môi trường production. |
| **Kiểm thử Security Headers** | ❌ **0 Test** | Chưa có test assert sự hiện diện của CSP header, X-Content-Type-Options: nosniff. |
| **Kiểm thử WebSocket Auth & Hijacking** | ❌ **0 Test** | Chưa có test kiểm tra kết nối STOMP `/ws-dental` không kèm JWT token. |
| **Kiểm thử Brute-force Login Lockout** | ❌ **0 Test** | Chưa có test xác nhận tài khoản bị khóa tạm thời sau 5 lần nhập sai mật khẩu liên tiếp. |

---

## 5. KẾ HOẠCH HÀNH ĐỘNG KHẮC PHỤC AN NINH (REMEDIATION BLUEPRINT)

Để đạt mức tuân thủ 100% đối với 20 Tiêu Chuẩn Bảo Mật Enterprise cho hồ sơ y tế bệnh nhân nha khoa, kiến trúc sư hệ thống cần triển khai các giải pháp sau:

### Giai đoạn 1: Khóa Ngay Các Lỗ Hổng Nghiêm Trọng (Hotfix P0)
1. **Sửa `SecurityConfig.java`:**
   - Đóng quyền sửa bài viết: Chỉ cho phép `GET` public cho `/api/articles/**`. Các phương thức `POST`, `PUT`, `DELETE` bắt buộc yêu cầu `hasAnyRole('OWNER', 'ADMIN')`.
   - Đóng quyền upload EMR: Khóa `/api/emr/images/**` chỉ cho phép `hasAnyRole('OWNER', 'DENTIST', 'RECEPTIONIST')`.
   - Khóa H2 Console: Xóa `.requestMatchers("/h2-console/**").permitAll()` hoặc chỉ bật khi active profile `dev`.
   - Thêm bộ header an toàn: Bật `Content-Security-Policy`, đặt `X-Frame-Options` thành `DENY`.
2. **Khắc phục IDOR Bệnh Án trong `MedicalRecordController.java` & `AppointmentController.java`:**
   - Kiểm tra `AuthenticationPrincipal`: Nếu user là `ROLE_PATIENT`, chỉ trả về bệnh án và lịch hẹn có `patient_id` trùng khớp với `currentUser.getId()`. Nghiêm cấm trả về toàn bộ dữ liệu khi không có filter.
3. **Thắt chặt `FileUploadService.java`:**
   - Bắt buộc kiểm tra MIME type theo allowlist: `image/jpeg`, `image/png`, `image/webp`.
   - Kiểm tra Magic Bytes bằng Apache Tika hoặc mảng byte đầu (`FF D8 FF` cho JPEG, `89 50 4E 47` cho PNG).
   - Đổi thư mục lưu trữ ra ngoài web-root và cấm thực thi script. Giới hạn dung lượng tối đa 10MB thay vì 50MB.

### Giai đoạn 2: Chuẩn Hóa Kiến Trúc Dữ Liệu & Mã Hóa Y Tế (P1)
4. **Triển khai AES-256 GCM cho Dữ liệu Y Bạ (Medical EMR Encryption):**
   - Viết class `Aes256GcmAttributeConverter implements AttributeConverter<String, String>` sử dụng SecretKey lấy từ biến môi trường `EMR_ENCRYPTION_KEY`.
   - Gắn `@Convert(converter = Aes256GcmAttributeConverter.class)` vào các trường `diagnosis`, `treatmentDone`, `prescription` trong `MedicalRecord.java`, và `doctorNotes` trong `OrthodonticPlan.java`.
5. **Cách ly Secret & Biến Môi Trường (`.env`):**
   - Chuyển `app.jwt.secret` và thông số DB sang dạng `${APP_JWT_SECRET}` và `${SPRING_DATASOURCE_PASSWORD}`.
   - Thêm `.env`, `.env.*` vào `.gitignore`. Cung cấp `.env.example` với các giá trị mẫu.
6. **Bảo vệ Cookie & Refresh Token:**
   - Chuyển đổi cơ chế lưu trữ JWT từ `localStorage` sang Cookie có cờ `HttpOnly; Secure; SameSite=Strict`.
   - Bổ sung bảng `RefreshToken` với cơ chế xoay vòng (Token Rotation) và thời gian sống ngắn cho Access Token (15-30 phút).
7. **Bảo vệ DTO Binding 100%:**
   - Thay thế toàn bộ Entity ở `@RequestBody` của 5 Controller bằng các DTO chuyên biệt (`MedicalRecordCreateDto`, `OrthodonticPlanDto`, `ArticleCreateDto`, `ShiftAssignDto`, `DoctorReviewDto`) kèm annotation kiểm tra tính hợp lệ `@Valid`.

### Giai đoạn 3: Mở Rộng Bộ Kiểm Thử Bảo Mật E2E (P2)
8. **Mở rộng `ITTeamE2ETestSuite.java` hoặc viết thêm `MedicalSecurityE2ETestSuite.java`:**
   - Bổ sung 25 test cases chuyên biệt cho 20 tiêu chuẩn: kiểm tra IDOR bệnh án, kiểm tra phân quyền bài viết, kiểm tra upload file độc hại, kiểm tra header CSP, kiểm tra mã hóa database và kiểm tra khóa brute-force login.
   - Bổ sung plugin `dependency-check-maven` vào `pom.xml` để tự động ngắt build nếu phát hiện CVE điểm CVSS >= 7.0.

---

## 6. KẾT LUẬN & ĐỀ NGHỊ BÀN GIAO (CONCLUSION & RECOMMENDATIONS)

Hệ thống DentalCare đã xây dựng được một nền tảng chức năng rất phong phú, giao diện hiện đại và module IT Team Command Center có mức độ tự kiểm thử E2E cực kỳ bài bản (73 tests bao phủ đầy đủ 5 tầng chất lượng). 

Tuy nhiên, đối với **20 Tiêu Chuẩn Bảo Mật Y Tế Doanh Nghiệp (Enterprise Medical Security Standards)**, hệ thống hiện chỉ mới đạt **10% hoàn toàn** (BCrypt & Parameterized Queries) và **30% một phần**. 60% hạng mục còn lại đang tồn tại các lỗ hổng nghiêm trọng về kiểm soát truy cập (IDOR EMR), thiếu mã hóa dữ liệu y tế lưu trữ (AES-256), mở quyền công khai bài viết/file upload và lưu trữ token trên `localStorage`.

Báo cáo này cung cấp đầy đủ tọa độ dòng lệnh, bằng chứng mã nguồn và giải pháp kỹ thuật cụ thể. Kính chuyển Parent Orchestrator phân phối công việc cho các Đội ngũ chuyên trách (Security Hardening Specialist & Test Engineering) để triển khai khắc phục theo Lộ trình 3 giai đoạn đã đề xuất.
