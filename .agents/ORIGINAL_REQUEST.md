# Original User Request

## Initial Request — 2026-09-12T14:55:24Z

Build the "IT Team Command Center" inside the DentalCare Management Portal at `D:\java\dental-clinic`, simulating an active internal IT organization with persistent memory, activity logging, safe API monitoring, and hashtag-based inter-agent communication, with optional orchestration via 9Router (http://localhost:20128).

Working directory: D:\java\dental-clinic
Integrity mode: development

## Requirements

### R1. Domain Model & Database Persistence
- Implement JPA Entities & Repositories in `com.dentalclinic.itteam`:
  - `ITAgentProfile`: Agent code, hashtag (e.g. `#it-backend`), display name, role, status, expertise.
  - `ITAgentMemory`: Agent ID, memory key, memory content, priority level, last updated timestamp.
  - `ITAgentMessage`: Sender ID, recipient ID, message body, parsed hashtags, read status, sent timestamp.
  - `ITAgentActivity`: Agent ID, action type, description, result summary, related entity link, timestamp.
  - `ITBrowserTabRecord`: Agent ID, tab title, URL/route, tab category, status, opened/closed timestamp.
  - `ITApiRunLog`: Endpoint, HTTP method, status code, execution duration (ms), sanitized request/response payload, run timestamp.
- Data Seed: Automatically seed the 5 standard IT profiles on initial application startup:
  1. `#it-backend` (Spring Boot, Database, Security, REST APIs)
  2. `#it-frontend` (Management Portal UI, API Client, Responsive UX)
  3. `#it-qa` (API Testing, Authorization Checks, Regression)
  4. `#it-devops` (Build, Server Runtime, Logs, Tunnel Integration)
  5. `#it-security` (RBAC, Data Privacy, Input Validation, Audit Logs)
- Privacy Guardrail: Strictly prohibit storing cookies, plaintext passwords, JWTs, credentials, medical records (EMR), or real patient PII in memory/activity/tab/API logs. All payloads must be redacted/sanitized.

### R2. Hashtag Parsing & Inter-Agent Messaging Engine
- Implement a message parser that extracts mentions matching `#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`.
- Automatically create recipient links and route `ITAgentMessage` records.
- When an agent is tagged/mentioned, trigger an `ITAgentActivity` record of type `MENTIONED`.
- Support threaded conversation replies and chronological message retrieval.

### R3. REST API Layer (Admin RBAC Protected)
- Implement REST endpoints in `ITTeamController`, strictly protected by JWT authentication with `ROLE_ADMIN`:
  - `GET /api/it-team/agents` & `PUT /api/it-team/agents/{id}/status`: Profile management.
  - `GET /api/it-team/memories` & `POST /api/it-team/memories`: Memory retrieval and creation.
  - `GET /api/it-team/messages` & `POST /api/it-team/messages`: Hashtag message dispatch and thread view.
  - `GET /api/it-team/activities`: Paginated agent activity audit trail.
  - `GET /api/it-team/browser-tabs` & `POST /api/it-team/browser-tabs`: Tab session logging.
  - `POST /api/it-team/api-runs`: Safe execution testing of internal localhost APIs.
- 9Router AI Integration: Connect with local 9Router instance at `http://localhost:20128` to provide intelligent agent responses when triggered.

### R4. Management Portal UI — "IT Team" Command Center
- Integrate a new sub-module / tab "IT Team" into the DentalCare Management Portal.
- Provide 5 sub-views:
  1. **Nhân Sự (Team Profiles)**: Cards showing the 5 IT agents, their current status, roles, and core competencies.
  2. **Hội Thoại (Conversations)**: Chat channel with hashtag autocomplete (`#it-...`), threaded replies, and message history.
  3. **Bộ Nhớ (Agent Memories)**: Structured key-value memory store per agent with priority indicators.
  4. **Nhật Ký Thao Tác (Activity Logs)**: Chronological audit feed of agent actions and mentions.
  5. **API Monitor**: Interactive tester and monitoring dashboard displaying endpoints, methods, HTTP status badges, latency, and sanitized outputs.
- Filter controls by Agent, status, and date range.
- Maintain seamless visual consistency with DentalCare's existing design system.

### R5. Safety & System Non-Interference
- Do not modify or delete existing clinic data directories (`data/`, `uploads/`, `.m2/`, `target/`).
- Preserve 100% existing functionality: Booking, Login/Auth, Dashboard, and WebSocket real-time alerts.
- Ensure all test data uses synthetic dummy placeholders; never use real patient records.

## Acceptance Criteria

### Data & Model Verification
- [ ] Database contains all 5 seeded IT agent profiles upon startup without duplicate entries on restarts.
- [ ] Database tables `it_agent_profile`, `it_agent_memory`, `it_agent_message`, `it_agent_activity`, `it_browser_tab_record`, `it_api_run_log` are created and operational.
- [ ] Sensitive data sanitizer prevents storing JWTs, passwords, or PII in logs and memory.

### Messaging & Routing Verification
- [ ] Sending a message containing `#it-qa` automatically links `#it-qa` as recipient and generates an activity log.
- [ ] Threaded replies correctly group under their parent message thread.

### API & Security Verification
- [ ] Non-ADMIN requests (unauthenticated or `ROLE_PATIENT`) to `/api/it-team/**` return `401 Unauthorized` or `403 Forbidden`.
- [ ] Valid ADMIN JWT can perform all CRUD operations on IT team endpoints.
- [ ] Automated tests pass for hashtag parser, RBAC enforcement, and core API operations.

### Frontend Command Center Verification
- [ ] The "IT Team" tab is visible to authorized staff in the Management Portal.
- [ ] Switching between the 5 sub-tabs (Profiles, Chat, Memory, Logs, API Monitor) renders smoothly without console errors.
- [ ] Autocomplete triggers when typing `#it-` in the message compose box.
- [ ] Existing booking, login, and clinic features remain 100% functional.

## Follow-up — 2026-09-13T03:43:29Z

Tiếp tục công việc của toàn bộ đội ngũ IT Team cho DentalCare Clinic tại `D:\java\dental-clinic`:

1. Khảo sát và tối ưu hóa hệ thống IT Team Command Center vừa xây dựng.
2. Kiểm tra lại toàn diện 5 phân hệ công việc:
   - #it-backend: Kiểm tra các service, entity, repository, bảo mật JWT và xử lý dữ liệu.
   - #it-frontend: Kiểm tra giao diện Management Portal, chat room đa đặc vụ, bộ gõ hashtag autocomplete và nút Hỏi AI Copilot.
   - #it-qa: Chạy toàn bộ các kịch bản test API nội bộ (ITApiRunnerService), RBAC admin/patient, và hồi quy chức năng phòng khám.
   - #it-devops: Giám sát server runtime port 8080, logs, Maven build và kết nối mạng nội bộ/Cloudflare tunnel.
   - #it-security: Kiểm toán lại cơ chế lọc và xóa vết PII/JWT/mật khẩu trong SensitiveDataSanitizer, bảo vệ SSRF cho localhost runner.
3. Đảm bảo quy trình phối hợp giữa 5 đặc vụ mượt mà và lưu lại nhật ký hoạt động đầy đủ trong cơ sở dữ liệu.
4. Báo cáo tiến độ và kết quả chi tiết cho CEO/IT Director.

