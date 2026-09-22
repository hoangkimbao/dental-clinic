package com.dentalclinic.repository;

import com.dentalclinic.model.PorcelainCrownWarranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PorcelainCrownWarrantyRepository extends JpaRepository<PorcelainCrownWarranty, Long> {
    Optional<PorcelainCrownWarranty> findByWarrantyCode(String warrantyCode);
    Optional<PorcelainCrownWarranty> findByQrCodeString(String qrCodeString);
    List<PorcelainCrownWarranty> findByPatientPhone(String patientPhone);
    boolean existsByWarrantyCode(String warrantyCode);
}
