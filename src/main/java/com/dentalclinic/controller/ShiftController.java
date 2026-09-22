package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.StaffShift;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.StaffShiftRepository;
import com.dentalclinic.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@CrossOrigin(origins = "*")
@Tag(name = "8. Shifts", description = "Lịch phân ca trực nhân sự")
public class ShiftController {

    private final StaffShiftRepository staffShiftRepository;
    private final UserRepository userRepository;

    public ShiftController(StaffShiftRepository staffShiftRepository, UserRepository userRepository) {
        this.staffShiftRepository = staffShiftRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Lấy lịch phân ca nhân sự")
    public ResponseEntity<ApiResponse<List<StaffShift>>> getShifts(@RequestParam(required = false) Long staffId) {
        if (staffId != null) {
            User staff = userRepository.findById(staffId).orElse(null);
            if (staff != null) {
                return ResponseEntity.ok(ApiResponse.success(staffShiftRepository.findByStaffOrderByShiftDateDesc(staff)));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(staffShiftRepository.findAllByOrderByShiftDateDesc()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
    @Operation(summary = "Phân ca làm việc cho nhân viên (Chủ & Lễ tân)")
    public ResponseEntity<ApiResponse<StaffShift>> assignShift(@RequestBody StaffShift shift) {
        StaffShift saved = staffShiftRepository.save(shift);
        return ResponseEntity.ok(ApiResponse.success("Phân ca thành công!", saved));
    }
}
