package com.dentalclinic.repository;

import com.dentalclinic.model.DentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalOrderRepository extends JpaRepository<DentalOrder, Long> {
    Optional<DentalOrder> findByOrderCode(String orderCode);
    List<DentalOrder> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);
    boolean existsByOrderCode(String orderCode);
}
