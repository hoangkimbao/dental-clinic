package com.dentalclinic.repository;
import com.dentalclinic.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientPhoneOrderByRecordDateDesc(String patientPhone);
    List<MedicalRecord> findAllByOrderByRecordDateDesc();
}
