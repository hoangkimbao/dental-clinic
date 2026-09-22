package com.dentalclinic.repository;

import com.dentalclinic.model.MaterialOrder;
import com.dentalclinic.model.MaterialOrderStatus;
import com.dentalclinic.model.Tier2Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialOrderRepository extends JpaRepository<MaterialOrder, Long> {
    Optional<MaterialOrder> findByOrderCode(String orderCode);
    List<MaterialOrder> findByAgentOrderByOrderDateDesc(Tier2Agent agent);
    List<MaterialOrder> findByStatusOrderByOrderDateDesc(MaterialOrderStatus status);
    List<MaterialOrder> findAllByOrderByOrderDateDesc();
}
