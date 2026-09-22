package com.dentalclinic.repository;

import com.dentalclinic.model.DentalOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DentalOrderItemRepository extends JpaRepository<DentalOrderItem, Long> {
    List<DentalOrderItem> findByOrderId(Long orderId);
}
