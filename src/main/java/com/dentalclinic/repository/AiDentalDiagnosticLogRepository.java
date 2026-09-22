package com.dentalclinic.repository;

import com.dentalclinic.model.AiDentalDiagnosticLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiDentalDiagnosticLogRepository extends JpaRepository<AiDentalDiagnosticLog, Long> {
    List<AiDentalDiagnosticLog> findByPatientPhoneOrderByAnalyzedAtDesc(String patientPhone);
    List<AiDentalDiagnosticLog> findTop10ByOrderByAnalyzedAtDesc();
}
