# KỊCH BẢN KHIẾU NẠI KHÁCH HÀNG & YÊU CẦU NGHIỆM THU BỔ SUNG

Dự án website, web app PC và trải nghiệm điện thoại – DentalCare
Tình huống mô phỏng phục vụ họp rà soát/ nghiệm thu với đội phát triển.

## 7 NÓM KHIẾU NẠI CẦN XỬ LÝ TRIỆT ĐỂ:

1. **Chất lượng Nghiệm thu & Tuyên bố Bàn giao**:
   - Đối soát khoảng lệch giữa báo cáo cũ và thực tế. Chạy lại 100% E2E Test Suite (250/250 tests pass) cùng 5 hành trình người dùng thực tế.
2. **Cấu trúc Bố cục Website & Thẻ HTML Lồng Sai**:
   - Sửa lỗi thẻ đóng HTML thiếu khiến FAQ, chi nhánh, diễn đàn và footer bị lồng sai vào portal quản trị. Bổ sung khu vực Đánh giá Khách hàng (`#testimonials`).
3. **Trải nghiệm Di động & Viewport (360px, 375px, 390px)**:
   - Triệt tiêu hoàn toàn tràn lề ngang, đảm bảo touch target >= 44px, nút bấm không bị bàn phím ảo che khuất.
4. **Luồng Đặt lịch & Thanh toán Cọc (Booking Edge Cases)**:
   - Kiểm thử hết chỗ, ngày quá khứ, SĐT sai, gửi trùng, gián đoạn mạng, coupon sale. Làm rõ trạng thái thanh toán sandbox/COD.
5. **AI Chẩn đoán & Ranh giới An toàn Y tế**:
   - Fix lỗi JS mapping `pathologyName`/`pathologyNameVi`. Bổ sung Disclaimer miễn trừ trách nhiệm y tế ("Chỉ mang tính tham khảo, không thay thế chẩn đoán bác sĩ").
6. **Giỏ hàng & Đơn hàng (Cart & Checkout)**:
   - Xử lý giỏ hàng rỗng, tăng giảm số lượng, lưu đơn khi F5 reload, tránh bị bàn phím che khuất nút xác nhận.
7. **Bảo mật, Mật khẩu Mặc định & Tài liệu PWA**:
   - Thay đổi toàn bộ mật khẩu mặc định (`admin/admin123`), cập nhật `HUONG_DAN_SU_DUNG.docx` và `HUONG_DAN_SU_DUNG.md` để giải thích rõ ràng cơ chế PWA thay vì gây hiểu lầm là ứng dụng Native Store.
8. **Triệt tiêu Lỗi Ô Vuông Nhỏ Thay Biểu Tượng (Icon Tofu Boxes)**:
   - Loại bỏ 100% thẻ `<i class="fa...">` và ký tự unicode glyph cũ. Thay thế toàn bộ logo, navbar, nhãn đánh giá, thông tin dịch vụ, nút đặt lịch và nút nổi bên phải màn hình bằng **SVG Vector nội tại (`dental-icons.js`)**, đảm bảo 0 lỗi ô vuông trên mọi trình duyệt và thiết bị.

