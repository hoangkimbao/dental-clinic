# Detailed Specification & Interface Report: DentalCare IT Team Command Center

**Document Version**: 1.0.0  
**Date**: 2026-09-12  
**Author**: Specification & Interface Miner  
**Project**: DentalCare Management Portal IT Team Command Center  
**Status**: Ready for Implementation  

---

## Executive Summary

This specification defines the complete technical, architectural, and data contract requirements for the **IT Team Command Center** inside the DentalCare Management Portal (`D:\java\dental-clinic`).

The feature introduces a simulated internal IT organization comprising five specialized agents (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`), equipped with persistent memory, hashtag-based inter-agent messaging, activity logging, tab tracking, safe API monitoring, and optional orchestration with local 9Router (`http://localhost:20128`).

All components adhere strictly to enterprise RBAC (`ROLE_ADMIN`), privacy guardrails (automatic redaction of JWTs, passwords, cookies, and medical PII), and seamless integration with DentalCare's existing Spring Boot 3.2.5 / TailwindCSS stack.

---

## 1. Domain Model & Database Persistence Schema

The IT Team Command Center utilizes 6 relational entities mapped via Jakarta Persistence (JPA) into H2 (and production-ready PostgreSQL), extending `com.dentalclinic.common.BaseEntity` to inherit `createdAt` and `updatedAt`.

Package: `com.dentalclinic.itteam.model`

### 1.1 `ITAgentProfile`
- **Table Name**: `it_agent_profile`
- **Description**: Stores persistent profile metadata, operational status, and core competencies of each IT agent.
- **Fields**:
  | Field Name | Type | Nullable | Unique | Constraints / Defaults | Description |
  |---|---|---|---|---|---|
  | `id` | `Long` | NO | YES | `@Id @GeneratedValue(IDENTITY)` | Primary key |
  | `agentCode` | `String` | NO | YES | `length = 50`, e.g. `it-backend` | Unique identifier code |
  | `hashtag` | `String` | NO | YES | `length = 50`, e.g. `#it-backend` | Mention hashtag |
  | `displayName` | `String` | NO | NO | `length = 100` | Human-readable title |
  | `role` | `String` | NO | NO | `length = 100` | Team role title |
  | `status` | `String` | NO | NO | `length = 30`, default `ONLINE` | `ONLINE`, `BUSY`, `IDLE`, `OFFLINE` |
  | `expertise` | `String` | NO | NO | `columnDefinition = "TEXT"` | Core competencies & tech stack |
  | `avatarUrl` | `String` | YES | NO | `length = 255` | Profile icon / avatar URL |
  | `systemPrompt` | `String` | YES | NO | `columnDefinition = "TEXT"` | System instructions for 9Router AI |
  | `createdAt` | `LocalDateTime` | NO | NO | From `BaseEntity` | Audit creation timestamp |
  | `updatedAt` | `LocalDateTime` | YES | NO | From `BaseEntity` | Audit modification timestamp |

- **Indexes**:
  - `idx_agent_code` on `(agent_code)`
  - `idx_agent_hashtag` on `(hashtag)`
  - `idx_agent_status` on `(status)`

---

### 1.2 `ITAgentMemory`
- **Table Name**: `it_agent_memory`
- **Description**: Key-value cognitive memory store per agent with priority indicators and categorization.
- **Fields**:
  | Field Name | Type | Nullable | Unique | Constraints / Defaults | Description |
  |---|---|---|---|---|---|
  | `id` | `Long` | NO | YES | `@Id @GeneratedValue(IDENTITY)` | Primary key |
  | `agentId` | `Long` | NO | NO | FK to `it_agent_profile.id` | Owning agent ID |
  | `agentCode` | `String` | NO | NO | `length = 50` | Denormalized code for fast lookup |
  | `memoryKey` | `String` | NO | NO | `length = 150` | Semantic memory identifier |
  | `memoryContent` | `String` | NO | NO | `columnDefinition = "TEXT"` | Content (sanitized of PII/secrets) |
  | `priorityLevel` | `String` | NO | NO | `length = 20`, default `MEDIUM` | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
  | `category` | `String` | YES | NO | `length = 50` | Category tag (e.g. `CONFIG`, `POLICY`) |
  | `lastUpdated` | `LocalDateTime` | NO | NO | Current timestamp | Explicit last modified timestamp |
  | `createdAt` | `LocalDateTime` | NO | NO | From `BaseEntity` | Audit creation timestamp |
  | `updatedAt` | `LocalDateTime` | YES | NO | From `BaseEntity` | Audit modification timestamp |

- **Indexes & Constraints**:
  - Unique composite index: `uk_agent_memory_key` on `(agent_id, memory_key)`
  - Index: `idx_mem_agent_id` on `(agent_id)`
  - Index: `idx_mem_priority` on `(priority_level)`

---

### 1.3 `ITAgentMessage`
- **Table Name**: `it_agent_message`
- **Description**: Stores inter-agent chat messages, parsed hashtag links, read status, and thread hierarchy.
- **Fields**:
  | Field Name | Type | Nullable | Unique | Constraints / Defaults | Description |
  |---|---|---|---|---|---|
  | `id` | `Long` | NO | YES | `@Id @GeneratedValue(IDENTITY)` | Primary key |
  | `threadId` | `Long` | NO | NO | Default = `id` (for root) | Root thread message ID |
  | `parentMessageId` | `Long` | YES | NO | Nullable for top-level messages | Immediate parent message ID |
  | `senderId` | `Long` | YES | NO | Null for external/admin | Sender agent profile ID |
  | `senderCode` | `String` | NO | NO | `length = 50`, e.g. `admin`, `it-backend` | Sender code |
  | `senderDisplayName` | `String` | YES | NO | `length = 100` | Sender display name |
  | `recipientId` | `Long` | YES | NO | Nullable if broadcast | Target agent profile ID |
  | `recipientCode` | `String` | YES | NO | `length = 50` | Target agent code |
  | `messageBody` | `String` | NO | NO | `columnDefinition = "TEXT"` | Full message body |
  | `parsedHashtags` | `String` | YES | NO | `length = 255` | Comma-separated hashtags extracted |
  | `readStatus` | `Boolean` | NO | NO | Default = `false` | Message read acknowledgment |
  | `isAiGenerated` | `Boolean` | NO | NO | Default = `false` | True if generated by 9Router/AI |
  | `sentTimestamp` | `LocalDateTime` | NO | NO | Default = `now()` | Message dispatch timestamp |
  | `createdAt` | `LocalDateTime` | NO | NO | From `BaseEntity` | Audit creation timestamp |
  | `updatedAt` | `LocalDateTime` | YES | NO | From `BaseEntity` | Audit modification timestamp |

- **Indexes**:
  - `idx_msg_thread_id` on `(thread_id)`
  - `idx_msg_parent_id` on `(parent_message_id)`
  - `idx_msg_recipient` on `(recipient_id)`
  - `idx_msg_sent_time` on `(sent_timestamp)`

---

### 1.4 `ITAgentActivity`
- **Table Name**: `it_agent_activity`
- **Description**: Chronological audit feed of all agent actions, mentions, memory updates, and system operations.
- **Fields**:
  | Field Name | Type | Nullable | Unique | Constraints / Defaults | Description |
  |---|---|---|---|---|---|
  | `id` | `Long` | NO | YES | `@Id @GeneratedValue(IDENTITY)` | Primary key |
  | `agentId` | `Long` | YES | NO | Nullable for system actions | Acting or target agent ID |
  | `agentCode` | `String` | YES | NO | `length = 50` | Acting or target agent code |
  | `actionType` | `String` | NO | NO | `length = 50` | `MENTIONED`, `MESSAGE_SENT`, `MEMORY_UPDATED`, `API_EXECUTED`, `STATUS_CHANGED`, `SYSTEM_STARTUP`, `BROWSER_NAVIGATED` |
  | `description` | `String` | NO | NO | `columnDefinition = "TEXT"` | Detailed log description |
  | `resultSummary` | `String` | YES | NO | `length = 255` | Summary (e.g. `SUCCESS`, `MENTION_LOGGED`, `ERROR`) |
  | `relatedEntityLink` | `String` | YES | NO | `length = 255` | Reference uri (e.g. `message:42`, `api-run:15`) |
  | `timestamp` | `LocalDateTime` | NO | NO | Default = `now()` | Activity execution timestamp |
  | `createdAt` | `LocalDateTime` | NO | NO | From `BaseEntity` | Audit creation timestamp |
  | `updatedAt` | `LocalDateTime` | YES | NO | From `BaseEntity` | Audit modification timestamp |

- **Indexes**:
  - `idx_act_agent_id` on `(agent_id)`
  - `idx_act_action_type` on `(action_type)`
  - `idx_act_timestamp` on `(timestamp)`

---

### 1.5 `ITBrowserTabRecord`
- **Table Name**: `it_browser_tab_record`
- **Description**: Records virtual browsing tabs, documentation links, and consoles consulted by agents.
- **Fields**:
  | Field Name | Type | Nullable | Unique | Constraints / Defaults | Description |
  |---|---|---|---|---|---|
  | `id` | `Long` | NO | YES | `@Id @GeneratedValue(IDENTITY)` | Primary key |
  | `agentId` | `Long` | NO | NO | FK to `it_agent_profile.id` | Owning agent ID |
  | `agentCode` | `String` | NO | NO | `length = 50` | Owning agent code |
  | `tabTitle` | `String` | NO | NO | `length = 200` | Human-readable tab label |
  | `urlRoute` | `String` | NO | NO | `length = 500` | Internal route or URL |
  | `tabCategory` | `String` | YES | NO | `length = 50` | `DOCUMENTATION`, `CONSOLE`, `METRICS`, `INTERNAL_TOOL`, `SWAGGER`, `REPO` |
  | `status` | `String` | NO | NO | `length = 30`, default `ACTIVE` | `ACTIVE`, `BACKGROUND`, `CLOSED` |
  | `openedAt` | `LocalDateTime` | NO | NO | Default = `now()` | Tab opened timestamp |
  | `closedAt` | `LocalDateTime` | YES | NO | Nullable | Tab closed timestamp |
  | `createdAt` | `LocalDateTime` | NO | NO | From `BaseEntity` | Audit creation timestamp |
  | `updatedAt` | `LocalDateTime` | YES | NO | From `BaseEntity` | Audit modification timestamp |

- **Indexes**:
  - `idx_tab_agent_id` on `(agent_id)`
  - `idx_tab_status` on `(status)`
  - `idx_tab_opened_at` on `(opened_at)`

---

### 1.6 `ITApiRunLog`
- **Table Name**: `it_api_run_log`
- **Description**: Execution log and latency metrics of internal API tests conducted via API Monitor.
- **Fields**:
  | Field Name | Type | Nullable | Unique | Constraints / Defaults | Description |
  |---|---|---|---|---|---|
  | `id` | `Long` | NO | YES | `@Id @GeneratedValue(IDENTITY)` | Primary key |
  | `endpoint` | `String` | NO | NO | `length = 500` | Tested URI (e.g. `/api/coupons/active`) |
  | `httpMethod` | `String` | NO | NO | `length = 10` | `GET`, `POST`, `PUT`, `DELETE`, `PATCH` |
  | `statusCode` | `Integer` | NO | NO | HTTP response code (e.g. 200, 401) | HTTP status |
  | `executionDurationMs` | `Long` | NO | NO | Latency in milliseconds | Execution time |
  | `sanitizedRequestPayload` | `String` | YES | NO | `columnDefinition = "TEXT"` | Redacted request payload |
  | `sanitizedResponsePayload` | `String` | YES | NO | `columnDefinition = "TEXT"` | Redacted response payload |
  | `executedBy` | `String` | YES | NO | `length = 100` | User or agent initiator |
  | `errorMessage` | `String` | YES | NO | `columnDefinition = "TEXT"` | Sanitized error or exception details |
  | `runTimestamp` | `LocalDateTime` | NO | NO | Default = `now()` | Test execution timestamp |
  | `createdAt` | `LocalDateTime` | NO | NO | From `BaseEntity` | Audit creation timestamp |
  | `updatedAt` | `LocalDateTime` | YES | NO | From `BaseEntity` | Audit modification timestamp |

- **Indexes**:
  - `idx_apilog_endpoint` on `(endpoint)`
  - `idx_apilog_status` on `(status_code)`
  - `idx_apilog_run_time` on `(run_timestamp)`

---

## 2. Seed Data Specifications for 5 IT Agents

The application must seed the 5 standard IT profiles on initial startup idempotently.

### 2.1 Agent Profiles Roster

1. **`#it-backend`**
   - **agentCode**: `it-backend`
   - **hashtag**: `#it-backend`
   - **displayName**: `Backend Architect & API Engineer`
   - **role**: `Lead Backend Engineer`
   - **status**: `ONLINE`
   - **expertise**: `Spring Boot, Database, Security, REST APIs`
   - **avatarUrl**: `fa-server`
   - **systemPrompt**: `You are #it-backend, the Lead Backend Engineer at DentalCare. You specialize in Spring Boot 3, JPA/Hibernate, H2/PostgreSQL, Spring Security, and REST APIs. Always provide precise, secure architectural guidance.`

2. **`#it-frontend`**
   - **agentCode**: `it-frontend`
   - **hashtag**: `#it-frontend`
   - **displayName**: `Management Portal UX Architect`
   - **role**: `Senior Frontend Engineer`
   - **status**: `ONLINE`
   - **expertise**: `Management Portal UI, API Client, Responsive UX`
   - **avatarUrl**: `fa-desktop`
   - **systemPrompt**: `You are #it-frontend, Senior Frontend Engineer for the DentalCare Management Portal. You specialize in TailwindCSS, Vanilla JS modular design, dynamic Chart.js dashboards, and responsive clinical UI workflows.`

3. **`#it-qa`**
   - **agentCode**: `it-qa`
   - **hashtag**: `#it-qa`
   - **displayName**: `QA Automation & Verification Lead`
   - **role**: `QA & Automation Specialist`
   - **status**: `ONLINE`
   - **expertise**: `API Testing, Authorization Checks, Regression`
   - **avatarUrl**: `fa-bug-slash`
   - **systemPrompt**: `You are #it-qa, QA Automation Specialist at DentalCare. You verify all REST endpoints, validate RBAC access policies, prevent regressions, and enforce testing with synthetic dummy data.`

4. **`#it-devops`**
   - **agentCode**: `it-devops`
   - **hashtag**: `#it-devops`
   - **displayName**: `DevOps & Site Reliability Engineer`
   - **role**: `DevOps & SRE Engineer`
   - **status**: `ONLINE`
   - **expertise**: `Build, Server Runtime, Logs, Tunnel Integration`
   - **avatarUrl**: `fa-cloud`
   - **systemPrompt**: `You are #it-devops, Site Reliability Engineer at DentalCare. You manage Java 17 runtimes, Maven builds, Actuator telemetry, Cloudflared tunnels, and system stability.`

5. **`#it-security`**
   - **agentCode**: `it-security`
   - **hashtag**: `#it-security`
   - **displayName**: `Security & Compliance Officer`
   - **role**: `Security & Privacy Officer`
   - **status**: `ONLINE`
   - **expertise**: `RBAC, Data Privacy, Input Validation, Audit Logs`
   - **avatarUrl**: `fa-shield-halved`
   - **systemPrompt**: `You are #it-security, Security & Compliance Officer at DentalCare. You enforce strict RBAC (ROLE_ADMIN), audit trail integrity, and zero tolerance for leaking JWTs, passwords, or patient medical PII in logs or memory.`

---

### 2.2 Initial Seed Memories

Each agent starts with 2 critical operational memories:

| Agent | Memory Key | Content | Priority | Category |
|---|---|---|---|---|
| `it-backend` | `db-engine-config` | `H2 file DB at ./data/dentaldb;AUTO_SERVER=TRUE. JPA ddl-auto=update. PostgreSQL production driver configured.` | `HIGH` | `INFRASTRUCTURE` |
| `it-backend` | `security-architecture` | `Stateless JWT Bearer token authentication. Standardized response format via ApiResponse<T>.` | `CRITICAL` | `SECURITY` |
| `it-frontend` | `portal-framework` | `TailwindCSS 3 styling, tab-switching engine in app.js, dynamic Chart.js analytics, WebSocket alerts.` | `MEDIUM` | `FRONTEND` |
| `it-frontend` | `role-workspace-matrix` | `Role-based tab rendering: Chủ phòng, Lễ tân, Nha sĩ, Phụ tá, Tạp vụ, Khách hàng.` | `HIGH` | `UX_STRUCTURE` |
| `it-qa` | `regression-policy` | `Universal login and RBAC tests run on each build. MockMvc test suites in DentalClinicApplicationTests.` | `HIGH` | `TESTING` |
| `it-qa` | `synthetic-data-rule` | `Zero real patient data allowed. All tests strictly utilize synthetic mock names and test phones.` | `CRITICAL` | `COMPLIANCE` |
| `it-devops` | `server-runtime-spec` | `Spring Boot 3.2.5 on Java 17. Actuator health/metrics exposed at /actuator. Port 8080.` | `HIGH` | `DEVOPS` |
| `it-devops` | `tunnel-config` | `Cloudflared integrated for secure external preview. Log files in cloudflared.log.` | `MEDIUM` | `NETWORKING` |
| `it-security` | `privacy-guardrail-rule` | `Payload sanitizer strictly blocks passwords, JWTs, Bearer tokens, cookies, and medical PII.` | `CRITICAL` | `SECURITY_POLICY` |
| `it-security` | `admin-rbac-enforcement` | `All /api/it-team/** endpoints require authenticated ROLE_ADMIN. Non-admin access returns 401 or 403.` | `CRITICAL` | `RBAC` |

---

### 2.3 Initial Seed Virtual Browser Tabs

| Agent | Tab Title | URL / Route | Category | Status |
|---|---|---|---|---|
| `it-backend` | `OpenAPI 3 / Swagger Documentation` | `/swagger-ui/index.html` | `SWAGGER` | `ACTIVE` |
| `it-backend` | `H2 Database Console` | `/h2-console` | `CONSOLE` | `BACKGROUND` |
| `it-frontend` | `Management Portal Workspace` | `/#management-portal` | `INTERNAL_TOOL` | `ACTIVE` |
| `it-qa` | `Spring Boot Actuator Health Check` | `/actuator/health` | `METRICS` | `ACTIVE` |
| `it-devops` | `Actuator Application Metrics` | `/actuator/metrics` | `METRICS` | `ACTIVE` |
| `it-security` | `Security Audit Feed & Log Analyzer` | `/api/it-team/activities` | `INTERNAL_TOOL` | `ACTIVE` |

---

### 2.4 Idempotent Startup Seeding Logic

```
ALGORITHM SeedItTeamData():
  FOR EACH agent IN PredefinedAgents:
    existing = itAgentProfileRepository.findByAgentCode(agent.agentCode)
    IF existing IS NULL:
      profile = itAgentProfileRepository.save(agent)
      SeedMemoriesForAgent(profile)
      SeedBrowserTabsForAgent(profile)
    ELSE:
      // Verify existing profile integrity without overwriting user changes
      profile = existing
  
  IF itAgentActivityRepository.countByActionType("SYSTEM_STARTUP") == 0:
    itAgentActivityRepository.save(new ITAgentActivity(
      agentId = null,
      agentCode = "system",
      actionType = "SYSTEM_STARTUP",
      description = "IT Team Command Center successfully initialized with 5 specialized agents.",
      resultSummary = "SYSTEM_ONLINE",
      timestamp = now()
    ))
```

**Restart Safety Guarantees**:
1. Checks `findByAgentCode(agentCode)` prior to insertion.
2. Unique database constraint on `(agent_code)` and `(hashtag)` prevents race condition duplicates.
3. Checks `itAgentMemoryRepository.findByAgentIdAndMemoryKey(...)` before inserting initial memories.
4. Consecutive server boots execute in under 10ms with 0 duplicate rows.

---

## 3. Hashtag Parsing, Routing & Threading Specifications

### 3.1 Hashtag Grammar & Regex Definition

- **Regex Specification**:
  ```java
  public static final Pattern IT_AGENT_HASHTAG_REGEX = 
      Pattern.compile("(?i)#(it-(?:backend|frontend|qa|devops|security))\\b");
  ```
- **Grammar Rules**:
  1. Starts with literal `#`.
  2. Followed by `it-` (case-insensitive).
  3. Must strictly match one of the 5 canonical agent codes: `backend`, `frontend`, `qa`, `devops`, `security`.
  4. Followed by a word boundary `\b`, allowing punctuation such as `#it-qa,`, `#it-backend!`, `#it-security.`, `(#it-devops)`.
  5. Extracted hashtags are normalized to lower-case: e.g. `#IT-QA` -> `#it-qa`.

### 3.2 Parsing Algorithm & Edge Cases

```
FUNCTION ParseHashtags(messageText):
  IF messageText IS NULL OR messageText.isBlank():
    RETURN []
  
  matches = []
  matcher = IT_AGENT_HASHTAG_REGEX.matcher(messageText)
  WHILE matcher.find():
    tag = matcher.group(0).toLowerCase()
    IF tag NOT IN matches:
      matches.add(tag)
  
  RETURN matches
```

### 3.3 Mention Linking & Activity Generation

When a message is received at `POST /api/it-team/messages`:
1. Parse hashtags from `messageBody`.
2. Determine Primary Recipient:
   - If one or more agent hashtags are present, the **first** mentioned agent is set as `recipientId` and `recipientCode`.
   - If no hashtags are present, `recipientId` and `recipientCode` remain `null` (general broadcast).
3. Set `parsedHashtags` as a comma-separated string (e.g. `#it-qa,#it-devops`).
4. Persist `ITAgentMessage`.
5. For **every** mentioned agent in the extracted list:
   - Create an `ITAgentActivity` record:
     - `agentId`: Target agent profile ID
     - `agentCode`: Target agent code
     - `actionType`: `MENTIONED`
     - `description`: `Được nhắc tới trong tin nhắn #${savedMessage.id} bởi ${savedMessage.senderCode}: "${truncate(savedMessage.messageBody, 80)}"`
     - `resultSummary`: `MENTION_LOGGED`
     - `relatedEntityLink`: `message:${savedMessage.id}`
     - `timestamp`: `now()`

### 3.4 Threading Semantics

Messages support flat channels and hierarchical conversation threads:

- **Root Message**:
  - `parentMessageId`: `null`
  - `threadId`: Upon database save, if `threadId` is null, it is set equal to `savedMessage.id`.
- **Threaded Reply**:
  - Client sends `parentMessageId` (e.g. 10).
  - Server verifies that parent message exists:
    - If `parent.threadId != null`, assign `threadId = parent.threadId`.
    - If `parent.threadId == null`, assign `threadId = parent.id`.
    - Set `parentMessageId = parent.id`.
- **Thread Retrieval Query**:
  - Query: `findByThreadIdOrderBySentTimestampAsc(Long threadId)`
  - Returns the root message and all sequential replies ordered chronologically.

---

## 4. REST API Contract Definitions (Admin RBAC Protected)

All endpoints reside under `/api/it-team/**` and are strictly guarded by JWT authentication with `ROLE_ADMIN`.

### 4.1 RBAC Enforcement Specification
- **Security Rule**:
  - In `SecurityConfig`: `.requestMatchers("/api/it-team/**").hasRole("ADMIN")`
  - Or controller class-level annotation: `@PreAuthorize("hasRole('ADMIN')")`
- **Role Model Update**:
  - In `com.dentalclinic.model.Role`: add `ROLE_ADMIN`.
  - Also allow `ROLE_OWNER` access if business grants full owner-admin equivalence: `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")`.
- **Unauthorized Behavior**:
  - Unauthenticated request -> HTTP `401 Unauthorized` or `403 Forbidden` (`{"success": false, "message": "Access Denied"}`).
  - Authenticated with non-admin role (`ROLE_PATIENT`, `ROLE_DENTIST`, `ROLE_RECEPTIONIST`) -> HTTP `403 Forbidden` (`{"success": false, "message": "Bạn không có quyền truy cập chức năng này!"}`).
  - Authenticated with `ROLE_ADMIN` (or `ROLE_OWNER`) -> HTTP `200 OK` or `201 Created`.

---

### 4.2 Endpoint Specifications

#### 1. Agent Profile Endpoints
- **`GET /api/it-team/agents`**
  - **Summary**: Retrieve all 5 IT agent profiles with statuses.
  - **Response 200**:
    ```json
    {
      "success": true,
      "message": "Thành công",
      "data": [
        {
          "id": 1,
          "agentCode": "it-backend",
          "hashtag": "#it-backend",
          "displayName": "Backend Architect & API Engineer",
          "role": "Lead Backend Engineer",
          "status": "ONLINE",
          "expertise": "Spring Boot, Database, Security, REST APIs",
          "avatarUrl": "fa-server"
        }
      ],
      "timestamp": "2026-09-12T15:00:00"
    }
    ```

- **`PUT /api/it-team/agents/{id}/status`**
  - **Summary**: Update an agent's operational status.
  - **Path Variable**: `id` (`Long`)
  - **Request Body**:
    ```json
    {
      "status": "BUSY"
    }
    ```
  - **Validation**: `status` must be one of `ONLINE`, `BUSY`, `IDLE`, `OFFLINE`.
  - **Response 200**: `ApiResponse<ITAgentProfileDto>`
  - **Side Effect**: Logs `STATUS_CHANGED` activity.

---

#### 2. Agent Memories Endpoints
- **`GET /api/it-team/memories`**
  - **Summary**: List agent memories with optional filters.
  - **Query Parameters**:
    - `agentId` (`Long`, optional): Filter by agent ID.
    - `agentCode` (`String`, optional): Filter by agent code (e.g. `it-backend`).
    - `priority` (`String`, optional): Filter by `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.
    - `page` (`int`, default 0), `size` (`int`, default 50).
  - **Response 200**: `ApiResponse<Page<ITAgentMemoryDto>>` or `ApiResponse<List<ITAgentMemoryDto>>`.

- **`POST /api/it-team/memories`**
  - **Summary**: Upsert or create memory record for an agent.
  - **Request Body**:
    ```json
    {
      "agentId": 1,
      "memoryKey": "api-gateway-routes",
      "memoryContent": "Routes configured for /api/** under rate limit 100 req/min.",
      "priorityLevel": "HIGH",
      "category": "CONFIGURATION"
    }
    ```
  - **Validation**: `agentId` (`@NotNull`), `memoryKey` (`@NotBlank`), `memoryContent` (`@NotBlank`).
  - **Security Filter**: `memoryContent` automatically sanitized against PII/secrets.
  - **Response 201**: `ApiResponse<ITAgentMemoryDto>`.
  - **Side Effect**: Logs `MEMORY_UPDATED` activity.

- **`DELETE /api/it-team/memories/{id}`**
  - **Summary**: Delete a specific memory item.
  - **Response 200**: `ApiResponse<Void>`.

---

#### 3. Inter-Agent Messaging Endpoints
- **`GET /api/it-team/messages`**
  - **Summary**: Retrieve chat messages.
  - **Query Parameters**:
    - `threadId` (`Long`, optional): If provided, returns all messages in this thread ordered by `sentTimestamp ASC`.
    - `recipientCode` (`String`, optional): Filter by tagged recipient.
    - `senderCode` (`String`, optional): Filter by sender.
    - `page` (`int`, default 0), `size` (`int`, default 50).
  - **Response 200**: `ApiResponse<List<ITAgentMessageDto>>`.

- **`POST /api/it-team/messages`**
  - **Summary**: Dispatch message with hashtag parsing and optional AI trigger.
  - **Request Body**:
    ```json
    {
      "parentMessageId": null,
      "senderCode": "admin",
      "senderDisplayName": "Hệ Thống Quản Trị",
      "messageBody": "Xin chào #it-qa, vui lòng chạy kiểm thử hồi quy cho API đặt lịch.",
      "triggerAi": true
    }
    ```
  - **Validation**: `messageBody` (`@NotBlank`).
  - **Response 201**:
    ```json
    {
      "success": true,
      "message": "Tin nhắn đã được gửi thành công",
      "data": {
        "message": {
          "id": 15,
          "threadId": 15,
          "parentMessageId": null,
          "senderCode": "admin",
          "recipientCode": "it-qa",
          "messageBody": "Xin chào #it-qa, vui lòng chạy kiểm thử hồi quy cho API đặt lịch.",
          "parsedHashtags": "#it-qa",
          "readStatus": false,
          "isAiGenerated": false,
          "sentTimestamp": "2026-09-12T15:05:00"
        },
        "aiReply": {
          "id": 16,
          "threadId": 15,
          "parentMessageId": 15,
          "senderCode": "it-qa",
          "recipientCode": "admin",
          "messageBody": "Chào bạn! #it-qa đã tiếp nhận yêu cầu. Bộ kiểm thử tự động (Universal Login, RBAC checks, Booking API) đang sẵn sàng thực thi.",
          "parsedHashtags": "",
          "readStatus": false,
          "isAiGenerated": true,
          "sentTimestamp": "2026-09-12T15:05:01"
        }
      }
    }
    ```

---

#### 4. Activity Logs Endpoints
- **`GET /api/it-team/activities`**
  - **Summary**: Chronological audit trail of agent actions and mentions.
  - **Query Parameters**:
    - `agentCode` (`String`, optional): Filter by agent code.
    - `actionType` (`String`, optional): Filter by `MENTIONED`, `MESSAGE_SENT`, etc.
    - `page` (`int`, default 0), `size` (`int`, default 30).
  - **Response 200**: `ApiResponse<Page<ITAgentActivityDto>>` ordered by `timestamp DESC`.

---

#### 5. Virtual Browser Tab Endpoints
- **`GET /api/it-team/browser-tabs`**
  - **Summary**: List virtual browser tab records.
  - **Query Parameters**:
    - `agentCode` (`String`, optional)
    - `status` (`String`, optional: `ACTIVE`, `BACKGROUND`, `CLOSED`)
  - **Response 200**: `ApiResponse<List<ITBrowserTabRecordDto>>`.

- **`POST /api/it-team/browser-tabs`**
  - **Summary**: Open or log a browser tab record.
  - **Request Body**:
    ```json
    {
      "agentId": 1,
      "tabTitle": "Springdoc Swagger UI",
      "urlRoute": "/swagger-ui/index.html",
      "tabCategory": "SWAGGER",
      "status": "ACTIVE"
    }
    ```
  - **Response 201**: `ApiResponse<ITBrowserTabRecordDto>`.

---

#### 6. API Monitor & Safe Execution Endpoints
- **`GET /api/it-team/api-runs`**
  - **Summary**: List past API test run logs.
  - **Query Parameters**:
    - `endpoint` (`String`, optional)
    - `statusCode` (`Integer`, optional)
    - `page` (`int`, default 0), `size` (`int`, default 20)
  - **Response 200**: `ApiResponse<Page<ITApiRunLogDto>>` ordered by `runTimestamp DESC`.

- **`POST /api/it-team/api-runs`**
  - **Summary**: Safely execute an internal localhost endpoint test, measure latency, and log sanitized result.
  - **Request Body**:
    ```json
    {
      "endpoint": "/api/coupons/active",
      "httpMethod": "GET",
      "requestPayload": null,
      "headers": {
        "Accept": "application/json"
      }
    }
    ```
  - **Safety Validation**:
    1. Endpoint MUST start with `/api/` or `http://localhost:8080/api/`.
    2. External non-localhost hosts are rejected with `400 Bad Request`.
    3. Payload is sanitized before saving to database.
  - **Response 200**: `ApiResponse<ITApiRunLogDto>` with latency, status code, and sanitized output.

---

## 5. Sensitive Data Redaction & Privacy Guardrails

Per Requirement R1, storing cookies, plaintext passwords, JWTs, credentials, medical records (EMR), or real patient PII in memory, activity, browser tab, or API run logs is strictly prohibited.

### 5.1 Redaction Replacement Format
- General Secrets: `[REDACTED]`
- Medical Diagnoses/Prescriptions: `[REDACTED_MEDICAL]`
- National ID / CCCD: `[REDACTED_ID]`
- Bearer Tokens: `Bearer [REDACTED]`

### 5.2 Canonical Regex Rules

1. **JWT Strings (3-part base64url separated by periods)**:
   ```java
   Pattern JWT_PATTERN = Pattern.compile("ey[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");
   // Replacement: "[REDACTED_JWT]"
   ```

2. **Authorization / Bearer Headers**:
   ```java
   Pattern BEARER_PATTERN = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9_\\-\\.]+");
   // Replacement: "$1[REDACTED]"
   ```

3. **Sensitive JSON Key-Value Fields (Passwords, Secrets, Tokens)**:
   ```java
   Pattern JSON_SECRET_PATTERN = Pattern.compile(
       "(?i)\"(password|passwd|pwd|secret|token|apiKey|api_key|refreshToken|accessToken)\"\\s*:\\s*\"[^\"]*\""
   );
   // Replacement: "\"$1\": \"[REDACTED]\""
   ```

4. **Cookie & Session Identifiers**:
   ```java
   Pattern COOKIE_PATTERN = Pattern.compile("(?i)(Set-Cookie|Cookie)\\s*:\\s*[^\\r\\n;]+");
   Pattern JSESSION_PATTERN = Pattern.compile("(?i)(JSESSIONID|remember-me|dental_token)=[^;\\s]+");
   // Replacement: "\"$1=[REDACTED]\""
   ```

5. **Vietnamese Citizen ID (CCCD/CMND)**:
   ```java
   Pattern CCCD_PATTERN = Pattern.compile("\\b(0\\d{11}|\\d{9})\\b");
   // Replacement: "[REDACTED_ID]"
   ```

6. **Medical EMR Fields in JSON**:
   ```java
   Pattern EMR_FIELD_PATTERN = Pattern.compile(
       "(?i)\"(diagnosis|prescription|treatmentDone|doctorNotes)\"\\s*:\\s*\"[^\"]*\""
   );
   // Replacement: "\"$1\": \"[REDACTED_MEDICAL]\""
   ```

7. **Patient Phone & Email Masking**:
   ```java
   Pattern PHONE_MASK_PATTERN = Pattern.compile("(?i)\"(phone|patientPhone)\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{3})\"");
   // Replacement: "\"$1\": \"$2****$3\""
   ```

### 5.3 Enforcement Points
- `ITSanitizerService.sanitize(String payload)` must be invoked in:
  - `ITApiRunLog` creation for `sanitizedRequestPayload` and `sanitizedResponsePayload`.
  - `ITAgentMemory` creation and update for `memoryContent`.
  - `ITAgentActivity` creation for `description` and `resultSummary`.
  - `ITAgentMessage` storage for `messageBody`.

---

## 6. 9Router AI Integration & Fallback Protocol

### 6.1 Integration Specification
- **Base Endpoint**: `http://localhost:20128`
- **Path**: `/v1/chat/completions`
- **Method**: `POST`
- **Protocol**: OpenAI-compatible JSON Chat Completions
- **Timeout**: `3000ms` connect and read timeout

### 6.2 Request Payload Format
```json
{
  "model": "gpt-4o-mini",
  "messages": [
    {
      "role": "system",
      "content": "You are #it-qa, QA Automation Specialist at DentalCare. Expertise: API Testing, Authorization Checks, Regression. Respond concisely in Vietnamese."
    },
    {
      "role": "user",
      "content": "Kiểm tra endpoint /api/appointments/book"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 500
}
```

### 6.3 Fallback Engine (Offline / Unreachable 9Router)

When 9Router is offline or unreachable:
1. `RestClient` catches `ResourceAccessException`, `ConnectException`, `SocketTimeoutException`, or HTTP 5xx.
2. The exception is logged at `WARN` level: `9Router offline at http://localhost:20128. Invoking rule-based agent fallback response.`
3. An activity log entry is recorded with summary `FALLBACK_TRIGGERED`.
4. The system immediately invokes the **Deterministic Expert Rule Engine** tailored to the tagged agent:

| Mentioned Agent | Deterministic Fallback Response Content |
|---|---|
| `#it-backend` | `"Chào bạn! #it-backend đã ghi nhận thông tin. Đang kiểm tra logic Spring Boot, ORM Hibernate và kết nối database H2/PostgreSQL. Tất cả các service backend đang hoạt động ổn định."` |
| `#it-frontend` | `"Chào bạn! #it-frontend đã tiếp nhận. Giao diện Management Portal và các thành phần TailwindCSS/Chart.js đang hoạt động mượt mà."` |
| `#it-qa` | `"Chào bạn! #it-qa đã tiếp nhận. Bộ kiểm thử tự động (Universal Login, RBAC checks, Booking API) đang sẵn sàng thực thi."` |
| `#it-devops` | `"Chào bạn! #it-devops đã tiếp nhận. Server runtime Java 17, Cloudflared tunnel và Spring Boot Actuator metrics đều bình thường."` |
| `#it-security` | `"Chào bạn! #it-security đã tiếp nhận. Bộ lọc RBAC JWT và chính sách làm sạch dữ liệu nhạy cảm (Data Redaction Guardrail) đang được kích hoạt 100%."` |

5. The fallback response is saved as a genuine `ITAgentMessage` record with `isAiGenerated = true` and returned to the client, guaranteeing zero downtime or error popups.

---

## 7. Features Discovered Table

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|---|---|---|---|---|---|---|
| 1 | Domain Model | `ITAgentProfile` JPA Entity | Models the 5 IT agents, statuses, roles, and skills | DB row / DTO | Entity with ID, timestamps | Unique constraint violation if duplicate `agent_code` | ORIGINAL_REQUEST R1 |
| 2 | Domain Model | `ITAgentMemory` JPA Entity | Key-value structured memory per agent with priority | `agentId`, `key`, `content`, `priority` | Persisted memory record | `400 Bad Request` if mandatory fields missing | ORIGINAL_REQUEST R1 |
| 3 | Domain Model | `ITAgentMessage` JPA Entity | Stores chat history, parsed hashtags, threads | `messageBody`, sender, recipient | Persisted message with `threadId` | `400 Bad Request` if blank body | ORIGINAL_REQUEST R1 |
| 4 | Domain Model | `ITAgentActivity` JPA Entity | Audit log of actions, mentions, updates | `agentId`, `actionType`, `description` | Persisted audit entry | Handled gracefully by service | ORIGINAL_REQUEST R1 |
| 5 | Domain Model | `ITBrowserTabRecord` JPA Entity | Tracks virtual opened tabs/routes | `agentId`, `tabTitle`, `urlRoute` | Persisted tab record | `400 Bad Request` if missing URL/title | ORIGINAL_REQUEST R1 |
| 6 | Domain Model | `ITApiRunLog` JPA Entity | Internal API testing metrics & logs | `endpoint`, `method`, status, latency | Persisted log record | Redacts payload before save | ORIGINAL_REQUEST R1 |
| 7 | Data Seeder | Idempotent Agent Seeding | Auto-seeds 5 agents on startup without duplicates | Startup lifecycle | 5 initialized agents | Skips existing agents | ORIGINAL_REQUEST R1, Acceptance Criteria |
| 8 | Data Seeder | Initial Agent Memory Seeding | Auto-seeds 10 core operational memories | Agent IDs | Populated memories | Idempotent upsert | ORIGINAL_REQUEST R1 |
| 9 | Data Seeder | Initial Browser Tab Seeding | Preloads Swagger, H2 console, Actuator tabs | Agent IDs | Initialized tabs | Idempotent check | ORIGINAL_REQUEST R1 |
| 10 | Messaging | Hashtag Parsing Grammar | Regex-based extraction of `#it-*` mentions | String text | Set of normalized hashtags | Ignores invalid/unrecognized tags | ORIGINAL_REQUEST R2 |
| 11 | Messaging | Multi-Agent Routing | Routes to primary recipient and logs all mentions | Message with multiple tags | Updated recipient + activities | Dispatches activity per tagged agent | ORIGINAL_REQUEST R2 |
| 12 | Messaging | Threaded Conversation Tree | Groups replies under root `threadId` | `parentMessageId` | Structured thread messages | If parent not found, starts new root | ORIGINAL_REQUEST R2 |
| 13 | Messaging | Mentioned Activity Generation | Auto-creates `MENTIONED` activity on tag | Parsed hashtags | Inserted `ITAgentActivity` | Logged silently | ORIGINAL_REQUEST R2 |
| 14 | REST API | Agent Status Toggle API | Allows admins to switch agent between ONLINE, BUSY, etc. | Agent ID + new status | Updated profile | `404 Not Found` if ID invalid, `400` if invalid status | ORIGINAL_REQUEST R3 |
| 15 | REST API | Agent Memories CRUD API | Retrieve, add, delete agent cognitive memories | Filter params / DTO | Memory DTO / Page | `400 Bad Request` on validation failure | ORIGINAL_REQUEST R3 |
| 16 | REST API | Message Dispatch & Feed API | Send messages, retrieve thread history | Message DTO / Thread ID | Message list / result DTO | `400 Bad Request` on empty body | ORIGINAL_REQUEST R3 |
| 17 | REST API | Activity Audit Log API | Paginated audit log retrieval | Pageable + filters | Page of activities | `200 OK` with empty page if none | ORIGINAL_REQUEST R3 |
| 18 | REST API | Browser Tab Session API | Read and record virtual tabs | Tab DTO / filters | List of tabs | `400 Bad Request` if invalid params | ORIGINAL_REQUEST R3 |
| 19 | REST API | Safe Internal API Runner | Executes internal GET/POST calls and logs latency | Endpoint, method, payload | `ITApiRunLogDto` with latency | Blocks non-localhost with `400 Bad Request` | ORIGINAL_REQUEST R3 |
| 20 | AI Integration | 9Router Chat Client | Connects to `http://localhost:20128` | System + user prompt | AI assistant reply | Switches to rule-based fallback on error | ORIGINAL_REQUEST R3 |
| 21 | AI Integration | Deterministic Rule-Based Fallback | Provides specialized responses when 9Router offline | Mentioned agent code | High-quality domain reply | Fallback always succeeds | ORIGINAL_REQUEST R3 |
| 22 | Security & RBAC | `ROLE_ADMIN` JWT Authorization | Restricts `/api/it-team/**` to admins | Bearer token in header | `200/201` or `401/403` | `401 Unauthorized` or `403 Forbidden` | ORIGINAL_REQUEST R3, SecurityConfig |
| 23 | Privacy | Sensitive Data Sanitizer | Redacts JWTs, passwords, cookies, medical EMR | Raw text/JSON | Sanitized text | Safely handles malformed strings | ORIGINAL_REQUEST R1 |
| 24 | UI Integration | Management Portal Tab "IT Team" | Adds 6th main tab to DentalCare portal | Admin authentication | Renders IT Team workspace | Hidden for non-admins | ORIGINAL_REQUEST R4 |
| 25 | UI Integration | 5 Sub-Views Navigation | Sub-tabs: Profiles, Chat, Memory, Logs, API Monitor | Sub-view selection | Switches visible panel | Defaults to Profiles view | ORIGINAL_REQUEST R4 |
| 26 | UI Integration | Compose Box Autocomplete | Shows popup suggestions when typing `#it-` | Input event on textarea | Autocomplete dropdown | Hides on escape or selection | ORIGINAL_REQUEST R4 |

---

## 8. Edge Cases & Observed Behaviors

| # | Feature | Input | Observed / Specified Behavior |
|---|---|---|---|
| 1 | Hashtag Parser | Duplicate mentions: `"Hey #it-backend and #it-backend please check"` | Extracts unique set `["#it-backend"]`. Generates exactly 1 `MENTIONED` activity, preventing duplicate spam. |
| 2 | Hashtag Parser | Multiple distinct agents: `"Hey #it-qa and #it-devops, check release"` | Sets primary `recipientCode` to `it-qa`. Generates TWO separate `MENTIONED` activity records (one for `it-qa`, one for `it-devops`). |
| 3 | Hashtag Parser | Case variations: `"#IT-BACKEND"`, `"#It-Qa"`, `"#it-SECURITY"` | Case-insensitively matched and normalized to canonical lowercase (`#it-backend`, `#it-qa`, `#it-security`). |
| 4 | Hashtag Parser | Trailing punctuation: `"Can #it-backend, help? Check (#it-qa)!"` | Regex word boundary `\b` cleanly captures `#it-backend` and `#it-qa` without trailing commas or parentheses. |
| 5 | Hashtag Parser | Unknown hashtags: `"#it-mobile"`, `"#it-ai"`, `"#developer"` | Ignored by parser; does not route to unknown recipient or crash. |
| 6 | Hashtag Parser | Empty or whitespace-only message body: `""` or `"   "` | Rejected with `400 Bad Request` via `@NotBlank` Bean Validation. |
| 7 | Threading | Reply to a reply (nested thread): child message replies to message ID 12 which is already a reply to root 10 | Assigns `threadId = 10` (root thread ID) and `parentMessageId = 12`. Maintains single coherent thread hierarchy. |
| 8 | Threading | Reply to non-existent parent ID: `parentMessageId = 99999` | Server falls back to treating it as a new root message with `parentMessageId = null`, avoiding orphan foreign key errors. |
| 9 | Data Seeder | Application restart with existing DB rows | Checks `findByAgentCode()` for each agent. Skips insert; table row count remains exactly 5. Zero duplicate key exceptions. |
| 10 | AI Integration | 9Router is offline / port 20128 closed | `RestClient` connection times out or fails. Catches exception within 3000ms, logs warning, invokes deterministic domain fallback message, returns HTTP 201. UI shows successful reply without error popup. |
| 11 | AI Integration | 9Router returns HTTP 500 or malformed JSON | Safely caught by exception handler, triggers fallback response, returns valid `ITAgentMessage`. |
| 12 | API Runner | External / Malicious URL: `endpoint = "https://malicious-site.com/steal"` | Regex validation detects non-localhost/non-relative URL and returns `400 Bad Request` (`"Chỉ cho phép chạy thử nghiệm các API nội bộ trên localhost"`). |
| 13 | API Runner | Request payload contains password: `{"password": "secret123"}` | Sanitizer replaces with `{"password": "[REDACTED]"}` before saving to `it_api_run_log`. Plaintext password never touches disk. |
| 14 | API Runner | Response payload contains JWT token | Sanitizer replaces `ey...` with `[REDACTED_JWT]` in `it_api_run_log.sanitizedResponsePayload`. |
| 15 | RBAC | Unauthenticated request to `/api/it-team/agents` | Intercepted by Spring Security, returns `401 Unauthorized` or `403 Forbidden`. |
| 16 | RBAC | Patient JWT (`ROLE_PATIENT`) requests `/api/it-team/memories` | Intercepted by `@PreAuthorize("hasRole('ADMIN')")`, returns `403 Forbidden` (`"Bạn không có quyền truy cập chức năng này!"`). |
| 17 | RBAC | Dentist JWT (`ROLE_DENTIST`) requests `/api/it-team/messages` | Intercepted by `@PreAuthorize`, returns `403 Forbidden`. |
| 18 | RBAC | Valid Admin JWT (`ROLE_ADMIN`) requests `/api/it-team/**` | Successfully executes request and returns standard `ApiResponse<T>` with HTTP 200/201. |
| 19 | Memory Store | Agent adds memory with existing key: `memoryKey = "db-engine-config"` | Service detects existing `(agentId, memoryKey)` and updates `memoryContent` and `lastUpdated` rather than failing. |
| 20 | Frontend UI | User types `#it-` in message compose box | Autocomplete popup appears with 5 agent options; selecting one inserts `#it-agentCode ` at cursor position. |

---

## 9. Verification Guidelines for Implementation Agents

1. **Unit & Integration Tests**:
   - `ITHashtagParserTest`: Test regex matching, case insensitivity, boundary handling, deduplication, multi-tag extraction.
   - `ITSanitizerTest`: Test redaction of passwords, JWTs, Bearer headers, CCCD, cookies, and medical EMR fields.
   - `ITTeamControllerSecurityTest`: Use `MockMvc` to verify:
     - Unauthenticated requests -> 401/403.
     - `ROLE_PATIENT` -> 403 Forbidden.
     - `ROLE_ADMIN` -> 200 OK.
   - `ITDataInitializerTest`: Verify startup seeding and idempotent restart behavior.
   - `NineRouterClientFallbackTest`: Mock offline 9Router and assert fallback message generation.
2. **Database Verification**:
   - Verify table creation: `it_agent_profile`, `it_agent_memory`, `it_agent_message`, `it_agent_activity`, `it_browser_tab_record`, `it_api_run_log`.
   - Verify row count in `it_agent_profile` equals 5.
3. **Frontend UI Verification**:
   - Verify presence of "IT Team" tab for admin users.
   - Verify switching between the 5 sub-tabs (Profiles, Chat, Memory, Logs, API Monitor).
   - Verify typing `#it-` triggers autocomplete popup.
