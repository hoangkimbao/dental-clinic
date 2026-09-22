package com.dentalclinic.repository;
import com.dentalclinic.model.StaffShift;
import com.dentalclinic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {
    List<StaffShift> findByShiftDateOrderByShiftType(LocalDate shiftDate);
    List<StaffShift> findByStaffOrderByShiftDateDesc(User staff);
    List<StaffShift> findAllByOrderByShiftDateDesc();
}
