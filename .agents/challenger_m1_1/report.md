# Milestone 1 Empirical Challenge Report

**Agent:** `challenger_m1_1` (EMPIRICAL CHALLENGER — critic, specialist)  
**Milestone:** M1 — Domain Model & Database Persistence  
**Date:** 2026-09-12  
**Target Codebase:** `src/main/java/com/dentalclinic/itteam/`  
**Worker Under Review:** `worker_m1_1`  
**Overall Risk Assessment:** **HIGH**  
**Verdict:** **REQUEST_CHANGES**  

---

## 1. Executive Summary

Milestone 1 delivered 6 JPA entities, 6 Spring Data JPA repositories, `ITTeamDataInitializer`, and `SensitiveDataSanitizer`.
An empirical stress-testing suite (`ITTeamMilestone1EmpiricalStressTest.java` and `SensitiveDataSanitizerAdversarialTest.java`) was authored and executed against the implementation to evaluate:
1. `ITTeamDataInitializer` idempotency across multiple invocations, restarts, and partial states.
2. Model field validations, column lengths, and nullability constraints.
3. `SensitiveDataSanitizer` privacy guardrails under adversarial inputs (escaped JSON quotes, OAuth snake_case tokens, and false positives).

While standard sequential invocations of `ITTeamDataInitializer.run()` successfully prevent duplicate records due to the top-level count check, empirical challenge revealed **5 critical vulnerabilities and defects** across persistence constraints, schema definitions, and privacy regex patterns that require resolution before progressing to Milestone 2 & 3.

---

## 2. Empirical Challenge Findings

### Challenge 1: Privacy Leak & JSON Syntax Corruption via Escaped Quotes in SensitiveDataSanitizer
- **Severity**: **CRITICAL** (HIPAA / Privacy Guardrail Violation)
- **Component**: `com.dentalclinic.itteam.service.SensitiveDataSanitizer` (Lines 39, 74)
- **Empirical Test**: `SensitiveDataSanitizerAdversarialTest#testEscapedQuotesInJsonMedicalField`, `SensitiveDataSanitizerAdversarialTest#testEscapedQuotesInJsonProperty`
- **Observation**:
  `JSON_MEDICAL_PATTERN` and `JSON_SECRET_STRING_PATTERN` use `\"(?!\\[REDACTED)[^\"]*\"`.
  When a JSON payload contains escaped quotes `\"` within a field value, e.g.:
  ```json
  {"diagnosis": "Bệnh nhân bị \"sâu răng nặng\" ở hàm trên"}
  ```
  The regex character class `[^\"]*` halts at the first quote preceding `sâu răng nặng`.
  The replacement string yields:
  ```json
  {"diagnosis": "[REDACTED_MEDICAL]sâu răng nặng\" ở hàm trên"}
  ```
- **Blast Radius**:
  1. The clinical diagnosis (`sâu răng nặng`) is **leaked unredacted** into logs and persistent storage, violating requirement §R1.
  2. The resulting JSON is malformed and causes `JsonParseException` when downstream UI or services parse it.
- **Remediation**:
  Use proper JSON string matching that accounts for escaped quotes: `\"(?!\\[REDACTED)(?:\\\\.|[^\"])*\"`.

---

### Challenge 2: Omission of Standard OAuth2 snake_case Tokens (`access_token`, `refresh_token`)
- **Severity**: **HIGH** (Security Credential Leakage)
- **Component**: `com.dentalclinic.itteam.service.SensitiveDataSanitizer` (Line 39)
- **Empirical Test**: `SensitiveDataSanitizerAdversarialTest#testSnakeCaseOAuthTokens`
- **Observation**:
  Line 39 specifies:
  ```java
  "(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|refreshToken|accessToken)\"..."
  ```
  Notice that while `apiKey` and `api_key` are both present, `refreshToken` and `accessToken` are only provided in camelCase. The official RFC 6749 OAuth2 keys `access_token` and `refresh_token` are missing.
  In input:
  ```json
  {"access_token": "gho_16C7e42F292c6912E7710c838347Ae178B4a", "refresh_token": "rfr_SecretOauthRefreshToken9988"}
  ```
  Both tokens pass through 100% unredacted.
- **Blast Radius**: OAuth2 tokens returned by authentication providers or external APIs logged in `ITApiRunLog` will be stored in plaintext.
- **Remediation**:
  Include `access_token` and `refresh_token` in the alternation: `(password|...|refreshToken|refresh_token|accessToken|access_token)`.

---

### Challenge 3: Incomplete Security Audit Method (`containsUnsanitizedSensitiveData`)
- **Severity**: **HIGH** (False Sense of Security / Audit Blindspot)
- **Component**: `com.dentalclinic.itteam.service.SensitiveDataSanitizer` (Lines 151–162)
- **Empirical Test**: `SensitiveDataSanitizerAdversarialTest#testContainsUnsanitizedSensitiveDataOmissions`
- **Observation**:
  `containsUnsanitizedSensitiveData(String payload)` tests only 7 out of the 15 patterns declared in the class. It completely omits:
  1. `BASIC_AUTH_PATTERN` (Basic authorization headers)
  2. `TEXT_PASSWORD_PATTERN` (Plaintext passwords)
  3. `JSON_SECRET_UNQUOTED_PATTERN` (Unquoted passwords)
  4. `SESSION_COOKIE_KEY_PATTERN` (`JSESSIONID=...`)
  5. `TEXT_MEDICAL_PATTERN` (`Diagnosis: ...`)
  6. `CCCD_PATTERN` (Citizen ID)
  7. `PHONE_MASK_PATTERN` (Patient phone)
- **Blast Radius**: Calling `containsUnsanitizedSensitiveData("Authorization: Basic dXNlcjpwYXNz")` returns `false`, erroneously claiming the payload contains no sensitive data.
- **Remediation**:
  Update `containsUnsanitizedSensitiveData` to evaluate all active sensitive regex patterns.

---

### Challenge 4: Missing Database-Level Unique Constraint on `ITAgentMemory` & Potential Crash
- **Severity**: **MEDIUM**
- **Component**: `com.dentalclinic.itteam.model.ITAgentMemory`, `com.dentalclinic.itteam.repository.ITAgentMemoryRepository`
- **Empirical Test**: `ITTeamMilestone1EmpiricalStressTest#testDuplicateMemoryKeyCrash`
- **Observation**:
  `ITAgentMemory` defines indexes on `agent_code` and `memory_key`, but has NO unique constraint on `(agent_code, memory_key)` in the `@Table` annotation.
  While `ITTeamDataInitializer` performs an application-level check (`existsByAgentCodeAndMemoryKey`), any concurrent write or direct API call via `POST /api/it-team/memories` can insert duplicate rows.
  When duplicate rows exist, calling `ITAgentMemoryRepository.findByAgentCodeAndMemoryKey(agentCode, key)` fails with:
  ```
  org.springframework.dao.IncorrectResultSizeDataAccessException: Query did not return a unique result: 2 results were returned
  ```
- **Blast Radius**: Unhandled 500 error in M3 REST controller when retrieving or updating a memory record.
- **Remediation**:
  Add `@Table(name = "it_agent_memory", uniqueConstraints = @UniqueConstraint(name = "uk_agent_mem_key", columnNames = {"agent_code", "memory_key"}), ...)` to `ITAgentMemory.java`.

---

### Challenge 5: Fragile Partial-State Seeding in `ITTeamDataInitializer`
- **Severity**: **MEDIUM**
- **Component**: `com.dentalclinic.itteam.config.ITTeamDataInitializer` (Line 53)
- **Empirical Test**: `ITTeamMilestone1EmpiricalStressTest#testPartialStateSkipFailure`
- **Observation**:
  `ITTeamDataInitializer.run()` encapsulates all child seeding (`seedAgentMemories`, `seedBrowserTabs`, `seedInitialMessages`, `seedInitialActivities`) inside:
  ```java
  if (profileRepository.count() == 0) { ... }
  ```
  If an environment already has profiles (e.g. seeded previously or manually created), but memories or tabs were wiped, reset, or failed during initial bootstrap, `run()` skips all child seeders completely.
- **Blast Radius**: The system can become stuck in a partially seeded state where agents exist without memories or browser tabs.
- **Remediation**:
  Remove the monolithic `profileRepository.count() == 0` guard, and let each individual seeder method run its own idempotency check (`if (!memoryRepository.existsByAgentCodeAndMemoryKey(...))`).

---

### Challenge 6: Schema Field Constraints in `ITAgentActivity` and `ITApiRunLog`
- **Severity**: **MEDIUM**
- **Component**: `ITAgentActivity` (Line 34), `ITApiRunLog` (Line 31)
- **Empirical Test**: `ITTeamMilestone1EmpiricalStressTest#testActivityDescriptionExceeds1000`, `ITTeamMilestone1EmpiricalStressTest#testApiRunLogNullStatusCodeFails`
- **Observation**:
  1. `ITAgentActivity.description` is limited to `@Column(length = 1000)`. When logging large diagnostic stack traces or command outputs, text > 1000 chars triggers `DataIntegrityViolationException`. It should use `columnDefinition = "TEXT"`, consistent with `ITAgentMemory.memoryContent` and `ITAgentMessage.messageBody`.
  2. `ITApiRunLog.statusCode` is marked `@Column(nullable = false)`. In real-world API test runners (M3 `ITApiRunnerService`), connection timeouts, connection refused, or SSRF rejections occur BEFORE receiving an HTTP status code. If `statusCode` is null, persisting the log entry fails with `PropertyValueException`.
- **Remediation**:
  1. Set `columnDefinition = "TEXT"` on `ITAgentActivity.description`.
  2. Allow `statusCode` to be nullable, or default to 0 in constructor when null.

---

### Challenge 7: False Positive on 9-Digit Transaction Amounts by `CCCD_PATTERN`
- **Severity**: **LOW**
- **Component**: `com.dentalclinic.itteam.service.SensitiveDataSanitizer` (Line 83)
- **Empirical Test**: `SensitiveDataSanitizerAdversarialTest#testNineDigitAmountFalselyRedacted`
- **Observation**:
  `CCCD_PATTERN = Pattern.compile("(?<!\\d)(0\\d{11}|\\d{9})(?!\\d)")`.
  In a dental clinic system, Vietnamese payment amounts are frequently 9 digits (e.g. `100000000` VND for orthodontic packages).
  `amount: 100000000` becomes `amount: [REDACTED_ID]`.
- **Remediation**:
  Refine `CCCD_PATTERN` to avoid matching numeric values inside JSON keys like `"amount"`, `"price"`, `"total"`, `"balance"` or require CCCD/CMND contextual prefixes.

---

## 3. Stress Test Results Matrix

| Test Case | Scenario | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|---|
| **ST-1.1** | Sequential double invocation of `ITTeamDataInitializer` | No duplicate profiles, memories, tabs | Count remains 5 profiles, 12 memories, 5 tabs | **PASS** |
| **ST-1.2** | Partial state (profiles exist, memories wiped) | Seeder reconciles and restores memories | Seeder completely skips memories (count remains 0) | **FAIL (Defect)** |
| **ST-1.3** | Insert duplicate memory key `(backend, CUSTOM_KEY)` | DB rejects or query handles safely | DB allows insert; `findByAgentCodeAndMemoryKey` throws `IncorrectResultSizeDataAccessException` | **FAIL (Defect)** |
| **ST-2.1** | Save orphaned memory with null agentCode/agentId | Validated or rejected | Persisted with null references (no nullability constraint) | **OBSERVED** |
| **ST-2.2** | Save memory with null `memoryContent` | Rejected by DB | `DataIntegrityViolationException` thrown | **PASS** |
| **ST-2.3** | Save activity with description > 1000 chars | Large diagnostic logs handled | Truncation failure (`DataIntegrityViolationException`) | **FAIL (Defect)** |
| **ST-2.4** | Save API run log with null `statusCode` | Connection timeouts recorded | `PropertyValueException` thrown (cannot log timeout) | **FAIL (Defect)** |
| **ST-3.1** | JSON string with escaped quote in diagnosis | Medical data redacted cleanly | Halts at quote, leaks `sâu răng nặng`, corrupts JSON | **FAIL (Defect)** |
| **ST-3.2** | OAuth2 snake_case `access_token` and `refresh_token` | Tokens redacted to `[REDACTED]` | Tokens completely unredacted | **FAIL (Defect)** |
| **ST-3.3** | `containsUnsanitizedSensitiveData` on Basic auth & passwords | Returns `true` (secrets detected) | Returns `false` (7 patterns omitted) | **FAIL (Defect)** |
| **ST-3.4** | 9-digit payment amount `100000000` | Preserves financial amount | Falsely converted to `[REDACTED_ID]` | **FAIL (Defect)** |

---

## 4. Required Remediation Actions

To achieve approval for Milestone 1:

1. **Fix `SensitiveDataSanitizer` Regex Patterns**:
   - Update `JSON_SECRET_STRING_PATTERN` and `JSON_MEDICAL_PATTERN` to support escaped quotes: `(?:\\\\.|[^\"])*`.
   - Add `access_token` and `refresh_token` to `JSON_SECRET_STRING_PATTERN`.
   - Include all 15 sensitive patterns in `containsUnsanitizedSensitiveData`.
2. **Harden JPA Entity Schemas**:
   - Add unique constraint `@UniqueConstraint(name = "uk_agent_mem_key", columnNames = {"agent_code", "memory_key"})` in `ITAgentMemory`.
   - Change `ITAgentActivity.description` to `columnDefinition = "TEXT"`.
   - Make `ITApiRunLog.statusCode` nullable or default nulls to `0`.
3. **Decouple Seeder Idempotency in `ITTeamDataInitializer`**:
   - Remove the overarching `profileRepository.count() == 0` guard in `run()`. Allow each sub-method to run independently with its own existence checks.
