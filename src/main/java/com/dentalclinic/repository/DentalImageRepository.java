package com.dentalclinic.repository;

import com.dentalclinic.model.DentalImageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DentalImageRepository extends JpaRepository<DentalImageAttachment, Long> {
    List<DentalImageAttachment> findByMedicalRecordIdOrderByUploadedAtDesc(Long medicalRecordId);
    List<DentalImageAttachment> findByPatientIdOrderByUploadedAtDesc(Long patientId);
}
