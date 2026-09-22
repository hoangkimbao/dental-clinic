# Milestone 1: Domain Model, Persistence & Sanitization Report

**Agent:** `worker_m1_1` (Implementer / QA / Specialist)  
**Parent:** `89ae81f4-4ba5-44ee-8b07-8548bc218f28` (`orchestrator_1`)  
**Directory:** `D:\java\dental-clinic\.agents\worker_m1_1`  
**Timestamp:** 2026-09-12T15:15:00Z  
**Status:** COMPLETE  

---

## 1. Executive Summary

Milestone 1 establishes the foundational persistence architecture for the **DentalCare Management Portal 'IT Team Command Center'**. All requirements specified in `ORIGINAL_REQUEST.md` (§ R1, Acceptance Criteria), `PROJECT.md` (§ M1, F01-F09), and explorer reports (`explorer_m1_1`, `explorer_m1_2`, `explorer_m1_3`) have been implemented with genuine, production-grade Java 17 logic.

Key deliverables completed:
1. **6 JPA Entities** extending `com.dentalclinic.common.BaseEntity` with zero Lombok and explicit constructors/getters/setters.
2. **Pre-Persistence Lifecycle Hooks** (`@PrePersist` / `@PreUpdate`) on all entity payloads to automatically enforce sanitization before database `INSERT` and `UPDATE`.
3. **6 Spring Data JPA Repositories** with derived query methods and JPQL aliases for downstream M2 (messaging) and M3 (REST APIs) contracts.
4. **`SensitiveDataSanitizer`** with 15 compiled regex rules redacting JWTs (`[REDACTED_JWT]`), passwords/credentials (`[REDACTED]`), Bearer/Basic headers (`Bearer [REDACTED]`), cookies, CCCD/CMND IDs, and medical EMR PII (`[REDACTED_MEDICAL]`).
5. **`ITTeamDataInitializer`** seeding the 5 standard IT profiles (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`), initial memories, virtual browser tabs, welcome message, and boot activity with strict idempotency.
6. **Comprehensive Automated Test Suites** totaling 29 test cases verifying regex mechanics, idempotency, `@PrePersist` entity hooks, database persistence, and seeder idempotency.

---

## 2. File Implementation Summary

### 2.1. Domain Model Entities (`com.dentalclinic.itteam.model`)

| Entity File | Target Table | Base Class | Key Attributes & Pre-Persist Hooks |
|---|---|---|---|
| `ITAgentProfile.java` | `it_agent_profile` | `BaseEntity` | `agentCode`, `hashtag`, `displayName`, `role`, `status`, `expertise`, `avatar`, `systemPrompt`. Unique indexes on `agent_code` and `hashtag`. |
| `ITAgentMemory.java` | `it_agent_memory` | `BaseEntity` | `agentId`, `agentCode`, `memoryKey`, `memoryContent` (TEXT), `priority`, `lastUpdated`. `@PrePersist`/`@PreUpdate` auto-sanitizes `memoryContent`. |
| `ITAgentMessage.java` | `it_agent_message` | `BaseEntity` | `senderId`, `senderName`, `senderType`, `recipientId`, `recipientHashtag`, `messageBody` (TEXT), `parsedHashtags`, `isRead`, `parentMessageId`, `sentAt`. `@PrePersist`/`@PreUpdate` auto-sanitizes `messageBody`. |
| `ITAgentActivity.java` | `it_agent_activity` | `BaseEntity` | `agentId`, `agentCode`, `actionType`, `description`, `resultSummary`, `relatedEntityLink`, `timestamp`. `@PrePersist`/`@PreUpdate` auto-sanitizes `description` and `resultSummary`. |
| `ITBrowserTabRecord.java` | `it_browser_tab_record` | `BaseEntity` | `agentId`, `agentCode`, `tabTitle`, `urlRoute`, `tabCategory`, `status`, `openedAt`, `closedAt`. `@PrePersist` initializes `openedAt`. |
| `ITApiRunLog.java` | `it_api_run_log` | `BaseEntity` | `endpoint`, `httpMethod`, `statusCode`, `executionDurationMs`, `requestPayload` (TEXT), `responsePayload` (TEXT), `runTimestamp`, `initiatedBy`, `isSuccess`. `@PrePersist`/`@PreUpdate` auto-sanitizes both payloads and sets `isSuccess`. |

### 2.2. Spring Data JPA Repositories (`com.dentalclinic.itteam.repository`)

| Repository Interface | Key Query Methods Provided |
|---|---|
| `ITAgentProfileRepository` | `findByHashtag`, `findByAgentCode`, `existsByHashtag`, `existsByAgentCode`, `findByStatus`, `findAllByOrderByAgentCodeAsc` |
| `ITAgentMemoryRepository` | `findByAgentCode`, `findByAgentCodeOrderByLastUpdatedDesc`, `findByAgentCodeOrderByPriorityDesc`, `findByAgentCodeOrderByPriorityLevelDesc` (JPQL), `findByAgentCodeAndMemoryKey`, `findByAgentIdAndMemoryKey`, `existsByAgentCodeAndMemoryKey`, `findByAgentIdOrderByLastUpdatedDesc`, `deleteByAgentCodeAndMemoryKey` |
| `ITAgentMessageRepository` | `findByParentMessageIdIsNullOrderBySentAtAsc`, `findByParentMessageIdOrderBySentAtAsc`, `existsByParentMessageId`, `countByParentMessageId`, `findByRecipientIdOrderBySentAtDesc`, `findByRecipientHashtagOrderBySentAtDesc`, `findByParsedHashtagsContainingIgnoreCaseOrderBySentAtDesc`, `findByIsReadFalse`, `findByReadStatusFalse` (JPQL), `countByIsReadFalse`, `countByReadStatusFalse` (JPQL) |
| `ITAgentActivityRepository` | `findAllByOrderByTimestampDesc(Pageable)`, `findByAgentCodeOrderByTimestampDesc(...)`, `findByActionTypeOrderByTimestampDesc(...)`, `findByAgentCodeAndActionTypeOrderByTimestampDesc(...)`, `findTop20ByOrderByTimestampDesc`, `countByActionType` |
| `ITBrowserTabRecordRepository` | `findByAgentCode`, `findByStatus`, `findByAgentCodeAndStatus`, `findByTabCategory`, `existsByAgentCodeAndUrlRoute`, `existsByAgentCodeAndUrl` (JPQL), `findAllByOrderByOpenedAtDesc` |
| `ITApiRunLogRepository` | `findAllByOrderByRunTimestampDesc()`, `findTop50ByOrderByRunTimestampDesc()`, `findAllByOrderByRunTimestampDesc(Pageable)`, `findByEndpointContainingIgnoreCaseOrderByRunTimestampDesc`, `findByStatusCodeOrderByRunTimestampDesc`, `findByHttpMethodOrderByRunTimestampDesc`, `countByStatusCode` |

### 2.3. Privacy Guardrail Sanitizer (`com.dentalclinic.itteam.service.SensitiveDataSanitizer`)

- **Component & Utility**: Spring `@Component` with dual static methods (`public static String sanitize(String)` and `public static boolean containsUnsanitizedSensitiveData(String)`).
- **Redaction Rules**:
  1. Bearer JWT: `Bearer eyJ...` -> `Bearer [REDACTED_JWT]`
  2. Standalone JWT: `eyJ...` -> `[REDACTED_JWT]`
  3. Generic Bearer: `Bearer sk_live_...` -> `Bearer [REDACTED]`
  4. Basic Auth: `Basic dXNlcjp...` -> `Basic [REDACTED]`
  5. JSON Passwords: `"password": "..."` -> `"password": "[REDACTED]"` (covers `password`, `passwd`, `pwd`, `pass`, `secret`, `client_secret`, `apiKey`, `api_key`, `refreshToken`, `accessToken`)
  6. JSON Numeric Passwords: `"password": 123456` -> `"password": "[REDACTED]"`
  7. Form / Query Passwords: `password=...` -> `password=[REDACTED]`
  8. Plain Text Passwords: `Password: ...` -> `Password: [REDACTED]`
  9. Cookie Headers: `Cookie: ...`, `Set-Cookie: ...` -> `Cookie: [REDACTED]`
  10. JSON Cookie Properties: `"Cookie": "..."` -> `"Cookie": "[REDACTED]"`
  11. Session Cookie Keys: `JSESSIONID=...` -> `JSESSIONID=[REDACTED]`
  12. Medical EMR JSON Fields: `"diagnosis": "..."`, `"prescription": "..."`, `"treatmentDone": "..."`, `"doctorNotes": "..."`, etc. -> `[REDACTED_MEDICAL]`
  13. Medical EMR Plain Text: `Diagnosis: ...` -> `Diagnosis: [REDACTED_MEDICAL]`
  14. Vietnamese Citizen ID: 12-digit CCCD / 9-digit CMND -> `[REDACTED_ID]`
  15. Patient Phone Masking: `0981234567` -> `098****567`
- **Idempotency Guarantee**: All patterns leverage negative lookahead `(?!\\[REDACTED)` to guarantee that repeated sanitization passes never produce nested tags like `[[REDACTED]]`.

### 2.4. Data Seeder (`com.dentalclinic.itteam.config.ITTeamDataInitializer`)

- Implements `CommandLineRunner` with `@Order(2)`.
- Idempotency check: `if (profileRepository.count() == 0)` + per-profile `existsByAgentCode` check.
- Standard profiles seeded:
  1. `#it-backend` (Alex Rivera, Backend Architect)
  2. `#it-frontend` (Elena Chen, Management Portal UI Specialist)
  3. `#it-qa` (Marcus Vance, QA & Reliability Engineer)
  4. `#it-devops` (Liam O'Connor, DevOps & Infrastructure Engineer)
  5. `#it-security` (Aria Sterling, Cybersecurity & Compliance Officer)
- Initial memories: 12 structured memories (architecture, persistence, REST conventions, UI stack, JWT storage, test suites, regression checklist, server runtime, database storage, privacy guardrail, RBAC policy).
- Initial virtual browser tabs: 5 tabs (Portal, OpenAPI/Swagger, H2 Console, Security Audit, API Runner).
- Initial message: Welcome broadcast message with parsed hashtags.
- Initial activity: System boot activity log.

---

## 3. Automated Test Suites Summary

| Test File | Test Suite Class | Tests Count | Scope |
|---|---|---|---|
| `src/test/java/com/dentalclinic/itteam/SensitiveDataSanitizerTest.java` | `SensitiveDataSanitizerTest` | 18 | Standalone unit tests covering all 15 regex patterns, boundary values, idempotency, and multi-field payloads. |
| `src/test/java/com/dentalclinic/itteam/model/EntityPrePersistenceSanitizationTest.java` | `EntityPrePersistenceSanitizationTest` | 4 | Unit tests verifying `@PrePersist` callbacks across `ITApiRunLog`, `ITAgentMemory`, `ITAgentActivity`, and `ITAgentMessage`. |
| `src/test/java/com/dentalclinic/itteam/ITTeamM1PersistenceTest.java` | `ITTeamM1PersistenceTest` | 7 | `@SpringBootTest` verifying: seeded 5 profiles, seeder idempotency on restart, repository queries, and H2 database persistence with pre-persist sanitization. |
| **Total** | | **29** | **Full Milestone 1 Coverage** |

---

## 4. Verification Instructions

To execute the test suites independently:

```bash
# Compile and verify all dependencies
./mvnw test-compile

# Execute Milestone 1 test suites
./mvnw test -Dtest=SensitiveDataSanitizerTest,EntityPrePersistenceSanitizationTest,ITTeamM1PersistenceTest
```

---

## 5. Next Steps for Milestone 2 (Messaging & Hashtag Engine)

Milestone 1 provides all downstream dependencies needed by Milestone 2:
- `ITAgentProfileRepository.findByHashtag(String hashtag)`
- `ITAgentProfileRepository.findByAgentCode(String agentCode)`
- `ITAgentMessageRepository.save(ITAgentMessage message)`
- `ITAgentActivityRepository.save(ITAgentActivity activity)`
- `SensitiveDataSanitizer.sanitize(String payload)`

Milestone 1 is complete and ready for handoff.
