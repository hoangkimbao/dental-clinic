package com.dentalclinic.repository;

import com.dentalclinic.model.StaffAttendance;
import com.dentalclinic.model.StaffShift;
import com.dentalclinic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {
    Optional<StaffAttendance> findByShift(StaffShift shift);
    Optional<StaffAttendance> findByShiftId(Long shiftId);
    List<StaffAttendance> findByStaffOrderByCheckInTimeDesc(User staff);
    List<StaffAttendance> findByStaff_IdOrderByCheckInTimeDesc(Long staffId);

    @Query("SELECT a FROM StaffAttendance a WHERE a.shift.shiftDate = :date ORDER BY a.checkInTime DESC")
    List<StaffAttendance> findByShiftDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM StaffAttendance a WHERE a.shift.shiftDate = :date AND a.staff = :staff")
    Optional<StaffAttendance> findByShiftDateAndStaff(@Param("date") LocalDate date, @Param("staff") User staff);
}
