package com.dentalclinic.repository;

import com.dentalclinic.model.MaterialOrder;
import com.dentalclinic.model.MaterialOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialOrderItemRepository extends JpaRepository<MaterialOrderItem, Long> {
    List<MaterialOrderItem> findByOrder(MaterialOrder order);
}
