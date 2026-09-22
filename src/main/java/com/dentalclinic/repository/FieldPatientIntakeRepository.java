package com.dentalclinic.repository;

import com.dentalclinic.model.FieldLeadStatus;
import com.dentalclinic.model.FieldPatientIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldPatientIntakeRepository extends JpaRepository<FieldPatientIntake, Long> {
    Optional<FieldPatientIntake> findByLeadCode(String leadCode);
    List<FieldPatientIntake> findByEventNameContainingIgnoreCaseOrderByCreatedAtDesc(String eventName);
    List<FieldPatientIntake> findAllByOrderByCreatedAtDesc();
    List<FieldPatientIntake> findByStatus(FieldLeadStatus status);
    List<FieldPatientIntake> findByPhone(String phone);
}
