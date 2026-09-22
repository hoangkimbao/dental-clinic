# Project: DentalCare Management Portal 'IT Team Command Center'

## Architecture
The DentalCare Management Portal is an enterprise dental clinic system built with Spring Boot 3.2.5 (Java 17) backend and a vanilla ES6+ / Tailwind CSS statically served web frontend (`src/main/resources/static`).
The 'IT Team Command Center' simulates an active internal IT organization providing:
- Domain model & database persistence (`com.dentalclinic.itteam`) with 6 JPA entities extending `com.dentalclinic.common.BaseEntity`.
- Inter-agent messaging engine with hashtag parsing (`#it-*`), mention activity triggers, and threaded replies.
- Admin-protected REST API layer (`/api/it-team/**`) secured by JWT authentication with `ROLE_ADMIN` / `ROLE_OWNER`.
- 9Router AI integration (`http://localhost:20128`) with deterministic offline fallback.
- Management Portal UI with 5 sub-views: Nhân Sự (Profiles), Hội Thoại (Chat with autocomplete), Bộ Nhớ (Memories), Nhật Ký Thao Tác (Activity Logs), API Monitor.
- Privacy guardrail sanitizer redacting JWTs, passwords, headers, cookies, and medical PII from logs and memory.
- Safe localhost API tester with strict SSRF prevention.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F01 | Entity ITAgentProfile | Entity storing agent code, hashtag, display name, role, status, expertise, avatar, system prompt | M1 | ORIGINAL_REQUEST §R1 |
| F02 | Entity ITAgentMemory | Entity storing agent ID, key, content, priority, last updated | M1 | ORIGINAL_REQUEST §R1 |
| F03 | Entity ITAgentMessage | Entity storing sender, recipient, message body, parsed hashtags, read status, parentMessageId, timestamp | M1 | ORIGINAL_REQUEST §R1 |
| F04 | Entity ITAgentActivity | Entity storing agent ID, action type, description, result summary, related entity link, timestamp | M1 | ORIGINAL_REQUEST §R1 |
| F05 | Entity ITBrowserTabRecord | Entity storing agent ID, tab title, URL/route, category, status, opened/closed timestamp | M1 | ORIGINAL_REQUEST §R1 |
| F06 | Entity ITApiRunLog | Entity storing endpoint, method, status code, duration ms, sanitized request/response, run timestamp | M1 | ORIGINAL_REQUEST §R1 |
| F07 | Spring Data Repositories | 6 JPA repositories for all IT team entities | M1 | ORIGINAL_REQUEST §R1 |
| F08 | Data Seeder & Idempotency | Seed 5 IT profiles (#it-backend, #it-frontend, #it-qa, #it-devops, #it-security) on startup idempotently | M1 | ORIGINAL_REQUEST §R1 |
| F09 | Privacy Guardrail & Sanitizer | Redact passwords, JWTs, Bearer headers, cookies, and medical EMR PII | M1 | ORIGINAL_REQUEST §R1 |
| F10 | Hashtag Parser Engine | Extract mentions matching `#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security` | M2 | ORIGINAL_REQUEST §R2 |
| F11 | Recipient Linking & Routing | Automatically link recipient agent and route ITAgentMessage records | M2 | ORIGINAL_REQUEST §R2 |
| F12 | Mention Activity Logging | Automatically trigger ITAgentActivity record of type MENTIONED when tagged | M2 | ORIGINAL_REQUEST §R2 |
| F13 | Threaded Conversation Replies | Support threaded message replies and chronological thread retrieval | M2 | ORIGINAL_REQUEST §R2 |
| F14 | REST API: Agent Profiles | GET /api/it-team/agents & PUT /api/it-team/agents/{id}/status | M3 | ORIGINAL_REQUEST §R3 |
| F15 | REST API: Agent Memories | GET /api/it-team/memories & POST /api/it-team/memories | M3 | ORIGINAL_REQUEST §R3 |
| F16 | REST API: Messaging & Threads | GET /api/it-team/messages & POST /api/it-team/messages | M3 | ORIGINAL_REQUEST §R3 |
| F17 | REST API: Activity Audit Trail | GET /api/it-team/activities with pagination & agent filtering | M3 | ORIGINAL_REQUEST §R3 |
| F18 | REST API: Browser Tab Sessions | GET /api/it-team/browser-tabs & POST /api/it-team/browser-tabs | M3 | ORIGINAL_REQUEST §R3 |
| F19 | REST API: Safe Localhost API Runner | POST /api/it-team/api-runs & GET /api/it-team/api-runs with SSRF protection | M3 | ORIGINAL_REQUEST §R3 |
| F20 | RBAC Security & ROLE_ADMIN | Add ROLE_ADMIN, protect /api/it-team/** strictly for ROLE_ADMIN/ROLE_OWNER, reject patient/unauth with 401/403 | M3 | ORIGINAL_REQUEST §R3 |
| F21 | 9Router AI Integration | Connect to http://localhost:20128 for agent responses with deterministic fallback | M3 | ORIGINAL_REQUEST §R3 |
| F22 | UI: Portal Tab Navigation | Integrate "IT Team" tab button into #management-portal with RBAC visibility | M4 | ORIGINAL_REQUEST §R4 |
| F23 | UI Sub-view 1: Nhân Sự | 5 IT Agent Profile cards with status, roles, skills, and status toggles | M4 | ORIGINAL_REQUEST §R4 |
| F24 | UI Sub-view 2: Hội Thoại | Chat channel with #it- autocomplete, threaded replies, and message history | M4 | ORIGINAL_REQUEST §R4 |
| F25 | UI Sub-view 3: Bộ Nhớ | Structured key-value memory store per agent with priority indicators | M4 | ORIGINAL_REQUEST §R4 |
| F26 | UI Sub-view 4: Nhật Ký Thao Tác | Chronological audit feed of agent actions and mentions with filters | M4 | ORIGINAL_REQUEST §R4 |
| F27 | UI Sub-view 5: API Monitor | Interactive tester & monitoring dashboard displaying status, latency, payloads | M4 | ORIGINAL_REQUEST §R4 |
| F28 | UI Filters & Responsive Design | Filters by agent, status, date range matching DentalCare design system | M4 | ORIGINAL_REQUEST §R4 |
| F29 | System Non-Interference | Preserve 100% existing booking, auth, EMR, shifts, and websocket alerts without touching clinic data | M5 | ORIGINAL_REQUEST §R5 |
| F30 | E2E & System Verification | Automated integration test suite validating models, RBAC, hashtag routing, and APIs | M5 | ORIGINAL_REQUEST §Acceptance |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Domain Model & Persistence | 6 JPA entities, BaseEntity, 6 Repositories, DataInitializer seeder (5 agents), SensitiveDataSanitizer | none | PLANNED |
| M2 | Messaging & Hashtag Engine | Hashtag parser, message router, mention activity listener, threaded reply model | M1 | PLANNED |
| M3 | REST API Layer & 9Router AI | ITTeamController, RBAC (ROLE_ADMIN in Role.java & SecurityConfig), 9Router client with offline fallback, safe localhost API runner | M1, M2 | PLANNED |
| M4 | Management Portal UI | IT Team tab in index.html, it-team.js with 5 sub-views (Profiles, Chat with #it- autocomplete, Memories, Activity Logs, API Monitor) | M3 | PLANNED |
| M5 | Final E2E Test Pass & Hardening | Pass 100% E2E test suite (Tiers 1-4), adversarial test coverage hardening (Tier 5), non-interference check | M1, M2, M3, M4 | PLANNED |

## Interface Contracts
### M1 ↔ M2
- `ITAgentProfileRepository.findByHashtag(String hashtag)`: Optional<ITAgentProfile>
- `ITAgentProfileRepository.findByAgentCode(String agentCode)`: Optional<ITAgentProfile>
- `ITAgentMessageRepository.save(ITAgentMessage message)`: ITAgentMessage
- `ITAgentActivityRepository.save(ITAgentActivity activity)`: ITAgentActivity
- `SensitiveDataSanitizer.sanitize(String payload)`: String

### M2 ↔ M3
- `ITMessagingService.dispatchMessage(DispatchMessageRequest request, User sender)`: ITAgentMessage
- `ITMessagingService.getMessages(Long threadId, String hashtag)`: List<ITAgentMessage>
- `ITMessagingService.extractHashtags(String content)`: Set<String>
- `NineRouterAiClient.generateAgentResponse(ITAgentProfile agent, String message)`: CompletableFuture<String>

### M3 ↔ M4
- REST Endpoints base: `/api/it-team`
- Response Envelope: `ApiResponse<T>` with `success`, `message`, `data`, `timestamp`
- Auth Header: `Authorization: Bearer <jwt>`
- Endpoints:
  - `GET /api/it-team/agents` -> List<ITAgentProfile>
  - `PUT /api/it-team/agents/{id}/status` -> ITAgentProfile
  - `GET /api/it-team/memories?agentCode=...` -> List<ITAgentMemory>
  - `POST /api/it-team/memories` -> ITAgentMemory
  - `GET /api/it-team/messages?parentMessageId=...` -> List<ITAgentMessage>
  - `POST /api/it-team/messages` -> ITAgentMessage
  - `GET /api/it-team/activities?agentCode=...&actionType=...` -> Page<ITAgentActivity>
  - `GET /api/it-team/browser-tabs` -> List<ITBrowserTabRecord>
  - `POST /api/it-team/browser-tabs` -> ITBrowserTabRecord
  - `POST /api/it-team/api-runs` -> ITApiRunLog
  - `GET /api/it-team/api-runs` -> List<ITApiRunLog>

## Code Layout
### Backend (`src/main/java/com/dentalclinic/`)
- `model/Role.java`: Added `ROLE_ADMIN`
- `security/SecurityConfig.java`: Configured `/api/it-team/**` permissions
- `itteam/model/`:
  - `ITAgentProfile.java`
  - `ITAgentMemory.java`
  - `ITAgentMessage.java`
  - `ITAgentActivity.java`
  - `ITBrowserTabRecord.java`
  - `ITApiRunLog.java`
- `itteam/repository/`:
  - `ITAgentProfileRepository.java`
  - `ITAgentMemoryRepository.java`
  - `ITAgentMessageRepository.java`
  - `ITAgentActivityRepository.java`
  - `ITBrowserTabRecordRepository.java`
  - `ITApiRunLogRepository.java`
- `itteam/service/`:
  - `ITTeamService.java`
  - `ITMessagingService.java`
  - `ITApiRunnerService.java`
  - `SensitiveDataSanitizer.java`
  - `NineRouterAiClient.java`
- `itteam/controller/`:
  - `ITTeamController.java`
- `itteam/config/`:
  - `ITTeamDataInitializer.java`
- `itteam/dto/`:
  - `AgentStatusUpdateRequest.java`
  - `CreateMemoryRequest.java`
  - `DispatchMessageRequest.java`
  - `RecordBrowserTabRequest.java`
  - `ApiRunTestRequest.java`

### Frontend (`src/main/resources/static/`)
- `index.html`: Added `#tab-itteam` button and `#section-itteam` container
- `js/app.js`: Added `'itteam'` to tab array and role check in `renderDynamicRoleView`
- `js/it-team.js`: Modular script managing the 5 sub-views, hashtag autocomplete, and API monitor

### Tests (`src/test/java/com/dentalclinic/`)
- `itteam/ITTeamTests.java`: Comprehensive backend integration tests (seeding, hashtag parsing, RBAC, sanitizer, 9Router fallback, CRUD)
- `e2e/ITTeamE2ETestSuite.java`: Requirement-driven E2E test harness covering Tiers 1-5
