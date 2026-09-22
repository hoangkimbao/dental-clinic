package com.dentalclinic.repository;
import com.dentalclinic.model.OrthodonticPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrthodonticPlanRepository extends JpaRepository<OrthodonticPlan, Long> {
    Optional<OrthodonticPlan> findByPatientPhone(String patientPhone);
    List<OrthodonticPlan> findAllByOrderByNextAdjustmentDateAsc();
}
