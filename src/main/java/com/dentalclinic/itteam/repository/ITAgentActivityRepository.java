package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITAgentActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITAgentActivityRepository extends JpaRepository<ITAgentActivity, Long> {

    /**
     * Paginated audit log retrieval ordered by timestamp descending.
     * Core requirement for GET /api/it-team/activities.
     */
    Page<ITAgentActivity> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Paginated audit logs filtered by agent code (e.g. "backend", "security").
     */
    Page<ITAgentActivity> findByAgentCodeOrderByTimestampDesc(String agentCode, Pageable pageable);

    /**
     * Paginated audit logs matching candidate agent codes (e.g. "backend" and "it-backend").
     */
    Page<ITAgentActivity> findByAgentCodeInOrderByTimestampDesc(List<String> agentCodes, Pageable pageable);

    /**
     * Paginated audit logs filtered by action type (e.g. "MENTIONED", "API_RUN", "STATUS_CHANGE").
     */
    Page<ITAgentActivity> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);

    /**
     * Paginated audit logs filtered by both agent code and action type.
     */
    Page<ITAgentActivity> findByAgentCodeAndActionTypeOrderByTimestampDesc(String agentCode, String actionType, Pageable pageable);

    /**
     * Paginated audit logs matching candidate agent codes and action type.
     */
    Page<ITAgentActivity> findByAgentCodeInAndActionTypeOrderByTimestampDesc(List<String> agentCodes, String actionType, Pageable pageable);

    /**
     * Quick list of top 20 recent activities for dashboard widgets.
     */
    List<ITAgentActivity> findTop20ByOrderByTimestampDesc();

    /**
     * List all activities for a specific agent code.
     */
    List<ITAgentActivity> findByAgentCode(String agentCode);

    /**
     * Count total activities by action type (e.g. count MENTIONED events).
     */
    long countByActionType(String actionType);

    /**
     * Count activities for a specific agent.
     */
    long countByAgentCode(String agentCode);

    /**
     * Check if an activity of a specific type exists for an agent code.
     * Used for idempotent seeding of the SYSTEM_BOOT activity record.
     */
    boolean existsByAgentCodeAndActionType(String agentCode, String actionType);
}
