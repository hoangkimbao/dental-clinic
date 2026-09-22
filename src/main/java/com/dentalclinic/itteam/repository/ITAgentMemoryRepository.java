package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITAgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITAgentMemoryRepository extends JpaRepository<ITAgentMemory, Long> {

    /**
     * Retrieve all memories belonging to an agent by agent code.
     */
    List<ITAgentMemory> findByAgentCode(String agentCode);

    /**
     * Retrieve all memories for an agent ordered by lastUpdated timestamp descending.
     * Core requirement for M3 memory view.
     */
    List<ITAgentMemory> findByAgentCodeOrderByLastUpdatedDesc(String agentCode);

    /**
     * Retrieve memories matching any of the candidate agent codes (e.g. "backend" and "it-backend").
     */
    List<ITAgentMemory> findByAgentCodeInOrderByLastUpdatedDesc(List<String> agentCodes);

    /**
     * Retrieve all memories across all agents ordered by lastUpdated descending.
     */
    List<ITAgentMemory> findAllByOrderByLastUpdatedDesc();

    /**
     * Retrieve memories for an agent ordered by priority descending.
     */
    List<ITAgentMemory> findByAgentCodeOrderByPriorityDesc(String agentCode);

    /**
     * JPQL alias for callers querying by 'priorityLevel'.
     */
    @Query("SELECT m FROM ITAgentMemory m WHERE m.agentCode = :agentCode ORDER BY m.priority DESC")
    List<ITAgentMemory> findByAgentCodeOrderByPriorityLevelDesc(@Param("agentCode") String agentCode);

    /**
     * Retrieve memories by agent code and priority (e.g. "HIGH", "MEDIUM", "LOW").
     */
    List<ITAgentMemory> findByAgentCodeAndPriority(String agentCode, String priority);

    /**
     * Find a specific memory key for an agent (e.g. "ARCHITECTURE_OVERVIEW").
     */
    Optional<ITAgentMemory> findByAgentCodeAndMemoryKey(String agentCode, String memoryKey);

    /**
     * Find a specific memory key for an agent by numeric agentId.
     */
    Optional<ITAgentMemory> findByAgentIdAndMemoryKey(Long agentId, String memoryKey);

    /**
     * Check if a memory key already exists for an agent (idempotency check).
     */
    boolean existsByAgentCodeAndMemoryKey(String agentCode, String memoryKey);

    /**
     * Retrieve memories by numeric agent ID.
     */
    List<ITAgentMemory> findByAgentId(Long agentId);

    /**
     * Retrieve memories by numeric agent ID ordered by last updated.
     */
    List<ITAgentMemory> findByAgentIdOrderByLastUpdatedDesc(Long agentId);

    /**
     * Search memory keys across agents matching a keyword.
     */
    List<ITAgentMemory> findByMemoryKeyContainingIgnoreCase(String keyword);

    /**
     * Delete memory record by agent code and key.
     */
    void deleteByAgentCodeAndMemoryKey(String agentCode, String memoryKey);
}
