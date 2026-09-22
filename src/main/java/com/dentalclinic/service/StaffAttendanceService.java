package com.dentalclinic.service;

import com.dentalclinic.dto.AttendanceCheckInRequest;
import com.dentalclinic.dto.AttendanceCheckOutRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.StaffAttendanceRepository;
import com.dentalclinic.repository.StaffShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StaffAttendanceService {

    private final StaffAttendanceRepository attendanceRepository;
    private final StaffShiftRepository staffShiftRepository;

    public StaffAttendanceService(StaffAttendanceRepository attendanceRepository,
                                  StaffShiftRepository staffShiftRepository) {
        this.attendanceRepository = attendanceRepository;
        this.staffShiftRepository = staffShiftRepository;
    }

    public StaffAttendance checkIn(AttendanceCheckInRequest req, User currentUser) {
        if (req.getShiftId() == null) {
            throw new BadRequestException("Mã ca trực (shiftId) không được để trống!");
        }

        StaffShift shift = staffShiftRepository.findById(req.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca trực với ID: " + req.getShiftId()));

        Optional<StaffAttendance> existing = attendanceRepository.findByShiftId(req.getShiftId());
        if (existing.isPresent() && existing.get().getCheckInTime() != null) {
            throw new BadRequestException("Ca làm việc đã được chấm công vào!");
        }

        User staff = (currentUser != null) ? currentUser : shift.getStaff();
        if (shift.getStaff() != null && (staff == null || staff.getRole() == Role.ROLE_RECEPTIONIST)) {
            // Receptionist or admin checking in for the assigned shift staff
            staff = shift.getStaff();
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceStatus status = calculateCheckInStatus(shift, now.toLocalTime());

        boolean isGps = (req.getLatitude() != null && req.getLongitude() != null);

        StaffAttendance attendance = existing.orElse(new StaffAttendance());
        attendance.setStaff(staff != null ? staff : shift.getStaff());
        attendance.setShift(shift);
        attendance.setCheckInTime(now);
        attendance.setCheckInIp(req.getNetworkIp());
        attendance.setCheckInLatitude(req.getLatitude());
        attendance.setCheckInLongitude(req.getLongitude());
        attendance.setIsGpsVerified(isGps);
        attendance.setStatus(status);

        return attendanceRepository.save(attendance);
    }

    public StaffAttendance checkOut(AttendanceCheckOutRequest req, User currentUser) {
        if (req.getShiftId() == null) {
            throw new BadRequestException("Mã ca trực (shiftId) không được để trống!");
        }

        StaffAttendance attendance = attendanceRepository.findByShiftId(req.getShiftId())
                .orElseThrow(() -> new BadRequestException("Chưa tìm thấy bản ghi chấm công vào cho ca trực: " + req.getShiftId()));

        if (attendance.getCheckInTime() == null) {
            throw new BadRequestException("Chưa thực hiện chấm công vào cho ca trực này!");
        }

        attendance.setCheckOutTime(LocalDateTime.now());
        if (req.getNotes() != null && !req.getNotes().trim().isEmpty()) {
            String combined = (attendance.getNotes() != null && !attendance.getNotes().isEmpty())
                    ? attendance.getNotes() + " | Check-out: " + req.getNotes()
                    : req.getNotes();
            attendance.setNotes(combined);
        }

        // Mark shift as completed
        StaffShift shift = attendance.getShift();
        if (shift != null) {
            shift.setStatus(ShiftStatus.COMPLETED);
            staffShiftRepository.save(shift);
        }

        return attendanceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public List<StaffAttendance> getMyHistory(User currentUser) {
        if (currentUser == null) {
            return List.of();
        }
        return attendanceRepository.findByStaffOrderByCheckInTimeDesc(currentUser);
    }

    @Transactional(readOnly = true)
    public List<StaffAttendance> getAuditHistory(LocalDate date) {
        if (date != null) {
            return attendanceRepository.findByShiftDate(date);
        }
        return attendanceRepository.findAll();
    }

    private AttendanceStatus calculateCheckInStatus(StaffShift shift, LocalTime checkInTime) {
        if (shift == null || shift.getShiftType() == null) {
            return AttendanceStatus.ON_TIME;
        }
        LocalTime expectedStart = switch (shift.getShiftType()) {
            case CA_SANG_8H_12H, CA_FULL_NGAY -> LocalTime.of(8, 0);
            case CA_CHIEU_13H_17H -> LocalTime.of(13, 0);
            case CA_TOI_17H_20H -> LocalTime.of(17, 0);
        };
        // Allow 15 minutes grace window
        if (checkInTime.isAfter(expectedStart.plusMinutes(15))) {
            return AttendanceStatus.LATE;
        }
        return AttendanceStatus.ON_TIME;
    }
}
