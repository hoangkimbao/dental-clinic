package com.dentalclinic.repository;

import com.dentalclinic.model.DentalServiceCatalog;
import com.dentalclinic.model.DentalServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalServiceCatalogRepository extends JpaRepository<DentalServiceCatalog, Long> {
    Optional<DentalServiceCatalog> findByCode(String code);
    List<DentalServiceCatalog> findByCategory(DentalServiceCategory category);
    List<DentalServiceCatalog> findByIsFeaturedTrue();
    boolean existsByCode(String code);
}
