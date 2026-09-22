# Dispatch Assignment — Orchestrator Generation 4

## 2026-09-22T17:16:28Z

You are the Project Orchestrator for DentalCare Clinic at `D:\java\dental-clinic`.
Your working directory is `D:\java\dental-clinic\.agents\orchestrator_4`.

Your mission is to lead and orchestrate the team to fulfill the comprehensive user request and Product Owner directive recorded verbatim in `D:\java\dental-clinic\ORIGINAL_REQUEST.md` (and `D:\java\dental-clinic\.agents\ORIGINAL_REQUEST.md`):

1. Khách Hàng (Bệnh Nhân / Khách Nha Khoa - Mobile App & Web Portal):
   - Đặt lịch hẹn khám, Dịch vụ răng miệng (Niềng răng chỉnh nha, Trồng răng Implant, Bọc răng sứ, Tẩy trắng răng, Nhổ răng khôn...).
   - Mua sắm sản phẩm chăm sóc răng miệng (Bàn chải điện, máy tăm nước, kem đánh răng chuyên dụng, chỉ nha khoa, máng duy trì...), giỏ hàng & quy cách đóng gói (hộp/combo).
   - Quét mã QR bảo hành răng sứ / dự thưởng tích điểm Loyalty Dental Care.
   - Bác Sĩ Nha Khoa AI (AI Dental Diagnostic): Chẩn đoán hình ảnh răng miệng (sâu răng, vôi răng, viêm nướu, răng khôn mọc lệch).
   - Chợ / Diễn đàn cộng đồng trao đổi kinh nghiệm nha khoa & Bản đồ hệ thống phòng khám / chi nhánh nha khoa DentalCare kèm chỉ đường.

2. Nhân Sự & NPP / Đại Lý Cấp 2 / Bác Sĩ & Đối Tác Nha Khoa (Mobile App / Web Portal):
   - Đại lý cấp 2 / Chi nhánh nha khoa vệ tinh / NPP vật liệu & thiết bị nha khoa (vật tư tiêu hao, mắc cài, trụ implant...).
   - Quản lý tồn kho vật tư nha khoa, duyệt đơn hàng vật liệu.
   - Phân ca trực bác sĩ, y tá, phụ tá; Chấm công; Theo dõi KPI ca khám & doanh số dịch vụ.
   - Thu thập thông tin bệnh nhân tại hiện trường/hội nghị nha khoa, sự kiện khám răng học đường.

3. PC Desktop App (Phần Mềm Quản Trị Phòng Khám Nha Khoa Độc Lập):
   - Chạy độc lập trên máy tính Windows/Mac (thông qua Electron hoặc WebView/Native Desktop Runner) đồng bộ dữ liệu thời gian thực.
   - CMS Command Center quản lý dịch vụ nha khoa, quản lý bác sĩ, quản lý kho vật tư y tế, quản lý chi nhánh/đại lý C2, cấu hình menu/footer.
   - Notification Hub giám sát thông báo tập trung, xuất báo cáo thống kê Excel/CSV chuyên sâu.

4. Thư Viện Thu Thập Hành Vi Người Dùng (Agrid / Analytics SDK):
   - Tích hợp module theo dõi hành vi người dùng (User Behavior Tracking SDK tương tự GA4/Agrid) đồng bộ trên cả Web và Mobile/PC App.
   - Thu thập sự kiện: Xem trang, click luồng đặt hàng/đặt lịch, tìm kiếm, quét mã QR, thao tác điều hướng.
   - Bất đồng bộ (non-blocking), 0ms initial render latency, bảo mật an toàn tuyệt đối, không thu thập dữ liệu nhạy cảm trái phép.

5. Tuân Thủ Checklist 20 Tiêu Chuẩn Bảo Mật An Ninh Enterprise (Medical / Dental Security):
   - Đáp ứng đầy đủ 20 hạng mục bảo mật: .env/secrets, no git secrets, db security, RLS/tenant isolation, AES-256 sensitive data encryption, JWT/OAuth2 rotation, Input sanitization, RBAC, DTO binding protection, HttpOnly/Strict cookies, BCrypt password hashing, Rate limiting brute-force, Bot/spam throttling, Parameterized queries 100%, XSS escaping, File upload validation, PII masking, Security headers, HTTPS/WSS, CVE dependency scanning.

Acceptance Criteria:
- Automated tests pass for features and security standards.
- E2E verification of customer journey, staff/agent workflows, PC Desktop app, analytics SDK, and 20 security tests.
