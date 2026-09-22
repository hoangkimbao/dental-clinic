# Milestone 5 Handoff Report: AES-256 Medical Data Encryption & IDOR Protection

**Agent:** `explorer_m5_crypto_idor`  
**Working Directory:** `D:\java\dental-clinic\.agents\explorer_m5_crypto_idor`  
**Recipient Parent Agent:** `parent` (`a97c769a-d41a-4add-8acc-8fb2a3d22336`)  
**Handoff Type:** Hard (Exploration & Blueprint Completed)  
**Date:** 2026-09-23  

---

## 1. Observation

Direct observations from examining the codebase:

1. **Absence of AES-256 JPA AttributeConverter:**
   - Search across `src/main/java/com/dentalclinic/` revealed 0 occurrences of `Aes256GcmAttributeConverter` or any JPA crypto converter.
   - In `src/main/java/com/dentalclinic/model/MedicalRecord.java` (lines 26–34):
     ```java
     @Column(length = 1000)
     private String diagnosis;

     @Column(length = 2000)
     private String treatmentDone;

     @Column(length = 1000)
     private String prescription;
     ```
     These fields are stored as unencrypted plaintext in the `medical_records` table.
   - In `src/main/java/com/dentalclinic/model/OrthodonticPlan.java` (line 40):
     ```java
     @Column(length = 2000)
     private String doctorNotes;
     ```
     Orthodontic clinical doctor notes are also stored in plaintext.

2. **Critical IDOR in `MedicalRecordController.java`:**
   - In `src/main/java/com/dentalclinic/controller/MedicalRecordController.java` (lines 26–33):
     ```java
     @GetMapping
     @Operation(summary = "Tra cứu bệnh án EMR (Lọc theo SĐT hoặc Toàn bộ)")
     public ResponseEntity<ApiResponse<List<MedicalRecord>>> getMedicalRecords(@RequestParam(required = false) String phone) {
         if (phone != null && !phone.isBlank()) {
             return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(phone.trim())));
         }
         return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findAllByOrderByRecordDateDesc()));
     }
     ```
     No `@PreAuthorize` exists on this endpoint. Authenticated patients can query any patient's phone (`?phone=...`) or omit `phone` to retrieve all clinic records across all patients.

3. **Critical IDOR in `AppointmentController.java`:**
   - In `src/main/java/com/dentalclinic/controller/AppointmentController.java` (lines 71–75):
     ```java
     @GetMapping("/patient/{patientId}")
     @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")
     public ResponseEntity<ApiResponse<List<Appointment>>> getForPatient(@PathVariable Long patientId) {
         return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForPatient(patientId)));
     }
     ```
     `@PreAuthorize` allows any caller with `ROLE_PATIENT`, without verifying that `patientId` matches the caller's authenticated user ID. Line 78 (`getById`) similarly allows any user to retrieve any appointment by ID without ownership checks.

4. **Missing Access Control in `OrthodonticController.java`:**
   - In `src/main/java/com/dentalclinic/controller/OrthodonticController.java` (lines 28–36):
     ```java
     @GetMapping
     @Operation(summary = "Xem lộ trình niềng răng (Lọc theo SĐT bệnh nhân)")
     public ResponseEntity<ApiResponse<List<OrthodonticPlan>>> getOrthoPlans(@RequestParam(required = false) String phone) {
         if (phone != null && !phone.isBlank()) {
             List<OrthodonticPlan> list = orthodonticPlanRepository.findByPatientPhone(phone.trim())
                     .map(List::of).orElseGet(Collections::emptyList);
             return ResponseEntity.ok(ApiResponse.success(list));
         }
         return ResponseEntity.ok(ApiResponse.success(orthodonticPlanRepository.findAllByOrderByNextAdjustmentDateAsc()));
     }
     ```
     No role or ownership checks exist on `getOrthoPlans`.

5. **Permissive Route Mappings in `SecurityConfig.java`:**
   - In `src/main/java/com/dentalclinic/security/SecurityConfig.java` (lines 87–92):
     ```java
     .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
     .requestMatchers("/api/articles/**").permitAll()
     ...
     .requestMatchers("/api/emr/images/**").permitAll()
     ```
     Allows unauthenticated deletion/modification of articles and arbitrary file uploads to `/api/emr/images/upload`.

6. **Target Verification Test Suite in `MedicalSecurityE2ETest.java`:**
   - 31 test methods covering:
     - `Tier1IdorProtectionTests` (T1-IDOR-01 to T1-IDOR-05): Blocks dumping all records, querying other patients by phone or appointments by ID.
     - `Tier1SensitiveMedicalDataProtectionTests` (T1-ENC-01 to T1-ENC-03): Verifies encryption of clinical fields (`diagnosis`, `treatmentDone`, `prescription`) and seamless decryption.
     - `Tier1RbacAccessControlTests` (T1-RBAC-01 to T1-RBAC-05): Verifies strict RBAC on articles, uploads, IT team, and Tier-2 agents.
     - `Tier1FileUploadValidationTests` (T1-UPL-01 to T1-UPL-05): Magic byte, MIME, size boundary checks.
     - `Tier1SecurityHeadersTests` (T1-HDR-01 to T1-HDR-05): Security headers and suppression of internal stack traces.

---

## 2. Logic Chain

1. From Observation 1, because `MedicalRecord.java` stores `diagnosis`, `treatmentDone`, and `prescription` directly as plaintext, clinical data at rest is unencrypted. This directly fails test `testCreateMedicalRecordEncrypted` (T1-ENC-01) and violates Milestone 5 standard F49.
2. From Observation 1, because Base64 encoding of AES-GCM ciphertext (including a 12-byte IV and 16-byte authentication tag) expands the length beyond original database column limits (1000/2000 chars), changing columns to `@Column(columnDefinition = "TEXT")` is required to prevent truncation and SQL errors.
3. From Observation 2, because `MedicalRecordController.getMedicalRecords` lacks any check against `SecurityContextHolder` / `CustomUserDetails`, any user with `ROLE_PATIENT` calling `GET /api/medical-records` receives all clinic records (failing `T1-IDOR-01`), and providing `?phone=0901234567` returns the target patient's records (failing `T1-IDOR-02`).
4. From Observation 3, because `AppointmentController.getForPatient` does not check `#patientId == principal.user.id`, a patient can inspect other patients' appointments (failing `T1-IDOR-03`).
5. From Observation 4, `OrthodonticController` exhibits the identical horizontal IDOR vulnerability as `MedicalRecordController`, leaking orthodontic plans to unauthorized callers.
6. From Observation 5, because `SecurityConfig.java` permits all requests to `/api/articles/**` and `/api/emr/images/**`, unauthenticated users can modify CMS content (failing `T1-RBAC-01`, `T1-RBAC-02`) and upload unauthorized files (failing `T1-RBAC-03`).
7. Therefore, implementing `Aes256GcmAttributeConverter`, applying it to clinical entity fields, introducing ownership & role enforcement in `MedicalRecordController`, `AppointmentController`, and `OrthodonticController`, and tightening `SecurityConfig.java` will simultaneously resolve all identified vulnerabilities and satisfy the 31 test scenarios in `MedicalSecurityE2ETest.java`.

---

## 3. Caveats

- **No Caveats on Core Scope:** The scope of AES-256 encryption and IDOR defense was thoroughly explored down to exact line numbers and code implementations.
- **Assumptions:** 
  1. The default encryption key in `application.yml` is used when no environment variable `AES_SECRET_KEY` is provided. The converter applies SHA-256 key derivation, ensuring uniform 256-bit key length regardless of configuration string format.
  2. Legacy data fallback is incorporated into the converter (`convertToEntityAttribute` returns raw string if not valid Base64 or if GCM tag authentication fails), allowing seamless backward compatibility with existing databases and tests.

---

## 4. Conclusion

1. **AES-256 GCM Architecture:** Implement `com.dentalclinic.security.crypto.Aes256GcmAttributeConverter` using `AES/GCM/NoPadding`, 96-bit (12-byte) random IV per operation, 128-bit authentication tag, and Base64 output format. Apply `@Convert(converter = Aes256GcmAttributeConverter.class)` and `@Column(columnDefinition = "TEXT")` to `diagnosis`, `treatmentDone`, and `prescription` in `MedicalRecord.java`, and `doctorNotes` in `OrthodonticPlan.java`. Provide alias getters/setters for `treatmentPlan` and `prescriptionNotes`.
2. **IDOR Remediation in `MedicalRecordController`:** Check the authenticated principal. If `ROLE_PATIENT`, restrict queries strictly to their own registered phone number (`currentUser.getPhone()`). If the caller supplies another patient's phone, return `403 Forbidden`. If phone is omitted, auto-scope to the authenticated patient's records. Allow `ROLE_DENTIST`, `ROLE_OWNER`, and `ROLE_ADMIN` full access. Block other roles. Add `GET /api/medical-records/{id}` with ownership validation.
3. **IDOR Remediation in `AppointmentController`:** In `getForPatient`, verify `patientId.equals(currentUser.getId())` when caller is `ROLE_PATIENT`; otherwise return `403 Forbidden`. In `getById`, verify appointment ownership for patients.
4. **IDOR Remediation in `OrthodonticController`:** Enforce authentication and restrict `ROLE_PATIENT` to their own phone number.
5. **RBAC & Upload Hardening:** Restrict mutating `/api/articles/**` to `ROLE_ADMIN` and `ROLE_OWNER`. Restrict `/api/emr/images/**` to authenticated users with `ROLE_DENTIST`, `ROLE_OWNER`, or `ROLE_ADMIN` on `/upload`. Enforce 5MB limit, whitelist extensions (`.jpg`, `.jpeg`, `.png`, `.webp`), and inspect magic bytes.

All blueprints, code designs, and step-by-step instructions are documented in:
`D:\java\dental-clinic\.agents\explorer_m5_crypto_idor\report.md`.

---

## 5. Verification Method

To independently verify the implementation when applied by the implementing agent:

1. **Inspection of Created & Modified Source Files:**
   - `src/main/java/com/dentalclinic/security/crypto/Aes256GcmAttributeConverter.java` (Newly created)
   - `src/main/java/com/dentalclinic/model/MedicalRecord.java` (Converter applied, TEXT column)
   - `src/main/java/com/dentalclinic/model/OrthodonticPlan.java` (Converter applied)
   - `src/main/java/com/dentalclinic/controller/MedicalRecordController.java` (IDOR defenses)
   - `src/main/java/com/dentalclinic/controller/AppointmentController.java` (Ownership check)
   - `src/main/java/com/dentalclinic/controller/OrthodonticController.java` (Auth and phone check)
   - `src/main/java/com/dentalclinic/security/SecurityConfig.java` (RBAC tightened)
   - `src/main/java/com/dentalclinic/service/FileUploadService.java` (File upload checks)

2. **Automated Test Execution Command:**
   ```bash
   ./mvnw test -Dtest=MedicalSecurityE2ETest
   ```
   *(Windows PowerShell: `.\mvnw.cmd test -Dtest=MedicalSecurityE2ETest`)*

3. **Pass Criteria:**
   All 31 tests in `MedicalSecurityE2ETest.java` must pass with `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`.

4. **Invalidation Conditions:**
   - Any test failure in `MedicalSecurityE2ETest.java` (e.g., `T1-IDOR-01`, `T1-ENC-01`, `T1-UPL-04`).
   - Regression in previously passing test suites:
     ```bash
     .\mvnw.cmd test -Dtest=DentalCustomerE2ETest,StaffAndOperationsE2ETest,ITTeamE2ETestSuite
     ```
