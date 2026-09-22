package com.dentalclinic.repository;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentStatus;
import com.dentalclinic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDentistOrderByAppointmentTimeDesc(User dentist);
    List<Appointment> findByDentistId(Long dentistId);
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByStatusOrderByAppointmentTimeDesc(AppointmentStatus status);
    List<Appointment> findAllByOrderByAppointmentTimeDesc();
}
