# Project Plan — Orchestrator Generation 3
DentalCare Clinic IT Team Command Center Optimization & 5-Subsystem Verification

## Objective
Khảo sát, tối ưu hóa hệ thống IT Team Command Center, kiểm tra toàn diện 5 phân hệ công việc (#it-backend, #it-frontend, #it-qa, #it-devops, #it-security), đảm bảo quy trình phối hợp mượt mà và lưu log DB đầy đủ, lập báo cáo chi tiết cho CEO/IT Director.

---

## Phases & Milestones

### Phase 1: In-depth Survey & Technical Audit (3 Parallel Explorers)
- **Explorer 1 (Backend & Security Focus)**:
  - #it-backend: Services, JPA Entities, Repositories, Data flow, JWT validation, exception handling.
  - #it-security: SensitiveDataSanitizer rules (PII, JWT, passwords, cookies, medical EMR), SSRF protection logic in ITApiRunnerService (IP filtering, URL schemes, localhost redirection evasion).
- **Explorer 2 (Frontend & UI/UX Focus)**:
  - #it-frontend: Management Portal `#section-itteam`, 5 sub-views rendering, multi-agent chat room, hashtag autocomplete dropdown (`#it-`), AI Copilot query trigger / 9Router integration button, dynamic role tab switching (`ROLE_ADMIN` / `ROLE_OWNER`).
- **Explorer 3 (DevOps, QA & Runtime Verification)**:
  - #it-devops: Server runtime port 8080 configuration, loggers, Maven build files, network / tunnel readiness.
  - #it-qa: Status of all existing test suites (`ITTeamE2ETestSuite`, unit & stress tests), API runner execution capabilities, RBAC patient vs admin checks, clinic regression baseline.

### Phase 2: Targeted Optimization & Subsystem Refinements (Worker)
- Synthesize Explorer findings.
- If gaps, performance bottlenecks, or UX rough edges (such as Copilot button UX, hashtag matching edge cases, sanitization gaps, activity logging edge cases) are identified:
  - Dispatch Worker to implement necessary code & configuration optimizations.
  - Require Worker to run `./mvnw test` / specific suites and report results.

### Phase 3: Comprehensive Multi-Agent Verification (Reviewers, Challengers, Auditor)
- Dispatch 2 independent Reviewers to inspect code correctness, UI fidelity, and non-regression.
- Dispatch 2 Challengers to empirically verify API safety, SSRF defenses, and chat/hashtag workflows.
- Dispatch 1 Forensic Auditor (`teamwork_preview_auditor`) for static & dynamic integrity analysis (anti-cheating, no hardcoding, genuine logic).

### Phase 4: Executive Synthesis & Reporting
- Synthesize all results across the 5 subsystems.
- Verify that inter-agent coordination flows log activities to database (`it_agent_activity`).
- Compile comprehensive, professional report for CEO / IT Director.
- Notify Sentinel / Parent.
