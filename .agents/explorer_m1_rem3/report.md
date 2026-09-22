# Milestone 1 Iteration 2 Remediation Analysis Report: Seeder & Test Suite

**Agent:** Explorer 3 (`explorer_m1_rem3`)  
**Mission:** Analyze and design fixes for `ITTeamDataInitializer`, repositories, and test suites (`ITTeamMilestone1EmpiricalStressTest.java`, `SensitiveDataSanitizerAdversarialTest.java`, `SensitiveDataSanitizerChallengerTest.java`, `ITTeamM1PersistenceTest.java`).  
**Working Directory:** `D:\java\dental-clinic\.agents\explorer_m1_rem3`  
**Date:** 2026-09-12  

---

## 1. Executive Summary

Milestone 1 delivered the initial persistence layer, data seeder, and privacy sanitizer. However, empirical stress testing conducted by Challengers 1 and 2 revealed critical defects in seeder resilience, entity constraints, and privacy guardrails.

Specifically regarding **Seeder & Test Suites**:
1. **Monolithic Seeder Guard (`ITTeamDataInitializer.java:53`)**: The seeder wrapped all child entity seeding (`seedAgentMemories()`, `seedBrowserTabs()`, `seedInitialMessages()`, `seedInitialActivities()`) inside `if (profileRepository.count() == 0)`. If profiles existed but memories or tabs were cleared, reset, or partially seeded, the seeder skipped child data completely.
2. **Missing Granular Existence Checks**: Welcome messages and bootstrap activities relied on table-wide `count() == 0` instead of per-item identifiers.
3. **Challenger Tests Inversion**: The tests authored in `ITTeamMilestone1EmpiricalStressTest.java` and `SensitiveDataSanitizerAdversarialTest.java` were written to **reproduce and assert existing defects** (e.g. asserting that memories remained 0, asserting that passwords leaked, asserting that DB truncation errors were thrown). To remediate Milestone 1, these tests must be transitioned to **compliance regression tests** that verify correct behavior.
4. **Missing Repository Existence Methods**: `ITAgentMessageRepository` and `ITAgentActivityRepository` lacked targeted existence query methods for broadcast welcome messages and system boot activities.

This report provides the complete architectural design and code specifications to remediate `ITTeamDataInitializer`, update the repository interfaces, and harmonize all 5 test suites.

---

## 2. ITTeamDataInitializer Remediation Design

### 2.1 Problem Analysis
In `src/main/java/com/dentalclinic/itteam/config/ITTeamDataInitializer.java`:
- Lines 53–64:
  ```java
  if (profileRepository.count() == 0) {
      log.info("🌱 No IT agent profiles found. Seeding 5 standard IT profiles...");
      seedStandardAgentProfiles();
      seedAgentMemories();
      seedBrowserTabs();
      seedInitialMessages();
      seedInitialActivities();
      log.info("✅ IT Team Command Center initial data seeded successfully.");
  } else {
      log.info("ℹ️ IT Team profiles already exist (count={}). Skipping initial seeding.", profileRepository.count());
  }
  ```
- Because of this top-level check, deleting memories (`memoryRepository.deleteAll()`) followed by running `dataInitializer.run()` resulted in zero memories restored, failing stress test ST-1.2.
- Furthermore, `seedInitialMessages()` checked `messageRepository.count() == 0` and `seedInitialActivities()` checked `activityRepository.count() == 0`. If any subsequent message or activity existed in the database, initial welcome messages and boot activity could not be restored.

### 2.2 Proposed Architectural Fix
1. **Decouple `run()`**: Remove the overarching `profileRepository.count() == 0` conditional. Unconditionally execute all 5 seeding methods:
   - `seedStandardAgentProfiles()`
   - `seedAgentMemories()`
   - `seedBrowserTabs()`
   - `seedInitialMessages()`
   - `seedInitialActivities()`
2. **Granular Per-Item Idempotency**:
   - **Profiles**: Check `!profileRepository.existsByAgentCode(profile.getAgentCode())` (Already implemented).
   - **Memories**: Check `!memoryRepository.existsByAgentCodeAndMemoryKey(s.agentCode(), s.key())` (Already implemented).
   - **Browser Tabs**: Check `!tabRepository.existsByAgentCodeAndUrlRoute(t.agentCode(), t.url())` (Already implemented).
   - **Initial Welcome Message**: Change check from `messageRepository.count() == 0` to `!messageRepository.existsByRecipientHashtag("BROADCAST")`.
   - **Initial System Boot Activity**: Change check from `activityRepository.count() == 0` to `!activityRepository.existsByAgentCodeAndActionType("devops", "SYSTEM_BOOT")`.

### 2.3 Proposed Code Changes for `ITTeamDataInitializer.java`

```java
// Target: src/main/java/com/dentalclinic/itteam/config/ITTeamDataInitializer.java

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🤖 Checking IT Team Command Center initial data...");

        seedStandardAgentProfiles();
        seedAgentMemories();
        seedBrowserTabs();
        seedInitialMessages();
        seedInitialActivities();

        log.info("✅ IT Team Command Center data check and initialization complete.");
    }

    private void seedInitialMessages() {
        if (!messageRepository.existsByRecipientHashtag("BROADCAST")) {
            ITAgentMessage welcomeMessage = new ITAgentMessage(
                    1L,
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
        if (!activityRepository.existsByAgentCodeAndActionType("devops", "SYSTEM_BOOT")) {
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
```

---

## 3. Required Repository Enhancements

To support per-item existence checks in `ITTeamDataInitializer`, the following Spring Data JPA query methods must be added:

### 3.1 `ITAgentMessageRepository.java`
File: `src/main/java/com/dentalclinic/itteam/repository/ITAgentMessageRepository.java`

Add method:
```java
    /**
     * Check if a broadcast or specific recipient message exists.
     * Used for idempotent seeding of the initial welcome message.
     */
    boolean existsByRecipientHashtag(String recipientHashtag);
```

### 3.2 `ITAgentActivityRepository.java`
File: `src/main/java/com/dentalclinic/itteam/repository/ITAgentActivityRepository.java`

Add method:
```java
    /**
     * Check if an activity of a specific type exists for an agent code.
     * Used for idempotent seeding of the SYSTEM_BOOT activity record.
     */
    boolean existsByAgentCodeAndActionType(String agentCode, String actionType);
```

---

## 4. Test Suite Harmonization: `ITTeamMilestone1EmpiricalStressTest.java`

The stress test suite `ITTeamMilestone1EmpiricalStressTest.java` contains 11 test cases across 3 sections.
The table below details each test case's original diagnostic assertion vs. the remediated compliance assertion:

| Test # | Scenario | Original Defect Assertion | Remediated Compliance Assertion | Rationale |
|---|---|---|---|---|
| **ST-1.1** | Double invocation idempotency | No duplicates (`pCount1 == 5`, etc.) | No duplicates (`assertThat(count).isEqualTo(initial)`) | Already valid; passes. |
| **ST-1.2** | Partial state (memories wiped) | `assertThat(memoryRepository.count()).isZero()` | `assertThat(memoryRepository.count()).isEqualTo(12)` | With per-item seeder, memories re-seed even when profiles exist. |
| **ST-1.3** | Duplicate `(agentCode, key)` insertion | Throws `IncorrectResultSizeDataAccessException` on `findByAgentCodeAndMemoryKey` | Throws `DataIntegrityViolationException` on `saveAndFlush(mem2)` | DB `@UniqueConstraint(name="uk_agent_mem_key")` prevents duplicate insert at source. |
| **ST-2.1** | Orphaned memory | Asserts null references permitted | Retain as is | Validates nullable agentId/agentCode. |
| **ST-2.2** | Null `memoryContent` | Throws exception | Retain as is | Validates `@Column(nullable = false)`. |
| **ST-2.3** | Activity description > 1000 chars | Throws `DataIntegrityViolationException` | `activityRepository.saveAndFlush(activity)` succeeds without error | `ITAgentActivity.description` is upgraded to `columnDefinition = "TEXT"`. |
| **ST-2.4** | API run log null `statusCode` | Throws `PropertyValueException` | `apiRunLogRepository.saveAndFlush(log)` succeeds without error | `ITApiRunLog.statusCode` is made nullable for connection timeouts. |
| **ST-3.1** | Escaped quotes in JSON medical field | `assertThat(sanitized).contains("sâu răng nặng")` | `assertThat(sanitized).doesNotContain("sâu răng nặng").contains("[REDACTED_MEDICAL]")` | `SensitiveDataSanitizer` regex handles escaped quotes without truncation. |
| **ST-3.2** | OAuth snake_case tokens | Asserts tokens remain unredacted | `assertThat(sanitized).contains("\"access_token\": \"[REDACTED]\"").doesNotContain("gho_...")` | `SensitiveDataSanitizer` includes `access_token` and `refresh_token`. |
| **ST-3.3** | `containsUnsanitizedSensitiveData` | Asserts `.isFalse()` (demonstrating false negatives) | Asserts `.isTrue()` for all 6 patterns | `containsUnsanitizedSensitiveData` tests all 15 declared patterns. |
| **ST-3.4** | 9-digit transaction amount | Asserts amount converted to `[REDACTED_ID]` | `assertThat(sanitized).contains("\"amount\": 100000000").doesNotContain("[REDACTED_ID]")` | `CCCD_PATTERN` avoids false-positive on financial amounts. |

### 4.1 Exact Replacement Code for `ITTeamMilestone1EmpiricalStressTest.java`

```java
package com.dentalclinic.itteam;

import com.dentalclinic.itteam.config.ITTeamDataInitializer;
import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.repository.*;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("IT Team Milestone 1 Empirical Challenger Stress Test Suite")
public class ITTeamMilestone1EmpiricalStressTest {

    @Autowired
    private ITAgentProfileRepository profileRepository;

    @Autowired
    private ITAgentMemoryRepository memoryRepository;

    @Autowired
    private ITAgentMessageRepository messageRepository;

    @Autowired
    private ITAgentActivityRepository activityRepository;

    @Autowired
    private ITBrowserTabRecordRepository tabRepository;

    @Autowired
    private ITApiRunLogRepository apiRunLogRepository;

    @Autowired
    private ITTeamDataInitializer dataInitializer;

    @Autowired
    private EntityManager entityManager;

    // =========================================================================
    // 1. ITTeamDataInitializer Idempotency & Failure Mode Stress Tests
    // =========================================================================

    @Test
    @DisplayName("ST-1.1 - Double invocation of ITTeamDataInitializer does not duplicate records in standard state")
    void testStandardDoubleInvocation() throws Exception {
        long pCount1 = profileRepository.count();
        long mCount1 = memoryRepository.count();
        long tCount1 = tabRepository.count();

        // 2nd run
        dataInitializer.run();

        assertThat(profileRepository.count()).isEqualTo(pCount1);
        assertThat(memoryRepository.count()).isEqualTo(mCount1);
        assertThat(tabRepository.count()).isEqualTo(tCount1);
    }

    @Test
    @DisplayName("ST-1.2 - Partial State Reconciliation: If profiles exist but memories/tabs are deleted, re-run re-seeds child data")
    void testPartialStateReconciliation() throws Exception {
        // Given profiles exist (seeded on startup)
        assertThat(profileRepository.count()).isEqualTo(5);

        // Suppose memories were wiped or failed during initial deployment
        memoryRepository.deleteAll();
        memoryRepository.flush();
        assertThat(memoryRepository.count()).isZero();

        // When dataInitializer is run again
        dataInitializer.run();

        // After remediation: Seeder reconciles and re-seeds memories even when profiles already exist
        assertThat(memoryRepository.count()).isEqualTo(12);

        // Test tabs reconciliation as well
        tabRepository.deleteAll();
        tabRepository.flush();
        assertThat(tabRepository.count()).isZero();

        dataInitializer.run();
        assertThat(tabRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("ST-1.3 - Database Unique Constraint on (agent_code, memory_key) prevents duplicate memory keys")
    void testDuplicateMemoryKeyConstraint() {
        ITAgentMemory mem1 = new ITAgentMemory(1L, "backend", "CUSTOM_KEY", "Content 1", "MEDIUM");
        ITAgentMemory mem2 = new ITAgentMemory(1L, "backend", "CUSTOM_KEY", "Content 2", "HIGH");

        memoryRepository.saveAndFlush(mem1);

        // With unique constraint, saving duplicate (agent_code, memory_key) throws DataIntegrityViolationException
        assertThatThrownBy(() -> memoryRepository.saveAndFlush(mem2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================================
    // 2. Model Field Validation, Nullability & Size Limit Stress Tests
    // =========================================================================

    @Test
    @DisplayName("ST-2.1 - Orphaned Memory Allowed: agentCode and agentId both null can be persisted")
    void testOrphanedMemoryPersisted() {
        ITAgentMemory orphaned = new ITAgentMemory(null, null, "ORPHAN_KEY", "Orphaned content", "LOW");
        ITAgentMemory saved = memoryRepository.saveAndFlush(orphaned);
        entityManager.clear();

        ITAgentMemory reloaded = memoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAgentCode()).isNull();
        assertThat(reloaded.getAgentId()).isNull();
    }

    @Test
    @DisplayName("ST-2.2 - Null memoryContent violates nullable = false")
    void testNullMemoryContentFails() {
        ITAgentMemory invalid = new ITAgentMemory(1L, "backend", "KEY_X", null, "LOW");
        assertThatThrownBy(() -> memoryRepository.saveAndFlush(invalid))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("ST-2.3 - ITAgentActivity description supports large text (>1000 chars) via TEXT columnDefinition")
    void testActivityDescriptionExceeds1000() {
        // Large stack trace or audit log entry (1050 chars)
        String longDescription = "API failure stack trace: " + "a".repeat(1050);
        ITAgentActivity activity = new ITAgentActivity(
                1L, "backend", "API_ERROR", longDescription, "Failed", "/api/error"
        );

        ITAgentActivity saved = activityRepository.saveAndFlush(activity);
        entityManager.clear();

        ITAgentActivity reloaded = activityRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo(longDescription);
    }

    @Test
    @DisplayName("ST-2.4 - ITApiRunLog supports null statusCode for connection timeouts/errors")
    void testApiRunLogNullStatusCodeSupported() {
        // When HTTP request fails before handshake (timeout/connect refused), statusCode is null
        ITApiRunLog connectionFailedLog = new ITApiRunLog(
                "/api/external/timeout", "GET", null, 5000L, null, "Connection timed out", "ROLE_ADMIN"
        );

        ITApiRunLog saved = apiRunLogRepository.saveAndFlush(connectionFailedLog);
        entityManager.clear();

        ITApiRunLog reloaded = apiRunLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getStatusCode()).isNull();
        assertThat(reloaded.getErrorMessage()).isEqualTo("Connection timed out");
    }

    // =========================================================================
    // 3. SensitiveDataSanitizer Empirical Vulnerability Stress Tests
    // =========================================================================

    @Test
    @DisplayName("ST-3.1 - Escaped quotes in JSON string are fully redacted without leaking medical data or corrupting JSON")
    void testEscapedQuotesLeakMedicalData() {
        String input = "{\"diagnosis\": \"Bệnh nhân bị \\\"sâu răng nặng\\\" ở hàm trên\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(sanitized).doesNotContain("sâu răng nặng");
    }

    @Test
    @DisplayName("ST-3.2 - OAuth2 snake_case access_token and refresh_token are redacted")
    void testSnakeCaseOAuthTokensRedacted() {
        String oauthPayload = "{\"access_token\": \"gho_16C7e42F292c6912E7710c838347Ae178B4a\", \"refresh_token\": \"rfr_SecretOauthRefreshToken9988\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(oauthPayload);

        assertThat(sanitized).contains("\"access_token\": \"[REDACTED]\"");
        assertThat(sanitized).contains("\"refresh_token\": \"[REDACTED]\"");
        assertThat(sanitized).doesNotContain("gho_16C7e42F292c6912E7710c838347Ae178B4a");
        assertThat(sanitized).doesNotContain("rfr_SecretOauthRefreshToken9988");
    }

    @Test
    @DisplayName("ST-3.3 - containsUnsanitizedSensitiveData detects all sensitive data patterns")
    void testContainsUnsanitizedSensitiveDataOmissions() {
        // 1. Basic Auth detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Authorization: Basic dXNlcjpwYXNz"))
                .as("Basic auth header should be detected").isTrue();

        // 2. Plain text password detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Password: SuperSecretPassword123"))
                .as("Plaintext password should be detected").isTrue();

        // 3. Unquoted numeric password detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"password\": 987654}"))
                .as("Unquoted numeric password should be detected").isTrue();

        // 4. Standalone session cookie detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Session param JSESSIONID=SECRET98765"))
                .as("Session cookie should be detected").isTrue();

        // 5. Plaintext medical diagnosis detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Diagnosis: Viêm tủy răng cấp"))
                .as("Plaintext medical diagnosis should be detected").isTrue();

        // 6. CCCD detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("CCCD: 001201012345"))
                .as("CCCD should be detected").isTrue();
    }

    @Test
    @DisplayName("ST-3.4 - 9-digit payment amount is preserved without false positive CCCD/CMND redaction")
    void testNineDigitAmountFalselyRedacted() {
        String payment = "{\"amount\": 100000000, \"currency\": \"VND\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(payment);

        assertThat(sanitized).contains("\"amount\": 100000000");
        assertThat(sanitized).doesNotContain("[REDACTED_ID]");
    }
}
```

---

## 5. Companion Test Suite Adjustments

### 5.1 `SensitiveDataSanitizerAdversarialTest.java`
File: `src/test/java/com/dentalclinic/itteam/SensitiveDataSanitizerAdversarialTest.java`

Update the 5 tests to assert remediation:
- **ADV-1 (`testEscapedQuotesInJsonMedicalField`)**: Assert that `"diagnosis"` with escaped quotes is redacted to `[REDACTED_MEDICAL]`, and `"sâu răng nặng"` is NOT in output.
- **ADV-2 (`testEscapedQuotesInJsonProperty`)**: Assert that `"password"` with escaped quotes is redacted to `[REDACTED]`, and `"12345"` is NOT in output.
- **ADV-3 (`testSnakeCaseOAuthTokens`)**: Assert that `access_token` and `refresh_token` are redacted to `[REDACTED]`.
- **ADV-4 (`testContainsUnsanitizedSensitiveDataOmissions`)**: Assert that `containsUnsanitizedSensitiveData` returns `.isTrue()` for all 6 patterns.
- **ADV-5 (`testNineDigitAmountFalselyRedacted`)**: Assert that `"amount": 100000000` is retained and does not contain `[REDACTED_ID]`.

### 5.2 `SensitiveDataSanitizerChallengerTest.java`
File: `src/test/java/com/dentalclinic/itteam/SensitiveDataSanitizerChallengerTest.java`

- **Line 433 (`testBrowserTabRecordMissingSanitization`)**:
  Currently:
  ```java
  assertThat(urlLeaked).as("ITBrowserTabRecord urlRoute leaks sensitive data without pre-persist sanitizer!").isTrue();
  ```
  Change to:
  ```java
  assertThat(urlLeaked).as("ITBrowserTabRecord urlRoute must not leak sensitive data after pre-persist sanitizer").isFalse();
  assertThat(titleLeaked).as("ITBrowserTabRecord tabTitle must not leak sensitive data after pre-persist sanitizer").isFalse();
  assertThat(tab.getUrlRoute()).contains("password=[REDACTED]");
  assertThat(tab.getUrlRoute()).contains("[REDACTED_JWT]");
  ```

### 5.3 `ITTeamM1PersistenceTest.java`
File: `src/test/java/com/dentalclinic/itteam/ITTeamM1PersistenceTest.java`

Add test `M1.8` for `ITBrowserTabRecord` pre-persistence sanitization to complete the test suite:
```java
    @Test
    @DisplayName("M1.8 - Database Persistence with Pre-Persist Sanitization for ITBrowserTabRecord")
    void testBrowserTabRecordPrePersistSanitization() {
        String dirtyUrl = "http://localhost:8080/api/auth/reset?password=SecretPass123&token=" + SAMPLE_JWT;
        String dirtyTitle = "Reset Password (password=SecretPass123)";

        ITBrowserTabRecord tab = new ITBrowserTabRecord(
                1L,
                "backend",
                dirtyTitle,
                dirtyUrl,
                "SECURITY",
                "OPEN"
        );

        ITBrowserTabRecord saved = tabRepository.saveAndFlush(tab);
        entityManager.clear();

        ITBrowserTabRecord reloaded = tabRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getUrlRoute()).contains("password=[REDACTED]");
        assertThat(reloaded.getUrlRoute()).contains("[REDACTED_JWT]");
        assertThat(reloaded.getUrlRoute()).doesNotContain("SecretPass123");
        assertThat(reloaded.getUrlRoute()).doesNotContain(SAMPLE_JWT);

        assertThat(reloaded.getTabTitle()).contains("password=[REDACTED]");
        assertThat(reloaded.getTabTitle()).doesNotContain("SecretPass123");
        assertThat(reloaded.getOpenedAt()).isNotNull();
    }
```

---

## 6. Verification Command & Worker Execution Plan

### 6.1 Verification Command
The standard Maven verification command to validate all 5 test suites for Milestone 1 Iteration 2 is:
```bash
./mvnw test -Dtest=SensitiveDataSanitizerTest,SensitiveDataSanitizerAdversarialTest,SensitiveDataSanitizerChallengerTest,ITTeamM1PersistenceTest,ITTeamMilestone1EmpiricalStressTest
```
Or on Windows PowerShell / CMD:
```powershell
.\mvnw.cmd test -Dtest=SensitiveDataSanitizerTest,SensitiveDataSanitizerAdversarialTest,SensitiveDataSanitizerChallengerTest,ITTeamM1PersistenceTest,ITTeamMilestone1EmpiricalStressTest
```

### 6.2 Worker Implementation Dependency Checklist
To ensure all 5 test suites pass, Worker must apply changes in the following sequence:
1. **Explorer 1 Remediation**: Update `SensitiveDataSanitizer.java` (escaped quotes, OAuth tokens, all-pattern audit, CCCD boundary).
2. **Explorer 2 Remediation**: Update JPA entities (`ITBrowserTabRecord` lifecycle hooks, `ITAgentActivity` TEXT description, `ITAgentMemory` unique constraint, `ITApiRunLog` nullable statusCode).
3. **Explorer 3 Remediation**:
   - Add query methods to `ITAgentMessageRepository.java` (`existsByRecipientHashtag`) and `ITAgentActivityRepository.java` (`existsByAgentCodeAndActionType`).
   - Update `ITTeamDataInitializer.java` to perform per-item existence checks for child memories, virtual tabs, and welcome message.
   - Update `ITTeamMilestone1EmpiricalStressTest.java`, `SensitiveDataSanitizerAdversarialTest.java`, and `SensitiveDataSanitizerChallengerTest.java` to reflect compliance assertions.
   - Add `testBrowserTabRecordPrePersistSanitization` to `ITTeamM1PersistenceTest.java`.
4. Run the verification command to confirm 100% test pass.
