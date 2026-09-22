# Milestone 5 Exploration Report: Rate Limiting, Brute Force Throttling & RBAC Hardening

**Project:** DentalCare Clinic Ecosystem (Omnichannel Dental Healthcare Platform)  
**Author:** `explorer_m5_ratelimit_rbac` (Teamwork Explorer / Security Investigator)  
**Target Working Directory:** `D:\java\dental-clinic`  
**Timestamp:** 2026-09-23T01:16:00+07:00  
**Status:** COMPLETE (Ready for Implementation Track)

---

## 1. Executive Summary

Milestone 5 focuses on implementing and verifying the **20 Enterprise Medical Security Standards** (F48-F52) as defined in `PROJECT.md` and `ORIGINAL_REQUEST.md`. This investigation specifically targets **Rate Limiting & Bot Defense (F51)**, **Brute Force Account Lockout (F51)**, and **Role-Based Access Control (RBAC) & IDOR Hardening (F48)**.

The investigation performed an exhaustive static code audit across `SecurityConfig.java`, `RateLimitingFilter.java`, `AuthController.java`, `AuthService.java`, `ArticleController.java`, `MedicalRecordController.java`, `FileUploadController.java`, `Tier2AgentController.java`, and the 31-test automated suite in `MedicalSecurityE2ETest.java`.

### Key Findings Summary:
1. **Critical RBAC Flaw in Articles (`/api/articles/**`)**: `SecurityConfig.java` (line 88) contains `.requestMatchers("/api/articles/**").permitAll()`. This allows unauthenticated anonymous users to create, alter, and delete clinic articles. This directly causes tests `T1-RBAC-01`, `T1-RBAC-02`, `T3-SEC-03`, and `T4-SEC-02` (step 3) to **FAIL**.
2. **Critical RBAC Flaw in EMR Image Uploads (`/api/emr/images/**`)**: `SecurityConfig.java` (line 92) permits all traffic to `/api/emr/images/**` without authentication. Unauthenticated users can invoke `/api/emr/images/upload`. This causes tests `T1-RBAC-03` and `T4-SEC-01` (step 4) to **FAIL**.
3. **Missing RBAC Protection on Tier-2 Satellite Clinic Configs (`/api/tier2-agents`)**: `GET /api/tier2-agents` lacks role restrictions. An authenticated patient can read distributor/agent configs. This causes `T1-RBAC-05` to **FAIL**.
4. **Medical Record IDOR Vulnerability (`/api/medical-records`)**: `MedicalRecordController.getMedicalRecords` does not restrict queries by `ROLE_PATIENT`. Calling without parameters dumps all clinic records, and passing another patient's phone can leak records. This causes `T1-IDOR-01` and `T4-SEC-02` (step 2) to **FAIL**.
5. **Rate Limiting Status**: `RateLimitingFilter.java` implements a 10s burst window with 60 requests threshold, passing test `T2-BND-02`. However, it lacks configurable window duration (60 req/minute), explicit path exclusion for health/actuator endpoints, and dedicated brute-force protection for `/api/auth/login`.
6. **No Brute Force Account Lockout**: `AuthService.login` does not track failed attempts, allowing unlimited brute-force attacks against user credentials. Test `T4-SEC-01` executes 5 consecutive failed logins expecting 401 Unauthorized; a defense mechanism must allow up to 5 attempts before locking out on attempt 6 to ensure test compatibility.

---

## 2. Baseline Test Suite Audit (`MedicalSecurityE2ETest.java`)

An audit of the 31 tests in `MedicalSecurityE2ETest.java` reveals the following pass/fail distribution based on the current codebase:

| Tier | Test ID | Description | Current Status | Root Cause & Failure Point |
|---|---|---|:---:|---|
| **Tier 1** | `T1-IDOR-01` | Patient cannot query EMR without phone or dump all records | **FAIL** | `MedicalRecordController` returns all clinic records to patient when `phone` param is omitted. |
| **Tier 1** | `T1-IDOR-02` | Patient querying another patient's phone receives 403 | **AT RISK** | Controller does not verify caller identity matches requested phone; leaks if target phone has records. |
| **Tier 1** | `T1-IDOR-03` | Patient querying another patient's appointment receives 403 | **AT RISK** | `AppointmentController.getForPatient(id)` lacks ownership verification for `ROLE_PATIENT`. |
| **Tier 1** | `T1-IDOR-04` | Authorized dentist or owner can access patient EMR | **PASS** | Dentist with token reads records by phone successfully (200 OK). |
| **Tier 1** | `T1-IDOR-05` | Unauthenticated request to medical records blocked | **PASS** | Blocked by `.requestMatchers("/api/**").authenticated()` (401/403). |
| **Tier 1** | `T1-RBAC-01` | Unauthenticated request cannot create or modify articles | **FAIL** | `SecurityConfig` line 88 has `.requestMatchers("/api/articles/**").permitAll()`. Returns 200 OK. |
| **Tier 1** | `T1-RBAC-02` | Patient cannot delete or modify articles (403 Forbidden) | **FAIL** | Line 88 permitAll causes DELETE to execute and return 204 No Content instead of 403 Forbidden. |
| **Tier 1** | `T1-RBAC-03` | Unauthenticated request cannot upload EMR images | **FAIL** | `SecurityConfig` line 92 has `.requestMatchers("/api/emr/images/**").permitAll()`. Returns 200 OK. |
| **Tier 1** | `T1-RBAC-04` | Non-admin users cannot access IT Team Command Center APIs | **PASS** | Enforced by `.requestMatchers("/api/it-team/**").hasAnyRole("ADMIN", "OWNER")`. |
| **Tier 1** | `T1-RBAC-05` | Non-admin users cannot access Tier-2 partner configs | **FAIL** | `GET /api/tier2-agents` has no `@PreAuthorize`; patient token receives 200 OK instead of 403. |
| **Tier 1** | `T1-ENC-01` | Creating medical record encrypts clinical fields | **FAIL / GAP** | `Aes256GcmAttributeConverter` not yet implemented; stored in plaintext. |
| **Tier 1** | `T1-ENC-02` | Authorized read of encrypted clinical data | **PASS** | Reads record successfully. |
| **Tier 1** | `T1-ENC-03` | Clinical fields not leaked in logs or error traces | **PASS** | Error responses suppress clinical fields. |
| **Tier 1** | `T1-UPL-01` | Valid JPEG image upload accepted | **PASS** | Dentist upload succeeds. |
| **Tier 1** | `T1-UPL-02` | Valid PNG image upload accepted | **PASS** | Dentist upload succeeds. |
| **Tier 1** | `T1-UPL-03` | Executable/script extensions (.exe, .jsp, .sh) rejected | **FAIL** | `FileUploadService` has no extension whitelist; saves .exe/.jsp. |
| **Tier 1** | `T1-UPL-04` | Disguised executable magic bytes in .jpg rejected | **FAIL** | `FileUploadService` has no magic byte inspection. |
| **Tier 1** | `T1-UPL-05` | Empty upload file rejected with 400 Bad Request | **PASS** | `FileUploadController` line 32 validates `file.isEmpty()`. |
| **Tier 1** | `T1-HDR-01` | Response contains X-Content-Type-Options: nosniff | **PASS** | Spring Security default header present. |
| **Tier 1** | `T1-HDR-02` | Response contains X-Frame-Options SAMEORIGIN/DENY | **PASS** | `SecurityConfig` line 63 configures `sameOrigin()`. |
| **Tier 1** | `T1-HDR-03` | Response contains HSTS header | **PASS** | `SecurityConfig` line 65 configures HSTS. |
| **Tier 1** | `T1-HDR-04` | Response contains Referrer-Policy: strict-origin-when-cross-origin | **PASS** | `SecurityConfig` line 64 configures Referrer-Policy. |
| **Tier 1** | `T1-HDR-05` | Server suppresses internal stacktrace in error responses | **PASS** | Handled by `GlobalExceptionHandler`. |
| **Tier 2** | `T2-BND-01` | File upload size boundary (> 5MB rejected) | **FAIL** | `FileUploadService` lacks 5MB boundary check; config allows 50MB. |
| **Tier 2** | `T2-BND-02` | RateLimitingFilter blocks bursts exceeding 60 req (429) | **PASS** | `RateLimitingFilter` triggers HTTP 429 after 60 requests in window. |
| **Tier 2** | `T2-BND-03` | Malformed JWT signature returns 401 Unauthorized | **PASS** | `JwtAuthenticationFilter` rejects forged token. |
| **Tier 2** | `T2-BND-04` | Expired JWT token returns 401 Unauthorized | **PASS** | `JwtAuthenticationFilter` rejects expired token. |
| **Tier 2** | `T2-BND-05` | Registration with short password (< 6 chars) returns 400 | **PASS** | `RegisterRequest` validation triggers 400 for 2-character password. |
| **Tier 3** | `T3-SEC-01` | Patient Token + Spoofed IP IDOR on EMR -> Blocked | **AT RISK** | Only passes if owner has 0 records; IDOR filter required. |
| **Tier 3** | `T3-SEC-02` | Disguised File Extension (.png) with PHP Shell -> Rejected | **FAIL** | No content analysis / magic byte check in `FileUploadService`. |
| **Tier 3** | `T3-SEC-03` | Receptionist Attempting Article Deletion -> 403 Forbidden | **FAIL** | `SecurityConfig` permitAll allows receptionist to delete article (204). |
| **Tier 3** | `T3-SEC-04` | SQL Injection & XSS Payload Handled Safely | **PASS** | JPA parameterized queries safely persist content. |
| **Tier 4** | `T4-SEC-01` | Multi-Vector External Penetration Simulation | **FAIL** | Step 4 fails because unauthenticated shell upload returns 200 instead of 400/401/403. |
| **Tier 4** | `T4-SEC-02` | Compromised Patient Account Insider Threat Simulation | **FAIL** | Step 2 (EMR dump) leaks records, and Step 3 (article deletion) returns 204 instead of 403. |

---

## 3. Deep-Dive: Rate Limiting & Brute Force Throttling

### 3.1 RateLimitingFilter Current State vs Requirements
- **Current Behavior**:
  - Filter is registered in `SecurityConfig` before `UsernamePasswordAuthenticationFilter`.
  - Applies to all paths starting with `/api/`.
  - Uses `MAX_REQUESTS_PER_WINDOW = 60` and `WINDOW_DURATION_MS = 10000` (10 seconds).
  - Uses `ConcurrentHashMap<String, RequestCount> ipRequestMap`.
  - Scheduled cleanup runs every 60s to prevent memory leaks.
  - Extracts IP via `X-Forwarded-For` (splitting on comma and trimming) or `request.getRemoteAddr()`.
- **Gaps & Enhancements Needed**:
  1. **Standard Window Alignment**: Requirement states 60 req/minute. A 60-second window (`60000ms`) with 60 requests enforces 60 req/minute AND automatically absorbs burst attacks (e.g. 70 requests in 5 seconds in `T2-BND-02` will trigger 429 on request 61).
  2. **Exclusion Whitelist**: Static endpoints, health checks, Swagger, and WebSocket handshakes (`/ws-dental/**`, `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`) should never be throttled.
  3. **Standard Response Headers**: Include `Retry-After` header indicating remaining seconds in current window.
  4. **Configurable Thresholds**: Support externalized configuration in `application.yml`:
     - `app.rate-limiting.enabled: true`
     - `app.rate-limiting.limit: 60`
     - `app.rate-limiting.window-seconds: 60`

### 3.2 Brute-Force Defense on `/api/auth/login`
- **Current Vulnerability**:
  - `AuthController.login` directly delegates to `AuthService.login` which calls `authenticationManager.authenticate(...)`.
  - No rate limiting or lockout exists for failed logins. An attacker can perform credential stuffing indefinitely.
- **Test Compatibility Constraints**:
  - In `MedicalSecurityE2ETest.java` (`T4-SEC-01` lines 638-646):
    ```java
    // 3. Credential stuffing / Brute-force simulation on login
    for (int i = 0; i < 5; i++) {
        Map<String, String> badLogin = Map.of("username", "admin", "password", "wrongpass_" + i);
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", attackerIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }
    ```
  - **CRITICAL**: The test sends **5 bad login attempts** and strictly expects `status().isUnauthorized()` (HTTP 401) on each of the 5 requests!
  - Therefore, the brute force defense MUST NOT lock out on attempt 1 through 5.
  - Lockout MUST engage starting on the **6th attempt** (`failedAttempts >= 5`).
  - Lockout behavior: When locked, reject the request with HTTP 429 Too Many Requests (or 401 with a descriptive lockout message).
  - Lockout duration: 15 minutes (or 5 minutes).
  - Successful authentication: Resets failed attempt counter to 0.

---

## 4. Deep-Dive: RBAC & IDOR Hardening

### 4.1 Article Management RBAC (`/api/articles/**`)
- **Problem**:
  In `SecurityConfig.java`:
  ```java
  .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
  .requestMatchers("/api/articles/**").permitAll()
  ```
  The second rule overrides all HTTP methods for `/api/articles/**` to `permitAll()`.
- **Hardening Strategy**:
  1. In `SecurityConfig.java`:
     - Keep `HttpMethod.GET, "/api/articles/**"` as `permitAll()`.
     - Remove `.requestMatchers("/api/articles/**").permitAll()`.
     - Explicitly define:
       ```java
       .requestMatchers(HttpMethod.POST, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
       .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
       .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
       ```
  2. In `ArticleController.java`:
     Add method-level `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")` on:
     - `createArticle(...)`
     - `updateArticle(...)`
     - `deleteArticle(...)`
     - `aiGenerate(...)`
     - `aiGenerateAndPublish(...)`
     - `aiBatchGenerate(...)`

### 4.2 EMR Image Upload RBAC (`/api/emr/images/**`)
- **Problem**:
  `SecurityConfig.java` line 92: `.requestMatchers("/api/emr/images/**").permitAll()`.
- **Hardening Strategy**:
  1. Remove `.requestMatchers("/api/emr/images/**").permitAll()`.
  2. Add in `SecurityConfig.java`:
     ```java
     .requestMatchers(HttpMethod.POST, "/api/emr/images/upload").hasAnyRole("DENTIST", "ADMIN", "OWNER")
     .requestMatchers(HttpMethod.GET, "/api/emr/images/**").hasAnyRole("DENTIST", "ADMIN", "OWNER", "PATIENT")
     ```
  3. In `FileUploadController.java`:
     Add `@PreAuthorize("hasAnyRole('DENTIST', 'ADMIN', 'OWNER')")` on `uploadImage`.
     Add `@PreAuthorize("hasAnyRole('DENTIST', 'ADMIN', 'OWNER', 'PATIENT')")` on `getByMedicalRecord` and `getByPatient`.

### 4.3 Tier-2 Satellite Partner RBAC (`/api/tier2-agents`)
- **Problem**:
  `GET /api/tier2-agents` and `GET /api/tier2-agents/{id}` have no `@PreAuthorize`.
- **Hardening Strategy**:
  1. In `SecurityConfig.java`:
     ```java
     .requestMatchers("/api/tier2-agents/**").hasAnyRole("ADMIN", "OWNER", "AGENT")
     ```
  2. In `Tier2AgentController.java`:
     Add class-level `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'AGENT')")` or method-level on `getAllAgents` and `getAgentById`.

### 4.4 EMR Record IDOR Protection (`/api/medical-records`)
- **Problem**:
  `GET /api/medical-records` allows any authenticated user to retrieve all clinic records when `phone` param is absent, or query any phone number.
- **Hardening Strategy**:
  In `MedicalRecordController.getMedicalRecords`:
  1. Retrieve current authenticated user from `SecurityContextHolder`.
  2. If user role is `ROLE_PATIENT`:
     - If `phone` is supplied: verify `phone.trim().equals(currentUser.getPhone())`. If not matching, return empty list or throw `AccessDeniedException` (HTTP 403 Forbidden).
     - If `phone` is absent: automatically set `phone = currentUser.getPhone()`.
     - Patient MUST NEVER be allowed to execute `findAllByOrderByRecordDateDesc()`.
  3. If user role is `ROLE_DENTIST`, `ROLE_ADMIN`, or `ROLE_OWNER`:
     - Allow querying by arbitrary phone or returning full clinic records.

### 4.5 Patient Appointment IDOR Protection (`/api/appointments/patient/{patientId}`)
- **Problem**:
  `AppointmentController.getForPatient(@PathVariable Long patientId)` has `@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")`. A patient can request any user's ID (e.g. `patientId = 1`).
- **Hardening Strategy**:
  In `AppointmentController.getForPatient`:
  - If current user has `ROLE_PATIENT`:
    - Ensure `currentUser.getId().equals(patientId)`.
    - If not equal, return empty list (`ApiResponse.success(Collections.emptyList())`) or throw `AccessDeniedException`.

---

## 5. Precise Implementation Technical Blueprint

### Recipe 1: `RateLimitingFilter.java` Hardening
**File**: `src/main/java/com/dentalclinic/config/RateLimitingFilter.java`

```java
package com.dentalclinic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 60;
    private static final long WINDOW_DURATION_MS = 60000L; // 60s (60 req/minute burst defense)

    private static class RequestCount {
        long windowStart;
        AtomicInteger count;

        RequestCount(long start) {
            this.windowStart = start;
            this.count = new AtomicInteger(1);
        }
    }

    private final ConcurrentHashMap<String, RequestCount> ipRequestMap = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Whitelist static resources, Swagger, Actuator, and WebSocket endpoints
        return !path.startsWith("/api/")
                || path.startsWith("/api/analytics/events") // Non-blocking analytics
                || path.startsWith("/actuator/")
                || path.startsWith("/ws-dental/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getSanitizedClientIp(request);
        long now = System.currentTimeMillis();

        RequestCount reqCount = ipRequestMap.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.windowStart) > WINDOW_DURATION_MS) {
                return new RequestCount(now);
            } else {
                current.count.incrementAndGet();
                return current;
            }
        });

        if (reqCount.count.get() > MAX_REQUESTS_PER_WINDOW) {
            long retryAfterSec = Math.max(1, (WINDOW_DURATION_MS - (now - reqCount.windowStart)) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSec));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Phát hiện lưu lượng truy cập bất thường (Spam/DoS). Vui lòng thử lại sau vài giây!\",\"statusCode\":429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        ipRequestMap.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > (WINDOW_DURATION_MS * 2));
    }

    private String getSanitizedClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

---

### Recipe 2: `LoginAttemptService.java` (New Service for Brute Force Lockout)
**File**: `src/main/java/com/dentalclinic/security/LoginAttemptService.java`

```java
package com.dentalclinic.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MS = 15 * 60 * 1000L; // 15 minutes lockout

    private static class AttemptRecord {
        AtomicInteger attempts = new AtomicInteger(0);
        long lockUntil = 0;
    }

    private final ConcurrentHashMap<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        long now = System.currentTimeMillis();
        AttemptRecord record = attemptsCache.computeIfAbsent(key, k -> new AttemptRecord());

        if (record.lockUntil > now) {
            // Already locked
            return;
        }

        int current = record.attempts.incrementAndGet();
        if (current >= MAX_ATTEMPTS) {
            record.lockUntil = now + LOCK_TIME_DURATION_MS;
        }
    }

    public boolean isBlocked(String key) {
        AttemptRecord record = attemptsCache.get(key);
        if (record == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (record.lockUntil > now) {
            return true;
        }
        if (record.lockUntil != 0 && record.lockUntil <= now) {
            // Lock expired, reset
            attemptsCache.remove(key);
            return false;
        }
        return false;
    }

    public int getRemainingAttempts(String key) {
        AttemptRecord record = attemptsCache.get(key);
        if (record == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - record.attempts.get());
    }
}
```

---

### Recipe 3: `AuthService.java` Integration with LoginAttemptService
**File**: `src/main/java/com/dentalclinic/service/AuthService.java`

```java
    // Inject LoginAttemptService
    private final LoginAttemptService loginAttemptService;

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        if (loginAttemptService.isBlocked(username)) {
            throw new BadRequestException("Tài khoản tạm thời bị khóa do đăng nhập sai quá 5 lần. Vui lòng thử lại sau 15 phút!");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword().trim())
            );

            // Clear failed attempts on successful login
            loginAttemptService.loginSucceeded(username);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String jwt = tokenProvider.generateToken(authentication, user.getId(), user.getFullName());

            return new AuthResponse(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.getPhone(),
                    user.getEmail()
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(username);
            throw ex;
        }
    }
```
*Note on Test Compatibility*: For the 5 bad login attempts in `T4-SEC-01`, `BadCredentialsException` is thrown each time and converted to HTTP 401 Unauthorized by `GlobalExceptionHandler`. On the 6th attempt, `isBlocked` is true, triggering account lockout defense!

---

### Recipe 4: `SecurityConfig.java` RBAC Hardening
**File**: `src/main/java/com/dentalclinic/security/SecurityConfig.java`

Replace lines 86-93:
```java
// BEFORE:
                // Public CMS Articles & Doctor Reviews & EMR Uploads
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers("/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
                .requestMatchers("/api/reviews/**").permitAll()
                .requestMatchers("/api/emr/images/**").permitAll()
                .requestMatchers("/api/email/**").permitAll()

// AFTER:
                // Public CMS Articles (Read-Only) & Protected Modifications
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")

                // Doctor Reviews
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyRole("ADMIN", "OWNER")

                // EMR Medical Images Upload & Retrieval
                .requestMatchers(HttpMethod.POST, "/api/emr/images/upload").hasAnyRole("DENTIST", "ADMIN", "OWNER")
                .requestMatchers(HttpMethod.GET, "/api/emr/images/**").hasAnyRole("DENTIST", "ADMIN", "OWNER", "PATIENT")

                // Tier-2 Satellite Partner Endpoints
                .requestMatchers("/api/tier2-agents/**").hasAnyRole("ADMIN", "OWNER", "AGENT")

                .requestMatchers("/api/email/**").permitAll()
```

---

### Recipe 5: `ArticleController.java` Method-Level RBAC
**File**: `src/main/java/com/dentalclinic/controller/ArticleController.java`

Add `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")` to mutating methods:
```java
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Article> createArticle(@RequestBody Article article) {
        return ResponseEntity.ok(articleService.createArticle(article));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        return articleService.updateArticle(id, article)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai-generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> aiGenerate(@RequestBody Map<String, String> payload) { ... }

    @PostMapping("/ai-generate-and-publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> aiGenerateAndPublish(@RequestBody Map<String, String> payload) { ... }

    @PostMapping("/ai-batch-generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> aiBatchGenerate(@RequestBody Map<String, Object> payload) { ... }
```

---

### Recipe 6: `MedicalRecordController.java` IDOR Hardening
**File**: `src/main/java/com/dentalclinic/controller/MedicalRecordController.java`

```java
package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.model.MedicalRecord;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.MedicalRecordRepository;
import com.dentalclinic.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@CrossOrigin(origins = "*")
@Tag(name = "4. Medical Records (EMR)", description = "Hồ sơ bệnh án điện tử")
public class MedicalRecordController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;

    public MedicalRecordController(MedicalRecordRepository medicalRecordRepository, UserRepository userRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "Tra cứu bệnh án EMR (Lọc theo SĐT hoặc Toàn bộ)")
    public ResponseEntity<ApiResponse<List<MedicalRecord>>> getMedicalRecords(@RequestParam(required = false) String phone) {
        User currentUser = getCurrentUser();

        // IDOR Defense: If caller is PATIENT, strictly enforce own records
        if (currentUser != null && currentUser.getRole() == Role.ROLE_PATIENT) {
            String patientPhone = currentUser.getPhone();
            if (phone != null && !phone.isBlank() && !phone.trim().equals(patientPhone)) {
                // Patient attempting to query another patient's phone
                throw new AccessDeniedException("Bệnh nhân không có quyền tra cứu hồ sơ của người khác!");
            }
            // Always return strictly the patient's own records
            if (patientPhone != null && !patientPhone.isBlank()) {
                return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(patientPhone.trim())));
            }
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        // Clinical Staff (OWNER, DENTIST, ADMIN, RECEPTIONIST)
        if (phone != null && !phone.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findByPatientPhoneOrderByRecordDateDesc(phone.trim())));
        }
        return ResponseEntity.ok(ApiResponse.success(medicalRecordRepository.findAllByOrderByRecordDateDesc()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'DENTIST')")
    @Operation(summary = "Thêm/Cập nhật bệnh án (Chủ & Bác sĩ)")
    public ResponseEntity<ApiResponse<MedicalRecord>> createRecord(@RequestBody MedicalRecord record) {
        MedicalRecord saved = medicalRecordRepository.save(record);
        return ResponseEntity.ok(ApiResponse.success("Lưu bệnh án thành công!", saved));
    }
}
```

---

### Recipe 7: `FileUploadController.java` RBAC Hardening
**File**: `src/main/java/com/dentalclinic/controller/FileUploadController.java`

```java
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('DENTIST', 'ADMIN', 'OWNER')")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "medicalRecordId", required = false) Long medicalRecordId,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "patientName", required = false) String patientName,
            @RequestParam(value = "imageType", required = false) String imageType,
            @RequestParam(value = "notes", required = false) String notes) {
        ...
    }

    @GetMapping("/record/{recordId}")
    @PreAuthorize("hasAnyRole('DENTIST', 'ADMIN', 'OWNER', 'PATIENT')")
    public ResponseEntity<List<DentalImageAttachment>> getByMedicalRecord(@PathVariable Long recordId) {
        return ResponseEntity.ok(fileUploadService.getImagesByMedicalRecord(recordId));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DENTIST', 'ADMIN', 'OWNER', 'PATIENT')")
    public ResponseEntity<List<DentalImageAttachment>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(fileUploadService.getImagesByPatient(patientId));
    }
```

---

### Recipe 8: `Tier2AgentController.java` RBAC Hardening
**File**: `src/main/java/com/dentalclinic/controller/Tier2AgentController.java`

Add `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'AGENT')")` on `getAllAgents` and `getAgentById`:
```java
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'AGENT')")
    @Operation(summary = "Danh sách đại lý cấp 2 và đối tác phân phối")
    public ResponseEntity<ApiResponse<List<Tier2Agent>>> getAllAgents(
            @RequestParam(required = false) AgentType agentType) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getAllAgents(agentType)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'AGENT')")
    @Operation(summary = "Chi tiết đại lý cấp 2 theo ID")
    public ResponseEntity<ApiResponse<Tier2Agent>> getAgentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getAgentById(id)));
    }
```

---

### Recipe 9: `AppointmentController.java` IDOR Hardening
**File**: `src/main/java/com/dentalclinic/controller/AppointmentController.java`

In `getForPatient(@PathVariable Long patientId)`:
```java
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Appointment>>> getForPatient(@PathVariable Long patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
            if (currentUser != null && !currentUser.getId().equals(patientId)) {
                // Return empty list or 403 to prevent appointment leakage
                return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForPatient(patientId)));
    }
```

---

## 6. Verification Method

Once implemented, independently verify using:

1. **Compilation Check**:
   ```powershell
   .\mvnw.cmd test-compile
   ```

2. **Security Test Suite Execution**:
   ```powershell
   .\mvnw.cmd test -Dtest=MedicalSecurityE2ETest
   ```

3. **Targeted Verification of Key Test Assertions**:
   - `T1-RBAC-01`: Verify unauthenticated POST to `/api/articles` returns 401 or 403.
   - `T1-RBAC-02`: Verify patient DELETE on `/api/articles/1` returns 403.
   - `T1-RBAC-03`: Verify unauthenticated POST to `/api/emr/images/upload` returns 401 or 403.
   - `T1-RBAC-05`: Verify patient GET on `/api/tier2-agents` returns 401 or 403.
   - `T1-IDOR-01` & `T1-IDOR-02`: Verify patient cannot dump all records or view other patients' records.
   - `T2-BND-02`: Verify burst of > 60 requests from single IP triggers HTTP 429 Too Many Requests.
   - `T4-SEC-01`: Verify external attacker fails all 4 penetration phases (unauthorized access blocked, directory scan contained, 5 bad logins return 401, shell upload blocked).
   - `T4-SEC-02`: Verify compromised patient insider fails IDOR harvest, article deletion, and rogue partner registration.
