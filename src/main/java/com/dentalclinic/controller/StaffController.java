package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.StaffCreateRequest;
import com.dentalclinic.model.User;
import com.dentalclinic.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "3. Staff & Doctors", description = "Quản lý nhân sự & Cấp tài khoản nhân viên")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/dentists")
    @Operation(summary = "Lấy danh sách Bác sĩ chuyên khoa (Công khai)")
    public ResponseEntity<ApiResponse<List<User>>> getDentists() {
        return ResponseEntity.ok(ApiResponse.success(staffService.getDentists()));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy danh sách toàn bộ nhân sự (Chủ & Lễ tân)")
    public ResponseEntity<ApiResponse<List<User>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.success(staffService.getAllStaff()));
    }

    @PostMapping("/staff/create")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
    @Operation(summary = "Cấp tài khoản nhân viên mới (Chủ & Lễ tân)")
    public ResponseEntity<ApiResponse<User>> createStaff(@Valid @RequestBody StaffCreateRequest request) {
        User user = staffService.createStaff(request);
        return ResponseEntity.ok(ApiResponse.success("Cấp tài khoản nhân viên thành công!", user));
    }
}
