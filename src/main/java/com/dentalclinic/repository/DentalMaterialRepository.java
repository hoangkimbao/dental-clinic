package com.dentalclinic.repository;

import com.dentalclinic.model.DentalMaterial;
import com.dentalclinic.model.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DentalMaterialRepository extends JpaRepository<DentalMaterial, Long> {
    Optional<DentalMaterial> findByMaterialCode(String materialCode);
    List<DentalMaterial> findByCategory(MaterialCategory category);
    List<DentalMaterial> findByActiveTrue();
    boolean existsByMaterialCode(String materialCode);

    @Query("SELECT m FROM DentalMaterial m WHERE m.stockQuantity <= m.minStockAlert AND m.active = true")
    List<DentalMaterial> findLowStockMaterials();

    @Query("SELECT m FROM DentalMaterial m WHERE m.expiryDate IS NOT NULL AND m.expiryDate <= :thresholdDate AND m.active = true ORDER BY m.expiryDate ASC")
    List<DentalMaterial> findExpiringMaterials(@Param("thresholdDate") LocalDate thresholdDate);
}
