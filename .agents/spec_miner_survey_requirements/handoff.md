# Handoff Report: Specification & Interface Survey for DentalCare IT Team

**Agent**: Specification & Interface Miner (`spec_miner_survey_requirements`)  
**Parent Agent**: Orchestrator (`89ae81f4-4ba5-44ee-8b07-8548bc218f28`)  
**Working Directory**: `D:\java\dental-clinic\.agents\spec_miner_survey_requirements`  
**Date**: 2026-09-12  
**Handoff Type**: Hard (Survey and Specification Mining Task Complete)

---

## 1. Observation

Direct observations from codebase inspection and authoritative specifications:

1. **Requirements & Scope**:
   - `D:\java\dental-clinic\ORIGINAL_REQUEST.md`, lines 12-59:
     - Defines 6 entities in package `com.dentalclinic.itteam`: `ITAgentProfile`, `ITAgentMemory`, `ITAgentMessage`, `ITAgentActivity`, `ITBrowserTabRecord`, `ITApiRunLog`.
     - Defines 5 standard IT profiles: `#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`.
     - Mandates strict privacy guardrails: redact cookies, plaintext passwords, JWTs, credentials, medical records (EMR), real patient PII.
     - Mandates hashtag-based inter-agent messaging with `#it-*` mentions and `MENTIONED` activity logging.
     - Mandates REST endpoints in `ITTeamController` under `/api/it-team/**` protected by JWT with `ROLE_ADMIN`.
     - Mandates integration with 9Router at `http://localhost:20128`.
     - Mandates 5 sub-views in the Management Portal UI: Nhân Sự (Profiles), Hội Thoại (Conversations), Bộ Nhớ (Memories), Nhật Ký Thao Tác (Activity Logs), API Monitor.

2. **Backend Architecture & Existing Patterns**:
   - `D:\java\dental-clinic\pom.xml`, lines 8-22:
     - Spring Boot `3.2.5`, Java `17`, JJWT `0.12.5`, Springdoc OpenAPI `2.5.0`, H2 Database runtime.
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\common\BaseEntity.java`, lines 12-28:
     - Base entity class with `createdAt` and `updatedAt` (`@CreatedDate`, `@LastModifiedDate`, `AuditingEntityListener`).
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\common\ApiResponse.java`, lines 5-42:
     - Universal response wrapper `ApiResponse<T>` with `boolean success`, `String message`, `T data`, `LocalDateTime timestamp`.
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\model\Role.java`, lines 3-10:
     - Currently defined roles: `ROLE_OWNER`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`, `ROLE_PATIENT`.
     - Note: `ROLE_ADMIN` is not yet explicitly declared in `Role.java`. To satisfy the requirement for `ROLE_ADMIN` JWT authorization, `ROLE_ADMIN` must be added to `Role.java` or mapped appropriately.
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\security\SecurityConfig.java`, lines 21-105:
     - `@EnableMethodSecurity(prePostEnabled = true)` is active.
     - `.requestMatchers("/api/**").authenticated()`.
     - Adding `.requestMatchers("/api/it-team/**").hasAnyRole("ADMIN", "OWNER")` and `@PreAuthorize("hasRole('ADMIN')")` will integrate cleanly.
   - `D:\java\dental-clinic\src\main\resources\application.yml`, lines 11-27:
     - H2 database located at `./data/dentaldb;AUTO_SERVER=TRUE` with `ddl-auto: update`.

3. **Frontend UI Architecture**:
   - `D:\java\dental-clinic\src\main\resources\static\index.html`, lines 1401-1452:
     - Role-based dynamic portal (`#management-portal`) with navigation tabs: `tab-dashboard`, `tab-appointments`, `tab-emr`, `tab-coupons`, `tab-shifts`, `tab-notifications`.
   - `D:\java\dental-clinic\src\main\resources\static\js\app.js`, lines 47-81, 460-481:
     - `apiFetch` attaches Bearer token from `currentUser.token`.
     - `switchTab(tabId)` toggles visibility of `#section-${tabId}` and highlights `#tab-${tabId}`.

---

## 2. Logic Chain

1. **From Schema Requirements to JPA Entity Modeling**:
   - Observation 1 and 2 show that all existing entities extend `BaseEntity` and use `GenerationType.IDENTITY` for IDs.
   - Therefore, the 6 new entities (`ITAgentProfile`, `ITAgentMemory`, `ITAgentMessage`, `ITAgentActivity`, `ITBrowserTabRecord`, `ITApiRunLog`) should extend `BaseEntity`, live in `com.dentalclinic.itteam.model`, and map to the exact table names mandated in the Acceptance Criteria (`it_agent_profile`, `it_agent_memory`, etc.).

2. **From Role Inspection to Security & RBAC Design**:
   - Observation 2 reveals `Role.java` currently contains `ROLE_OWNER` but not `ROLE_ADMIN`.
   - The Acceptance Criteria specifically tests that:
     `Non-ADMIN requests (unauthenticated or ROLE_PATIENT) to /api/it-team/** return 401 Unauthorized or 403 Forbidden` and `Valid ADMIN JWT can perform all CRUD operations`.
   - Step: Add `ROLE_ADMIN` to `Role.java`, seed an admin user in `DataInitializer`, and enforce `@PreAuthorize("hasRole('ADMIN')")` or `.hasAnyRole("ADMIN", "OWNER")` in `SecurityConfig`.

3. **From Privacy Guardrail to Sanitizer Architecture**:
   - Observation 1 explicitly prohibits storing passwords, JWTs, cookies, credentials, and medical EMR in memory/activity/API run logs.
   - Step: A dedicated `ITSanitizerService` using regex replacement (`[REDACTED]`, `[REDACTED_JWT]`, `[REDACTED_MEDICAL]`) must intercept payloads in `ITApiRunLog`, `ITAgentMemory`, and `ITAgentActivity` prior to database writes.

4. **From 9Router Mandate to Fallback Protocol**:
   - Observation 1 specifies 9Router at `http://localhost:20128`.
   - Because 9Router is a local AI endpoint that may not be running during test suites or offline environments, the client must use a 3000ms timeout with a fallback rule engine. If 9Router fails or is offline, the service falls back to pre-authored agent-specific responses, ensuring tests and user interactions never fail.

5. **From Messaging Requirements to Hashtag & Threading Engine**:
   - Regex `(?i)#(it-(?:backend|frontend|qa|devops|security))\b` guarantees matching all 5 agents regardless of case or trailing punctuation while ignoring other hashtags.
   - Multi-mentions trigger individual `MENTIONED` activity logs per mentioned agent.
   - Storing `parentMessageId` and `threadId` allows hierarchical threading without schema recursion issues.

---

## 3. Caveats

1. **Localhost API Execution Scope**:
   - The safe API runner (`POST /api/it-team/api-runs`) must only execute internal endpoints on `localhost:8080` or relative paths starting with `/api/`. Executing external arbitrary URLs is restricted to prevent Server-Side Request Forgery (SSRF).
2. **H2 In-Memory vs File Persistence**:
   - The database is configured with `./data/dentaldb;AUTO_SERVER=TRUE`. Data persistence survives restarts, making idempotent startup checks (`findByAgentCode`) mandatory.
3. **9Router Availability**:
   - 9Router at `http://localhost:20128` may be offline in CI or headless environments. The deterministic rule fallback specified in `report.md` ensures 100% test reliability.

---

## 4. Conclusion

All 7 requirement dimensions have been surveyed, mined, and fully specified in `D:\java\dental-clinic\.agents\spec_miner_survey_requirements\report.md`:
1. **6 JPA Entities**: Schema, fields, data types, nullability, relations, and indexes defined.
2. **5 IT Agents**: Profiles, skills, memories, virtual browser tabs, and idempotent startup seeding logic defined.
3. **Hashtag Engine**: Regex grammar, multi-mention routing, `MENTIONED` activity generation, and threading semantics defined.
4. **REST APIs**: Endpoints, request/response DTOs, query parameters, error responses, and RBAC (`ROLE_ADMIN`) contracts defined.
5. **Sanitizer Guardrails**: Regex rules for passwords, JWTs, Bearer tokens, cookies, CCCD/SSN, and EMR medical text defined.
6. **9Router Integration**: OpenAI-compatible format specified alongside the deterministic offline fallback engine.
7. **Discovered Features & Edge Cases**: 26 features and 20 edge cases comprehensively documented.

The specification is complete, robust, and directly actionable for backend, frontend, and test development teams.

---

## 5. Verification Method

To independently verify the specification findings and consistency with the codebase:

1. **Verify Files Created**:
   - Inspect `D:\java\dental-clinic\.agents\spec_miner_survey_requirements\report.md`.
   - Inspect `D:\java\dental-clinic\.agents\spec_miner_survey_requirements\handoff.md`.
   - Inspect `D:\java\dental-clinic\.agents\spec_miner_survey_requirements\progress.md`.
2. **Verify Codebase Alignment**:
   - Check `D:\java\dental-clinic\ORIGINAL_REQUEST.md` for complete coverage of R1, R2, R3, R4, R5, and Acceptance Criteria.
   - Check `D:\java\dental-clinic\src\main\java\com\dentalclinic\model\Role.java` to confirm existing roles.
   - Check `D:\java\dental-clinic\src\main\java\com\dentalclinic\security\SecurityConfig.java` to verify `filterChain` structure.
3. **Build & Test Verification (Once implemented)**:
   - Run `mvn clean test` to verify zero regression and all new IT team tests passing.
