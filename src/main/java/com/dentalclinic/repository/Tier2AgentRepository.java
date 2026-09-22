package com.dentalclinic.repository;

import com.dentalclinic.model.AgentType;
import com.dentalclinic.model.Tier2Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Tier2AgentRepository extends JpaRepository<Tier2Agent, Long> {
    Optional<Tier2Agent> findByAgentCode(String agentCode);
    List<Tier2Agent> findByAgentType(AgentType agentType);
    List<Tier2Agent> findByActiveTrue();
    boolean existsByAgentCode(String agentCode);
}
