package com.dentalclinic.repository;

import com.dentalclinic.model.DentalProduct;
import com.dentalclinic.model.DentalProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalProductRepository extends JpaRepository<DentalProduct, Long> {
    Optional<DentalProduct> findByCode(String code);
    List<DentalProduct> findByCategory(DentalProductCategory category);
    boolean existsByCode(String code);
}
