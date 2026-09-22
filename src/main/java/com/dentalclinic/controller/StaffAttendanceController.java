package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.AttendanceCheckInRequest;
import com.dentalclinic.dto.AttendanceCheckOutRequest;
import com.dentalclinic.model.StaffAttendance;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.StaffAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
@Tag(name = "Staff Attendance", description = "Chấm công nhân sự bằng GPS & địa chỉ IP")
public class StaffAttendanceController {

    private final StaffAttendanceService attendanceService;
    private final UserRepository userRepository;

    public StaffAttendanceController(StaffAttendanceService attendanceService, UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @PostMapping("/check-in")
    @Operation(summary = "Nhân viên chấm công vào ca (GPS & IP verification)")
    public ResponseEntity<ApiResponse<StaffAttendance>> checkIn(@RequestBody AttendanceCheckInRequest req) {
        StaffAttendance attendance = attendanceService.checkIn(req, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Chấm công vào thành công!", attendance));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Nhân viên chấm công ra ca kết thúc ngày làm việc")
    public ResponseEntity<ApiResponse<StaffAttendance>> checkOut(@RequestBody AttendanceCheckOutRequest req) {
        StaffAttendance attendance = attendanceService.checkOut(req, getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Chấm công ra thành công!", attendance));
    }

    @GetMapping("/my-history")
    @Operation(summary = "Lịch sử chấm công cá nhân của nhân viên đăng nhập")
    public ResponseEntity<ApiResponse<List<StaffAttendance>>> getMyHistory() {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getMyHistory(getCurrentUser())));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Ban quản lý kiểm tra nhật ký chấm công toàn bộ chi nhánh theo ngày")
    public ResponseEntity<ApiResponse<List<StaffAttendance>>> auditAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAuditHistory(date)));
    }
}
