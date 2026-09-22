# Dispatch for M1 Worker

Scope: Implement Milestone 1 - Domain Model, JPA Repositories, SensitiveDataSanitizer, ITTeamDataInitializer, and M1 Unit/Persistence Tests.
Working Directory: D:\java\dental-clinic\.agents\worker_m1_1
Parent: D:\java\dental-clinic\.agents\orchestrator_1
Input Reports:
- D:\java\dental-clinic\.agents\explorer_m1_1\report.md
- D:\java\dental-clinic\.agents\explorer_m1_2\report.md
- D:\java\dental-clinic\.agents\explorer_m1_3\report.md
- D:\java\dental-clinic\PROJECT.md
- D:\java\dental-clinic\ORIGINAL_REQUEST.md

## 2026-09-12T15:07:02Z
You are the Milestone 1 Worker for the DentalCare Management Portal IT Team project.
Working directory: D:\java\dental-clinic\.agents\worker_m1_1
Authoritative requirements file: D:\java\dental-clinic\ORIGINAL_REQUEST.md
Project architecture file: D:\java\dental-clinic\PROJECT.md
Explorer reports to implement:
- D:\java\dental-clinic\.agents\explorer_m1_1\report.md (JPA Entities)
- D:\java\dental-clinic\.agents\explorer_m1_2\report.md (Repositories & Seeder)
- D:\java\dental-clinic\.agents\explorer_m1_3\report.md (SensitiveDataSanitizer & Pre-Persist tests)
Read D:\java\dental-clinic\ORIGINAL_REQUEST.md and D:\java\dental-clinic\PROJECT.md first!

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your implementation scope:
1. JPA Entities in `src/main/java/com/dentalclinic/itteam/model/`:
   - `ITAgentProfile.java`
   - `ITAgentMemory.java`
   - `ITAgentMessage.java`
   - `ITAgentActivity.java`
   - `ITBrowserTabRecord.java`
   - `ITApiRunLog.java`
   All must extend `com.dentalclinic.common.BaseEntity`, use explicit getters/setters/constructors (NO Lombok), match table names `it_agent_*`, and include `@PrePersist`/`@PreUpdate` hooks that call `SensitiveDataSanitizer.sanitize(...)` on payloads before persistence.
2. Spring Data JPA Repositories in `src/main/java/com/dentalclinic/itteam/repository/`:
   - `ITAgentProfileRepository.java`
   - `ITAgentMemoryRepository.java`
   - `ITAgentMessageRepository.java`
   - `ITAgentActivityRepository.java`
   - `ITBrowserTabRecordRepository.java`
   - `ITApiRunLogRepository.java`
   With all necessary query methods.
3. Sensitive Data Sanitizer in `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`:
   - Redact JWTs (`[REDACTED_JWT]`), passwords/credentials (`[REDACTED]`), Bearer/Basic headers (`Bearer [REDACTED]`), Cookies, and medical EMR PII (`[REDACTED_MEDICAL]`).
4. Data Seeder in `src/main/java/com/dentalclinic/itteam/config/ITTeamDataInitializer.java`:
   - Implements `CommandLineRunner` or `ApplicationRunner`.
   - Checks `itAgentProfileRepository.count() == 0` for idempotency.
   - Seeds the 5 standard IT profiles (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`) with initial memories and virtual browser tabs.
5. Unit and persistence tests in `src/test/java/com/dentalclinic/itteam/`:
   - `SensitiveDataSanitizerTest.java`: Verify all regex replacements.
   - `ITTeamM1PersistenceTest.java`: Verify database persistence, seeder execution, 5 profiles seeded, and pre-persistence sanitization.
6. Run build and tests (`./mvnw test-compile` and `./mvnw test -Dtest=SensitiveDataSanitizerTest,ITTeamM1PersistenceTest`).
7. Write your report to `D:\java\dental-clinic\.agents\worker_m1_1\report.md` and handoff report to `handoff.md`. Notify parent when complete.
