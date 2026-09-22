package com.dentalclinic.repository;

import com.dentalclinic.model.ProductPackagingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPackagingOptionRepository extends JpaRepository<ProductPackagingOption, Long> {
    List<ProductPackagingOption> findByProductId(Long productId);
}
