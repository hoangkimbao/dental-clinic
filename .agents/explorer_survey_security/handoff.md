# Handoff Report: 20 Enterprise Medical Security Standards & E2E Testing Suite Audit

**Agent:** `explorer_survey_security`  
**Working Directory:** `D:\java\dental-clinic\.agents\explorer_survey_security`  
**Recipient:** Parent Orchestrator (`4110e379-52ae-4437-9f08-bb3a919ba41c`)  
**Date:** 2026-09-22T17:25:00Z  
**Type:** Hard Handoff  

---

## 1. Observation

Direct observations with exact file paths, line numbers, and verbatim code quotes from `D:\java\dental-clinic`:

1. **.env & Secret Isolation / Git Secrets:**
   - `src/main/resources/application.yml` (lines 14-15, 49):
     ```yaml
     14:     username: sa
     15:     password: password
     49:     secret: dentalcareluxurysecuresecretkey2026superstrongforproductiontokengeneration32bytes
     ```
   - `docker-compose.yml` (lines 13-14, 25-27):
     ```yaml
     13:       - SPRING_DATASOURCE_PASSWORD=dentalpass
     14:       - APP_JWT_SECRET=dentalcareluxurysecuresecretkey2026superstrongforproductiontokengeneration32bytes
     25:       POSTGRES_PASSWORD: dentalpass
     27:       - "5432:5432"
     ```
   - `.gitignore` (lines 1-44): Contains no entries for `.env`, `.env.*`, `*.key`, `*.pem`, `*.secret`, or `credentials.json`.
   - `src/main/java/com/dentalclinic/config/DataInitializer.java` (line 55):
     ```java
     String hash123 = passwordEncoder.encode("123");
     ```
     Seeds 8 production accounts (owner, admin, receptionist, dentists, staff, patient) with default password `"123"`.

2. **Database Security & Public H2 Console:**
   - `src/main/resources/application.yml` (lines 24-27):
     ```yaml
     h2:
       console:
         enabled: true
         path: /h2-console
     ```
   - `src/main/java/com/dentalclinic/security/SecurityConfig.java` (line 73):
     ```java
     .requestMatchers("/h2-console/**").permitAll()
     ```
   - Database connection in `application.yml` line 12: `jdbc:h2:file:./data/dentaldb;AUTO_SERVER=TRUE` has no database-level file encryption (CIPHER).

3. **Row-Level Security (RLS) & IDOR Medical Leakage:**
   - `src/main/java/com/dentalclinic/controller/MedicalRecordController.java` (lines 28-33):
     ```java
     @GetMapping
     public ResponseEntity<ApiResponse<List<MedicalRecord>>> getMedicalRecords(@RequestParam(required = false) String phone) {
         if (phone != null && !phone.isBlank()) {
             return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(phone.trim())));
         }
         return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findAllByOrderByRecordDateDesc()));
     }
     ```
   - `src/main/java/com/dentalclinic/controller/AppointmentController.java` (lines 71-75):
     ```java
     @GetMapping("/patient/{patientId}")
     @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")
     public ResponseEntity<ApiResponse<List<Appointment>>> getForPatient(@PathVariable Long patientId) {
         return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForPatient(patientId)));
     }
     ```
   - `src/main/java/com/dentalclinic/model/User.java` (lines 1-64) & `Role.java` (lines 1-12): No `tenant_id`, `distributor_id`, or `clinic_id`. Roles are strictly single-clinic (`ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`, `ROLE_PATIENT`).

4. **Medical Data Encryption (AES-256):**
   - `src/main/java/com/dentalclinic/model/MedicalRecord.java` (lines 26-37): Fields `diagnosis`, `treatmentDone`, `prescription`, `notes` are mapped to standard `VARCHAR` columns without `@Convert` or AES-256 encryption.
   - `src/main/java/com/dentalclinic/model/OrthodonticPlan.java` (lines 39-41): Field `doctorNotes` is stored in plaintext.

5. **JWT / OAuth2 Rotation & Expiration:**
   - `src/main/java/com/dentalclinic/security/JwtTokenProvider.java` (lines 22-25, 38-46): Single JWT generated with 24-hour expiration (`86400000` ms).
   - No refresh token table, endpoint, or rotation logic in `AuthService.java` or `AuthController.java`.
   - When user invokes `changePassword()` (`AuthService.java` lines 96-106), active JWTs are not revoked or blacklisted.
   - No OAuth2 dependencies in `pom.xml` (lines 24-104).

6. **RBAC & Public Exposure of Modification Endpoints:**
   - `src/main/java/com/dentalclinic/security/SecurityConfig.java` (lines 87-93):
     ```java
     .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
     .requestMatchers("/api/articles/**").permitAll()
     .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
     .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
     .requestMatchers("/api/reviews/**").permitAll()
     .requestMatchers("/api/emr/images/**").permitAll()
     ```
   - Unauthenticated requests can invoke `POST /api/articles`, `PUT /api/articles/{id}`, `DELETE /api/articles/{id}`, `POST /api/reviews`, and `POST /api/emr/images/upload`.

7. **DTO Binding Protection:**
   - Direct JPA entities accepted in `@RequestBody`:
     - `MedicalRecordController.java` (line 38): `@RequestBody MedicalRecord record`
     - `OrthodonticController.java` (line 41): `@RequestBody OrthodonticPlan plan`
     - `ArticleController.java` (lines 41, 46): `@RequestBody Article article`
     - `ShiftController.java` (line 45): `@RequestBody StaffShift shift`
     - `DoctorReviewController.java` (line 36): `@RequestBody DoctorReview review`

8. **Secure Cookies & Frontend Token Storage:**
   - `src/main/resources/static/js/app.js` (lines 84-90):
     ```javascript
     let saved = sessionStorage.getItem('DENTAL_USER');
     if (!saved) {
         saved = localStorage.getItem('DENTAL_USER');
     }
     ```
   - Backend does not set HttpOnly cookies; client stores raw JWT token in browser `localStorage`/`sessionStorage`.

9. **BCrypt Password Hashing & Password Policy:**
   - `src/main/java/com/dentalclinic/security/SecurityConfig.java` (line 41): Uses `BCryptPasswordEncoder()`.
   - `src/main/java/com/dentalclinic/dto/RegisterRequest.java` (lines 24-25): `@Size(min = 3, max = 100)` permits 3-character weak passwords.

10. **Rate Limiting & Brute-Force:**
    - `src/main/java/com/dentalclinic/config/RateLimitingFilter.java` (lines 40, 73-77): In-memory map (60 req / 10s). Extracts IP directly from `X-Forwarded-For.split(",")[0]` without verifying trusted reverse proxy. No account-level lockout on failed login.

11. **Bot / Spam Throttling:**
    - Zero Captcha validation or honeypot fields on `/api/auth/register`, `/api/auth/login`, `/api/appointments/book`, `/api/reviews`.

12. **Parameterized Queries:**
    - All queries in `com/dentalclinic/repository/*` and `com/dentalclinic/itteam/repository/*` use Spring Data JPA derived queries and JPQL (`sumTotalSuccessfulPayments`). Zero native SQL string concatenation.

13. **File Upload Validation:**
    - `src/main/java/com/dentalclinic/service/FileUploadService.java` (lines 35-44): Copies any file with client-supplied extension (`.lastIndexOf(".")`). No MIME allowlist check, no magic bytes check. Saves to `./uploads/dental-images/` and served publicly via `/uploads/**` (`SecurityConfig.java` line 72). Max file size is 50MB.

14. **PII Masking & Error Suppression:**
    - `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`: Implements regex masking for passwords, JWTs, cookies, CCCD, CMND, and phone numbers in IT Team logs/messages.
    - Clinical controllers (`MedicalRecordController`, `AppointmentController`) do not mask patient phone numbers or names.
    - `src/main/java/com/dentalclinic/exception/GlobalExceptionHandler.java` (lines 49-53):
      ```java
      @ExceptionHandler(Exception.class)
      public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Đã xảy ra lỗi máy chủ: " + ex.getMessage()));
      }
      ```
      Leaks `ex.getMessage()` directly in HTTP 500 error responses.

15. **Security Headers & HTTPS / WSS:**
    - `SecurityConfig.java` (lines 62-69): Configures HSTS and Referrer-Policy. CSP is completely absent. `X-Frame-Options` is relaxed to `sameOrigin()`.
    - No HTTPS channel enforcement.
    - `src/main/java/com/dentalclinic/websocket/WebSocketConfig.java` (lines 20-24):
      ```java
      registry.addEndpoint("/ws-dental")
              .setAllowedOriginPatterns("*")
              .withSockJS();
      ```
      No JWT validation on STOMP handshake; wildcard origin pattern allowed.

16. **Dependency CVE Audit & Test Suites:**
    - `pom.xml`: Spring Boot 3.2.5; no `dependency-check-maven` plugin.
    - `src/test/java/com/dentalclinic/e2e/ITTeamE2ETestSuite.java` (1,467 lines, 73 tests): Exhaustively tests the IT Team Command Center across 5 tiers (F01–F30), but contains 0 tests for clinical EMR authorization/IDOR, medical data encryption, file upload validation, or CSP headers.

---

## 2. Logic Chain

1. **Secret & Credential Isolation (TC 1, 2, 3):**
   - Observations 1 & 2 show hardcoded secrets in `application.yml`, `docker-compose.yml`, and `DataInitializer.java`, alongside public H2 console access in `SecurityConfig.java:73`.
   - *Inference:* Credentials and secrets are co-located in the versioned repository and reachable at runtime without externalization, violating standards 1, 2, and 3.

2. **Access Control & Tenant Isolation (TC 4, 8, 9):**
   - Observation 3 shows `MedicalRecordController:28-33` returns all clinical records if no phone parameter is given, and Observation 6 shows `SecurityConfig.java:87-93` allows unauthenticated writes to `/api/articles/**` and `/api/emr/images/**`.
   - Observation 7 shows controllers accept JPA entities directly in `@RequestBody`.
   - *Inference:* The system suffers from Broken Access Control (OWASP A01), IDOR on medical records, unauthenticated content modification, and Mass Assignment vulnerabilities, failing standards 4, 8, and 9.

3. **Data Protection at Rest & In Transit (TC 5, 10, 18, 19):**
   - Observation 4 confirms medical text fields are stored as unencrypted `VARCHAR` in the database.
   - Observation 8 shows JWTs are stored in `localStorage` instead of HttpOnly cookies.
   - Observation 15 shows no CSP header, relaxed frame options, and unauthenticated wildcard WebSocket access.
   - *Inference:* Medical data is unprotected at rest against database dump exfiltration, tokens are vulnerable to XSS exfiltration, and the transport layer lacks CSP and WebSocket origin enforcement, failing standards 5, 10, 18, and 19.

4. **Input & Upload Defense (TC 7, 13, 14, 15, 16):**
   - Observation 12 confirms all database operations use Spring Data JPA parameterized queries (Standard 14 PASS).
   - Observation 13 shows `FileUploadService` allows arbitrary file extensions, performs no MIME/magic bytes check, and serves files statically.
   - Observation 11 shows no bot throttling or Captcha.
   - *Inference:* Parameterized query defense is solid (TC 14 PASS), but file upload is vulnerable to malicious file execution (TC 16 FAIL), and public forms are vulnerable to automated bot spam (TC 13 FAIL).

5. **Sanitization, Masking, and Exception Handling (TC 11, 17):**
   - Observation 9 confirms passwords use BCrypt hashing (TC 11 PASS).
   - Observation 14 shows `SensitiveDataSanitizer` cleans IT Team data, but clinical endpoints return unmasked patient PII and `GlobalExceptionHandler` leaks raw exception messages (`ex.getMessage()`) in 500 responses (TC 17 PARTIAL).

6. **Test Suite Coverage & Gaps:**
   - Observation 16 shows `ITTeamE2ETestSuite.java` provides thorough 5-tier opaque-box validation for the IT Team subsystem (73 tests), but provides zero test coverage for the 20 medical security standards in the core clinical modules.

---

## 3. Caveats

- **Network-level Cloudflare Tunnel:** While the Spring Boot application configuration does not enforce HTTPS internally, the production environment relies on Cloudflare Tunnel (`cloudflared`) to terminate HTTPS at the edge. However, application-level defense-in-depth requires secure transport headers and internal TLS awareness.
- **Development vs. Production Profiles:** H2 console is enabled in the default `application.yml`. In a multi-profile environment (`application-prod.yml`), PostgreSQL is configured, but `SecurityConfig.java` currently has no profile-conditional bean logic to disable `/h2-console/**` on production.
- **Static Analysis Scope:** Audit was conducted via comprehensive source code inspection across all Java classes, configuration files, and test files without running active exploit probes against live production endpoints.

---

## 4. Conclusion

1. **Overall Compliance Score:**
   - **Compliant (Pass):** 2 standards (TC 11 BCrypt, TC 14 Parameterized Queries).
   - **Partially Compliant (Remediation Needed):** 6 standards (TC 6, 7, 8, 12, 15, 17, 18).
   - **Non-Compliant (High Risk / Fail):** 12 standards (TC 1, 2, 3, 4, 5, 9, 10, 13, 16, 19, 20).
2. **Top Critical Security Fixes Required:**
   - Restrict `GET /api/medical-records` to the authenticated patient's own ID (`ROLE_PATIENT`) or authorized medical staff (`ROLE_DENTIST`, `ROLE_OWNER`).
   - Fix `SecurityConfig.java` to require `ROLE_OWNER`/`ROLE_ADMIN` for modifying `/api/articles/**` and uploading `/api/emr/images/**`.
   - Disable `/h2-console/**` in production.
   - Implement AES-256 GCM JPA `AttributeConverter` for clinical fields in `MedicalRecord` and `OrthodonticPlan`.
   - Enforce MIME-type allowlist and magic bytes inspection in `FileUploadService.java`.
   - Add Content-Security-Policy (CSP) header.
3. **E2E Testing Suite Assessment:**
   - `ITTeamE2ETestSuite.java` is well-architected for the IT Team module (73 tests across 5 tiers).
   - A dedicated `MedicalSecurityE2ETestSuite.java` must be created to add automated tests for the 20 medical security standards across the clinical domain.

---

## 5. Verification Method

To independently verify the observations and findings:

1. **Inspect Security Configuration:**
   - View `src/main/java/com/dentalclinic/security/SecurityConfig.java` lines 70–101: Verify permitAll on `/h2-console/**`, `/api/articles/**`, `/api/emr/images/**`.
2. **Inspect Medical Record IDOR:**
   - View `src/main/java/com/dentalclinic/controller/MedicalRecordController.java` lines 26–33: Verify unparameterized retrieval returns `findAllByOrderByRecordDateDesc()`.
3. **Inspect File Upload Implementation:**
   - View `src/main/java/com/dentalclinic/service/FileUploadService.java` lines 30–60: Verify absence of MIME allowlist or magic byte verification.
4. **Inspect Application Configuration & Hardcoded Secrets:**
   - View `src/main/resources/application.yml` lines 14–15, 25–27, 49.
   - View `docker-compose.yml` lines 11–14, 23–27.
5. **Inspect Test Suite Coverage:**
   - View `src/test/java/com/dentalclinic/e2e/ITTeamE2ETestSuite.java`: Verify all 73 tests target `/api/it-team/**`, `/api/coupons/**`, `/api/auth/login`. Confirm zero tests for `/api/medical-records` authorization or file upload validation.
6. **Execute Existing Test Suite:**
   - Command: `.\mvnw.cmd test -Dtest=ITTeamE2ETestSuite` (Windows) or `./mvnw test -Dtest=ITTeamE2ETestSuite` (Linux).
   - Invalidation condition: If any endpoint `/api/articles/**` or `/api/emr/images/**` is modified in `SecurityConfig.java`, re-run the full regression suite to ensure zero impact on existing features.
