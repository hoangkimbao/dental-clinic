package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITAgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITAgentProfileRepository extends JpaRepository<ITAgentProfile, Long> {

    /**
     * Find an agent profile by its exact hashtag (e.g. "#it-backend", "#it-qa").
     * Used by M2 hashtag parser engine and message router.
     */
    Optional<ITAgentProfile> findByHashtag(String hashtag);

    /**
     * Find an agent profile by its unique agent code (e.g. "backend", "it-backend").
     * Used by REST APIs, memory store, and tab sessions.
     */
    Optional<ITAgentProfile> findByAgentCode(String agentCode);

    /**
     * Check if a profile exists by hashtag (idempotency checks).
     */
    boolean existsByHashtag(String hashtag);

    /**
     * Check if a profile exists by agent code (idempotency checks).
     */
    boolean existsByAgentCode(String agentCode);

    /**
     * Find all agent profiles matching a specific operational status (e.g. "ONLINE", "ACTIVE", "IDLE", "BUSY", "OFFLINE").
     */
    List<ITAgentProfile> findByStatus(String status);

    /**
     * Retrieve all agent profiles ordered consistently by agent code.
     */
    List<ITAgentProfile> findAllByOrderByAgentCodeAsc();
}
