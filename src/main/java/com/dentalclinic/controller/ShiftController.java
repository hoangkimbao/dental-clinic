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
    public ResponseEntity<ApiResponse<StaffShift>> assignShift(@RequestBody java.util.Map<String, Object> req) {
        StaffShift shift = new StaffShift();
        User staff = null;
        if (req.containsKey("staffId") && req.get("staffId") != null) {
            try {
                Long staffId = Long.valueOf(req.get("staffId").toString());
                staff = userRepository.findById(staffId).orElse(null);
            } catch (Exception ignored) {}
        }
        if (staff == null && req.containsKey("staffUsername") && req.get("staffUsername") != null) {
            staff = userRepository.findByUsername(req.get("staffUsername").toString()).orElse(null);
        }
        if (staff == null) {
            staff = userRepository.findByUsername("letan")
                    .or(() -> userRepository.findByUsername("bs_tuan"))
                    .or(() -> userRepository.findAll().stream()
                            .filter(u -> u.getRole() == com.dentalclinic.model.Role.ROLE_RECEPTIONIST || u.getRole() == com.dentalclinic.model.Role.ROLE_DENTIST)
                            .findFirst())
                    .orElse(null);
        }
        shift.setStaff(staff);

        if (req.containsKey("shiftDate") && req.get("shiftDate") != null) {
            shift.setShiftDate(java.time.LocalDate.parse(req.get("shiftDate").toString()));
        } else {
            shift.setShiftDate(java.time.LocalDate.now());
        }

        if (req.containsKey("shiftType") && req.get("shiftType") != null) {
            shift.setShiftType(com.dentalclinic.model.ShiftType.valueOf(req.get("shiftType").toString()));
        } else {
            shift.setShiftType(com.dentalclinic.model.ShiftType.CA_SANG_8H_12H);
        }

        if (req.containsKey("roleTitle") && req.get("roleTitle") != null) {
            shift.setRoleTitle(req.get("roleTitle").toString());
        }
        if (req.containsKey("dutyDescription") && req.get("dutyDescription") != null) {
            shift.setRoleTitle(req.get("dutyDescription").toString());
        }
        if (shift.getRoleTitle() == null) {
            shift.setRoleTitle("Nhân viên ca trực");
        }

        if (req.containsKey("roomOrChair") && req.get("roomOrChair") != null) {
            String room = req.get("roomOrChair").toString();
            shift.setNotes(room);
        }
        if (req.containsKey("notes") && req.get("notes") != null) {
            String n = req.get("notes").toString();
            String existing = shift.getNotes();
            shift.setNotes(existing != null && !existing.isBlank() ? existing + " | " + n : n);
        }

        shift.setStatus(com.dentalclinic.model.ShiftStatus.SCHEDULED);

        StaffShift saved = staffShiftRepository.save(shift);
        return ResponseEntity.ok(ApiResponse.success("Phân ca thành công!", saved));
    }
}

