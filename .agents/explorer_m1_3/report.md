# Milestone 1: Sensitive Data Sanitizer & Verification Architecture Report

**Author**: Explorer 3 (`explorer_m1_3`)  
**Date**: 2026-09-12  
**Target Package**: `com.dentalclinic.itteam.service`  
**Class Name**: `SensitiveDataSanitizer.java`  
**Test Classes**: `src/test/java/com/dentalclinic/itteam/service/SensitiveDataSanitizerTest.java`, `src/test/java/com/dentalclinic/itteam/model/EntityPrePersistenceSanitizationTest.java`  
**Authoritative Sources**: `ORIGINAL_REQUEST.md` (§ R1, Acceptance Criteria), `PROJECT.md` (§ M1, F09, M1↔M2 Interface Contract)

---

## 1. Executive Summary & Mission Scope

The DentalCare Management Portal IT Team Command Center requires an automated **Privacy Guardrail & Sensitive Data Sanitizer** (`SensitiveDataSanitizer`) to prevent sensitive credentials, authorization tokens, session cookies, and clinical medical records (EMR) from being persisted to disk or leaked into system logs, memory stores, chat messages, or API execution metrics.

### Authoritative Requirements:
1. **`ORIGINAL_REQUEST.md` (§ R1, Privacy Guardrail)**:
   > "Privacy Guardrail: Strictly prohibit storing cookies, plaintext passwords, JWTs, credentials, medical records (EMR), or real patient PII in memory/activity/tab/API logs. All payloads must be redacted/sanitized."
2. **`PROJECT.md` (§ Interface Contracts M1 ↔ M2)**:
   > `SensitiveDataSanitizer.sanitize(String payload): String`
3. **Mission Invariants**:
   - Verify regex rules for:
     1. JWT tokens (`[REDACTED_JWT]`)
     2. Passwords & credentials (`[REDACTED]`)
     3. Bearer authorization headers (`Bearer [REDACTED_JWT]` or `Bearer [REDACTED]`)
     4. Cookie headers (`Cookie: [REDACTED]`, `Set-Cookie: [REDACTED]`, session keys)
     5. Medical EMR PII (`[REDACTED_MEDICAL]`)
   - Develop a comprehensive **Unit Tests Strategy** to verify that sanitization is guaranteed prior to entity persistence into database tables (`it_api_run_log`, `it_agent_memory`, `it_agent_activity`, `it_agent_message`).

---

## 2. Comprehensive Specification of Regex Sanitization Rules

To prevent information leaks while maintaining valid JSON structure and readability in activity feeds, the sanitizer uses compiled `java.util.regex.Pattern` instances with negative lookaheads `(?!\\[REDACTED)`. This ensures **strict idempotency**: running the sanitizer multiple times over the same payload never results in duplicate redaction tags like `[[REDACTED]]`.

### 2.1. Summary Mapping of Sensitive Categories to Replacement Tokens

| # | Sensitive Data Category | Source / Context | Replacement Token | Compiled Regex Pattern |
|---|---|---|---|---|
| 1 | **JWT Tokens (Bearer)** | `Authorization: Bearer eyJ...` | `Bearer [REDACTED_JWT]` | `(?i)\\bBearer\\s+ey[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+` |
| 2 | **JWT Tokens (Standalone)** | JSON values, URL params, text | `[REDACTED_JWT]` | `\\beyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\b` |
| 3 | **Generic Bearer Headers** | `Bearer <opaque_token>` | `Bearer [REDACTED]` | `(?i)\\bBearer\\s+(?!\\[REDACTED)[^\\s\"',;}{]+` |
| 4 | **Basic Auth Headers** | `Authorization: Basic <base64>` | `Basic [REDACTED]` | `(?i)\\bBasic\\s+[A-Za-z0-9+/=]{6,}` |
| 5 | **JSON Password Fields** | `{"password": "secret"}` | `"$1": "[REDACTED]"` | `(?i)\"(password\|passwd\|pwd\|pass\|secret\|client_secret\|apiKey\|api_key\|refreshToken\|accessToken)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\"` |
| 6 | **Form / URL Passwords** | `password=123456&user=admin` | `$1=[REDACTED]` | `(?i)\\b(password\|passwd\|pwd\|secret\|api_key\|apikey)=(?!\\[REDACTED)[^&\\s\"',;]+` |
| 7 | **Plain Text Passwords** | `Password: secret123` | `$1: [REDACTED]` | `(?i)\\b(password\|passwd\|pwd\|secret)\\s*[:=]\\s*(?!\\[REDACTED)[^\\s\"',;]+` |
| 8 | **Cookie Headers** | `Cookie: ...`, `Set-Cookie: ...` | `$1: [REDACTED]` | `(?i)\\b(Set-Cookie\|Cookie)\\s*:\\s*(?!\\[REDACTED)[^\\r\\n;]+` |
| 9 | **JSON Cookie Properties** | `{"Cookie": "..."}` | `"$1": "[REDACTED]"` | `(?i)\"(Cookie\|Set-Cookie)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\"` |
| 10 | **Session Cookie Keys** | `JSESSIONID=...`, `remember-me=...` | `$1=[REDACTED]` | `(?i)\\b(JSESSIONID\|remember-me\|dental_token\|sessionid\|authToken)=(?!\\[REDACTED)[^;\\s\"',&]+` |
| 11 | **Medical EMR JSON Fields** | `{"diagnosis": "..."}`, `{"prescription": "..."}` | `"$1": "[REDACTED_MEDICAL]"` | `(?i)\"(diagnosis\|prescription\|treatmentDone\|treatment_done\|notes\|medicalHistory\|medical_history\|symptoms\|doctorNotes\|doctor_notes\|treatmentPlan\|treatment_plan)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\"` |
| 12 | **Medical EMR Plain Text** | `Diagnosis: Sâu răng...` | `$1: [REDACTED_MEDICAL]` | `(?i)\\b(Diagnosis\|Prescription\|TreatmentDone\|DoctorNotes\|MedicalHistory)\\s*:\\s*(?!\\[REDACTED)[^\\r\\n,;]+` |
| 13 | **Citizen ID (CCCD/CMND)** | 12-digit CCCD or 9-digit CMND | `[REDACTED_ID]` | `(?<!\\d)(0\\d{11}\|\\d{9})(?!\\d)` |
| 14 | **Patient Phone Masking** | `{"patientPhone": "0981234567"}` | `"$1": "$2****$3"` | `(?i)\"(patientPhone\|phone)\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{3})\"` |

---

### 2.2. Detailed Analysis & Boundary Handling

#### 1. JWT Tokens (`[REDACTED_JWT]`)
- **Structure**: A JWT has 3 parts: `header.payload.signature` joined by `.`. The header base64url always starts with `ey` (representing `{"` in JSON).
- **Rule Ordering**:
  1. `BEARER_JWT_PATTERN` matches `Bearer ey...` and replaces it with `Bearer [REDACTED_JWT]`.
  2. `STANDALONE_JWT_PATTERN` matches any remaining `eyJ...` tokens (e.g. inside `{"token": "ey..."}`) and replaces them with `[REDACTED_JWT]`.
- **Safety**:
  - Word boundary `\b` prevents matching substrings of unrelated long alphanumeric identifiers.
  - Length quantifier `{10,}` requires each segment to be at least 10 base64url characters, preventing false positives on small dotted notation (e.g. Java package names like `com.dentalclinic.itteam`).

#### 2. Passwords & Secrets (`[REDACTED]`)
- **Contexts Covered**:
  - **JSON strings**: `"password": "mySecretPass"` -> `"password": "[REDACTED]"`
  - **JSON non-strings** (e.g. numeric passwords): `"password": 123456` -> `"password": "[REDACTED]"`
  - **Query parameters**: `/api/login?password=foo&user=bar` -> `/api/login?password=[REDACTED]&user=bar`
  - **Text key-value**: `Password: 123` -> `Password: [REDACTED]`
- **Keywords Protected**: `password`, `passwd`, `pwd`, `pass`, `secret`, `client_secret`, `apiKey`, `api_key`, `refreshToken`, `accessToken`.

#### 3. Bearer Authorization Headers (`Bearer [REDACTED]` or `Bearer [REDACTED_JWT]`)
- **Contexts Covered**:
  - Bearer with JWT: `Authorization: Bearer eyJhbGci...` -> `Authorization: Bearer [REDACTED_JWT]`
  - Bearer with opaque token / API key: `Authorization: Bearer sk-ant-api03-...` -> `Authorization: Bearer [REDACTED]`
  - Negative lookahead `(?!\\[REDACTED)` ensures `Bearer [REDACTED_JWT]` is never re-redacted to `Bearer [REDACTED]`.

#### 4. Cookie Headers (`Cookie: [REDACTED]`, `Set-Cookie: [REDACTED]`)
- **Contexts Covered**:
  - Standard HTTP headers: `Cookie: JSESSIONID=abc; theme=dark` -> `Cookie: [REDACTED]`
  - Response headers: `Set-Cookie: token=xyz; Path=/; HttpOnly` -> `Set-Cookie: [REDACTED]`
  - Standalone session cookie key-value: `JSESSIONID=12345` -> `JSESSIONID=[REDACTED]`

#### 5. Medical EMR PII (`[REDACTED_MEDICAL]`)
- **Contexts Covered**:
  - Matches all clinical medical attributes defined in `com.dentalclinic.model.MedicalRecord.java`:
    `diagnosis`, `prescription`, `treatmentDone`, `treatment_done`, `doctorNotes`, `doctor_notes`, `medicalHistory`, `symptoms`, `treatmentPlan`.
  - JSON format: `{"diagnosis": "Viêm nha chu nặng"}` -> `{"diagnosis": "[REDACTED_MEDICAL]"}`
  - Plain text format: `Diagnosis: Nhổ răng số 8` -> `Diagnosis: [REDACTED_MEDICAL]`

#### 6. Auxiliary Privacy Protections
- **Vietnamese National ID (CCCD/CMND)**: Vietnam CCCD is 12 digits starting with `0` (e.g. `001201012345`), and older CMND is 9 digits. Replaced with `[REDACTED_ID]`.
- **Phone Masking**: Middle 4 digits masked with asterisks (e.g. `091****789`) to preserve identifiable prefix/suffix for customer lookup while protecting PII.

---

## 3. Production-Ready Component Design: `SensitiveDataSanitizer.java`

The component is designed with a **Dual-Access Architecture**:
1. **Spring Bean (`@Component`)**: Allows dependency injection via `@Autowired` or constructor injection in services (`ITApiRunnerService`, `ITTeamService`, `ITMessagingService`).
2. **Static Utility Method (`public static String sanitize(...)`)**: Allows direct invocation without Spring context, enabling use inside JPA `@PrePersist` entity lifecycle methods, static interceptors, and standalone unit tests.

### Source Code: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`

```java
package com.dentalclinic.itteam.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Privacy Guardrail & Sensitive Data Sanitizer.
 * Redacts JWT tokens, passwords, Bearer headers, cookies, and medical EMR PII
 * prior to persisting into database tables or outputting to activity logs.
 *
 * Implements both Spring @Component and static utility methods for maximum flexibility.
 */
@Component
public class SensitiveDataSanitizer {

    // 1. Bearer JWT Header: Bearer eyJ... -> Bearer [REDACTED_JWT]
    private static final Pattern BEARER_JWT_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+ey[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-+]+"
    );

    // 2. Standalone JWT token: eyJ... -> [REDACTED_JWT]
    private static final Pattern STANDALONE_JWT_PATTERN = Pattern.compile(
            "\\beyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-+]{10,}\\b"
    );

    // 3. Generic Bearer Header (non-JWT opaque tokens): Bearer abc... -> Bearer [REDACTED]
    private static final Pattern BEARER_GENERIC_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+(?!\\[REDACTED)[^\\s\"',;}{]+"
    );

    // 4. Basic Auth Header: Basic dXNlcjpwYXNz -> Basic [REDACTED]
    private static final Pattern BASIC_AUTH_PATTERN = Pattern.compile(
            "(?i)\\bBasic\\s+(?!\\[REDACTED)[A-Za-z0-9+/=]{6,}"
    );

    // 5. JSON Password & Secrets: "password": "value" -> "password": "[REDACTED]"
    private static final Pattern JSON_SECRET_STRING_PATTERN = Pattern.compile(
            "(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|refreshToken|accessToken)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\""
    );

    // 6. JSON Password numeric/unquoted: "password": 123456 -> "password": "[REDACTED]"
    private static final Pattern JSON_SECRET_UNQUOTED_PATTERN = Pattern.compile(
            "(?i)\"(password|passwd|pwd|pass|secret|apiKey|api_key)\"\\s*:\\s*(?!\\[REDACTED)[^,\\}\\]\\s\"]+"
    );

    // 7. Form-urlencoded or URL query params: password=secret -> password=[REDACTED]
    private static final Pattern FORM_PASSWORD_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|api_key|apikey)=(?!\\[REDACTED)[^&\\s\"',;]+"
    );

    // 8. Plain text password key-value: Password: xyz -> Password: [REDACTED]
    private static final Pattern TEXT_PASSWORD_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret)\\s*[:=]\\s*(?!\\[REDACTED)[^\\s\"',;]+"
    );

    // 9. HTTP Cookie & Set-Cookie headers: Cookie: ... -> Cookie: [REDACTED]
    private static final Pattern COOKIE_HEADER_PATTERN = Pattern.compile(
            "(?i)\\b(Set-Cookie|Cookie)\\s*:\\s*(?!\\[REDACTED)[^\\r\\n;]+"
    );

    // 10. JSON Cookie properties: "Cookie": "..." -> "Cookie": "[REDACTED]"
    private static final Pattern JSON_COOKIE_PATTERN = Pattern.compile(
            "(?i)\"(Cookie|Set-Cookie)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\""
    );

    // 11. Specific session cookie key-value pairs: JSESSIONID=... -> JSESSIONID=[REDACTED]
    private static final Pattern SESSION_COOKIE_KEY_PATTERN = Pattern.compile(
            "(?i)\\b(JSESSIONID|remember-me|dental_token|sessionid|authToken)=(?!\\[REDACTED)[^;\\s\"',&]+"
    );

    // 12. Medical EMR JSON Fields: "diagnosis": "..." -> "diagnosis": "[REDACTED_MEDICAL]"
    private static final Pattern JSON_MEDICAL_PATTERN = Pattern.compile(
            "(?i)\"(diagnosis|prescription|treatmentDone|treatment_done|notes|medicalHistory|medical_history|symptoms|doctorNotes|doctor_notes|treatmentPlan|treatment_plan)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\""
    );

    // 13. Medical EMR Plain Text: Diagnosis: ... -> Diagnosis: [REDACTED_MEDICAL]
    private static final Pattern TEXT_MEDICAL_PATTERN = Pattern.compile(
            "(?i)\\b(Diagnosis|Prescription|TreatmentDone|DoctorNotes|MedicalHistory)\\s*:\\s*(?!\\[REDACTED)[^\\r\\n,;]+"
    );

    // 14. Vietnamese Citizen ID (CCCD: 12 digits starting with 0, or CMND: 9 digits) -> [REDACTED_ID]
    private static final Pattern CCCD_PATTERN = Pattern.compile(
            "(?<!\\d)(0\\d{11}|\\d{9})(?!\\d)"
    );

    // 15. Patient Phone Masking: "patientPhone": "0981234567" -> "patientPhone": "098****567"
    private static final Pattern PHONE_MASK_PATTERN = Pattern.compile(
            "(?i)\"(patientPhone|phone)\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{3})\""
    );

    /**
     * Core sanitization method.
     * Sequentially applies redaction rules while preserving payload integrity.
     *
     * @param payload Raw text or JSON string to sanitize
     * @return Sanitized string with sensitive tokens redacted, or null if input is null
     */
    public static String sanitize(String payload) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }

        String result = payload;

        // Step 1: Sanitize Bearer with JWT
        result = BEARER_JWT_PATTERN.matcher(result).replaceAll("Bearer [REDACTED_JWT]");

        // Step 2: Sanitize Standalone JWT tokens
        result = STANDALONE_JWT_PATTERN.matcher(result).replaceAll("[REDACTED_JWT]");

        // Step 3: Sanitize Generic Bearer tokens
        result = BEARER_GENERIC_PATTERN.matcher(result).replaceAll("Bearer [REDACTED]");

        // Step 4: Sanitize Basic Authentication
        result = BASIC_AUTH_PATTERN.matcher(result).replaceAll("Basic [REDACTED]");

        // Step 5: Sanitize JSON Password/Secrets (Strings & Unquoted)
        result = JSON_SECRET_STRING_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED]\"");
        result = JSON_SECRET_UNQUOTED_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED]\"");

        // Step 6: Sanitize Form / Query / Text Passwords
        result = FORM_PASSWORD_PATTERN.matcher(result).replaceAll("$1=[REDACTED]");
        result = TEXT_PASSWORD_PATTERN.matcher(result).replaceAll("$1: [REDACTED]");

        // Step 7: Sanitize Cookie headers and JSON properties
        result = COOKIE_HEADER_PATTERN.matcher(result).replaceAll("$1: [REDACTED]");
        result = JSON_COOKIE_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED]\"");
        result = SESSION_COOKIE_KEY_PATTERN.matcher(result).replaceAll("$1=[REDACTED]");

        // Step 8: Sanitize Medical EMR fields (JSON and Text)
        result = JSON_MEDICAL_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED_MEDICAL]\"");
        result = TEXT_MEDICAL_PATTERN.matcher(result).replaceAll("$1: [REDACTED_MEDICAL]");

        // Step 9: Sanitize National ID / CCCD
        result = CCCD_PATTERN.matcher(result).replaceAll("[REDACTED_ID]");

        // Step 10: Mask Patient Phone Numbers
        result = PHONE_MASK_PATTERN.matcher(result).replaceAll("\"$1\": \"$2****$3\"");

        return result;
    }

    /**
     * Checks if a payload contains any unsanitized sensitive data.
     * Useful for assertion testing and security auditing.
     *
     * @param payload Text to check
     * @return true if payload contains potential secrets or unredacted PII
     */
    public static boolean containsUnsanitizedSensitiveData(String payload) {
        if (payload == null || payload.isBlank()) {
            return false;
        }
        return BEARER_JWT_PATTERN.matcher(payload).find()
                || STANDALONE_JWT_PATTERN.matcher(payload).find()
                || BEARER_GENERIC_PATTERN.matcher(payload).find()
                || JSON_SECRET_STRING_PATTERN.matcher(payload).find()
                || FORM_PASSWORD_PATTERN.matcher(payload).find()
                || COOKIE_HEADER_PATTERN.matcher(payload).find()
                || JSON_MEDICAL_PATTERN.matcher(payload).find();
    }
}
```

---

## 4. Pre-Persistence Architecture & Enforcement Strategy

To ensure zero leakage into the database even if an upstream controller or service forgets to sanitize, we recommend a **Defense-in-Depth Model** operating across two layers:

```
[ Incoming Request / Internal API Execution ]
                     │
                     ▼
       ┌───────────────────────────┐
       │   Layer 1: Service Layer  │  (Explicit Sanitization:
       │   ITTeamService           │   sanitizer.sanitize(...)
       │   ITApiRunnerService      │   called when constructing entities)
       │   ITMessagingService      │
       └─────────────┬─────────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │   Layer 2: JPA Lifecycle  │  (Ironclad Safety Net:
       │   @PrePersist & @PreUpdate│   Entity hooks auto-sanitize
       │   ITApiRunLog             │   all payload/content fields
       │   ITAgentMemory           │   before database INSERT/UPDATE)
       │   ITAgentActivity         │
       │   ITAgentMessage          │
       └─────────────┬─────────────┘
                     │
                     ▼
          [ Database Persistence ]
          (H2 / PostgreSQL disk)
```

### 4.1. Layer 1: Explicit Service Layer Sanitization

Services explicitly invoke `SensitiveDataSanitizer.sanitize(...)`:
- In `ITApiRunnerService`:
  ```java
  ITApiRunLog log = new ITApiRunLog(
      endpoint,
      httpMethod,
      response.getStatusCode().value(),
      durationMs,
      SensitiveDataSanitizer.sanitize(rawRequestPayload),
      SensitiveDataSanitizer.sanitize(rawResponsePayload),
      initiatedBy
  );
  apiRunLogRepository.save(log);
  ```
- In `ITTeamService` (`saveMemory`):
  ```java
  memory.setMemoryContent(SensitiveDataSanitizer.sanitize(request.getMemoryContent()));
  memoryRepository.save(memory);
  ```
- In `ITMessagingService` (`dispatchMessage`):
  ```java
  message.setMessageBody(SensitiveDataSanitizer.sanitize(request.getMessageBody()));
  messageRepository.save(message);
  ```

---

### 4.2. Layer 2: JPA Lifecycle Pre-Persistence Callbacks (`@PrePersist` / `@PreUpdate`)

Entities implement lifecycle methods that guarantee sanitation right before the entity is serialized into SQL `INSERT` or `UPDATE` statements. Because `SensitiveDataSanitizer.sanitize()` is idempotent, double-invoking it has zero negative side-effects.

#### Integration in `ITApiRunLog.java`:
```java
@PrePersist
@PreUpdate
public void prePersistSanitize() {
    if (this.requestPayload != null) {
        this.requestPayload = SensitiveDataSanitizer.sanitize(this.requestPayload);
    }
    if (this.responsePayload != null) {
        this.responsePayload = SensitiveDataSanitizer.sanitize(this.responsePayload);
    }
    if (this.runTimestamp == null) {
        this.runTimestamp = LocalDateTime.now();
    }
    if (this.isSuccess == null && this.statusCode != null) {
        this.isSuccess = (this.statusCode >= 200 && this.statusCode < 400);
    }
}
```

#### Integration in `ITAgentMemory.java`:
```java
@PrePersist
@PreUpdate
public void prePersistSanitize() {
    if (this.memoryContent != null) {
        this.memoryContent = SensitiveDataSanitizer.sanitize(this.memoryContent);
    }
    if (this.lastUpdated == null) {
        this.lastUpdated = LocalDateTime.now();
    }
}
```

#### Integration in `ITAgentActivity.java`:
```java
@PrePersist
@PreUpdate
public void prePersistSanitize() {
    if (this.description != null) {
        this.description = SensitiveDataSanitizer.sanitize(this.description);
    }
    if (this.resultSummary != null) {
        this.resultSummary = SensitiveDataSanitizer.sanitize(this.resultSummary);
    }
    if (this.timestamp == null) {
        this.timestamp = LocalDateTime.now();
    }
}
```

#### Integration in `ITAgentMessage.java`:
```java
@PrePersist
@PreUpdate
public void prePersistSanitize() {
    if (this.messageBody != null) {
        this.messageBody = SensitiveDataSanitizer.sanitize(this.messageBody);
    }
    if (this.sentAt == null) {
        this.sentAt = LocalDateTime.now();
    }
}
```

---

## 5. Unit Tests Strategy & Test Suite Implementation Code

To verify compliance with Acceptance Criteria ("Sensitive data sanitizer prevents storing JWTs, passwords, or PII in logs and memory"), we define four test suites covering pure regex mechanics, JPA lifecycle hooks, Mockito argument capturing, and integration with H2 persistence.

### 5.1. Test Suite 1: Pure Unit Test (`SensitiveDataSanitizerTest.java`)
**File Location**: `src/test/java/com/dentalclinic/itteam/service/SensitiveDataSanitizerTest.java`

```java
package com.dentalclinic.itteam.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    private static final String SAMPLE_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    @DisplayName("R1.1 - Should sanitize Bearer authorization header containing JWT to Bearer [REDACTED_JWT]")
    void testSanitizeBearerJwtHeader() {
        String input = "Authorization: Bearer " + SAMPLE_JWT;
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Authorization: Bearer [REDACTED_JWT]");
        assertThat(sanitized).doesNotContain(SAMPLE_JWT);
    }

    @Test
    @DisplayName("R1.2 - Should sanitize standalone JWT in JSON payload to [REDACTED_JWT]")
    void testSanitizeStandaloneJwtInJson() {
        String input = "{\"token\": \"" + SAMPLE_JWT + "\", \"status\": \"ACTIVE\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("{\"token\": \"[REDACTED_JWT]\", \"status\": \"ACTIVE\"}");
        assertThat(sanitized).doesNotContain(SAMPLE_JWT);
    }

    @Test
    @DisplayName("R1.3 - Should sanitize generic non-JWT Bearer tokens to Bearer [REDACTED]")
    void testSanitizeGenericBearerHeader() {
        String input = "GET /api/data HTTP/1.1\nAuthorization: Bearer sk_live_9876543210abcdef\nHost: localhost";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("Authorization: Bearer [REDACTED]");
        assertThat(sanitized).doesNotContain("sk_live_9876543210abcdef");
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "passwd", "pwd", "secret", "client_secret", "apiKey", "api_key"})
    @DisplayName("R1.4 - Should sanitize all password and credential keys in JSON to [REDACTED]")
    void testSanitizePasswordFieldsInJson(String key) {
        String input = String.format("{\"username\": \"owner\", \"%s\": \"SuperSecretP@ssw0rd!\"}", key);
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains(String.format("\"%s\": \"[REDACTED]\"", key));
        assertThat(sanitized).doesNotContain("SuperSecretP@ssw0rd!");
        assertThat(sanitized).contains("\"username\": \"owner\"");
    }

    @Test
    @DisplayName("R1.5 - Should sanitize unquoted numeric password in JSON to [REDACTED]")
    void testSanitizeUnquotedNumericPasswordInJson() {
        String input = "{\"username\": \"admin\", \"password\": 123456}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("{\"username\": \"admin\", \"password\": \"[REDACTED]\"}");
    }

    @Test
    @DisplayName("R1.6 - Should sanitize query parameter and form passwords to [REDACTED]")
    void testSanitizeFormAndQueryPasswords() {
        String input = "POST /api/login?password=mysecret&user=admin\nBody: password=bodysecret&remember=true";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("password=[REDACTED]");
        assertThat(sanitized).doesNotContain("mysecret");
        assertThat(sanitized).doesNotContain("bodysecret");
    }

    @Test
    @DisplayName("R1.7 - Should sanitize plain text password key-values")
    void testSanitizePlainTextPassword() {
        String input = "Current configuration: Password: myRootPassword123 for service admin";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Current configuration: Password: [REDACTED] for service admin");
        assertThat(sanitized).doesNotContain("myRootPassword123");
    }

    @Test
    @DisplayName("R1.8 - Should sanitize HTTP Cookie and Set-Cookie headers")
    void testSanitizeCookieHeaders() {
        String input = "GET /api/test\nCookie: JSESSIONID=ABCD1234EFGH; Path=/; HttpOnly\nSet-Cookie: token=secret; Secure";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("Cookie: [REDACTED]");
        assertThat(sanitized).contains("Set-Cookie: [REDACTED]");
        assertThat(sanitized).doesNotContain("ABCD1234EFGH");
    }

    @Test
    @DisplayName("R1.9 - Should sanitize JSON Cookie properties")
    void testSanitizeJsonCookie() {
        String input = "{\"headers\": {\"Cookie\": \"session=12345\"}}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"Cookie\": \"[REDACTED]\"");
        assertThat(sanitized).doesNotContain("12345");
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnosis", "prescription", "treatmentDone", "doctorNotes", "medicalHistory", "symptoms"})
    @DisplayName("R1.10 - Should sanitize medical EMR fields in JSON to [REDACTED_MEDICAL]")
    void testSanitizeMedicalEmrFieldsInJson(String key) {
        String input = String.format("{\"patientId\": 101, \"%s\": \"Viêm tủy cấp tính, chỉ định chữa tủy\"}", key);
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains(String.format("\"%s\": \"[REDACTED_MEDICAL]\"", key));
        assertThat(sanitized).doesNotContain("Viêm tủy cấp tính");
        assertThat(sanitized).contains("\"patientId\": 101");
    }

    @Test
    @DisplayName("R1.11 - Should sanitize plain text clinical diagnosis and prescriptions")
    void testSanitizePlainTextMedical() {
        String input = "Record note: Diagnosis: Sâu răng hàm số 6 kèm áp xe nướu";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Record note: Diagnosis: [REDACTED_MEDICAL]");
        assertThat(sanitized).doesNotContain("Sâu răng hàm số 6");
    }

    @Test
    @DisplayName("R1.12 - Should redact Vietnamese Citizen ID (CCCD/CMND) to [REDACTED_ID]")
    void testSanitizeVietnameseCitizenId() {
        String input = "Patient CCCD: 001201012345, Old CMND: 123456789 verified.";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Patient CCCD: [REDACTED_ID], Old CMND: [REDACTED_ID] verified.");
        assertThat(sanitized).doesNotContain("001201012345");
        assertThat(sanitized).doesNotContain("123456789");
    }

    @Test
    @DisplayName("R1.13 - Should mask patient phone numbers preserving first and last 3 digits")
    void testMaskPatientPhone() {
        String input = "{\"patientName\": \"Nguyen Van A\", \"patientPhone\": \"0981234567\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"patientPhone\": \"098****567\"");
        assertThat(sanitized).doesNotContain("0981234567");
    }

    @Test
    @DisplayName("R1.14 - Idempotency Verification: Repeated sanitization yields identical result")
    void testSanitizationIdempotency() {
        String rawPayload = "{\"password\": \"secret123\", \"token\": \"" + SAMPLE_JWT + "\", \"diagnosis\": \"Sâu răng\"}";
        String firstPass = SensitiveDataSanitizer.sanitize(rawPayload);
        String secondPass = SensitiveDataSanitizer.sanitize(firstPass);
        String thirdPass = SensitiveDataSanitizer.sanitize(secondPass);

        assertThat(secondPass).isEqualTo(firstPass);
        assertThat(thirdPass).isEqualTo(firstPass);
        assertThat(firstPass).doesNotContain("[[REDACTED");
    }

    @Test
    @DisplayName("R1.15 - Boundary Handling: Null, empty, and clean strings")
    void testBoundaryHandling() {
        assertThat(SensitiveDataSanitizer.sanitize(null)).isNull();
        assertThat(SensitiveDataSanitizer.sanitize("")).isEmpty();
        assertThat(SensitiveDataSanitizer.sanitize("   ")).isEqualTo("   ");

        String harmlessJson = "{\"status\": 200, \"message\": \"Thành công\", \"agent\": \"#it-backend\"}";
        assertThat(SensitiveDataSanitizer.sanitize(harmlessJson)).isEqualTo(harmlessJson);
    }

    @Test
    @DisplayName("R1.16 - Complex Multi-Field API Log sanitization")
    void testComplexMultiFieldPayload() {
        String rawApiLog = "POST /api/auth/login HTTP/1.1\n" +
                "Authorization: Bearer " + SAMPLE_JWT + "\n" +
                "Cookie: JSESSIONID=XYZ999\n" +
                "Body: {\"username\": \"dentist_user\", \"password\": \"myTopSecret!\", \"diagnosis\": \"Viêm nướu\"}";

        String sanitized = SensitiveDataSanitizer.sanitize(rawApiLog);

        assertThat(sanitized).contains("Authorization: Bearer [REDACTED_JWT]");
        assertThat(sanitized).contains("Cookie: [REDACTED]");
        assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
        assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(sanitized).doesNotContain("myTopSecret!");
        assertThat(sanitized).doesNotContain("XYZ999");
        assertThat(sanitized).doesNotContain(SAMPLE_JWT);
        assertThat(sanitized).doesNotContain("Viêm nướu");
    }
}
```

---

### 5.2. Test Suite 2: Entity Lifecycle Pre-Persistence Unit Test (`EntityPrePersistenceSanitizationTest.java`)
**File Location**: `src/test/java/com/dentalclinic/itteam/model/EntityPrePersistenceSanitizationTest.java`

```java
package com.dentalclinic.itteam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityPrePersistenceSanitizationTest {

    private static final String SAMPLE_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    @DisplayName("R2.1 - ITApiRunLog prePersist hook automatically sanitizes request and response payloads")
    void testApiRunLogPrePersistHook() {
        String rawRequest = "{\"username\": \"owner\", \"password\": \"UnsanitizedPass123\"}";
        String rawResponse = "{\"token\": \"" + SAMPLE_JWT + "\", \"diagnosis\": \"Sâu răng số 7\"}";

        ITApiRunLog log = new ITApiRunLog(
                "/api/test",
                "POST",
                200,
                45L,
                rawRequest,
                rawResponse,
                "ROLE_ADMIN:test"
        );

        // Act: trigger prePersist callback
        log.prePersist();

        // Assert: payloads sanitized prior to persistence
        assertThat(log.getRequestPayload()).contains("\"password\": \"[REDACTED]\"");
        assertThat(log.getRequestPayload()).doesNotContain("UnsanitizedPass123");

        assertThat(log.getResponsePayload()).contains("\"token\": \"[REDACTED_JWT]\"");
        assertThat(log.getResponsePayload()).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(log.getResponsePayload()).doesNotContain(SAMPLE_JWT);
        assertThat(log.getResponsePayload()).doesNotContain("Sâu răng số 7");
    }

    @Test
    @DisplayName("R2.2 - ITAgentMemory prePersist hook automatically sanitizes memoryContent")
    void testAgentMemoryPrePersistHook() {
        String rawMemory = "Configured admin password=DatabaseRootSecret! with API key=sk-secret-12345";
        ITAgentMemory memory = new ITAgentMemory(
                1L,
                "it-backend",
                "db-credentials",
                rawMemory,
                "CRITICAL"
        );

        // Act
        memory.prePersist();

        // Assert
        assertThat(memory.getMemoryContent()).contains("password=[REDACTED]");
        assertThat(memory.getMemoryContent()).contains("key=[REDACTED]");
        assertThat(memory.getMemoryContent()).doesNotContain("DatabaseRootSecret!");
        assertThat(memory.getMemoryContent()).doesNotContain("sk-secret-12345");
    }

    @Test
    @DisplayName("R2.3 - ITAgentActivity prePersist hook automatically sanitizes description")
    void testAgentActivityPrePersistHook() {
        String rawDescription = "User logged in with Cookie: JSESSIONID=SECRET98765 and Bearer " + SAMPLE_JWT;
        ITAgentActivity activity = new ITAgentActivity(
                null,
                "system",
                "SECURITY_AUDIT",
                rawDescription,
                "SUCCESS",
                "auth:login"
        );

        // Act
        activity.prePersist();

        // Assert
        assertThat(activity.getDescription()).contains("Cookie: [REDACTED]");
        assertThat(activity.getDescription()).contains("Bearer [REDACTED_JWT]");
        assertThat(activity.getDescription()).doesNotContain("SECRET98765");
        assertThat(activity.getDescription()).doesNotContain(SAMPLE_JWT);
    }
}
```

---

### 5.3. Test Suite 3: Service Layer ArgumentCaptor Unit Test (`ITTeamServicePrePersistenceTest.java`)
**File Location**: `src/test/java/com/dentalclinic/itteam/service/ITTeamServicePrePersistenceTest.java`

```java
package com.dentalclinic.itteam.service;

import com.dentalclinic.itteam.dto.CreateMemoryRequest;
import com.dentalclinic.itteam.model.ITAgentMemory;
import com.dentalclinic.itteam.repository.ITAgentMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ITTeamServicePrePersistenceTest {

    @Mock
    private ITAgentMemoryRepository memoryRepository;

    private ITTeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new ITTeamService(memoryRepository, /* other mocked repos */ null, null, null, null, null);
    }

    @Test
    @DisplayName("R2.4 - Service layer sanitizes memoryContent before passing entity to repository.save()")
    void testServiceSanitizesPriorToRepositorySave() {
        CreateMemoryRequest request = new CreateMemoryRequest();
        request.setAgentId(1L);
        request.setMemoryKey("security-api-keys");
        request.setMemoryContent("{\"apiKey\": \"secret-stripe-key-9988\", \"diagnosis\": \"Tẩy trắng răng\"}");
        request.setPriorityLevel("HIGH");

        when(memoryRepository.findByAgentIdAndMemoryKey(1L, "security-api-keys")).thenReturn(Optional.empty());
        when(memoryRepository.save(any(ITAgentMemory.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        teamService.createMemory(request);

        // Assert: capture entity passed to repository
        ArgumentCaptor<ITAgentMemory> captor = ArgumentCaptor.forClass(ITAgentMemory.class);
        verify(memoryRepository).save(captor.capture());

        ITAgentMemory savedMemory = captor.getValue();
        assertThat(savedMemory.getMemoryContent()).contains("\"apiKey\": \"[REDACTED]\"");
        assertThat(savedMemory.getMemoryContent()).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(savedMemory.getMemoryContent()).doesNotContain("secret-stripe-key-9988");
        assertThat(savedMemory.getMemoryContent()).doesNotContain("Tẩy trắng răng");
    }
}
```

---

### 5.4. Test Suite 4: JPA H2 Integration Test (`ITTeamRepositoryPersistenceTest.java`)
**File Location**: `src/test/java/com/dentalclinic/itteam/repository/ITTeamRepositoryPersistenceTest.java`

```java
package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITApiRunLog;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ITTeamRepositoryPersistenceTest {

    @Autowired
    private ITApiRunLogRepository apiRunLogRepository;

    @Autowired
    private EntityManager entityManager;

    private static final String SAMPLE_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    @DisplayName("R2.5 - Persisted entity in database never contains raw JWT or password on reload")
    void testDatabasePersistenceDoesNotStoreSecrets() {
        String dirtyRequest = "{\"username\": \"owner\", \"password\": \"RawPassword123\"}";
        String dirtyResponse = "{\"token\": \"" + SAMPLE_JWT + "\"}";

        ITApiRunLog log = new ITApiRunLog(
                "/api/auth/login",
                "POST",
                200,
                30L,
                dirtyRequest,
                dirtyResponse,
                "test-client"
        );

        // Save and flush to database
        ITApiRunLog saved = apiRunLogRepository.saveAndFlush(log);

        // Clear Hibernate 1st-level cache to force reading from H2 disk/table
        entityManager.clear();

        // Reload from database
        ITApiRunLog reloaded = apiRunLogRepository.findById(saved.getId()).orElseThrow();

        // Verify stored contents in database
        assertThat(reloaded.getRequestPayload()).contains("\"password\": \"[REDACTED]\"");
        assertThat(reloaded.getRequestPayload()).doesNotContain("RawPassword123");

        assertThat(reloaded.getResponsePayload()).contains("\"token\": \"[REDACTED_JWT]\"");
        assertThat(reloaded.getResponsePayload()).doesNotContain(SAMPLE_JWT);
    }
}
```

---

## 6. Implementation Checklist & Integration Guide for M1 Implementer

When the worker agent implements Milestone 1:

1. **Create `SensitiveDataSanitizer.java`**:
   - Location: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`
   - Include all 15 compiled `Pattern` definitions and static `sanitize(String)` method.
2. **Integrate `@PrePersist` / `@PreUpdate` in Entities**:
   - `ITApiRunLog`: sanitize `requestPayload` and `responsePayload`.
   - `ITAgentMemory`: sanitize `memoryContent`.
   - `ITAgentActivity`: sanitize `description` and `resultSummary`.
   - `ITAgentMessage`: sanitize `messageBody`.
3. **Add Unit Tests**:
   - Create `src/test/java/com/dentalclinic/itteam/service/SensitiveDataSanitizerTest.java` (16 test cases).
   - Create `src/test/java/com/dentalclinic/itteam/model/EntityPrePersistenceSanitizationTest.java` (3 test cases).
4. **Run Verification**:
   - Verify that `./mvnw test -Dtest=SensitiveDataSanitizerTest,EntityPrePersistenceSanitizationTest` passes with 100% green.
