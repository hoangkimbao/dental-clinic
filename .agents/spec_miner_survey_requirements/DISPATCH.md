# Dispatch for Survey Spec Miner

Assigned: Survey and mine comprehensive specification requirements, data contracts, edge cases, 9Router protocol, privacy guardrails.
Working Directory: D:\java\dental-clinic\.agents\spec_miner_survey_requirements
Parent: D:\java\dental-clinic\.agents\orchestrator_1

## 2026-09-12T14:57:04Z
You are the Specification & Interface Miner for the DentalCare Management Portal IT Team project.
Working directory: D:\java\dental-clinic\.agents\spec_miner_survey_requirements
Authoritative requirements file: D:\java\dental-clinic\ORIGINAL_REQUEST.md
Read D:\java\dental-clinic\ORIGINAL_REQUEST.md first!

Mine and specify all requirements, data contracts, and constraints:
1. Detailed schema for the 6 entities: ITAgentProfile, ITAgentMemory, ITAgentMessage, ITAgentActivity, ITBrowserTabRecord, ITApiRunLog (fields, data types, nullability, relations, indexes).
2. Exact seed data for the 5 agents (#it-backend, #it-frontend, #it-qa, #it-devops, #it-security): agent code, display name, roles, status, expertise, idempotent startup seeding logic.
3. Hashtag parsing specifications: regex/grammar for #it-*, mention linking, MENTIONED activity creation, threading semantics (parentMessageId, threadId, replies).
4. REST API contract definitions: exact paths, methods, request bodies, response DTOs, query parameters (pagination, filters), error responses, RBAC (ROLE_ADMIN enforcement).
5. Sensitive data redaction/sanitizer rules: regex and fields to redact (passwords, JWTs, Authorization headers, Bearer tokens, cookies, SSN/medical/PII).
6. 9Router AI integration specification: endpoint `http://localhost:20128`, request/response format, fallback/error handling when 9Router is offline or unreachable.
7. Write your detailed specification report to D:\java\dental-clinic\.agents\spec_miner_survey_requirements\report.md and handoff.md.
Send your completion report to parent when done.

