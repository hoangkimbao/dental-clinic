package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Async
    public void sendBookingConfirmationEmail(Appointment appointment) {
        sendAppointmentBookingEmail(appointment);
    }

    @Async
    public void sendAppointmentBookingEmail(Appointment appointment) {
        String patientEmail = appointment.getPatientEmail();
        if (patientEmail == null || patientEmail.trim().isEmpty() || !patientEmail.contains("@")) {
            log.info("[EmailService] No valid patient email for appointment #{}. Skipped.", appointment.getId());
            return;
        }

        String subject = "[DentalCare Luxury] Xác nhận đặt lịch hẹn khám nha khoa #" + appointment.getId();
        String body = buildBookingConfirmationHtml(appointment);

        sendHtmlEmail(patientEmail, subject, body);
    }

    @Async
    public void sendDepositSuccessEmail(Appointment appointment, String paymentCode) {
        double amount = appointment.getDepositAmount() != null ? appointment.getDepositAmount() : 100000.0;
        sendDepositSuccessEmail(appointment, paymentCode, amount);
    }

    @Async
    public void sendDepositSuccessEmail(Appointment appointment, String paymentCode, double amount) {
        String patientEmail = appointment.getPatientEmail();
        if (patientEmail == null || patientEmail.trim().isEmpty() || !patientEmail.contains("@")) {
            log.info("[EmailService] No valid patient email for deposit appointment #{}. Skipped.", appointment.getId());
            return;
        }

        String subject = "[DentalCare Luxury] Xác nhận thanh toán đặt cọc thành công #" + appointment.getId();
        String body = buildDepositSuccessHtml(appointment, paymentCode, amount);

        sendHtmlEmail(patientEmail, subject, body);
    }

    @Async
    public void sendAppointmentReminderEmail(Appointment appointment) {
        String patientEmail = appointment.getPatientEmail();
        if (patientEmail == null || patientEmail.trim().isEmpty() || !patientEmail.contains("@")) {
            return;
        }

        String timeStr = appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().format(DATE_FMT) : "";
        String subject = "[Nhắc hẹn DentalCare] Lịch khám của bạn vào lúc " + timeStr;
        String body = buildReminderHtml(appointment);

        sendHtmlEmail(patientEmail, subject, body);
    }

    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("[EmailService] Sending email to: {} | Subject: {}", to, subject);
        if (mailSender == null) {
            log.info("[EmailService (Simulation)] MailSender not configured. Email logged successfully to console.\nSubject: {}\nTo: {}", subject, to);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("DentalCare Luxury <cskh.dentalcare@gmail.com>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Email sent successfully to: {}", to);
            return true;
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private String buildBookingConfirmationHtml(Appointment a) {
        String timeStr = a.getAppointmentTime() != null ? a.getAppointmentTime().format(DATE_FMT) : "Đang xếp lịch";
        String dentistStr = a.getDentist() != null ? a.getDentist().getFullName() : "Đang xếp bác sĩ";
        String serviceStr = a.getServiceName() != null ? a.getServiceName() : "Khám tổng quát";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; font-family: 'Segoe UI', Arial, sans-serif; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); padding: 30px 20px; text-align: center; color: #ffffff;\">");
        sb.append("<h1 style=\"margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;\">NHA KHOA QUỐC TẾ DENTALCARE</h1>");
        sb.append("<p style=\"margin: 8px 0 0 0; opacity: 0.9; font-size: 14px;\">Hệ thống Nha khoa Thẩm mỹ & Trồng Răng Implant Tiêu chuẩn Thụy Sĩ</p></div>");
        sb.append("<div style=\"padding: 30px 25px;\">");
        sb.append("<p style=\"font-size: 16px; color: #1e293b; margin-top: 0;\">Xin chào <strong>").append(escape(a.getPatientName())).append("</strong>,</p>");
        sb.append("<p style=\"font-size: 15px; color: #475569; line-height: 1.6;\">Cảm ơn bạn đã tin tưởng lựa chọn Nha khoa DentalCare. Lịch hẹn khám của bạn đã được ghi nhận thành công trên hệ thống:</p>");
        sb.append("<div style=\"background: #f8fafc; border-left: 4px solid #0284c7; padding: 18px 20px; border-radius: 6px; margin: 20px 0;\">");
        sb.append("<table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">");
        sb.append("<tr><td style=\"padding: 6px 0; color: #64748b; width: 40%;\">Mã lịch hẹn:</td><td style=\"padding: 6px 0; color: #0f172a; font-weight: 600;\">#").append(a.getId()).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 6px 0; color: #64748b;\">Dịch vụ điều trị:</td><td style=\"padding: 6px 0; color: #0284c7; font-weight: 600;\">").append(escape(serviceStr)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 6px 0; color: #64748b;\">Bác sĩ phụ trách:</td><td style=\"padding: 6px 0; color: #0f172a; font-weight: 600;\">").append(escape(dentistStr)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 6px 0; color: #64748b;\">Thời gian khám:</td><td style=\"padding: 6px 0; color: #dc2626; font-weight: 700;\">").append(timeStr).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 6px 0; color: #64748b;\">Địa chỉ phòng khám:</td><td style=\"padding: 6px 0; color: #0f172a;\">123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh</td></tr></table></div>");
        sb.append("<div style=\"background: #ecfdf5; border: 1px dashed #10b981; border-radius: 8px; padding: 15px; margin: 20px 0; text-align: center;\">");
        sb.append("<p style=\"margin: 0; color: #065f46; font-size: 14px; font-weight: 600;\">🎁 Bạn được tặng 1 suất chụp phim CT Cone Beam 3D & Khám tư vấn miễn phí!</p></div>");
        sb.append("<p style=\"font-size: 13px; color: #64748b; line-height: 1.5;\"><i>Lưu ý: Quý khách vui lòng đến trước 10 phút so với giờ hẹn để hoàn tất thủ tục đón tiếp. Nếu cần thay đổi thời gian khám, xin vui lòng gọi hotline <strong>1900 8989</strong> hoặc phản hồi email này.</i></p></div>");
        sb.append("<div style=\"background: #f1f5f9; padding: 18px 25px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b;\">");
        sb.append("<p style=\"margin: 0 0 5px 0;\">Nha khoa Quốc tế DentalCare Luxury &bull; Hotline CSKH 24/7: 1900 8989</p>");
        sb.append("<p style=\"margin: 0;\">Website: <a href=\"https://nhakhoadentalcare.id.vn\" style=\"color: #0284c7; text-decoration: none;\">https://nhakhoadentalcare.id.vn</a></p></div></div>");
        return sb.toString();
    }

    private String buildDepositSuccessHtml(Appointment a, String code, double amount) {
        String amountFormatted = String.format("%,.0f VNĐ", amount);
        String serviceStr = a.getServiceName() != null ? a.getServiceName() : "Dịch vụ nha khoa";
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; font-family: 'Segoe UI', Arial, sans-serif; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"background: linear-gradient(135deg, #059669 0%, #047857 100%); padding: 30px 20px; text-align: center; color: #ffffff;\">");
        sb.append("<h1 style=\"margin: 0; font-size: 24px; font-weight: 700;\">BIÊN LAI ĐẶT CỌC GIỮ CHỖ THÀNH CÔNG</h1>");
        sb.append("<p style=\"margin: 8px 0 0 0; opacity: 0.9; font-size: 14px;\">Hệ thống Thanh toán Trực tuyến Nha khoa DentalCare</p></div>");
        sb.append("<div style=\"padding: 30px 25px;\">");
        sb.append("<p style=\"font-size: 16px; color: #1e293b; margin-top: 0;\">Kính gửi <strong>").append(escape(a.getPatientName())).append("</strong>,</p>");
        sb.append("<p style=\"font-size: 15px; color: #475569; line-height: 1.6;\">Chúng tôi đã nhận được khoản thanh toán đặt cọc giữ chỗ khám ưu tiên của bạn:</p>");
        sb.append("<div style=\"background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;\">");
        sb.append("<p style=\"margin: 0; color: #15803d; font-size: 13px; text-transform: uppercase; font-weight: 600;\">Số tiền đã thanh toán</p>");
        sb.append("<p style=\"margin: 8px 0; color: #166534; font-size: 28px; font-weight: 800;\">").append(amountFormatted).append("</p>");
        sb.append("<p style=\"margin: 0; color: #15803d; font-size: 13px;\">Mã giao dịch: <strong>").append(escape(code)).append("</strong></p></div>");
        sb.append("<table style=\"width: 100%; border-collapse: collapse; font-size: 14px; background: #f8fafc; padding: 15px; border-radius: 6px;\">");
        sb.append("<tr><td style=\"padding: 8px 12px; color: #64748b;\">Khách hàng:</td><td style=\"padding: 8px 12px; color: #0f172a; font-weight: 600;\">").append(escape(a.getPatientName())).append(" (").append(escape(a.getPatientPhone())).append(")</td></tr>");
        sb.append("<tr><td style=\"padding: 8px 12px; color: #64748b;\">Dịch vụ đăng ký:</td><td style=\"padding: 8px 12px; color: #0f172a; font-weight: 600;\">").append(escape(serviceStr)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 8px 12px; color: #64748b;\">Trạng thái giữ chỗ:</td><td style=\"padding: 8px 12px; color: #059669; font-weight: 700;\">&#10004; Ưu tiên không chờ đợi (VIP Track)</td></tr></table></div>");
        sb.append("<div style=\"background: #f1f5f9; padding: 18px 25px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b;\">");
        sb.append("<p style=\"margin: 0;\">Nha khoa Quốc tế DentalCare Luxury &bull; Hotline hỗ trợ: 1900 8989</p></div></div>");
        return sb.toString();
    }

    private String buildReminderHtml(Appointment a) {
        String timeStr = a.getAppointmentTime() != null ? a.getAppointmentTime().format(DATE_FMT) : "";
        String serviceStr = a.getServiceName() != null ? a.getServiceName() : "Dịch vụ nha khoa";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"max-width: 600px; margin: 0 auto; font-family: 'Segoe UI', Arial, sans-serif; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0;\">");
        sb.append("<div style=\"background: linear-gradient(135deg, #d97706 0%, #b45309 100%); padding: 25px 20px; text-align: center; color: #ffffff;\">");
        sb.append("<h2 style=\"margin: 0; font-size: 22px;\">NHẮC LỊCH HẸN KHÁM NGÀY MAI</h2>");
        sb.append("<p style=\"margin: 5px 0 0 0; opacity: 0.9; font-size: 14px;\">Nha khoa Quốc tế DentalCare</p></div>");
        sb.append("<div style=\"padding: 25px;\">");
        sb.append("<p>Kính gửi <strong>").append(escape(a.getPatientName())).append("</strong>,</p>");
        sb.append("<p>Nha khoa DentalCare xin nhắc bạn về lịch hẹn khám ngày mai lúc <strong>").append(timeStr).append("</strong> cho dịch vụ <strong>").append(escape(serviceStr)).append("</strong>.</p>");
        sb.append("<p>Vui lòng đến đúng giờ để bác sĩ phục vụ tốt nhất.</p></div></div>");
        return sb.toString();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
