# BÁO CÁO KHẢO SÁT CHUYÊN SÂU: HỆ THỐNG VẬN HÀNH NHÂN SỰ & ĐẠI LÝ B2B, ỨNG DỤNG MÁY TÍNH (PC DESKTOP APP) & THƯ VIỆN THEO DÕI HÀNH VI (ANALYTICS / AGRID SDK)

**Dự án:** DentalCare Luxury Management Portal & Ecosystem  
**Thư mục làm việc:** `D:\java\dental-clinic\.agents\explorer_survey_staff_pc`  
**Tác giả:** Explorer Agent (`explorer_survey_staff_pc`)  
**Ngày báo cáo:** 2026-09-22T17:25:00Z  
**Đối tượng bàn giao:** Parent Orchestrator (`4110e379-52ae-4437-9f08-bb3a919ba41c`) & Các Team Thực Thi (Backend, Frontend, Mobile, Desktop, Security)

---

## 1. TỔNG QUAN HIỆN TRẠNG TOÀN BỘ CODEBASE (BASELINE ARCHITECTURE)

### 1.1. Cấu trúc Backend & Công nghệ
- **Khung ứng dụng:** Spring Boot 3.2.5 (Java 17), Spring Data JPA, Spring Security 6, Spring WebSocket (STOMP + SockJS).
- **Cơ sở dữ liệu:**
  - Môi trường Dev/Test: H2 Database file-based tại `./data/dentaldb;AUTO_SERVER=TRUE` với `ddl-auto: update`.
  - Môi trường Prod: Docker Compose hỗ trợ PostgreSQL 16 Alpine (`docker-compose.yml`).
- **Xác thực & Phân quyền:**
  - Token JWT (JJWT 0.12.5) với thời hạn 24h (`86400000 ms`), thuật ngữ Secret Key 256-bit.
  - Phân quyền theo vai trò (`Role.java`): `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`, `ROLE_PATIENT`.
  - Chống DoS / Brute-force: `RateLimitingFilter.java` giới hạn 60 requests / 10s window theo IP.
- **Hạ tầng Real-time:**
  - `WebSocketConfig.java`: Đăng ký STOMP endpoint `/ws-dental` (SockJS), message broker `/topic`, application prefix `/app`.
  - `NotificationService.java`: Phát thông báo qua `SimpMessagingTemplate.convertAndSend("/topic/notifications", notification)`.

### 1.2. Cấu trúc Frontend Web & Mobile App Hiện Tại
- **Web Frontend (`src/main/resources/static/`):**
  - Giao diện Single Page Application (SPA) viết bằng ES6 thuần + Tailwind CSS + FontAwesome 6 + Chart.js.
  - Tích hợp cổng quản trị `#management-portal` với các tab: Dashboard Thống Kê, Lịch Hẹn Khám, Bệnh Án EMR & Chỉnh Nha, Quản Lý Mã Ưu Đãi, Phân Ca & Nhân Sự, Thông Báo Realtime, IT Command Center.
- **Mobile Staff App (`mobile-app/`):**
  - Ứng dụng React Native trên nền Expo SDK 57 (`dental-staff-app`).
  - Đã có màn hình đăng nhập nhân viên (`letan`, `ROLE_DENTIST`...), quản lý lịch hẹn (lọc theo Hôm nay, Ngày mai, 7 ngày tới, Tương lai), cập nhật trạng thái lịch, xem bệnh án EMR, xem lộ trình chỉnh nha, xem ca trực, và xuất CSV lịch hẹn.
- **Thực trạng khoảng trống (Gap Analysis):**
  1. **Chưa có PC Desktop App độc lập**: Hiện tại chỉ có Web tĩnh và Expo Mobile App, hoàn toàn chưa có thư mục mã nguồn Desktop App (Electron / WebView2 / Native Desktop Runner).
  2. **Chưa có B2B Đại lý Cấp 2 / Chi nhánh vệ tinh / Quản lý kho vật tư**: Chưa có entity nào trong Database đại diện cho Chi nhánh vệ tinh, Nhà phân phối (NPP), Vật tư nha khoa (mắc cài, trụ implant, vật tư tiêu hao) và Quy trình phê duyệt xuất nhập kho.
  3. **Chưa có Module Chấm công & KPI chuyên sâu**: Bác sĩ chỉ có lịch hẹn; chưa có hệ thống chấm công vào/ra (timekeeping) bằng IP/GPS, chưa có theo dõi KPI doanh số, tỷ lệ chuyển đổi tư vấn dịch vụ.
  4. **Chưa có Field Intake**: Chưa có tính năng tiếp nhận bệnh nhân tại hiện trường (hội nghị nha khoa, khám răng học đường).
  5. **Chưa có User Behavior Tracking SDK (Agrid / Analytics SDK)**: Trong `app.js` chỉ có stub hàm rỗng `trackGaEvent`, chưa có SDK độc lập cho Web/Mobile/PC, chưa có Endpoint backend thu thập và lưu trữ sự kiện hành vi.

---

## 2. PHÂN HỆ 1: VẬN HÀNH NHÂN SỰ & ĐẠI LÝ B2B / PHÒNG KHÁM VỆ TINH

### 2.1. Khảo sát Mô hình B2B Đại Lý Cấp 2, Phòng Khám Vệ Tinh & Nhà Phân Phối (NPP)
Ngành nha khoa có đặc thù vận hành theo chuỗi liên kết:
1. **Trụ sở chính DentalCare (Command Center)**: Sở hữu kho trung tâm, đội ngũ chuyên gia phẫu thuật/chỉnh nha cao cấp, kiểm soát chất lượng và cấp phép vật tư.
2. **Chi nhánh vệ tinh / Đại lý Cấp 2 (Satellite Clinics / B2B Agents)**:
   - Các phòng khám nha khoa liên kết tại các quận/tỉnh thành vệ tinh.
   - Nhận cung ứng vật liệu độc quyền: mắc cài tự buộc Damon, khay niềng trong suốt, trụ Implant chuẩn quốc tế (Straumann, Osstem, Dentium), vật tư tiêu hao (bông gạc, kim tiêm, thuốc tê, chỉ phẫu thuật).
   - Hưởng chiết khấu đại lý (commission / margin) dựa trên hợp đồng phân phối.
3. **Nhà phân phối vật tư & Thiết bị nha khoa (Dental Distributor)**:
   - Cung cấp số lượng lớn vật tư tiêu hao, khí cụ chỉnh nha và vật liệu cấy ghép cho toàn hệ thống.

#### Đề xuất Thiết kế Thực Thể (Domain Entities):
- **`com.dentalclinic.model.Tier2Agent` (Bảng `tier2_agents`):**
  - `id`: Long (PK)
  - `agentCode`: String (Unique, e.g. `AGT-HCM-01`, `AGT-BDG-02`)
  - `agentName`: String (Tên phòng khám / đại lý vệ tinh)
  - `agentType`: Enum `AgentType` (`SATELLITE_CLINIC`, `DISTRIBUTOR`, `FRANCHISE_PARTNER`)
  - `address`, `province`, `city`: String (Địa chỉ hành chính)
  - `phone`, `email`, `representativeName`: String (Thông tin người đại diện)
  - `commissionRate`: Double (Tỷ lệ chiết khấu, e.g. 15.0%)
  - `creditLimit`: Double (Hạn mức công nợ vật tư)
  - `currentBalance`: Double (Số dư công nợ hiện tại)
  - `active`: Boolean (Trạng thái kích hoạt)
  - Mở rộng quan hệ: `@ManyToOne` với `User` (Quản lý vùng phụ trách).

- **`com.dentalclinic.model.DentalMaterial` (Bảng `dental_materials`):**
  - `id`: Long (PK)
  - `materialCode`: String (Unique, e.g. `IMP-STRAUMANN-01`, `BRK-DAMON-Q2`, `CONS-GLOVE-M`)
  - `name`: String (Tên vật liệu: "Trụ Implant Straumann SLActive Thụy Sĩ", "Mắc cài tự buộc Damon Q2", "Găng tay cao su y tế không bột")
  - `category`: Enum `MaterialCategory` (`CONSUMABLE`, `BRACKET`, `IMPLANT_POST`, `ORTHO_SUPPLY`, `CHEMICAL_MEDICINE`)
  - `specification`: String (Quy cách đóng gói: Hộp 100 chiếc, Vỉ 1 bộ mắc cài 20 răng, Lọ 50ml)
  - `unit`: String (Đơn vị tính: Cái, Hộp, Bộ, Trụ, Lọ)
  - `stockQuantity`: Integer (Tồn kho hiện có tại kho trung tâm)
  - `minSafetyStock`: Integer (Ngưỡng cảnh báo an toàn tồn kho)
  - `unitPrice`: Double (Đơn giá nhập / xuất)
  - `supplierName`: String (Tên nhà sản xuất / cung ứng)
  - `active`: Boolean

- **`com.dentalclinic.model.MaterialOrder` & `MaterialOrderItem` (Bảng `material_orders`, `material_order_items`):**
  - Quản lý quy trình đề xuất - duyệt - xuất kho vật tư cho Đại lý C2 / Phòng khám vệ tinh:
  - `orderCode`: String (e.g. `ORD-MAT-2026-001`)
  - `agent`: `@ManyToOne Tier2Agent`
  - `requester`: `@ManyToOne User` (Nhân viên tạo đơn)
  - `status`: Enum `MaterialOrderStatus` (`DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `DISPATCHED`, `RECEIVED`, `REJECTED`)
  - `approvedBy`: `@ManyToOne User` (Chủ phòng hoặc Trưởng kho phê duyệt)
  - `approvedAt`: LocalDateTime
  - `rejectionReason`: String
  - `totalAmount`: Double
  - Danh sách mặt hàng `MaterialOrderItem`: Số lượng yêu cầu, số lượng thực xuất, đơn giá, thành tiền.

#### Luồng Phê Duyệt Đơn Hàng Vật Tư (Workflow & State Machine):
```
[Đại lý / Chi nhánh Vệ tinh]
           │
           ▼ (Tạo đơn hàng vật liệu: Mắc cài, Trụ Implant, Thuốc tê...)
     [PENDING_APPROVAL]
           │
     ┌─────┴──────────────────────┐
     │                            │
     ▼ (Trưởng kho / Chủ duyệt)   ▼ (Từ chối, lý do nợ xấu/hết hàng)
 [APPROVED]                   [REJECTED]
     │
     ▼ (Trừ tồn kho trung tâm, xuất phiếu kho)
 [DISPATCHED]
     │
     ▼ (Đại lý nhận hàng, xác nhận vào kho vệ tinh)
 [RECEIVED]
```

### 2.2. Khảo sát Chấm Công, Phân Ca Làm Việc & KPI Bác Sĩ
- **Hiện trạng Codebase:**
  - `StaffShift.java`: Có `staff` (User), `shiftDate` (LocalDate), `shiftType` (`CA_SANG_8H_12H`, `CA_CHIEU_13H_17H`, `CA_TOI_17H_20H`, `CA_FULL_NGAY`), `roleTitle`, `status` (`SCHEDULED`, `COMPLETED`, `ABSENT`), `notes`.
  - `ShiftController.java`: Cung cấp `GET /api/shifts` và `POST /api/shifts`.
- **Nhu cầu Nghiệp vụ Bổ sung:**
  1. **Chấm công thời gian thực (Timekeeping / Attendance):**
     - Bác sĩ, y tá, phụ tá cần thực hiện Check-in / Check-out tại phòng khám.
     - Cần thực thể `StaffAttendance` liên kết với `StaffShift`: Lưu `checkInTime`, `checkOutTime`, `checkInStatus` (`ON_TIME`, `LATE`, `EARLY_LEAVE`), `ipAddress`, `locationGps` (hoặc định vị Wi-Fi nội bộ phòng khám để chống chấm công hộ).
  2. **Theo dõi KPI & Doanh số Bác sĩ (Doctor Consultation KPI & Revenue Tracking):**
     - Đánh giá năng suất bác sĩ qua:
       * Số lượng ca khám hoàn thành (`COMPLETED` appointments).
       * Tỷ lệ chuyển đổi bệnh nhân từ khám tổng quát sang điều trị chuyên sâu (Cấy Implant, Niềng răng mắc cài/Invisalign, Bọc răng sứ).
       * Tổng doanh thu dịch vụ thực tế mà bác sĩ trực tiếp thực hiện trong tháng.
     - Bổ sung thực thể `DoctorKpiRecord` hoặc Controller thống kê theo bác sĩ: `GET /api/staff/kpi?dentistId=...&month=...`.

### 2.3. Khảo sát Tiếp Nhận Bệnh Nhân Hiện Trường (Field Patient Intake)
- **Nghiệp vụ Đặc thù:**
  - Phòng khám thường tổ chức các sự kiện tầm soát sức khỏe cộng đồng:
    * Khám nha khoa học đường tại các trường tiểu học/trung học (phát hiện sâu răng, khớp cắn ngược, lệch lạc răng trẻ em).
    * Sự kiện hội nghị nha khoa, hội thảo răng hàm mặt (VIDEC, triển lãm thiết bị).
  - Yêu cầu kỹ thuật:
    * Thiết bị tại hiện trường (máy tính xách tay, máy tính bảng, điện thoại di động) cần thu thập nhanh thông tin bệnh nhân/học sinh.
    * Hỗ trợ lưu trữ tạm thời ngoại tuyến (Offline Caching qua IndexedDB / LocalStorage) khi mạng chập chờn.
    * Tự động phát hành mã ưu đãi / voucher tầm soát (liên kết với `Coupon.java`).
    * Đồng bộ 1 chạm về hệ thống trung tâm để lễ tân theo dõi và liên hệ đặt lịch chính thức.
- **Thực thể Đề xuất `FieldPatientIntake` (Bảng `field_patient_intakes`):**
  - `id`: Long
  - `eventName`: String (e.g. "Chương Trình Nụ Cười Học Đường 2026 - THCS Lê Quý Đôn")
  - `eventType`: Enum `FieldEventType` (`SCHOOL_SCREENING`, `DENTAL_CONFERENCE`, `COMMUNITY_OUTREACH`)
  - `eventDate`: LocalDate
  - `patientFullName`: String
  - `birthYear` / `age`: Integer
  - `studentClass` / `organization`: String (Lớp / Đơn vị)
  - `phone`: String
  - `parentName`, `parentPhone`: String (Dành cho học sinh)
  - `screeningFindings`: String (Kết quả khám sơ bộ: Sâu răng răng hàm, vôi răng độ 2, khớp cắn hở)
  - `recommendation`: String (Khuyên nhổ răng sữa lung lay, hàn răng composite, khám chỉnh nha)
  - `voucherCode`: String (Mã ưu đãi tặng kèm)
  - `leadStatus`: Enum `FieldLeadStatus` (`NEW`, `CONTACTED`, `BOOKED_APPOINTMENT`, `CONVERTED`)
  - `createdById`: Long (Nhân viên hiện trường thu thập)

---

## 3. PHÂN HỆ 2: ỨNG DỤNG QUẢN TRỊ MÁY TÍNH (PC DESKTOP APP)

### 3.1. Đánh giá Kiến trúc Standalone Desktop Client (Electron vs WebView2 / Native Runner)
Để đáp ứng yêu cầu một bản Desktop Client độc lập, chạy trên máy tính Windows/Mac của phòng khám và chi nhánh với khả năng đồng bộ thời gian thực:

| Tiêu chí | Giải pháp 1: Electron Desktop Client | Giải pháp 2: WebView2 / Native Runner (Edge WebView2) | Đánh giá & Khuyến nghị |
| :--- | :--- | :--- | :--- |
| **Hệ điều hành** | Windows 10/11, macOS, Linux | Chủ yếu Windows 10/11 (Cần webview ngoài trên Mac) | Electron hỗ trợ đa nền tảng hoàn hảo (Windows & Mac) theo đúng yêu cầu đề bài. |
| **Kích thước đóng gói** | ~80 - 150 MB (kèm Node runtime & Chromium) | Siêu nhẹ (~5 - 15 MB do dùng Edge có sẵn) | WebView2 nhẹ hơn, nhưng Electron cung cấp API phần cứng, khay hệ thống (Tray), xuất file trực tiếp vào ổ đĩa và in ấn máy in nhiệt Bill/Invoice tốt hơn. |
| **Tích hợp phần cứng** | Tray icon, Desktop Notifications, Local File System, Auto Launch, In ấn hóa đơn trực tiếp (Silent Print) | Hạn chế hơn, phụ thuộc API COM / C# wrapper | Electron vượt trội về khả năng in hóa đơn khám bệnh và kết nối máy quét mã vạch nha khoa. |
| **Bảo mật & Isolation** | Tách biệt hoàn toàn `contextIsolation: true`, `nodeIntegration: false`, preload script bảo vệ API | Tùy biến webview wrapper | Electron chuẩn bảo mật Enterprise. |

**Khuyến nghị Kiến trúc:**
Xây dựng module `desktop-app/` sử dụng Electron làm khung chính, kết hợp với script khởi chạy độc lập Native Runner:
- Thư mục: `desktop-app/`
  - `package.json`: Định nghĩa dependencies (`electron`, `electron-builder`), scripts (`npm run start`, `npm run build:win`, `npm run build:mac`).
  - `main.js`: Main process Electron quản lý cửa sổ (BrowserWindow), khay hệ thống (System Tray), thông báo OS, xử lý phím tắt, menu bản địa và lắng nghe sự kiện in/xuất file.
  - `preload.js`: Cầu nối bảo mật (Context Bridge) expose các API desktop an toàn (`window.desktopBridge.showNotification`, `window.desktopBridge.saveCsvFile`, `window.desktopBridge.printReceipt`).
  - `renderer/`: Tải giao diện Command Center chuyên biệt cho PC với kích thước màn hình lớn (1080p, 2K, 4K), tối ưu thanh cuộn và bảng dữ liệu mật độ cao (High-density Data Grid).

### 3.2. Trung Tâm Quản Trị CMS Command Center
Trên màn hình PC Desktop kích thước lớn, giao diện CMS Command Center được cấu trúc thành 6 phân hệ cốt lõi:

1. **Quản Lý Dịch Vụ Nha Khoa (Dental Service CMS):**
   - Không còn hardcode trong thẻ `<select>` của HTML; chuyển thành bảng quản lý động:
   - Các trường: Tên dịch vụ, Danh mục (Chỉnh nha, Implant, Nha chu, Thẩm mỹ, Tiểu phẫu), Giá niêm yết, Thời gian thực hiện (phút), Quy cách đóng gói/liệu trình (combo/lẻ), Thời gian bảo hành (tháng), Ảnh minh họa, Trạng thái kích hoạt.
   - Thao tác: Thêm mới, Sửa giá, Ẩn/Hiện dịch vụ, Cập nhật chính sách bảo hành.
2. **Quản Lý Bác Sĩ & Chuyên Gia (Doctor & Specialist Directory):**
   - Danh sách bác sĩ, học hàm học vị (BS.CKI, BS.CKII, Thạc sĩ), số năm kinh nghiệm, chứng chỉ hành nghề, chuyên khoa mũi nhọn (Invisalign Platinum Provider, Bác sĩ Implant Thụy Sĩ).
   - Lịch làm việc trong tuần, năng lực tiếp nhận (tối đa ca khám/ngày), định mức doanh số.
3. **Quản Lý Kho Vật Tư Y Tế (Medical Supply Inventory):**
   - Bảng theo dõi tồn kho trực tiếp theo thời gian thực.
   - Cảnh báo màu đỏ đối với các mặt hàng dưới ngưỡng an toàn (`stockQuantity <= minSafetyStock`).
   - Phê duyệt đơn đặt hàng từ các chi nhánh vệ tinh/đại lý cấp 2.
4. **Quản Lý Chi Nhánh & Đại Lý Cấp 2 (Branch & Agent C2 Management):**
   - Bản đồ các chi nhánh vệ tinh và phòng khám đối tác.
   - Quản lý công nợ, doanh số phân phối, tỷ lệ chiết khấu và danh sách hợp đồng cung ứng.
5. **Cấu Hình Menu & Chân Trang (Menu & Footer CMS Configuration):**
   - Quản lý động các liên kết thanh điều hướng, số điện thoại hotline cấp cứu răng miệng (24/7), địa chỉ các chi nhánh phòng khám, thời gian mở cửa, mạng xã hội và chính sách điều khoản y tế.
6. **Trung Tâm Điều Hành Sự Cố & Hoạt Động (Operations Hub):**
   - Tích hợp giám sát thời gian thực số lượng ghế nha đang có bệnh nhân, thời gian chờ của khách tại sảnh lễ tân.

### 3.3. Notification Hub & Cơ Chế Đồng Bộ Real-time WebSocket
- **Kết nối STOMP/WebSocket:**
  - Client PC kết nối tới `ws://localhost:8080/ws-dental` (hoặc domain an toàn `wss://nhakhoadentalcare.id.vn/ws-dental`).
  - Auto-reconnect: Khi mất mạng hoặc server khởi động lại, client tự động thử kết nối lại với thuật toán exponential backoff (1s, 2s, 4s, 8s... tối đa 30s).
- **Các Kênh Lắng Nghe (Topics):**
  1. `/topic/notifications`: Thông báo nội bộ phòng khám (Ca khám mới, bệnh nhân check-in, hủy lịch).
  2. `/topic/appointments`: Cập nhật trạng thái lịch hẹn tức thì giữa Lễ tân và Bác sĩ trong phòng khám.
  3. `/topic/orders`: Thông báo đơn đặt vật tư mới từ Đại lý C2 gửi về kho trung tâm.
  4. `/topic/emergency-alerts`: Cảnh báo ca cấp cứu răng miệng hoặc sự cố kỹ thuật.
- **Tương tác OS Bản Địa (Native OS Notifications):**
  - Khi có sự kiện quan trọng (ví dụ: "Bệnh nhân đã thanh toán cọc 100K", "Có đơn đặt 20 trụ Implant mới"), Desktop App phát âm thanh chuông thông báo (sound chime) và hiển thị Windows Toast Notification / macOS Notification banner ngay cả khi ứng dụng đang thu nhỏ xuống thanh taskbar.

### 3.4. Khả Năng Xuất Dữ Liệu Excel / CSV Chuyên Sâu
Nâng cấp từ việc chỉ xuất lịch hẹn hiện tại thành bộ xuất báo cáo toàn diện với hỗ trợ **UTF-8 with BOM (`\uFEFF`)** để hiển thị chuẩn xác tiếng Việt có dấu trên Microsoft Excel:
1. **Xuất Danh Sách Lịch Khám & Bệnh Nhân**: Lọc theo mốc thời gian, trạng thái cọc, bác sĩ phụ trách.
2. **Xuất Báo Cáo Tồn Kho Vật Tư & Định Giá**: Mã vật tư, danh mục, quy cách, số lượng tồn, đơn giá, tổng giá trị tồn kho, trạng thái an toàn.
3. **Xuất Bảng Chấm Công & Giờ Làm Việc Nhân Sự**: Họ tên nhân viên, chức danh ca trực, số ca chuẩn, số giờ làm, trạng thái đúng giờ/đi muộn.
4. **Xuất Báo Cáo Doanh Số & KPI Bác Sĩ**: Doanh số dịch vụ khám, số ca chỉnh nha/implant, tỷ lệ hoàn thành KPI tháng.
5. **Xuất Báo Cáo Tiếp Nhận Hiện Trường (Field Screening Leads)**: Danh sách học sinh/khách hàng tiềm năng, kết quả khám sơ bộ, mã voucher đã phát hành.

---

## 4. PHÂN HỆ 3: THƯ VIỆN THEO DÕI HÀNH VI NGƯỜI DÙNG (ANALYTICS / AGRID SDK)

### 4.1. Kiến Trúc Client-Side Tracking SDK Đa Nền Tảng (Web, Mobile, PC App)
Thư viện `AgridAnalyticsSDK` được thiết kế theo chuẩn module dùng chung:
- Bản Web: `src/main/resources/static/js/agrid-sdk.js`
- Bản Mobile: `mobile-app/src/services/agrid-sdk.js`
- Bản PC Desktop: `desktop-app/renderer/agrid-sdk.js`

#### Bộ Danh Mục Sự Kiện Thu Thập (Event Taxonomy):
| Nhóm Sự Kiện | Mã Event | Dữ liệu kèm theo (Payload Properties) |
| :--- | :--- | :--- |
| **Điều hướng** | `page_view` / `screen_view` | `path`, `title`, `referrer`, `loadTimeMs`, `platform` |
| **Phễu Đặt Lịch** | `booking_funnel_step` | `step` (`1_view_service`, `2_select_doctor`, `3_select_time`, `4_apply_coupon`, `5_submit_form`, `6_deposit_paid`), `serviceName`, `hasCoupon` |
| **Thương Mại / Mua Sắm** | `product_view`, `add_to_cart`, `initiate_checkout` | `productId`, `productName`, `quantity`, `packageType`, `unitPrice` |
| **Tìm Kiếm** | `search_query` | `keyword`, `category`, `resultCount`, `searchLocation` |
| **Mã QR** | `qr_scanned` | `qrType` (`WARRANTY_CHECK`, `LOYALTY_REWARD`, `DEPOSIT_PAY`, `CHECKIN`), `codeId` |
| **Tương tác UI** | `tab_switch`, `cta_click` | `tabId`, `buttonId`, `component` |
| **Sự Cố Hệ Thống** | `client_error` | `errorType`, `message`, `stackSnippet` (đã lọc PII) |

### 4.2. Đảm Bảo Hiệu Năng: 0ms Initial Render Latency & Cơ Chế Bất Đồng Bộ
Để tuân thủ tuyệt đối tiêu chí "Không làm giảm hiệu năng ứng dụng, 0ms initial render latency":
1. **Tải Bất Đồng Bộ Tuyệt Đối:**
   - Trên Web: Tải bằng `<script src="/js/agrid-sdk.js" async defer></script>` hoặc lazy load qua `loadScriptAsync()`.
   - Quá trình khởi tạo (Initialization) diễn ra hoàn toàn trên `requestIdleCallback` hoặc `setTimeout(..., 0)` sau khi sự kiện `window.onload` hoàn tất, đảm bảo 0 block luồng render DOM chính.
2. **Bộ Nhớ Đệm & Gom Cụm Sự Kiện (Micro-Batching Buffer):**
   - Sự kiện sau khi gọi `Agrid.track(...)` sẽ được lưu vào hàng đợi bộ nhớ RAM nội bộ (In-memory Queue).
   - Tự động đẩy dữ liệu (Flush) theo 2 điều kiện:
     * Đủ **10 sự kiện** trong hàng đợi.
     * Hoặc định kỳ mỗi **5 giây** một lần (Batch timer).
3. **Cơ Chế Gửi Không Khóa (Non-blocking Transport):**
   - Ưu tiên hàng đầu: Sử dụng `navigator.sendBeacon(endpoint, blobData)`. Phương thức này được trình duyệt gửi ở luồng nền độc lập, không tiêu tốn chu kỳ CPU render và hoàn toàn không làm trễ chuyển trang kể cả khi người dùng đóng tab ngay lập tức.
   - Phương án dự phòng (Fallback): `fetch(endpoint, { method: 'POST', keepalive: true, body: ... })`.
4. **Khả Năng Chống Mất Dữ Liệu Khi Mất Mạng (Offline Persistence):**
   - Khi thiết bị offline (không có kết nối internet), hàng đợi sự kiện được lưu tạm vào `localStorage` / `AsyncStorage` (giới hạn tối đa 200 sự kiện, xoay vòng FIFO).
   - Lắng nghe sự kiện `window.addEventListener('online', ...)` để tự động xả hàng đợi lên máy chủ ngay khi có mạng trở lại.

### 4.3. Hàng Rào Bảo Mật & Phòng Chống Rò Rỉ PII Trái Phép (Enterprise Privacy Guardrails)
Tuân thủ nghiêm ngặt chuẩn y tế và bảo vệ quyền riêng tư:
1. **Bộ Lọc Dữ Liệu Tự Động (Sanitization Filter):**
   - Trước khi bất kỳ payload nào được đưa vào hàng đợi tracking, hàm `AgridSanitizer.clean(payload)` sẽ tự động quét và loại bỏ:
     * Mật khẩu, Token, JWT Bearer, Cookie, Session ID (`password`, `token`, `jwt`, `secret`, `authorization`).
     * Số CCCD / CMND (chuỗi 9 hoặc 12 chữ số).
     * Số thẻ tín dụng / thẻ ghi nợ ngân hàng (16 chữ số thỏa thuật toán Luhn).
     * Bệnh án chi tiết, đơn thuốc y tế, chẩn đoán bệnh lý của bệnh nhân (EMR diagnosis text).
   - Địa chỉ Email được mask tự động (e.g. `n***@gmail.com`).
   - Số điện thoại được mask tự động (e.g. `09****5678`).
2. **Định Danh Ẩn Danh (Pseudonymous Client ID):**
   - Mỗi thiết bị được cấp một `client_id` ngẫu nhiên dưới dạng UUID v4 (lưu trong storage), không liên kết với thông tin cá nhân khi người dùng chưa đăng nhập.
   - Khi người dùng đăng nhập, chỉ lưu `user_id` nội bộ (số nguyên) hoặc hash SHA-256; tuyệt đối không truyền tên thật hoặc số điện thoại vào dữ liệu sự kiện gửi đi.
3. **Bộ Giới Hạn Tần Suất Gửi Client-side (Client Rate Throttling):**
   - Giới hạn tối đa không quá 50 sự kiện / phút từ một client để ngăn ngừa hiện tượng vòng lặp sự kiện (Event Storming) làm nghẽn mạng người dùng.

### 4.4. Hạ Tầng Thu Thập & Lưu Trữ Phía Backend (Backend Ingestion Layer)
- **Thực Thể `AnalyticsEvent` (Bảng `analytics_events`):**
  - `id`: Long (PK)
  - `eventId`: String (UUID v4)
  - `eventName`: String (Index)
  - `sessionId`: String
  - `clientId`: String (Index)
  - `userId`: Long (Nullable)
  - `platform`: String (`WEB`, `MOBILE`, `PC_DESKTOP`)
  - `path`: String
  - `propertiesJson`: String (TEXT, lưu JSON sanitized)
  - `clientIp`: String (Đã mask octet cuối để ẩn danh IP theo chuẩn GDPR, e.g. `192.168.1.xxx`)
  - `userAgent`: String
  - `eventTimestamp`: LocalDateTime
  - `createdAt`: LocalDateTime (Index)
- **Controller Thu Thập (`AnalyticsController.java`):**
  - `POST /api/analytics/events`: Thu thập sự kiện đơn lẻ hoặc mảng batch.
  - Endpoint cho phép truy cập công khai (PermitAll trong `SecurityConfig`) để theo dõi hành vi khách vãng lai trước khi đăng nhập.
  - Xử lý bất đồng bộ (`@Async` hoặc ThreadPool nội bộ): Trả về ngay HTTP 202 Accepted trong `< 5ms`, việc lưu Database diễn ra ngầm phía sau, không gây tải cho request thread của Tomcat.
  - `GET /api/analytics/funnel-stats`: API dành riêng cho Admin/Chủ phòng tại CMS Command Center để xem tỷ lệ chuyển đổi từng bước trong phễu đặt lịch nha khoa.

---

## 5. ĐÁNH GIÁ MÃ NGUỒN HIỆN HỮU, SCRIPTS & RUNNERS

### 5.1. File Cấu Hình & Build System
- **`pom.xml`**:
  - Đã có đầy đủ các thư viện nền tảng: `spring-boot-starter-web`, `security`, `validation`, `data-jpa`, `websocket`, `mail`, `actuator`, `jjwt` (0.12.5), `springdoc-openapi` (2.5.0), `h2`, `postgresql`.
  - Khuyến nghị: Giữ nguyên các thư viện hiện tại, không thêm các dependency nặng làm tăng thời gian build. Không sử dụng Lombok (codebase 100% sử dụng explicit getters/setters/constructors).
- **`application.yml`**:
  - Đã có cấu hình file H2 `./data/dentaldb;AUTO_SERVER=TRUE` và cấu hình nén gzip (`server.compression.enabled: true`).
  - Gợi ý bổ sung: Thiết lập cấu hình async task executor cho Spring Boot (`spring.task.execution.pool.core-size: 8`).
- **`Dockerfile` & `docker-compose.yml`**:
  - Hỗ trợ đóng gói Multi-stage build với Java 17 Temurin và chạy trên Alpine container.
  - Phù hợp hoàn hảo cho việc triển khai máy chủ trung tâm.
- **Hạ tầng Test Hiện Tại (`TEST_INFRA.md`, `ITTeamE2ETestSuite.java`)**:
  - Bộ kiểm thử E2E hiện có chạy rất nghiêm ngặt với 73 tests qua 5 Tiers.
  - Bất kỳ thực thể hoặc phân hệ mới nào được thêm vào phải đảm bảo không làm gãy các test case hiện hữu (Non-Interference Guarantee).

### 5.2. Đối Chiếu 20 Tiêu Chuẩn Bảo Mật An Ninh Doanh Nghiệp (Enterprise Security Checklist)

| STT | Hạng Mục Tiêu Chuẩn | Trạng Thái Hiện Tại | Đánh Giá & Nhiệm Vụ Hoàn Thiện Tiếp Theo |
| :--- | :--- | :--- | :--- |
| 1 | Ẩn API Key / Secrets vào file môi trường | Đã cấu hình biến môi trường trong `application.yml` | Đảm bảo các secret như JWT key, SMTP password đọc từ `SPRING_MAIL_PASSWORD`, `APP_JWT_SECRET`. |
| 2 | Xóa sạch Git Secrets | Kiểm tra `.gitignore` | `.gitignore` đã chặn file logs, data db, .m2, và target. Cần đảm bảo không commit file cấu hình cục bộ. |
| 3 | Bảo mật Database (Cổng riêng, user tối thiểu) | H2 file-based nội bộ / PostgreSQL docker | Trong production, Postgres chỉ lắng nghe trong mạng nội bộ docker network. |
| 4 | Row-Level Security / Phân tách dữ liệu phân cấp | Đã có một phần trong Appointment | Cần bổ sung phân quyền chặt chẽ cho Đại lý C2: Đại lý nào chỉ xem đơn hàng và kho của chính đại lý đó. |
| 5 | Mã hóa dữ liệu lưu trữ (AES-256) | Passwords đã hash BCrypt | Áp dụng mã hóa cho các trường nhạy cảm nếu có trong hồ sơ đại lý / CCCD nhân viên. |
| 6 | Xác thực Server qua JWT & Refresh Token | JWT 24h đang hoạt động | Cần bổ sung cơ chế Refresh Token xoay vòng (Rotating Refresh Token) nếu nâng cấp phiên đăng nhập dài hạn. |
| 7 | Input Validation & Sanitization (Chống XSS/SQLi) | Jakarta Validation (`@Valid`, `@NotBlank`...) | Tiếp tục áp dụng `@Valid` cho toàn bộ DTO mới của đơn hàng vật tư, đại lý, và analytics. |
| 8 | Role-Based Access Control (RBAC) | `@EnableMethodSecurity` và `SecurityConfig` | Cấu hình tường minh cho các role mới (`ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_DENTIST`...). |
| 9 | DTO Binding Protection (Chặn sửa Field trái phép) | 100% dùng Request DTO chuyên biệt | Không bao giờ bind trực tiếp Request Body vào JPA Entity để tránh lỗ hổng Mass Assignment. |
| 10 | Bảo mật Cookie (HttpOnly, Secure, SameSite) | Hiện dùng JWT Bearer trong Header | Nếu sử dụng cookie cho Desktop/Web session, bắt buộc cờ `HttpOnly; Secure; SameSite=Strict`. |
| 11 | Băm Password chuẩn (BCrypt / Argon2) | Đã dùng `BCryptPasswordEncoder` | Duy trì 100% trong `StaffService` và `AuthService`. |
| 12 | Rate Limiting (Chống Brute Force) | `RateLimitingFilter.java` (60 req/10s) | Đã hoạt động tốt. Cần whitelist hoặc tăng trần riêng cho batch analytics để tránh 429 khi gửi cụm log. |
| 13 | Chặn Bot & Spam (Throttling / Captcha) | Rate limiter theo IP hoạt động tốt | Bổ sung kiểm tra honeypot field cho form đặt lịch công khai. |
| 14 | Parameterized Query 100% | Spring Data JPA / Hibernate | 100% an toàn chống SQL Injection. |
| 15 | Escape nội dung hiển thị an toàn | `escapeHtml()` trong JS | Duy trì render giao diện an toàn, tránh `innerHTML` chứa dữ liệu người dùng chưa escape. |
| 16 | Giới hạn Upload File & Chặn MIME độc hại | `FileUploadController.java` (50MB) | Cần kiểm tra kỹ magic bytes định dạng file ảnh X-quang/chỉnh nha (JPEG, PNG, DICOM). |
| 17 | Masking PII & Ẩn Stacktrace Lỗi | `GlobalExceptionHandler.java` & Sanitizer | Tuyệt đối không trả stacktrace lỗi Java ra JSON response của client. |
| 18 | Security Headers (CSP, HSTS, X-Frame-Options) | Đã có HSTS, SAMEORIGIN trong `SecurityConfig` | Bổ sung thêm Content-Security-Policy (CSP) và X-Content-Type-Options: nosniff. |
| 19 | Bắt buộc kết nối an toàn HTTPS / WSS | HSTS đã bật trong Spring Security | Đảm bảo toàn bộ WebSocket URL đổi sang `wss://` khi chạy trên production domain. |
| 20 | Quét Dependencies định kỳ (CVE Scan) | Spring Boot 3.2.5 mới, không chứa CVE nghiêm trọng | Duy trì phiên bản dependency an toàn từ Maven Central. |

---

## 6. KẾ HOẠCH BÀN GIAO & PHÂN CHIA HẠNG MỤC THỰC THI (ACTIONABLE ROADMAP)

Để triển khai trọn vẹn yêu cầu theo đúng tinh thần chuyên biệt cho ngành Nha Khoa DentalCare, quy trình chia làm 5 mốc công việc:

### Mốc 1: Xây Dựng Mô Hình Dữ Liệu & API Backend Vận Hành (Staff, Agent, Inventory, Analytics)
- Thêm các Entity: `Tier2Agent`, `DentalMaterial`, `MaterialOrder`, `MaterialOrderItem`, `StaffAttendance`, `DoctorKpiRecord`, `FieldPatientIntake`, `AnalyticsEvent`.
- Tạo các Repository tương ứng trong `com.dentalclinic.repository`.
- Tạo các Service & Controller:
  - `Tier2AgentController` & `MaterialInventoryController`: Quản lý đại lý, kho vật tư, quy trình duyệt đơn.
  - `AttendanceController` & `DoctorKpiController`: Chấm công và thống kê KPI bác sĩ.
  - `FieldIntakeController`: Tiếp nhận bệnh nhân hiện trường, đồng bộ offline.
  - `AnalyticsController`: Thu thập sự kiện bất đồng bộ và cung cấp phễu thống kê.
- Khởi tạo dữ liệu mẫu trong `DataInitializer.java` (Các đại lý vệ tinh mẫu, vật tư mẫu gồm trụ Straumann, mắc cài Damon Q2, chỉ nha khoa, găng tay...).

### Mốc 2: Xây Dựng Ứng Dụng PC Desktop Độc Lập (`desktop-app/`)
- Khởi tạo dự án Electron trong thư mục `desktop-app/`.
- Cấu hình `main.js` với cửa sổ Full HD tối ưu cho phòng khám, System Tray icon, Desktop Push Notification.
- Tích hợp kết nối WebSocket SockJS + STOMP với cơ chế tự phục hồi kết nối.
- Xây dựng giao diện CMS Command Center đa phân hệ: Quản lý dịch vụ nha khoa, Bác sĩ, Tồn kho vật tư, Đại lý cấp 2, Cấu hình Menu/Footer, và Xuất Excel/CSV đa bảng dữ liệu.

### Mốc 3: Phát Triển & Tích Hợp Thư Viện Tracking Agrid/Analytics SDK
- Xây dựng file SDK độc lập `agrid-sdk.js`:
  - Khởi tạo bất đồng bộ (0ms latency).
  - Tự động gom cụm micro-batching (10 events hoặc 5s).
  - Sử dụng `navigator.sendBeacon` không khóa luồng.
  - Tự động lọc sạch và xóa vết PII y tế nhạy cảm.
  - Hỗ trợ lưu trữ offline `localStorage` và tự động flush khi online.
- Nhúng SDK vào:
  - Web Portal (`index.html`)
  - Mobile App (`mobile-app/src/services/agrid-sdk.js`)
  - PC Desktop Client (`desktop-app/renderer/agrid-sdk.js`)

### Mốc 4: Tích Hợp Nghiệp Vụ B2B & Hiện Trường Lên Mobile App
- Mở rộng ứng dụng Expo Mobile App (`mobile-app/`):
  - Bổ sung tab "Kho & Vật Tư": Giúp nhân viên kiểm kê tồn kho và tạo yêu cầu xuất vật tư.
  - Bổ sung màn hình "Chấm công / Check-in ca": Xác nhận có mặt tại phòng khám.
  - Bổ sung màn hình "Khám Hiện Trường": Dành cho sự kiện khám học đường / hội nghị nha khoa, nhập danh sách học sinh và lưu trữ tạm offline.

### Mốc 5: Kiểm Thử Toàn Diện, Hardening Bảo Mật 20 Tiêu Chuẩn & Bàn Giao
- Viết test suite tự động kiểm thử toàn bộ các Controller và Service mới.
- Kiểm tra tính bảo mật của bộ lọc PII trong Analytics SDK.
- Kiểm tra khả năng chịu tải và chống DoS của RateLimitingFilter đối với traffic analytics.
- Xác nhận zero regression đối với các chức năng khám chữa bệnh và IT Command Center hiện hữu.

---
*Báo cáo được lập đầy đủ, khách quan và sẵn sàng chuyển giao cho Parent Orchestrator.*
