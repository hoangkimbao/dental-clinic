package com.dentalclinic.controller;

import com.dentalclinic.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody Map<String, String> payload) {
        String to = payload.getOrDefault("to", "patient@dentalcare.com");
        String subject = payload.getOrDefault("subject", "[DentalCare] Thư kiểm tra kết nối email");
        String content = payload.getOrDefault("content", "<p>Hệ thống gửi email tự động của DentalCare đang hoạt động rất tốt!</p>");

        boolean success = emailService.sendHtmlEmail(to, subject, content);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "to", to,
                "message", success ? "Email sent / logged successfully" : "Failed to send email"
        ));
    }
}
