# Technical Architecture Report: Spring Data JPA Repositories & ITTeamDataInitializer

**Author:** Explorer 2 (Milestone 1: JPA Repositories & Data Seeder)  
**Target Package:** `com.dentalclinic.itteam.repository`, `com.dentalclinic.itteam.config`  
**Cross-Verified With:** Explorer 1 (`explorer_m1_1` JPA Entity Specifications)  
**Working Directory:** `D:\java\dental-clinic\.agents\explorer_m1_2`  
**Date:** 2026-09-12  

---

## 1. Executive Summary

This report establishes the verified, production-grade design for the persistence layer of the **IT Team Command Center** inside the DentalCare Management Portal.

Key deliverables investigated and designed:
1. **6 Spring Data JPA Repositories** under `com.dentalclinic.itteam.repository`:
   - `ITAgentProfileRepository`
   - `ITAgentMemoryRepository`
   - `ITAgentMessageRepository`
   - `ITAgentActivityRepository`
   - `ITBrowserTabRecordRepository`
   - `ITApiRunLogRepository`
2. **Complete Query Derivation & JPQL Alias Analysis**:
   - `findByHashtag`, `findByAgentCode`, `existsByHashtag`, `existsByAgentCode`
   - `findByAgentCodeOrderByLastUpdatedDesc`, `findByAgentCodeOrderByPriorityDesc` (with alias `findByAgentCodeOrderByPriorityLevelDesc`), `findByAgentCodeAndMemoryKey`
   - `findByIsReadFalse` & `findByReadStatusFalse`, `findByRecipientIdAndIsReadFalse`, `findByParentMessageIdIsNullOrderBySentAtAsc`, `findByParentMessageIdOrderBySentAtAsc`, `findByParsedHashtagsContainingIgnoreCaseOrderBySentAtDesc`
   - `findAllByOrderByTimestampDesc(Pageable)`, `findByAgentCodeAndActionTypeOrderByTimestampDesc(...)`
   - `findByAgentCodeAndStatus`, `existsByAgentCodeAndUrlRoute` (with alias `existsByAgentCodeAndUrl`)
   - `findTop50ByOrderByRunTimestampDesc`, `findByEndpointContainingIgnoreCaseOrderByRunTimestampDesc`
3. **`ITTeamDataInitializer`** under `com.dentalclinic.itteam.config`:
   - Idempotent startup logic ensuring zero duplicate profiles, memories, or tabs across application restarts.
   - Comprehensive seed data for the 5 standard IT profiles (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`), initial structured memories, initial browser tab sessions, initial welcome broadcast message, initial activity log, and dedicated IT admin account.

---

## 2. Spring Data JPA Repositories Specification

All repositories extend Spring Data's `org.springframework.data.jpa.repository.JpaRepository<T, Long>` and provide both derived queries and JPQL aliases where appropriate to guarantee 100% caller compatibility.

### 2.1 ITAgentProfileRepository

**File Location:** `src/main/java/com/dentalclinic/itteam/repository/ITAgentProfileRepository.java`  
**Target Entity:** `com.dentalclinic.itteam.model.ITAgentProfile`

```java
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
     * Find all agent profiles matching a specific operational status (e.g. "ONLINE", "IDLE", "BUSY", "OFFLINE").
     */
    List<ITAgentProfile> findByStatus(String status);

    /**
     * Retrieve all agent profiles ordered consistently by agent code.
     */
    List<ITAgentProfile> findAllByOrderByAgentCodeAsc();
}
```

---

### 2.2 ITAgentMemoryRepository

**File Location:** `src/main/java/com/dentalclinic/itteam/repository/ITAgentMemoryRepository.java`  
**Target Entity:** `com.dentalclinic.itteam.model.ITAgentMemory`

```java
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
```

---

### 2.3 ITAgentMessageRepository

**File Location:** `src/main/java/com/dentalclinic/itteam/repository/ITAgentMessageRepository.java`  
**Target Entity:** `com.dentalclinic.itteam.model.ITAgentMessage`

```java
package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITAgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITAgentMessageRepository extends JpaRepository<ITAgentMessage, Long> {

    /**
     * Retrieve all top-level root messages (parentMessageId is null) in chronological order.
     * Used for the main channel chat feed.
     */
    List<ITAgentMessage> findByParentMessageIdIsNullOrderBySentAtAsc();

    /**
     * Retrieve all threaded replies for a specific parent message in chronological order.
     * Used for message thread view.
     */
    List<ITAgentMessage> findByParentMessageIdOrderBySentAtAsc(Long parentMessageId);

    /**
     * Check if a message has any child replies.
     */
    boolean existsByParentMessageId(Long parentMessageId);

    /**
     * Count child replies for a parent message.
     */
    long countByParentMessageId(Long parentMessageId);

    /**
     * Retrieve messages received by a specific agent or recipient ID.
     */
    List<ITAgentMessage> findByRecipientIdOrderBySentAtDesc(Long recipientId);

    /**
     * Retrieve messages received by a specific recipient hashtag (e.g. "#it-qa").
     */
    List<ITAgentMessage> findByRecipientHashtagOrderBySentAtDesc(String recipientHashtag);

    /**
     * Retrieve messages sent by a specific sender ID.
     */
    List<ITAgentMessage> findBySenderIdOrderBySentAtDesc(Long senderId);

    /**
     * Retrieve messages containing a specific hashtag in the parsed hashtags string.
     */
    List<ITAgentMessage> findByParsedHashtagsContainingIgnoreCaseOrderBySentAtDesc(String hashtag);

    /**
     * Find unread messages across the entire team (isRead = false).
     */
    List<ITAgentMessage> findByIsReadFalse();

    /**
     * JPQL alias for findByReadStatusFalse requested in prompt.
     */
    @Query("SELECT m FROM ITAgentMessage m WHERE m.isRead = false ORDER BY m.sentAt DESC")
    List<ITAgentMessage> findByReadStatusFalse();

    /**
     * Find unread messages targeted to a specific recipient ID.
     */
    List<ITAgentMessage> findByRecipientIdAndIsReadFalse(Long recipientId);

    /**
     * Find unread messages targeted to a specific recipient hashtag.
     */
    List<ITAgentMessage> findByRecipientHashtagAndIsReadFalse(String recipientHashtag);

    /**
     * Count total unread messages (for UI notification badges).
     */
    long countByIsReadFalse();

    /**
     * JPQL alias for countByReadStatusFalse.
     */
    @Query("SELECT COUNT(m) FROM ITAgentMessage m WHERE m.isRead = false")
    long countByReadStatusFalse();

    /**
     * Count unread messages for a specific recipient.
     */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /**
     * Retrieve all messages ordered chronologically (oldest to newest).
     */
    List<ITAgentMessage> findAllByOrderBySentAtAsc();

    /**
     * Retrieve all messages ordered newest first.
     */
    List<ITAgentMessage> findAllByOrderBySentAtDesc();
}
```

---

### 2.4 ITAgentActivityRepository

**File Location:** `src/main/java/com/dentalclinic/itteam/repository/ITAgentActivityRepository.java`  
**Target Entity:** `com.dentalclinic.itteam.model.ITAgentActivity`

```java
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
     * Paginated audit logs filtered by action type (e.g. "MENTIONED", "API_RUN", "STATUS_CHANGE").
     */
    Page<ITAgentActivity> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);

    /**
     * Paginated audit logs filtered by both agent code and action type.
     */
    Page<ITAgentActivity> findByAgentCodeAndActionTypeOrderByTimestampDesc(String agentCode, String actionType, Pageable pageable);

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
}
```

---

### 2.5 ITBrowserTabRecordRepository

**File Location:** `src/main/java/com/dentalclinic/itteam/repository/ITBrowserTabRecordRepository.java`  
**Target Entity:** `com.dentalclinic.itteam.model.ITBrowserTabRecord`

```java
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
```

---

### 2.6 ITApiRunLogRepository

**File Location:** `src/main/java/com/dentalclinic/itteam/repository/ITApiRunLogRepository.java`  
**Target Entity:** `com.dentalclinic.itteam.model.ITApiRunLog`

```java
package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITApiRunLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITApiRunLogRepository extends JpaRepository<ITApiRunLog, Long> {

    /**
     * Retrieve all API run logs ordered by run timestamp descending.
     */
    List<ITApiRunLog> findAllByOrderByRunTimestampDesc();

    /**
     * Retrieve top 50 recent API runs for the API Monitor view.
     */
    List<ITApiRunLog> findTop50ByOrderByRunTimestampDesc();

    /**
     * Paginated API run logs for history exploration.
     */
    Page<ITApiRunLog> findAllByOrderByRunTimestampDesc(Pageable pageable);

    /**
     * Search API runs by endpoint substring.
     */
    List<ITApiRunLog> findByEndpointContainingIgnoreCaseOrderByRunTimestampDesc(String endpoint);

    /**
     * Filter API runs by HTTP status code (e.g. 200, 401, 500).
     */
    List<ITApiRunLog> findByStatusCodeOrderByRunTimestampDesc(Integer statusCode);

    /**
     * Filter API runs by HTTP method (e.g. "GET", "POST", "PUT", "DELETE").
     */
    List<ITApiRunLog> findByHttpMethodOrderByRunTimestampDesc(String httpMethod);

    /**
     * Count API runs by HTTP status code.
     */
    long countByStatusCode(Integer statusCode);
}
```

---

## 3. `ITTeamDataInitializer` Implementation Specification

### 3.1 Architecture & Startup Lifecycle

- **Component:** `@Component`
- **Execution Interface:** `org.springframework.boot.CommandLineRunner`
- **Ordering:** `@Order(2)` (Executes safely after primary `com.dentalclinic.config.DataInitializer` which seeds baseline users and appointments at `@Order(1)` or default order).
- **Transactionality:** Method-level `@Transactional` to guarantee atomic seeding.
- **Idempotency Guarantee:**
  - **Profiles:** Checks `profileRepository.existsByAgentCode(profile.getAgentCode())` for each standard agent.
  - **Memories:** Checks `memoryRepository.existsByAgentCodeAndMemoryKey(agentCode, memoryKey)`.
  - **Browser Tabs:** Checks `tabRepository.existsByAgentCodeAndUrlRoute(agentCode, urlRoute)`.
  - **Messages:** Checks `messageRepository.count() == 0`.
  - **Activities:** Checks `activityRepository.count() == 0`.
  - **Admin User (Safety Check):** Checks `userRepository.findByUsername("admin")` and seeds `ROLE_ADMIN` if missing.

### 3.2 Complete Java Implementation

**File Location:** `src/main/java/com/dentalclinic/itteam/config/ITTeamDataInitializer.java`

```java
package com.dentalclinic.itteam.config;

import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.repository.*;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(2)
public class ITTeamDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ITTeamDataInitializer.class);

    private final ITAgentProfileRepository profileRepository;
    private final ITAgentMemoryRepository memoryRepository;
    private final ITAgentMessageRepository messageRepository;
    private final ITAgentActivityRepository activityRepository;
    private final ITBrowserTabRecordRepository tabRepository;
    private final ITApiRunLogRepository apiRunLogRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ITTeamDataInitializer(
            ITAgentProfileRepository profileRepository,
            ITAgentMemoryRepository memoryRepository,
            ITAgentMessageRepository messageRepository,
            ITAgentActivityRepository activityRepository,
            ITBrowserTabRecordRepository tabRepository,
            ITApiRunLogRepository apiRunLogRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.profileRepository = profileRepository;
        this.memoryRepository = memoryRepository;
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
        this.tabRepository = tabRepository;
        this.apiRunLogRepository = apiRunLogRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🤖 Checking IT Team Command Center initial data...");

        seedAdminUserIfMissing();
        seedStandardAgentProfiles();
        seedAgentMemories();
        seedBrowserTabs();
        seedInitialMessages();
        seedInitialActivities();

        log.info("✅ IT Team Command Center initial data check complete.");
    }

    private void seedAdminUserIfMissing() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            log.info("🌱 Seeding dedicated IT Team admin account (admin / 123) with ROLE_ADMIN...");
            String encodedPass = passwordEncoder.encode("123");
            User admin = new User(
                    "admin",
                    encodedPass,
                    "Quản Trị Viên Hệ Thống (IT Admin)",
                    "0909998877",
                    "admin@dental.vn",
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);
        }
    }

    private void seedStandardAgentProfiles() {
        List<ITAgentProfile> standardProfiles = List.of(
                new ITAgentProfile(
                        "backend",
                        "#it-backend",
                        "Alex Rivera (Backend Architect)",
                        "Backend Architect",
                        "ONLINE",
                        "Spring Boot, Database, Security, REST APIs",
                        "fa-server",
                        "You are Alex Rivera, Senior Backend Architect for DentalCare Management Portal. You specialize in Spring Boot 3.2.5, Spring Data JPA, REST API design, and robust transaction management. Always provide clean, production-ready Java code."
                ),
                new ITAgentProfile(
                        "frontend",
                        "#it-frontend",
                        "Elena Chen (Frontend Lead)",
                        "Management Portal UI Specialist",
                        "ONLINE",
                        "Management Portal UI, API Client, Responsive UX",
                        "fa-desktop",
                        "You are Elena Chen, Frontend Lead for DentalCare Management Portal. You are an expert in vanilla ES6+, Tailwind CSS, responsive healthcare dashboards, and dynamic UI interactions."
                ),
                new ITAgentProfile(
                        "qa",
                        "#it-qa",
                        "Marcus Vance (QA & Test Automation)",
                        "QA & Reliability Engineer",
                        "ONLINE",
                        "API Testing, Authorization Checks, Regression",
                        "fa-vial-circle-check",
                        "You are Marcus Vance, QA & Test Automation Specialist. You focus on automated API testing, regression validation, RBAC verification, and edge case detection across all DentalCare services."
                ),
                new ITAgentProfile(
                        "devops",
                        "#it-devops",
                        "Liam O'Connor (DevOps & SRE)",
                        "DevOps & Infrastructure Engineer",
                        "ONLINE",
                        "Build, Server Runtime, Logs, Tunnel Integration",
                        "fa-network-wired",
                        "You are Liam O'Connor, DevOps & SRE for DentalCare. You oversee Maven builds, H2/PostgreSQL runtime operations, application logs, and local proxy/tunnel routing."
                ),
                new ITAgentProfile(
                        "security",
                        "#it-security",
                        "Aria Sterling (Security Specialist)",
                        "Cybersecurity & Compliance Officer",
                        "ONLINE",
                        "RBAC, Data Privacy, Input Validation, Audit Logs",
                        "fa-shield-halved",
                        "You are Aria Sterling, Lead Security Architect. You strictly enforce HIPAA/GDPR healthcare privacy, JWT token security, role-based access control (RBAC), and prevent PII leakage."
                )
        );

        for (ITAgentProfile profile : standardProfiles) {
            if (!profileRepository.existsByAgentCode(profile.getAgentCode())) {
                profileRepository.save(profile);
                log.info("   -> Seeded agent profile: {} ({})", profile.getDisplayName(), profile.getHashtag());
            }
        }
    }

    private void seedAgentMemories() {
        record MemorySeed(String agentCode, String key, String content, String priority) {}

        List<MemorySeed> seeds = List.of(
                // #it-backend
                new MemorySeed(
                        "backend",
                        "ARCHITECTURE_OVERVIEW",
                        "DentalCare core runs Spring Boot 3.2.5 on Java 17 with Spring Data JPA and H2/PostgreSQL database.",
                        "HIGH"
                ),
                new MemorySeed(
                        "backend",
                        "PERSISTENCE_LAYER",
                        "Entities extend com.dentalclinic.common.BaseEntity for automatic createdAt and updatedAt auditing.",
                        "MEDIUM"
                ),
                new MemorySeed(
                        "backend",
                        "REST_API_CONVENTION",
                        "All controller endpoints return ApiResponse<T> envelope with standard success, message, data, and timestamp.",
                        "MEDIUM"
                ),

                // #it-frontend
                new MemorySeed(
                        "frontend",
                        "UI_STACK",
                        "Portal frontend uses vanilla ES6, Tailwind CSS, FontAwesome icons, and static SPA routing.",
                        "HIGH"
                ),
                new MemorySeed(
                        "frontend",
                        "CLIENT_AUTH",
                        "JWT token stored in sessionStorage('token'); sent in Authorization Bearer header.",
                        "HIGH"
                ),
                new MemorySeed(
                        "frontend",
                        "PORTAL_NAVIGATION",
                        "IT Team Command Center lives in #section-itteam with 5 tab views: Profiles, Chat, Memories, Logs, API Monitor.",
                        "MEDIUM"
                ),

                // #it-qa
                new MemorySeed(
                        "qa",
                        "TEST_SUITE_STATUS",
                        "Automated test suite covers auth endpoints, appointment booking, and role permissions.",
                        "HIGH"
                ),
                new MemorySeed(
                        "qa",
                        "REGRESSION_CHECKLIST",
                        "Verify 401/403 responses for unauthenticated requests and check non-interference with booking flow.",
                        "HIGH"
                ),

                // #it-devops
                new MemorySeed(
                        "devops",
                        "SERVER_RUNTIME",
                        "Application runs on port 8080. Local 9Router AI orchestrator accessible on port 20128.",
                        "HIGH"
                ),
                new MemorySeed(
                        "devops",
                        "DATABASE_STORAGE",
                        "H2 database file stored at ./data/dentaldb.mv.db with AUTO_SERVER=TRUE.",
                        "MEDIUM"
                ),

                // #it-security
                new MemorySeed(
                        "security",
                        "PRIVACY_GUARDRAIL",
                        "Strictly redact JWT tokens, passwords, Authorization headers, and EMR medical records from all logs.",
                        "HIGH"
                ),
                new MemorySeed(
                        "security",
                        "RBAC_POLICY",
                        "IT Team endpoints at /api/it-team/** strictly require ROLE_ADMIN or ROLE_OWNER authority.",
                        "HIGH"
                )
        );

        for (MemorySeed s : seeds) {
            if (!memoryRepository.existsByAgentCodeAndMemoryKey(s.agentCode(), s.key())) {
                ITAgentProfile profile = profileRepository.findByAgentCode(s.agentCode()).orElse(null);
                Long agentId = profile != null ? profile.getId() : null;

                ITAgentMemory mem = new ITAgentMemory(
                        agentId,
                        s.agentCode(),
                        s.key(),
                        s.content(),
                        s.priority()
                );
                memoryRepository.save(mem);
            }
        }
    }

    private void seedBrowserTabs() {
        record TabSeed(String agentCode, String title, String url, String category, String status) {}

        List<TabSeed> initialTabs = List.of(
                new TabSeed(
                        "frontend",
                        "DentalCare Management Portal",
                        "http://localhost:8080/index.html",
                        "PORTAL",
                        "OPEN"
                ),
                new TabSeed(
                        "backend",
                        "OpenAPI 3.0 / Swagger UI",
                        "http://localhost:8080/swagger-ui/index.html",
                        "DOCS",
                        "BACKGROUND"
                ),
                new TabSeed(
                        "devops",
                        "H2 Database Console",
                        "http://localhost:8080/h2-console",
                        "MONITORING",
                        "BACKGROUND"
                ),
                new TabSeed(
                        "security",
                        "IT Team Security Audit & RBAC Monitor",
                        "http://localhost:8080/api/it-team/activities",
                        "API_TESTER",
                        "OPEN"
                ),
                new TabSeed(
                        "qa",
                        "API Runner & Endpoint Inspector",
                        "http://localhost:8080/api/it-team/api-runs",
                        "API_TESTER",
                        "BACKGROUND"
                )
        );

        for (TabSeed t : initialTabs) {
            if (!tabRepository.existsByAgentCodeAndUrlRoute(t.agentCode(), t.url())) {
                ITAgentProfile profile = profileRepository.findByAgentCode(t.agentCode()).orElse(null);
                Long agentId = profile != null ? profile.getId() : null;

                ITBrowserTabRecord record = new ITBrowserTabRecord(
                        agentId,
                        t.agentCode(),
                        t.title(),
                        t.url(),
                        t.category(),
                        t.status()
                );
                tabRepository.save(record);
            }
        }
    }

    private void seedInitialMessages() {
        if (messageRepository.count() == 0) {
            User owner = userRepository.findByUsername("owner").orElse(null);
            Long ownerId = owner != null ? owner.getId() : 1L;

            ITAgentMessage welcomeMessage = new ITAgentMessage(
                    ownerId,
                    "BS.CKII Trần Văn Thắng (Chủ Phòng Khám)",
                    "USER",
                    null,
                    "BROADCAST",
                    "Welcome team to the DentalCare IT Command Center! Please report your current operational status. #it-backend #it-frontend #it-qa #it-devops #it-security",
                    "#it-backend,#it-frontend,#it-qa,#it-devops,#it-security",
                    null
            );
            messageRepository.save(welcomeMessage);
            log.info("   -> Seeded initial IT team broadcast welcome message.");
        }
    }

    private void seedInitialActivities() {
        if (activityRepository.count() == 0) {
            ITAgentProfile devops = profileRepository.findByAgentCode("devops").orElse(null);
            Long devopsId = devops != null ? devops.getId() : null;

            ITAgentActivity bootActivity = new ITAgentActivity(
                    devopsId,
                    "devops",
                    "SYSTEM_BOOT",
                    "IT Team Command Center services initialized successfully.",
                    "Seeded 5 IT agent profiles, initial memories, and browser tab records.",
                    "/api/it-team/agents"
            );
            activityRepository.save(bootActivity);
            log.info("   -> Seeded initial system boot activity record.");
        }
    }
}
```

---

## 4. Verification and Testing Guidelines

### 4.1 Unit & Integration Test Checks

1. **Idempotency Test (`testSeederIdempotencyOnRestart`)**:
   - Invoke `itTeamDataInitializer.run()`.
   - Assert `profileRepository.count() == 5`.
   - Invoke `itTeamDataInitializer.run()` again.
   - Assert `profileRepository.count() == 5` (no duplicates).
   - Assert `memoryRepository.count() == 12` (no duplicates).
   - Assert `tabRepository.count() == 5` (no duplicates).
   - Assert `messageRepository.count() == 1` (no duplicates).
2. **Query Derivation Test (`testCustomQueryMethods`)**:
   - Test `profileRepository.findByHashtag("#it-backend")` returns profile with agentCode "backend".
   - Test `memoryRepository.findByAgentCodeOrderByLastUpdatedDesc("backend")` returns non-empty list.
   - Test `messageRepository.findByReadStatusFalse()` returns list containing welcome message.
   - Test `activityRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 10))` returns `Page<ITAgentActivity>`.
   - Test `tabRepository.findByAgentCodeAndStatus("frontend", "OPEN")` returns open tab.

---
