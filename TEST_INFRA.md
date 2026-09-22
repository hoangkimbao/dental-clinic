# DentalCare Management Portal — IT Team Command Center
# Test Infrastructure & Specification Document (TEST_INFRA.md)

**Project:** DentalCare Management Portal — IT Team Command Center  
**Root Directory:** `D:\java\dental-clinic`  
**Test Suite Path:** `src/test/java/com/dentalclinic/e2e/ITTeamE2ETestSuite.java`  
**Author:** E2E Test Track Specialist (`test_writer_e2e`)  
**Parent Orchestrator:** `orchestrator_1`  
**Status:** PUBLISHED & READY  
**Version:** 1.0.0  

---

## 1. Executive Summary & Dual Track Architecture

The IT Team Command Center simulates an active internal IT organization within the DentalCare Management Portal. It provides persistent memory, activity audit logging, safe API monitoring, hashtag-based inter-agent communication, and 9Router AI orchestration.

To guarantee zero regression of existing clinic operations (Booking, EMR, Auth, WebSocket, Staff Shifts) while enabling rigorous quality assurance, the project adopts the **Dual Track Testing Strategy**:
- **Track A (Test Engineering / Test Writer):** Operates independently. Designs the complete testing infrastructure, defines formal interface contracts, establishes the 4-tier (+ Tier 5 adversarial) test methodology, and writes the opaque-box E2E test harness (`ITTeamE2ETestSuite.java`) against REST contracts before or alongside implementation.
- **Track B (Feature Implementation):** Executes milestones M1 (Domain Model & Seeding), M2 (Hashtag & Messaging Engine), M3 (REST API & RBAC), M4 (Command Center Frontend UI), and M5 (End-to-End Test Pass & Verification).

```
   Dual Track Execution Flow
   =========================
   Track A (Test Track)                 Track B (Implementation Track)
   --------------------                 ------------------------------
   TEST_INFRA.md Definition            M1: Domain Models & Seeder
            │                                     │
            ▼                                     ▼
   ITTeamE2ETestSuite.java              M2: Hashtag Engine & Router
   (Tiers 1-5 Opaque Box)                         │
            │                                     ▼
            ▼                           M3: REST API & RBAC Security
   TEST_READY.md Published                        │
            │                                     ▼
            │                           M4: Command Center UI (Portal)
            │                                     │
            └─────────────► M5 ◄──────────────────┘
                      100% E2E Execution
                      Zero Regressions
```

---

## 2. Test Environment & Harness Architecture

### 2.1 Framework & Toolchain
- **Runtime:** Java 17 LTS, Spring Boot 3.2.5
- **Testing Framework:** JUnit Jupiter 5 (`org.junit.jupiter.api.*`)
- **Integration Test Context:** `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)`
- **HTTP Mocking Engine:** `@AutoConfigureMockMvc` (`org.springframework.test.web.servlet.MockMvc`)
- **JSON Serialization:** `com.fasterxml.jackson.databind.ObjectMapper`
- **JSON Assertion:** Jayway `jsonPath` (`org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath`)
- **Database:** Embedded H2 database (`jdbc:h2:file:./data/dentaldb;AUTO_SERVER=TRUE`) with JPA auditing enabled. Existing database files are preserved in integrity mode.

### 2.2 Opaque-Box Isolation & Rate-Limiting Strategy
1. **Opaque-Box Philosophy:** Tests interact exclusively via HTTP JSON endpoints (`/api/it-team/**`, `/api/auth/login`). Tests do not couple tightly to uncommitted internal class signatures, ensuring forward-compatibility across all implementation stages.
2. **Rate Limiting Resilience:** The application enforces `RateLimitingFilter` (60 requests per 10 seconds per IP). The test harness injects rotating `X-Forwarded-For` headers per test group/method (e.g., `10.0.1.1`, `10.0.1.2`, ...) ensuring zero false-positive HTTP 429 errors during high-volume suite execution.
3. **Deterministic Auth Fixture:** The test harness utilizes a dynamic `obtainAuthToken(username, password)` routine against `/api/auth/login`. It acquires valid JWT tokens for:
   - `ROLE_OWNER` (`owner` / `123`) — Full administrative access to IT Team APIs.
   - `ROLE_ADMIN` (`admin` / `123` if present or configured).
   - `ROLE_PATIENT` (`benhnhan` / `123`) — Non-privileged user verifying 403 Forbidden enforcement.

---

## 3. The 4-Tier (+ Tier 5 Adversarial) Methodology

The test suite structure strictly implements the project's multi-tiered testing standard:

```
┌─────────────────────────────────────────────────────────────┐
│  Tier 5: Adversarial Coverage (SQLi, XSS, SSRF, Tampering)   │
├─────────────────────────────────────────────────────────────┤
│  Tier 4: Real-World Scenarios (End-to-End Workflows)        │
├─────────────────────────────────────────────────────────────┤
│  Tier 3: Cross-Feature Combinations (Multi-Component Flows)  │
├─────────────────────────────────────────────────────────────┤
│  Tier 2: Boundary & Corner Cases (>=5 per feature area)     │
├─────────────────────────────────────────────────────────────┤
│  Tier 1: Feature Coverage (>=5 per functional area)         │
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Tier 1: Primary Feature Coverage (>= 5 Tests Per Feature Area)

#### Area 1: Agent Profiles & Startup Seeding (F01, F08, F14)
- **T1-PRF-01:** `GET /api/it-team/agents` returns all 5 seeded profiles (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`).
- **T1-PRF-02:** Validate agent attributes: display name, role competencies, avatar, and system prompt.
- **T1-PRF-03:** Update agent status to `BUSY` via `PUT /api/it-team/agents/{id}/status` and confirm state transition.
- **T1-PRF-04:** Update agent status to `OFFLINE` and verify persistence.
- **T1-PRF-05:** Verify seeder idempotency: restart/subsequent calls yield exactly 5 unique profiles without duplicates.

#### Area 2: Agent Persistent Memories (F02, F15)
- **T1-MEM-01:** `POST /api/it-team/memories` creates a high-priority memory for `#it-backend`.
- **T1-MEM-02:** `GET /api/it-team/memories?agentCode=it-backend` retrieves filtered memories for `#it-backend`.
- **T1-MEM-03:** Create medium and low priority memories for `#it-frontend`.
- **T1-MEM-04:** Retrieve memories sorted/ordered by priority or timestamp.
- **T1-MEM-05:** Update or overwrite an existing memory key for an agent.

#### Area 3: Inter-Agent Messaging & Hashtag Engine (F03, F10, F11, F13, F16)
- **T1-MSG-01:** Send message with `#it-backend` -> verify recipient auto-linked to `#it-backend`.
- **T1-MSG-02:** Send message mentioning multiple agents (`#it-qa` and `#it-devops`) -> verify parsed hashtags array.
- **T1-MSG-03:** `GET /api/it-team/messages` returns top-level message feed chronologically.
- **T1-MSG-04:** Send threaded reply referencing `parentMessageId` -> verify reply links to parent thread.
- **T1-MSG-05:** `GET /api/it-team/messages?parentMessageId={id}` retrieves all conversation replies in thread.

#### Area 4: Mention Activity Logging & Audit Trail (F04, F12, F17)
- **T1-ACT-01:** Mentioning `#it-security` triggers an automatic `ITAgentActivity` record of type `MENTIONED`.
- **T1-ACT-02:** `GET /api/it-team/activities` returns paginated activity audit feed.
- **T1-ACT-03:** Filter activities by `agentCode=it-backend`.
- **T1-ACT-04:** Filter activities by `actionType=MENTIONED`.
- **T1-ACT-05:** Verify activity log includes related entity link and ISO-8601 timestamp.

#### Area 5: Browser Tab Session Records (F05, F18)
- **T1-TAB-01:** `POST /api/it-team/browser-tabs` logs a new tab session (URL, title, category `MONITORING`).
- **T1-TAB-02:** `GET /api/it-team/browser-tabs` lists all recorded browser tab sessions.
- **T1-TAB-03:** Update browser tab record to `CLOSED` status with closed timestamp.
- **T1-TAB-04:** Filter tab sessions by agent (`agentCode=it-frontend`).
- **T1-TAB-05:** Record and verify multiple tab categories (`DOCUMENTATION`, `CONSOLE`, `API_TOOL`).

#### Area 6: Safe Localhost API Runner (F06, F19)
- **T1-RUN-01:** `POST /api/it-team/api-runs` executes a safe GET request to `/api/coupons/active`.
- **T1-RUN-02:** Verify execution log records HTTP 200, duration > 0 ms, and timestamp.
- **T1-RUN-03:** `GET /api/it-team/api-runs` retrieves chronological execution logs.
- **T1-RUN-04:** Execute a safe POST request to internal endpoint with JSON body.
- **T1-RUN-05:** Verify request and response payloads are captured in `ITApiRunLog`.

#### Area 7: RBAC Authorization & Security (F20)
- **T1-SEC-01:** Unauthenticated request to `GET /api/it-team/agents` returns 401 Unauthorized or 403 Forbidden.
- **T1-SEC-02:** Unauthenticated request to `POST /api/it-team/messages` returns 401 Unauthorized or 403 Forbidden.
- **T1-SEC-03:** Authenticated `ROLE_PATIENT` user accessing `/api/it-team/**` returns 403 Forbidden.
- **T1-SEC-04:** Authenticated `ROLE_DENTIST` or `ROLE_RECEPTIONIST` accessing `/api/it-team/**` returns 403 Forbidden.
- **T1-SEC-05:** Authenticated `ROLE_OWNER` or `ROLE_ADMIN` user successfully accesses all `/api/it-team/**` endpoints.

#### Area 8: Sensitive Data Sanitizer & Privacy Guardrail (F09)
- **T1-SAN-01:** Message payload containing plaintext password (`password=secret123`) is redacted to `[REDACTED]`.
- **T1-SAN-02:** Message or log containing JWT Bearer token (`Bearer eyJhbGci...`) is sanitized.
- **T1-SAN-03:** Memory store containing cookie headers (`Cookie: JSESSIONID=...`) is stripped.
- **T1-SAN-04:** Medical record PII (patient medical diagnosis, EMR data) is masked before storage.
- **T1-SAN-05:** Clean, non-sensitive diagnostic payloads pass through uncorrupted without false alterations.

#### Area 9: 9Router AI Integration & Offline Fallback (F21)
- **T1-AI-01:** When 9Router (`http://localhost:20128`) is unreachable, fallback generator activates deterministically.
- **T1-AI-02:** Deterministic fallback response contains the persona and context of the tagged agent.
- **T1-AI-03:** Message persistence and activity logging succeed normally without 500 error on 9Router timeout.
- **T1-AI-04:** Offline warning is logged safely in system audit without interrupting user experience.
- **T1-AI-05:** Fallback response is correctly threaded as a reply to the originating message.

---

### 3.2 Tier 2: Boundary, Extreme & Corner Cases (>= 5 Per Feature Area)

#### Boundary Area 1: Messaging & Hashtag Engine
- **T2-BND-01:** Empty message body (`""`) or whitespace-only (`"   "`) rejected with 400 Bad Request.
- **T2-BND-02:** Message with unknown hashtag `#it-unknown` or `#it-fake` handled gracefully without error.
- **T2-BND-03:** Message with multiple consecutive duplicate hashtags `#it-backend #it-backend #it-backend` deduplicated cleanly.
- **T2-BND-04:** Message with hashtags embedded in punctuation (e.g., `(#it-qa)!`, `[#it-devops]`) correctly parsed.
- **T2-BND-05:** Extremely long message body (10,000+ characters) handled without server crash or database truncation error.

#### Boundary Area 2: Agent Profiles & Status Updates
- **T2-BND-06:** Updating status of non-existent agent ID (`999999`) returns 404 Not Found.
- **T2-BND-07:** Updating status with invalid enum/string (e.g. `INVALID_STATUS`) returns 400 Bad Request.
- **T2-BND-08:** Querying agent by non-existent agent code returns empty result or 404.
- **T2-BND-09:** Null or empty body in status update request returns 400 Bad Request.
- **T2-BND-10:** Case-insensitivity verification for agent hashtags (`#IT-BACKEND` vs `#it-backend`).

#### Boundary Area 3: Memory Store Boundaries
- **T2-BND-11:** Create memory with empty key or empty content returns 400 Bad Request.
- **T2-BND-12:** Retrieve memories for non-existent agent returns empty array (`[]`), not 500 error.
- **T2-BND-13:** Create memory with invalid priority level handled with default or 400 Bad Request.
- **T2-BND-14:** High-frequency memory updates for same key test atomic overwrite without race conditions.
- **T2-BND-15:** Large memory content payload (16 KB JSON blob) stored and retrieved intact.

#### Boundary Area 4: Safe API Runner & SSRF Boundaries
- **T2-BND-16:** API runner request targeting external domain (`http://evil.com/api`) strictly blocked (SSRF guard).
- **T2-BND-17:** API runner request targeting cloud metadata service (`http://169.254.169.254`) strictly blocked.
- **T2-BND-18:** API runner request targeting private IP ranges (`http://10.0.0.1`, `http://192.168.1.1`) blocked unless allowed.
- **T2-BND-19:** API runner request to non-existent localhost route (`/api/non-existent-404`) records 404 in log without crashing runner.
- **T2-BND-20:** API runner request with malformed URL or invalid HTTP method returns 400 Bad Request.

#### Boundary Area 5: Browser Tabs & Activity Feeds
- **T2-BND-21:** Logging tab session with empty URL returns 400 Bad Request.
- **T2-BND-22:** Closing already closed tab session returns consistent state without error.
- **T2-BND-23:** Activity query with negative page number or negative page size returns 400 or defaults to page 0.
- **T2-BND-24:** Activity query with non-existent action type returns empty list.
- **T2-BND-25:** Activity query with future date filter returns empty result.

---

### 3.3 Tier 3: Cross-Feature Combinations

- **T3-XFT-01: Message -> Hashtag Extraction -> Recipient Linking -> Mention Activity Pipeline**  
  Dispatch message `Please verify API security #it-security`. Verify:
  1. `ITAgentMessage` saved with recipient linked to `#it-security`.
  2. `ITAgentActivity` automatically generated with `actionType=MENTIONED` for `#it-security`.
  3. `GET /api/it-team/activities` includes the mention with a link back to the message.

- **T3-XFT-02: API Runner Execution -> Sanitizer -> API Run Log Persistence**  
  Execute API run against an endpoint passing authorization header. Verify:
  1. Internal endpoint executed.
  2. Sanitizer intercepts request/response payloads before persistence.
  3. Resulting `ITApiRunLog` contains redacted credentials (`[REDACTED]`).

- **T3-XFT-03: Agent Status Update -> Activity Audit -> Profile View Synchronization**  
  Update `#it-devops` status to `MAINTENANCE`. Verify:
  1. Status updated in `ITAgentProfile`.
  2. Activity record generated with `actionType=STATUS_CHANGE`.
  3. Subsequent `GET /api/it-team/agents` reflects `MAINTENANCE`.

- **T3-XFT-04: Memory Storage with Sensitive Tokens -> Redaction -> Retrieval Integrity**  
  Save memory containing `db_password=supersecret` and `jwt=eyJhbGci...`. Verify:
  1. Sanitizer triggers before storage.
  2. Stored memory contains masked values.
  3. Retrieval via `GET /api/it-team/memories` returns sanitized content.

- **T3-XFT-05: Multi-Agent Cascade Mention Flow**  
  Dispatch message: `Deployment failed, need #it-backend for logs and #it-qa for retest`. Verify:
  1. Both `#it-backend` and `#it-qa` extracted.
  2. Two distinct `ITAgentActivity` mention records generated for both agents.
  3. Activity feeds for both agents show the respective mention.

---

### 3.4 Tier 4: Real-World Operational Scenarios

- **T4-SCN-01: End-to-End Incident Diagnostic & Resolution Workflow**  
  1. `#it-devops` posts alert: `High latency on coupon endpoint #it-backend`.
  2. Mention activity logged for `#it-backend`.
  3. `#it-backend` invokes API Runner to test `GET /api/coupons/active`.
  4. API Run Log records latency and status 200.
  5. `#it-backend` creates a diagnostic memory: `Coupon endpoint latency normalized (42ms)`.
  6. `#it-backend` replies in thread (`parentMessageId`): `Verified healthy. Latency 42ms #it-devops`.
  7. Entire thread retrieved and validated for parent-child relationship.

- **T4-SCN-02: Security Audit & Credential Redaction Workflow**  
  1. User attempts to submit diagnostic report with raw session headers, bearer tokens, and passwords.
  2. System receives dispatch to `#it-security`.
  3. Privacy guardrail intercepts and redacts every token.
  4. `#it-security` receives clean payload.
  5. `#it-security` logs a browser tab session `Audit Console: /api/it-team/activities`.
  6. Audit trail query verifies zero plaintext credentials stored across all tables.

- **T4-SCN-03: Multi-Agent Shift Handoff Workflow**  
  1. `#it-frontend` logs open tabs: `/index.html#management-portal`, `/css/tailwind.css`.
  2. `#it-frontend` stores memory: `Sprint goal: Autocomplete #it- in chat view`.
  3. `#it-frontend` updates status to `OFFLINE`.
  4. `#it-frontend` dispatches handoff message: `Handoff complete, please test #it-qa`.
  5. `#it-qa` updates status to `ONLINE` and verifies open tasks.

---

### 3.5 Tier 5: Adversarial & Resilience Coverage

- **T5-ADV-01: SQL Injection & Escaping Integrity**  
  Payloads such as `' OR '1'='1; DROP TABLE it_agent_profile; --` sent in message bodies, memory keys, and search queries. Verify:
  1. Database engine processes as literal text.
  2. No syntax errors or unintended data modification occurs.
  3. All 5 agent profiles remain intact.

- **T5-ADV-02: Cross-Site Scripting (XSS) & HTML Escaping**  
  Payloads such as `<script>alert('xss')</script><img src=x onerror=alert(1)>` sent in chat messages and tab titles. Verify:
  1. Stored verbatim or safely sanitized.
  2. JSON responses properly escaped without raw script execution triggers.

- **T5-ADV-03: SSRF Bypass & Host Evasion Defense**  
  Attack payloads targeting API runner:
  - `http://127.0.0.1.nip.io/`
  - `http://0.0.0.0:8080/`
  - `http://[::1]:8080/`
  - `http://localhost@attacker.com/`
  Verify all non-whitelisted or disguised hosts are rejected with HTTP 400/403.

- **T5-ADV-04: Malformed JWT & Forged Role Injection**  
  Requests sent with:
  - Expired JWT token -> returns 401.
  - Tampered signature -> returns 401.
  - Self-signed JWT claiming `ROLE_ADMIN` -> returns 401.

- **T5-ADV-05: Rate-Limiting DoS Defense & System Non-Interference**  
  Simulate rapid requests from a single client IP to verify `RateLimitingFilter` returns HTTP 429 when threshold is exceeded, while requests from clinic booking and authentication continue operating seamlessly.

---

## 4. Feature Traceability Matrix (F01 – F30)

| Feature | Description | Milestone | Tier 1 Tests | Tier 2/3/4 Tests |
| :--- | :--- | :--- | :--- | :--- |
| **F01** | ITAgentProfile Entity | M1 | T1-PRF-01, T1-PRF-02 | T2-BND-06, T3-XFT-03 |
| **F02** | ITAgentMemory Entity | M1 | T1-MEM-01, T1-MEM-03 | T2-BND-11, T3-XFT-04 |
| **F03** | ITAgentMessage Entity | M1 | T1-MSG-01, T1-MSG-02 | T2-BND-01, T3-XFT-01 |
| **F04** | ITAgentActivity Entity | M1 | T1-ACT-01, T1-ACT-02 | T2-BND-24, T3-XFT-01 |
| **F05** | ITBrowserTabRecord Entity | M1 | T1-TAB-01, T1-TAB-02 | T2-BND-21, T4-SCN-03 |
| **F06** | ITApiRunLog Entity | M1 | T1-RUN-01, T1-RUN-02 | T2-BND-19, T3-XFT-02 |
| **F07** | Spring Data Repositories | M1 | T1-PRF-01, T1-MEM-02 | T1-MSG-03, T1-ACT-02 |
| **F08** | Data Seeder & Idempotency | M1 | T1-PRF-01, T1-PRF-05 | T5-ADV-01 |
| **F09** | Privacy Guardrail / Sanitizer | M1 | T1-SAN-01..05 | T3-XFT-02, T4-SCN-02 |
| **F10** | Hashtag Parser Engine | M2 | T1-MSG-01, T1-MSG-02 | T2-BND-02..04 |
| **F11** | Recipient Linking & Routing | M2 | T1-MSG-01, T1-MSG-02 | T3-XFT-01, T3-XFT-05 |
| **F12** | Mention Activity Logging | M2 | T1-ACT-01 | T3-XFT-01, T4-SCN-01 |
| **F13** | Threaded Conversation Replies | M2 | T1-MSG-04, T1-MSG-05 | T4-SCN-01 |
| **F14** | REST: Agent Profiles | M3 | T1-PRF-01..04 | T2-BND-06..08 |
| **F15** | REST: Agent Memories | M3 | T1-MEM-01..05 | T2-BND-11..15 |
| **F16** | REST: Messaging & Threads | M3 | T1-MSG-01..05 | T2-BND-01..05 |
| **F17** | REST: Activity Audit Trail | M3 | T1-ACT-01..05 | T2-BND-23..25 |
| **F18** | REST: Browser Tab Sessions | M3 | T1-TAB-01..05 | T2-BND-21..22 |
| **F19** | REST: Safe Localhost API Runner | M3 | T1-RUN-01..05 | T2-BND-16..20, T5-ADV-03 |
| **F20** | RBAC Security (ROLE_ADMIN) | M3 | T1-SEC-01..05 | T5-ADV-04 |
| **F21** | 9Router AI & Fallback | M3 | T1-AI-01..05 | T4-SCN-01 |
| **F22-F28** | Command Center UI Sub-views | M4 | Tested via E2E API & Static Asset delivery | T4-SCN-01..03 |
| **F29** | System Non-Interference | M5 | Clinic booking & auth tests remain 100% green | T5-ADV-05 |
| **F30** | E2E System Verification | M5 | Full Suite Execution (`ITTeamE2ETestSuite`) | All Tiers (1-5) |

---

## 5. Test Suite Implementation Details

The test suite is encapsulated in:
`src/test/java/com/dentalclinic/e2e/ITTeamE2ETestSuite.java`

Key architectural characteristics:
- **Class Structure:** Root test class `@SpringBootTest` with `@AutoConfigureMockMvc`, structured into nested test classes using `@Nested` and descriptive `@DisplayName` annotations.
- **Independence:** Tests are self-contained. Helper routines handle test data setup without depending on fixed sequence execution.
- **Fast Execution:** In-memory MockMvc tests avoid network overhead, completing the entire multi-tier test suite in seconds.
- **Compilation Safety:** Uses standard Java 17 and Spring Boot Test APIs.

---

## 6. Execution Guide & Validation Commands

### 6.1 Compile Tests
To verify test compilation without running the full test suite:
```bash
./mvnw test-compile
```
*(Windows PowerShell: `.\mvnw.cmd test-compile`)*

### 6.2 Execute E2E Test Suite
To run the IT Team Command Center E2E Test Suite specifically:
```bash
./mvnw test -Dtest=ITTeamE2ETestSuite
```

### 6.3 Execute Entire Clinic Regression Suite
To verify zero regressions across existing clinic operations (Auth, Booking, Coupons, etc.):
```bash
./mvnw test
```

---

## 7. Maintenance & Invalidation Criteria

The test suite must be updated if and only if:
1. **API Endpoint Route Changes:** The base path `/api/it-team` or any child path changes in `PROJECT.md`.
2. **Security Model Evolution:** Additional roles are granted access to IT Team APIs beyond `ROLE_ADMIN` and `ROLE_OWNER`.
3. **Data Model Additions:** New fields or entities are introduced to the IT Team domain.

Any test failure during development indicates an implementation defect in Milestone M1, M2, M3, or M4, and must be escalated to the respective implementation worker.
