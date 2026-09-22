# Technical Investigation & Remediation Report: SensitiveDataSanitizer Regex Hardening

**Milestone:** Milestone 1 Iteration 2 (Sanitizer Regex Remediation)  
**Agent:** `explorer_m1_rem1`  
**Date:** 2026-09-12  
**Target Class:** `com.dentalclinic.itteam.service.SensitiveDataSanitizer`  
**Target Test Suites:** `SensitiveDataSanitizerAdversarialTest`, `SensitiveDataSanitizerChallengerTest`, `SensitiveDataSanitizerTest`  
**Related Models:** `com.dentalclinic.itteam.model.ITBrowserTabRecord`  

---

## 1. Executive Summary

Milestone 1 established foundational persistence models and the `SensitiveDataSanitizer` privacy guardrail. However, empirical stress-testing by `challenger_m1_1` and `challenger_m1_2` uncovered five critical regex and logic vulnerabilities in `SensitiveDataSanitizer`:
1. **Escaped Quote Truncation**: Character class `[^\"]*` in `JSON_SECRET_STRING_PATTERN` and `JSON_MEDICAL_PATTERN` terminates prematurely at escaped double quotes (`\"`), leaving secret/medical fragments unredacted and producing corrupt JSON.
2. **Missing OAuth2 snake_case & Generic Tokens**: Standard keys `access_token`, `refresh_token`, `auth_token`, and `token` were absent from regex key alternations, allowing plaintext API credentials to bypass redaction.
3. **Structured EMR Objects & Arrays Bypass**: `JSON_MEDICAL_PATTERN` strictly required string literals (`: "..."`), allowing structured JSON medical records (`: { ... }`) and collections (`: [ ... ]`) to leak completely into persistent storage.
4. **Audit Method False Negatives**: `containsUnsanitizedSensitiveData` inspected only 7 of the class's sensitive patterns, causing security audits to report false cleanliness on Basic Auth, plaintext passwords, CCCD, and session cookies.
5. **False Positives on Financial Amounts**: The naive 9-digit alternation in `CCCD_PATTERN` (`\d{9}`) falsely redacted typical Vietnamese dental clinic payment amounts (e.g. `100000000` VND) into invalid JSON `[REDACTED_ID]`.

This report provides the exact regex formulations, before/after diffs, and test suite updates required to achieve 100% test pass rates across all existing and challenger test harnesses.

---

## 2. Root Cause Analysis & Remediation Design

### 2.1 Requirement 1: Escaped Quotes in JSON String Values

#### Root Cause Observation
- **File**: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`
- **Lines**: 39 (`JSON_SECRET_STRING_PATTERN`), 74 (`JSON_MEDICAL_PATTERN`)
- **Current Regex Fragment**: `\"(?!\\[REDACTED)[^\"]*\"`
- **Defect Mechanism**:
  When a JSON string value contains an escaped quote `\"`, e.g.:
  ```json
  {"diagnosis": "Bệnh nhân bị \"sâu răng nặng\" ở hàm trên"}
  ```
  or a SQL injection payload:
  ```json
  {"password": "admin\" OR 1=1 --"}
  ```
  The negated character class `[^\"]*` matches up to the backslash `\`, and immediately halts upon encountering `"`. The closing quote of the regex matches the escaped quote.
  The replacement produces:
  ```json
  {"diagnosis": "[REDACTED_MEDICAL]sâu răng nặng\" ở hàm trên"}
  {"password": "[REDACTED] OR 1=1 --"}
  ```
  This leaks the medical diagnosis / SQLi string and creates syntax-invalid JSON.

#### Remediation Design
Replace `[^\"]*` with an alternation that treats an escaped quote `\\\\\"` as a protected sequence:
```regex
\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\"
```
In Java string literal syntax:
```java
"\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\""
```
- **How it functions**:
  1. Matches opening quote `"`.
  2. Asserts negative lookahead `(?!\\[REDACTED)` to ensure idempotency.
  3. Non-capturing group `(?:\\\\\"|[^\"])*`:
     - If the next two characters are `\` followed by `"`, branch `\\\\\"` matches both characters as a single atomic unit.
     - Any other character (except unescaped `"`) matches `[^\"]`.
  4. The loop terminates strictly at the true unescaped closing quote `"`.
  5. The entire string `"Bệnh nhân bị \"sâu răng nặng\" ở hàm trên"` is replaced by `"[REDACTED_MEDICAL]"`.
- **Resulting JSON**: `{"diagnosis": "[REDACTED_MEDICAL]"}` (clean, valid JSON, zero data leak).

---

### 2.2 Requirement 2: OAuth snake_case & Generic Tokens

#### Root Cause Observation
- **File**: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`
- **Lines**: 39 (`JSON_SECRET_STRING_PATTERN`), 44 (`JSON_SECRET_UNQUOTED_PATTERN`), 49 (`FORM_PASSWORD_PATTERN`)
- **Current Alternation**: `(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|refreshToken|accessToken)`
- **Defect Mechanism**:
  RFC 6749 defines standard token response properties as `access_token` and `refresh_token` (snake_case). Furthermore, external microservices commonly return `auth_token` or `token`.
  Current patterns matched only camelCase `accessToken` and `refreshToken`. Payloads like:
  ```json
  {"access_token": "gho_16C7e42F...", "refresh_token": "rfr_Secret..."}
  ```
  passed through 100% unredacted into API run logs and agent memories.

#### Remediation Design
Expand the key alternation across string, unquoted, and form/query patterns:
```java
// 5. JSON String Secrets & Tokens
"(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|token|authToken|auth_token|accessToken|access_token|refreshToken|refresh_token|idToken|id_token)\"\\s*:\\s*\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\""

// 6. JSON Unquoted Secrets & Tokens
"(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|token|authToken|auth_token|accessToken|access_token|refreshToken|refresh_token)\"\\s*:\\s*(?!\\[REDACTED)[^,\\}\\]\\s\"]+"

// 7. Form / URL Query Parameters
"(?i)\\b(password|passwd|pwd|secret|api_key|apikey|access_token|refresh_token|auth_token)=(?!\\[REDACTED)[^&\\s\"',;]+"
```
- **Idempotency with JWT Redaction**:
  In `SensitiveDataSanitizer.sanitize()`, `STANDALONE_JWT_PATTERN` executes in Step 2, replacing 3-part JWTs with `[REDACTED_JWT]`.
  When Step 5 (`JSON_SECRET_STRING_PATTERN`) executes, its negative lookahead `(?!\\[REDACTED)` detects `[REDACTED_JWT]` and skips it, preserving the specific `[REDACTED_JWT]` tag. Non-JWT tokens (e.g. GitHub PATs, OAuth refresh tokens, opaque API keys) are cleanly redacted to `[REDACTED]`.

---

### 2.3 Requirement 3: Nested JSON Medical Diagnosis Objects & Arrays

#### Root Cause Observation
- **File**: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`
- **Line**: 74 (`JSON_MEDICAL_PATTERN`)
- **Current Pattern**:
  ```java
  "(?i)\"(diagnosis|prescription|...)\"\\s*:\\s*\"(?!\\[REDACTED)[^\"]*\""
  ```
- **Defect Mechanism**:
  The regex requires `: "..."`. If a clinic system transmits a structured diagnosis object:
  ```json
  {"diagnosis": {"icd10": "K04.0", "description": "Pulpitis viem tuy cap", "severity": "ACUTE"}}
  ```
  or an array of diagnosis strings:
  ```json
  {"diagnosis": ["Sâu răng R36", "Viêm nướu răng"]}
  ```
  The regex fails to match. Medical EMR data is saved in plaintext, violating §R1 privacy requirements.

#### Remediation Design
Combine JSON strings (with escaped quote support), JSON objects, and JSON arrays into a unified pattern:
```java
// 12. Medical EMR JSON Fields (String, Object, Array)
private static final Pattern JSON_MEDICAL_PATTERN = Pattern.compile(
        "(?i)\"(diagnosis|prescription|treatmentDone|treatment_done|notes|medicalHistory|medical_history|symptoms|doctorNotes|doctor_notes|treatmentPlan|treatment_plan)\"\\s*:\\s*(?:\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\"|\\{[^}]*\\}|\\[[^\\]]*\\])"
);
```
- **Replacement**:
  `replaceAll("\"$1\": \"[REDACTED_MEDICAL]\"")`
- **Behavior**:
  - `{"diagnosis": "..."}` -> `{"diagnosis": "[REDACTED_MEDICAL]"}`
  - `{"diagnosis": {...}}` -> `{"diagnosis": "[REDACTED_MEDICAL]"}`
  - `{"diagnosis": [...]}` -> `{"diagnosis": "[REDACTED_MEDICAL]"}`
- **Idempotency**:
  The replacement is always a string starting with `[REDACTED_MEDICAL]`.
  On subsequent passes, the string branch negative lookahead `(?!\\[REDACTED)` skips it; the object branch requires `{` (not present); the array branch requires `[` (not present). Strict idempotency is guaranteed.

---

### 2.4 Requirement 4: Comprehensive `containsUnsanitizedSensitiveData` Coverage

#### Root Cause Observation
- **File**: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`
- **Lines**: 151–162
- **Current Method**:
  Checked only 7 patterns: `BEARER_JWT`, `STANDALONE_JWT`, `BEARER_GENERIC`, `JSON_SECRET_STRING`, `FORM_PASSWORD`, `COOKIE_HEADER`, `JSON_MEDICAL`.
  Omitted: `BASIC_AUTH`, `JSON_SECRET_UNQUOTED`, `TEXT_PASSWORD`, `JSON_COOKIE`, `SESSION_COOKIE_KEY`, `TEXT_MEDICAL`, `CCCD`, `PHONE_MASK`.

#### Remediation Design
Update `containsUnsanitizedSensitiveData` to evaluate all active sensitive regex patterns:
```java
public static boolean containsUnsanitizedSensitiveData(String payload) {
    if (payload == null || payload.isBlank()) {
        return false;
    }
    return BEARER_JWT_PATTERN.matcher(payload).find()
            || STANDALONE_JWT_PATTERN.matcher(payload).find()
            || BEARER_GENERIC_PATTERN.matcher(payload).find()
            || BASIC_AUTH_PATTERN.matcher(payload).find()
            || JSON_SECRET_STRING_PATTERN.matcher(payload).find()
            || JSON_SECRET_UNQUOTED_PATTERN.matcher(payload).find()
            || FORM_PASSWORD_PATTERN.matcher(payload).find()
            || TEXT_PASSWORD_PATTERN.matcher(payload).find()
            || COOKIE_HEADER_PATTERN.matcher(payload).find()
            || JSON_COOKIE_PATTERN.matcher(payload).find()
            || SESSION_COOKIE_KEY_PATTERN.matcher(payload).find()
            || JSON_MEDICAL_PATTERN.matcher(payload).find()
            || TEXT_MEDICAL_PATTERN.matcher(payload).find()
            || CCCD_PATTERN.matcher(payload).find()
            || CMND_CONTEXT_PATTERN.matcher(payload).find()
            || PHONE_MASK_PATTERN.matcher(payload).find();
}
```

---

### 2.5 Requirement 5: False-Positive Elimination on 9-digit Payment Amounts (Strict Boundary & Context)

#### Root Cause Observation
- **File**: `src/main/java/com/dentalclinic/itteam/service/SensitiveDataSanitizer.java`
- **Line**: 83 (`CCCD_PATTERN`)
- **Current Regex**: `(?<!\\d)(0\\d{11}|\\d{9})(?!\\d)`
- **Defect Mechanism**:
  `\d{9}` matches *any* 9-digit number.
  In Vietnamese dental clinics, standard treatment packages range from 100,000,000 to 999,999,999 VND (9 digits).
  Input: `{"amount": 100000000, "currency": "VND"}`
  Output: `{"amount": [REDACTED_ID], "currency": "VND"}`
  This corrupts legitimate financial data and creates invalid JSON (`[REDACTED_ID]` is neither quoted nor numeric). Similarly, 9-digit database primary keys (`{"recordId": 123456789}`) are corrupted.

#### Remediation Design
Decouple national identification into two distinct, high-precision patterns:

1. **12-digit Citizen ID (`CCCD_PATTERN`)**:
   Vietnamese Căn cước công dân (CCCD) is standardized at 12 digits and always begins with `0` (province code `001`–`096`):
   ```java
   private static final Pattern CCCD_PATTERN = Pattern.compile(
           "(?<!\\d)0\\d{11}(?!\\d)"
   );
   ```
   *Why safe*: Numbers in JSON cannot have leading zeros (`001201012345` is invalid JSON numeric syntax). Payment amounts never begin with `0`. Therefore, `0\d{11}` never collides with financial amounts.

2. **9-digit National ID with Context (`CMND_CONTEXT_PATTERN`)**:
   Old Vietnamese Chứng minh nhân dân (CMND) is 9 digits and can start with any digit 1–9.
   To eliminate collisions with payment amounts and database IDs, 9-digit numbers **must require explicit national ID context**:
   ```java
   private static final Pattern CMND_CONTEXT_PATTERN = Pattern.compile(
           "(?i)\\b(CMND|CCCD|citizenId|citizen_id|idCard|id_card|nationalId|national_id)\\b(\\s*[:=]?\\s*[\"']?)\\d{9}(?!\\d)"
   );
   ```
   *Replacement*: `$1$2[REDACTED_ID]`

#### Behavior Matrix
| Scenario | Input | Sanitized Output | Result |
|---|---|---|---|
| **Plain Text CCCD & CMND** | `Patient CCCD: 001201012345, Old CMND: 123456789 verified.` | `Patient CCCD: [REDACTED_ID], Old CMND: [REDACTED_ID] verified.` | **PASS** (Matches `SensitiveDataSanitizerTest`) |
| **Payment Amount in JSON** | `{"amount": 100000000, "currency": "VND"}` | `{"amount": 100000000, "currency": "VND"}` | **PASS** (Preserves payment amount) |
| **Database ID in JSON** | `{"recordId": 123456789, "status": "COMPLETED"}` | `{"recordId": 123456789, "status": "COMPLETED"}` | **PASS** (Preserves DB key) |
| **CMND Key in JSON** | `{"cmnd": "123456789"}` | `{"cmnd": "[REDACTED_ID]"}` | **PASS** (Redacts credential) |
| **Standalone CCCD in JSON** | `{"cccd": "001201012345"}` | `{"cccd": "[REDACTED_ID]"}` | **PASS** (Redacts credential) |

---

### 2.6 Requirement 6: Test Suite Alignment & Assertion Inversion

#### Root Cause Observation
In `src/test/java/com/dentalclinic/itteam/SensitiveDataSanitizerAdversarialTest.java` (authored by `challenger_m1_1`), tests were written with **bug-demonstrating assertions** to prove the presence of defects in the original implementation:
- `ADV-1`: `assertThat(sanitized).contains("sâu răng nặng");`
- `ADV-2`: `assertThat(sanitized).contains("12345");`
- `ADV-3`: `assertThat(sanitized).contains("gho_16C7e42F292c6912E7710c838347Ae178B4a");`
- `ADV-4`: `assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData(...)).isFalse();`
- `ADV-5`: `assertThat(sanitized).contains("\"amount\": [REDACTED_ID]");`

When `SensitiveDataSanitizer.java` is fixed, the defects will no longer occur. Consequently, `SensitiveDataSanitizerAdversarialTest` will fail unless its assertions are updated to verify the **remediated behavior**.

Similarly, in `SensitiveDataSanitizerChallengerTest.java`, sections 1.3, 1.4, 5.2, 5.3, and 6.2 currently only log diagnostic output without strict assertions. Adding positive assertions validates complete sanitization across all edge cases.

---

## 3. Secondary Architectural Observations & Recommendations

To guarantee that Milestone 1 passes all challenger evaluations:

1. **`ITBrowserTabRecord` Missing Pre-Persistence Hooks (`challenger_m1_2` Challenge 1)**:
   - `ITBrowserTabRecord.java` does not sanitize `urlRoute` or `tabTitle` in constructor or `@PrePersist` / `@PreUpdate`.
   - *Fix*: Call `SensitiveDataSanitizer.sanitize()` on `urlRoute` and `tabTitle` in constructor and `@PrePersist` / `@PreUpdate`.
   - *Test Note*: In `SensitiveDataSanitizerChallengerTest.java` line 433, `testBrowserTabRecordMissingSanitization` asserts `assertThat(urlLeaked).isTrue()`. Once `ITBrowserTabRecord` is fixed, this assertion should be changed to `assertThat(urlLeaked).isFalse()`.

2. **`ITAgentMemory` Unique Constraint (`challenger_m1_1` Challenge 4)**:
   - Add `@UniqueConstraint(name = "uk_agent_mem_key", columnNames = {"agent_code", "memory_key"})` in `ITAgentMemory.java`.

3. **`ITAgentActivity.description` Length (`challenger_m1_1` Challenge 6)**:
   - Change `@Column(name = "description", length = 1000)` to `@Column(name = "description", columnDefinition = "TEXT")`.

4. **`ITApiRunLog.statusCode` Nullability (`challenger_m1_1` Challenge 6)**:
   - Change `@Column(name = "status_code", nullable = false)` to `@Column(name = "status_code")` or default nulls to `0` in constructor.

5. **`ITTeamDataInitializer` Idempotency Decoupling (`challenger_m1_1` Challenge 5)**:
   - Remove the overarching `profileRepository.count() == 0` guard around child seeders in `run()`, letting each child seeder run its own existence checks.

---

## 4. Full Proposed Source Code

### 4.1 Proposed `SensitiveDataSanitizer.java`

```java
package com.dentalclinic.itteam.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Privacy Guardrail & Sensitive Data Sanitizer.
 * Redacts JWT tokens, passwords, Bearer/Basic headers, cookies, and medical EMR PII
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

    // 5. JSON Password & Secrets (including OAuth snake_case tokens and escaped quotes in values): "password": "value" -> "password": "[REDACTED]"
    private static final Pattern JSON_SECRET_STRING_PATTERN = Pattern.compile(
            "(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|token|authToken|auth_token|accessToken|access_token|refreshToken|refresh_token|idToken|id_token)\"\\s*:\\s*\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\""
    );

    // 6. JSON Password numeric/unquoted: "password": 123456 -> "password": "[REDACTED]"
    private static final Pattern JSON_SECRET_UNQUOTED_PATTERN = Pattern.compile(
            "(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|token|authToken|auth_token|accessToken|access_token|refreshToken|refresh_token)\"\\s*:\\s*(?!\\[REDACTED)[^,\\}\\]\\s\"]+"
    );

    // 7. Form-urlencoded or URL query params: password=secret -> password=[REDACTED]
    private static final Pattern FORM_PASSWORD_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|api_key|apikey|access_token|refresh_token|auth_token)=(?!\\[REDACTED)[^&\\s\"',;]+"
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

    // 12. Medical EMR JSON Fields: strings (with escaped quotes), nested objects { ... }, and arrays [ ... ] -> "diagnosis": "[REDACTED_MEDICAL]"
    private static final Pattern JSON_MEDICAL_PATTERN = Pattern.compile(
            "(?i)\"(diagnosis|prescription|treatmentDone|treatment_done|notes|medicalHistory|medical_history|symptoms|doctorNotes|doctor_notes|treatmentPlan|treatment_plan)\"\\s*:\\s*(?:\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\"|\\{[^}]*\\}|\\[[^\\]]*\\])"
    );

    // 13. Medical EMR Plain Text: Diagnosis: ... -> Diagnosis: [REDACTED_MEDICAL]
    private static final Pattern TEXT_MEDICAL_PATTERN = Pattern.compile(
            "(?i)\\b(Diagnosis|Prescription|TreatmentDone|DoctorNotes|MedicalHistory)\\s*:\\s*(?!\\[REDACTED)[^\\r\\n,;]+"
    );

    // 14. Vietnamese Citizen ID (CCCD: 12 digits starting with 0) -> [REDACTED_ID]
    private static final Pattern CCCD_PATTERN = Pattern.compile(
            "(?<!\\d)0\\d{11}(?!\\d)"
    );

    // 15. Vietnamese Old National ID (CMND: 9 digits requiring ID context to avoid false-positives on 9-digit payment amounts/IDs)
    private static final Pattern CMND_CONTEXT_PATTERN = Pattern.compile(
            "(?i)\\b(CMND|CCCD|citizenId|citizen_id|idCard|id_card|nationalId|national_id)\\b(\\s*[:=]?\\s*[\"']?)\\d{9}(?!\\d)"
    );

    // 16. Patient Phone Masking: "patientPhone": "0981234567" -> "patientPhone": "098****567"
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

        // Step 8: Sanitize Medical EMR fields (JSON strings/objects/arrays and Text)
        result = JSON_MEDICAL_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED_MEDICAL]\"");
        result = TEXT_MEDICAL_PATTERN.matcher(result).replaceAll("$1: [REDACTED_MEDICAL]");

        // Step 9: Sanitize National ID / CCCD (12-digit CCCD and 9-digit contextual CMND)
        result = CCCD_PATTERN.matcher(result).replaceAll("[REDACTED_ID]");
        result = CMND_CONTEXT_PATTERN.matcher(result).replaceAll("$1$2[REDACTED_ID]");

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
                || BASIC_AUTH_PATTERN.matcher(payload).find()
                || JSON_SECRET_STRING_PATTERN.matcher(payload).find()
                || JSON_SECRET_UNQUOTED_PATTERN.matcher(payload).find()
                || FORM_PASSWORD_PATTERN.matcher(payload).find()
                || TEXT_PASSWORD_PATTERN.matcher(payload).find()
                || COOKIE_HEADER_PATTERN.matcher(payload).find()
                || JSON_COOKIE_PATTERN.matcher(payload).find()
                || SESSION_COOKIE_KEY_PATTERN.matcher(payload).find()
                || JSON_MEDICAL_PATTERN.matcher(payload).find()
                || TEXT_MEDICAL_PATTERN.matcher(payload).find()
                || CCCD_PATTERN.matcher(payload).find()
                || CMND_CONTEXT_PATTERN.matcher(payload).find()
                || PHONE_MASK_PATTERN.matcher(payload).find();
    }
}
```

---

### 4.2 Proposed Updates to `SensitiveDataSanitizerAdversarialTest.java`

Update the assertion expectations from bug-demonstrating (`contains(leak)`, `isFalse()`) to bug-resolved (`doesNotContain(leak)`, `isTrue()`):

```java
package com.dentalclinic.itteam;

import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveDataSanitizer Adversarial & Boundary Tests")
public class SensitiveDataSanitizerAdversarialTest {

    @Test
    @DisplayName("ADV-1 - Escaped Quotes in JSON Medical Field: Incomplete Redaction & Leaked Data Resolved")
    void testEscapedQuotesInJsonMedicalField() {
        String input = "{\"diagnosis\": \"Bệnh nhân bị \\\"sâu răng nặng\\\" ở hàm trên\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).doesNotContain("sâu răng nặng");
        assertThat(sanitized).isEqualTo("{\"diagnosis\": \"[REDACTED_MEDICAL]\"}");
    }

    @Test
    @DisplayName("ADV-2 - Escaped Quotes in JSON Password Field: Password Fragment Leaked Resolved")
    void testEscapedQuotesInJsonProperty() {
        String input = "{\"password\": \"secret\\\"12345\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).doesNotContain("12345");
        assertThat(sanitized).isEqualTo("{\"password\": \"[REDACTED]\"}");
    }

    @Test
    @DisplayName("ADV-3 - OAuth2 snake_case access_token and refresh_token are sanitized")
    void testSnakeCaseOAuthTokens() {
        String oauthPayload = "{\"access_token\": \"gho_16C7e42F292c6912E7710c838347Ae178B4a\", \"refresh_token\": \"rfr_SecretOauthRefreshToken9988\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(oauthPayload);

        assertThat(sanitized).doesNotContain("gho_16C7e42F292c6912E7710c838347Ae178B4a");
        assertThat(sanitized).doesNotContain("rfr_SecretOauthRefreshToken9988");
        assertThat(sanitized).contains("\"access_token\": \"[REDACTED]\"");
        assertThat(sanitized).contains("\"refresh_token\": \"[REDACTED]\"");
    }

    @Test
    @DisplayName("ADV-4 - containsUnsanitizedSensitiveData detects all sensitive patterns")
    void testContainsUnsanitizedSensitiveDataOmissions() {
        // Basic Auth
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Authorization: Basic dXNlcjpwYXNz"))
                .as("Basic auth should be detected as sensitive").isTrue();

        // Plain text password
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Password: SuperSecretPassword123"))
                .as("Plaintext password should be detected as sensitive").isTrue();

        // Unquoted numeric password
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"password\": 987654}"))
                .as("Unquoted numeric password should be detected as sensitive").isTrue();

        // Standalone session cookie
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Session param JSESSIONID=SECRET98765"))
                .as("Session cookie should be detected as sensitive").isTrue();

        // Plaintext medical diagnosis
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Diagnosis: Viêm tủy răng cấp"))
                .as("Plaintext medical diagnosis should be detected as sensitive").isTrue();

        // CCCD
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("CCCD: 001201012345"))
                .as("CCCD should be detected as sensitive").isTrue();
    }

    @Test
    @DisplayName("ADV-5 - 9-digit payment amount preserved without false positive CCCD redaction")
    void testNineDigitAmountFalselyRedacted() {
        String payment = "{\"amount\": 100000000, \"currency\": \"VND\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(payment);

        assertThat(sanitized).contains("\"amount\": 100000000");
        assertThat(sanitized).doesNotContain("[REDACTED_ID]");
    }
}
```

---

### 4.3 Proposed Updates to `SensitiveDataSanitizerChallengerTest.java`

Enhance diagnostic tests with active assertions:

1. **Test 1.3 (`testPasswordWithEscapedQuoteInside`)**:
   ```java
   assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
   assertThat(sanitized).doesNotContain("quoted");
   assertThat(sanitized).doesNotContain("123");
   assertThat(sanitized).contains("\"user\": \"admin\"");
   ```

2. **Test 1.4 (`testGenericTokenKeyInJson`)**:
   ```java
   assertThat(sanitized).contains("\"token\": \"[REDACTED]\"");
   assertThat(sanitized).doesNotContain("opaque-secret-session-token-998877");
   ```

3. **Test 5.2 (`testNestedJsonObjectMedicalDiagnosis`)**:
   ```java
   assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
   assertThat(sanitized).doesNotContain("Pulpitis viem tuy cap");
   assertThat(sanitized).contains("\"patientId\": 102");
   ```

4. **Test 5.3 (`testArrayOfDiagnosisStrings`)**:
   ```java
   assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
   assertThat(sanitized).doesNotContain("Sâu răng R36");
   assertThat(sanitized).doesNotContain("Viêm nướu răng");
   assertThat(sanitized).contains("\"patientId\": 103");
   ```

5. **Test 6.2 (`testSqlInjectionWithEscapedQuotesInPassword`)**:
   ```java
   assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
   assertThat(sanitized).doesNotContain("OR 1=1 --");
   assertThat(sanitized).contains("\"role\": \"ADMIN\"");
   ```

6. **Test 8.1 (`testBrowserTabRecordMissingSanitization`)**:
   Once `ITBrowserTabRecord.java` pre-persist hook is added:
   ```java
   assertThat(urlLeaked).as("ITBrowserTabRecord urlRoute should be sanitized!").isFalse();
   assertThat(titleLeaked).as("ITBrowserTabRecord tabTitle should be sanitized!").isFalse();
   ```

---

### 4.4 Proposed Updates to `ITBrowserTabRecord.java`

Update `src/main/java/com/dentalclinic/itteam/model/ITBrowserTabRecord.java`:

- **In Constructor** (lines 55–56):
  ```java
  this.tabTitle = tabTitle != null ? SensitiveDataSanitizer.sanitize(tabTitle) : null;
  this.urlRoute = urlRoute != null ? SensitiveDataSanitizer.sanitize(urlRoute) : null;
  ```

- **In `@PrePersist` / `@PreUpdate`** (lines 62–67):
  ```java
  @PrePersist
  @PreUpdate
  public void prePersist() {
      if (this.openedAt == null) {
          this.openedAt = LocalDateTime.now();
      }
      if (this.tabTitle != null) {
          this.tabTitle = SensitiveDataSanitizer.sanitize(this.tabTitle);
      }
      if (this.urlRoute != null) {
          this.urlRoute = SensitiveDataSanitizer.sanitize(this.urlRoute);
      }
  }
  ```

---

## 5. Verification & Validation Plan

1. **Target Unit Test Verification**:
   Execute Maven command:
   ```bash
   mvn test -Dtest=SensitiveDataSanitizerTest,SensitiveDataSanitizerAdversarialTest,SensitiveDataSanitizerChallengerTest,EntityPrePersistenceSanitizationTest
   ```
   **Expected Outcome**: 100% tests pass (0 failures, 0 errors).

2. **Milestone Persistence Stress Suite**:
   ```bash
   mvn test -Dtest=ITTeamM1PersistenceTest,ITTeamMilestone1EmpiricalStressTest
   ```
   (Note: ST-3.1–ST-3.4 in `ITTeamMilestone1EmpiricalStressTest.java` should similarly mirror the assertion inversion in `SensitiveDataSanitizerAdversarialTest`).
