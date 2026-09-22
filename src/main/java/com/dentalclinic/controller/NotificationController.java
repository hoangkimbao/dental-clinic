package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.Notification;
import com.dentalclinic.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@Tag(name = "7. Notifications", description = "Thông báo Realtime WebSocket")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo theo Role")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@RequestParam(required = false) String role) {
        List<Notification> list = (role != null && !role.isBlank()) 
                ? notificationService.getForRole(role) 
                : notificationService.getAllRecent();
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
