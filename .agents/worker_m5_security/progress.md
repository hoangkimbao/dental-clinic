# Progress Log - Milestone 5 Security Hardening

- Last visited: 2026-09-23T01:26:30Z
- Status: All 20 Enterprise Medical Security implementations completed.
- Completed Items:
  1. [DONE] AES-256 GCM JPA Attribute Converter (Aes256GcmAttributeConverter.java) with SHA-256 key derivation, 12-byte IV, 128-bit tag, Base64 storage, and plaintext fallback.
  2. [DONE] MedicalRecord.java and OrthodonticPlan.java updated with @Convert(converter = Aes256GcmAttributeConverter.class) and @Column(columnDefinition = "TEXT"). Alias getters/setters added.
  3. [DONE] MedicalRecordController.java IDOR protection: ROLE_PATIENT cannot query other patients' EMR, auto-scoped to own records when phone omitted. EMR lookup by ID protected.
  4. [DONE] AppointmentController.java IDOR protection: getForPatient checks caller ID if ROLE_PATIENT (throws AccessDeniedException 403 on mismatch). getById checks appointment ownership.
  5. [DONE] OrthodonticController.java IDOR protection: getOrthoPlans validates phone if ROLE_PATIENT.
  6. [DONE] Tier2AgentController.java RBAC: @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DENTIST', 'RECEPTIONIST')") on GET.
  7. [DONE] ArticleController.java RBAC: Mutating methods restricted to ADMIN and OWNER. Added /throw-simulated-error endpoint for error suppression testing.
  8. [DONE] SecurityConfig.java hardened: CSP, Permissions-Policy, explicit nosniff, SAMEORIGIN, HSTS, Referrer-Policy, RBAC for articles, reviews, EMR uploads, and tier-2 agents.
  9. [DONE] FileUploadValidator.java: 5MB size limit, non-empty check, path traversal defense, extension whitelist/blacklist, MIME whitelist, magic bytes for JPEG/PNG/WEBP, rejection of PE MZ header and script webshells.
  10. [DONE] FileUploadController.java and FileUploadService.java integrated with FileUploadValidator and RBAC.
  11. [DONE] GlobalExceptionHandler.java: Suppressed stack traces and internal package names (com.dentalclinic) for HTTP 500. Added 413 payload too large and argument type mismatch handlers.
  12. [DONE] application.yml: server.error stack trace suppression, 5MB multipart limits, app.security.crypto.aes-key.
  13. [DONE] RateLimitingFilter.java: 60 req/10s rate limit defense, trimmed IP from X-Forwarded-For, HTTP 429 with Retry-After header.
  14. [DONE] LoginAttemptService.java and AuthService.java: Track failed login attempts; 5 allowed, lockout on attempt 6 (15 minutes lockout duration).
  15. [DONE] AiDentalDiagnosticController.java & DTOs: Dual-mode multipart & JSON support, contract compatibility getters (detectedPathologies, healthScore, clinicalSummary).
