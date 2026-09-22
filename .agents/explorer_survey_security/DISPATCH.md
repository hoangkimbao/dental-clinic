## 2026-09-22T17:17:47Z
Read D:\java\dental-clinic\ORIGINAL_REQUEST.md and D:\java\dental-clinic\PROJECT.md.
Your working directory is D:\java\dental-clinic\.agents\explorer_survey_security.
Investigate the authoritative codebase in D:\java\dental-clinic for the 20 Enterprise Medical Security Standards and E2E Testing Suite:
1. Audit compliance with the 20 Enterprise Security Standards:
   1. .env / secret isolation
   2. No git secrets in repo/history
   3. Database security (access limits, credentials)
   4. Row-Level Security (RLS) / Tenant-based isolation (distributor, Tier-2 agent, patient)
   5. AES-256 encryption for sensitive medical data
   6. JWT / OAuth2 rotation & expiration
   7. Input sanitization & validation (XSS & injection defense)
   8. RBAC enforcement across endpoints
   9. DTO binding protection (no entity exposure)
   10. Secure cookies (HttpOnly, Secure, SameSite=Strict)
   11. BCrypt password hashing
   12. Rate limiting & brute-force defense
   13. Bot / spam throttling
   14. 100% Parameterized queries
   15. XSS escaping
   16. File upload validation (MIME-types, magic bytes)
   17. PII masking & error response suppression
   18. Security headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options)
   19. HTTPS / WSS enforcement
   20. Dependency CVE audit
2. Review existing automated test suites (ITTeamE2ETestSuite.java, etc.) and TEST_INFRA.md / TEST_READY.md.
3. Write a comprehensive report to D:\java\dental-clinic\.agents\explorer_survey_security\report.md.
4. When done, write handoff.md in your working directory and notify the parent orchestrator with send_message.
