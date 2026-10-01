# RÀ SOÁT & ĐẠI TU TOÀN BỘ LỖI KỸ THUẬT FRONTEND (3 HTML FILES)

## I. TRANG DASHBOARD QUẢN TRỊ (dashboard.html)
1. **Fix Vỡ Responsive Mobile**: Sửa `.sidebar { width: 260px }` đẩy vỡ nội dung dưới 768px. Bổ sung nút Hamburger Toggle trượt Sidebar mở/tắt trên mobile.
2. **Nút Thao tác Tĩnh & Alert**: Gắn hàm JS mở Modal & cập nhật ca khám cho các nút "Xem hồ sơ", "Vào khám", "Gọi nhắc lịch". Bỏ `alert()`.
3. **Biểu đồ Trực quan Metrics**: Tích hợp Chart.js theo dõi xu hướng doanh thu & lượt khám.
4. **Render Dữ liệu Động**: Render ngày tháng và danh sách bệnh nhân bằng JS sẵn sàng API Spring Boot.

## II. TRANG CHỦ LANDING PAGE (index.html)
1. **Modal UX Standards**: Bắt sự kiện phím `ESC` đóng 3 Modal và khóa cuộn trang phía dưới (`body.modal-open { overflow: hidden; }`).
2. **Ảnh Bác sĩ**: Thay thế thẻ SVG placeholder xám bằng hình chân dung bác sĩ thực tế sắc nét.
3. **Mobile Menu Backdrop**: Thêm lớp phủ mờ (Backdrop overlay) cho menu mobile `.nav-list.active`.

## III. TRANG ĐẶT LỊCH KHÁM (booking.html)
1. **Keyboard Avoidance**: Đảm bảo nút Submit `.sticky-footer-action` không bị bàn phím ảo che khuất.
2. **Inline Form Validation**: Hiển thị nhãn lỗi ngay dưới từng ô nhập liệu (vd: SĐT không đúng 10 số).

## IV. CHUẨN HÓA KIẾN TRÚC CODE
- Tách toàn bộ CSS/JS inline dồn cục ra thư mục `static/css/` và `static/js/` cho Backend Spring Boot.
