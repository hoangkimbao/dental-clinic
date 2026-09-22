package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITBrowserTabRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITBrowserTabRecordRepository extends JpaRepository<ITBrowserTabRecord, Long> {

    /**
     * Find tab records for an agent.
     */
    List<ITBrowserTabRecord> findByAgentCode(String agentCode);

    /**
     * Find tab records matching candidate agent codes (e.g. "frontend" or "it-frontend").
     */
    List<ITBrowserTabRecord> findByAgentCodeIn(List<String> agentCodes);
    List<ITBrowserTabRecord> findByAgentCodeInOrderByOpenedAtDesc(List<String> agentCodes);

    /**
     * Find tab records by status (e.g. "OPEN", "BACKGROUND", "CLOSED").
     */
    List<ITBrowserTabRecord> findByStatus(String status);

    /**
     * Find tab records for an agent by status.
     */
    List<ITBrowserTabRecord> findByAgentCodeAndStatus(String agentCode, String status);

    /**
     * Find tab records by category (e.g. "PORTAL", "DOCS", "API_TESTER", "MONITORING").
     */
    List<ITBrowserTabRecord> findByTabCategory(String tabCategory);

    /**
     * Check if a tab record already exists for an agent and URL route (idempotent seeding).
     */
    boolean existsByAgentCodeAndUrlRoute(String agentCode, String urlRoute);

    /**
     * JPQL alias for existsByAgentCodeAndUrl.
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM ITBrowserTabRecord t WHERE t.agentCode = :agentCode AND t.urlRoute = :url")
    boolean existsByAgentCodeAndUrl(@Param("agentCode") String agentCode, @Param("url") String url);

    /**
     * Retrieve all tab records ordered by opened timestamp descending.
     */
    List<ITBrowserTabRecord> findAllByOrderByOpenedAtDesc();

    /**
     * Retrieve all tab records ordered by creation timestamp descending.
     */
    List<ITBrowserTabRecord> findAllByOrderByCreatedAtDesc();
}
