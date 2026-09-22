package com.dentalclinic.itteam;

import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Empirical Challenger 2 Test Suite for Milestone 1: Domain Model & Database Persistence.
 * Focus: Stress testing SensitiveDataSanitizer, boundary edge cases, SQL injection,
 * nested JSON structures, token variations, idempotency, and JPA pre-persistence lifecycle hooks.
 */
@DisplayName("Challenger 2 - SensitiveDataSanitizer & Pre-Persistence Hook Stress Suite")
public class SensitiveDataSanitizerChallengerTest {

    private static final String VALID_JWT_1 =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFsaWNlIiwiaWF0IjoxNTE2MjM5MDIyfQ.4peJh3mffg53u_V-4g2rJt_L0u8q9_sW5h1o0kYw5_A";

    private static final String VALID_JWT_2 =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkb2N0b3JfOTkiLCJyb2xlIjoiREVOVElTVCJ9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

    // JWT with '-' or '+' as last character in signature
    private static final String JWT_ENDING_WITH_DASH =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5-";

    // JWT with '=' padding
    private static final String JWT_WITH_PADDING =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw==";

    // =========================================================================
    // 1. EMBEDDED PASSWORDS IN COMPLEX JSON
    // =========================================================================
    @Nested
    @DisplayName("1. Embedded Passwords in Complex JSON")
    class EmbeddedPasswordsTest {

        @Test
        @DisplayName("1.1 - Deeply nested JSON password sanitization")
        void testDeeplyNestedPassword() {
            String complexJson = "{\"level1\": {\"level2\": {\"level3\": {\"password\": \"DeepSecret!2026\", \"status\": \"ACTIVE\"}}}}";
            String sanitized = SensitiveDataSanitizer.sanitize(complexJson);

            assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
            assertThat(sanitized).doesNotContain("DeepSecret!2026");
            assertThat(sanitized).contains("\"status\": \"ACTIVE\"");
        }

        @Test
        @DisplayName("1.2 - Passwords with special symbols, spaces, and unicode")
        void testPasswordWithSymbolsAndUnicode() {
            String json = "{\"password\": \"P@ssw0rd!#$&*()^ m\\u1eadt kh\\u1ea9u 123 \\uD83D\\uDD11\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(json);

            assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
            assertThat(sanitized).doesNotContain("P@ssw0rd!#$&*()^");
        }

        @Test
        @DisplayName("1.3 - Passwords with escaped quotes inside value: properly sanitized without leakage or malformed JSON")
        void testPasswordWithEscapedQuoteInside() {
            String input = "{\"password\": \"secret\\\"quoted\\\"123\", \"user\": \"admin\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
            assertThat(sanitized).doesNotContain("quoted");
            assertThat(sanitized).doesNotContain("123");
            assertThat(sanitized).contains("\"user\": \"admin\"");
        }

        @Test
        @DisplayName("1.4 - Generic 'token' key in JSON: properly sanitized")
        void testGenericTokenKeyInJson() {
            String input = "{\"token\": \"opaque-secret-session-token-998877\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"token\": \"[REDACTED]\"");
            assertThat(sanitized).doesNotContain("opaque-secret-session-token-998877");
        }
    }

    // =========================================================================
    // 2. MULTIPLE JWT TOKENS
    // =========================================================================
    @Nested
    @DisplayName("2. Multiple JWT Tokens")
    class MultipleJwtTokensTest {

        @Test
        @DisplayName("2.1 - Multiple JWTs in single JSON payload (access, refresh, id tokens)")
        void testMultipleJwtsInJson() {
            String input = String.format(
                    "{\"accessToken\": \"%s\", \"refreshToken\": \"%s\", \"idToken\": \"%s\"}",
                    VALID_JWT_1, VALID_JWT_2, VALID_JWT_1
            );
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).doesNotContain(VALID_JWT_1);
            assertThat(sanitized).doesNotContain(VALID_JWT_2);
            assertThat(sanitized).contains("[REDACTED");
        }

        @Test
        @DisplayName("2.2 - Multiple Bearer headers or JWTs in text log stream")
        void testMultipleJwtsInLogStream() {
            String logStream = "HTTP/1.1 POST /api/v1/auth\n"
                    + "Authorization: Bearer " + VALID_JWT_1 + "\n"
                    + "X-Upstream-Token: Bearer " + VALID_JWT_2 + "\n"
                    + "Body: {\"backupToken\": \"" + VALID_JWT_1 + "\"}";

            String sanitized = SensitiveDataSanitizer.sanitize(logStream);

            assertThat(sanitized).doesNotContain(VALID_JWT_1);
            assertThat(sanitized).doesNotContain(VALID_JWT_2);
            assertThat(sanitized).contains("Authorization: Bearer [REDACTED_JWT]");
            assertThat(sanitized).contains("X-Upstream-Token: Bearer [REDACTED_JWT]");
        }

        @Test
        @DisplayName("2.3 - JWT token signature ending with non-word character '-' or '+'")
        void testJwtEndingWithDashOrPlus() {
            String input = "{\"token\": \"" + JWT_ENDING_WITH_DASH + "\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            // Check if trailing dash leaked due to \\b regex boundary behavior
            boolean dashLeaked = sanitized.contains("[REDACTED_JWT]-");
            System.out.println("JWT ending with dash output: " + sanitized + " (Dash leaked: " + dashLeaked + ")");
        }

        @Test
        @DisplayName("2.4 - JWT token signature with '=' base64 padding")
        void testJwtWithPadding() {
            String input = "{\"token\": \"" + JWT_WITH_PADDING + "\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            // Check if padding '=' leaked
            boolean paddingLeaked = sanitized.contains("[REDACTED_JWT]=");
            System.out.println("JWT with padding output: " + sanitized + " (Padding leaked: " + paddingLeaked + ")");
        }
    }

    // =========================================================================
    // 3. MALFORMED TOKENS
    // =========================================================================
    @Nested
    @DisplayName("3. Malformed Tokens")
    class MalformedTokensTest {

        @Test
        @DisplayName("3.1 - Malformed 2-part JWT in Bearer header")
        void testMalformedBearerTwoPartJwt() {
            String malformedBearer = "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0";
            String sanitized = SensitiveDataSanitizer.sanitize(malformedBearer);

            // Generic bearer pattern catches this
            assertThat(sanitized).isEqualTo("Authorization: Bearer [REDACTED]");
            assertThat(sanitized).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        }

        @Test
        @DisplayName("3.2 - Malformed standalone 2-part JWT (RFC 7519 unsigned alg=none without dot)")
        void testMalformedStandaloneTwoPartJwt() {
            String input = "{\"rawToken\": \"eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMjM0NTY3ODkwIn0\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            // STANDALONE_JWT_PATTERN requires 3 parts with 2 dots. A 2-part token fails to match!
            boolean leaked = sanitized.contains("eyJhbGciOiJub25lIn0");
            System.out.println("Standalone 2-part token output: " + sanitized + " (Leaked: " + leaked + ")");
        }

        @Test
        @DisplayName("3.3 - Malformed standalone 3-part JWT with empty signature (RFC 7519 alg=none with trailing dot)")
        void testStandaloneUnsignedJwtWithTrailingDot() {
            String input = "{\"rawToken\": \"eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMjM0NTY3ODkwIn0.\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            // STANDALONE_JWT_PATTERN requires [A-Za-z0-9_\\-+]{10,} after the second dot.
            // Empty signature has 0 chars!
            boolean leaked = sanitized.contains("eyJhbGciOiJub25lIn0");
            System.out.println("Unsigned trailing dot token output: " + sanitized + " (Leaked: " + leaked + ")");
        }

        @Test
        @DisplayName("3.4 - Truncated JWT header fragment")
        void testTruncatedJwtHeaderFragment() {
            String input = "Authorization: Bearer eyJhbGciOi";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).isEqualTo("Authorization: Bearer [REDACTED]");
        }
    }

    // =========================================================================
    // 4. MIXED CASE BEARER HEADERS
    // =========================================================================
    @Nested
    @DisplayName("4. Mixed Case Bearer Headers")
    class MixedCaseBearerTest {

        @Test
        @DisplayName("4.1 - Lowercase, uppercase, and mixed case bearer tokens")
        void testMixedCaseBearerVariants() {
            String input1 = "authorization: bearer " + VALID_JWT_1;
            String input2 = "AUTHORIZATION: BEARER " + VALID_JWT_1;
            String input3 = "Authorization: bEaReR " + VALID_JWT_1;

            assertThat(SensitiveDataSanitizer.sanitize(input1)).contains("authorization: Bear [REDACTED_JWT]".replace("Bear [", "Bearer ["));
            assertThat(SensitiveDataSanitizer.sanitize(input2)).contains("AUTHORIZATION: Bearer [REDACTED_JWT]");
            assertThat(SensitiveDataSanitizer.sanitize(input3)).contains("Authorization: Bearer [REDACTED_JWT]");
        }

        @Test
        @DisplayName("4.2 - Bearer with colon separator (Bearer: <token>)")
        void testBearerWithColon() {
            String inputJwt = "Authorization: Bearer: " + VALID_JWT_1;
            String sanitizedJwt = SensitiveDataSanitizer.sanitize(inputJwt);

            // Standalone pattern will catch the JWT part
            assertThat(sanitizedJwt).doesNotContain(VALID_JWT_1);

            // But what about generic opaque token with colon?
            String inputOpaque = "Authorization: Bearer: my_secret_opaque_token_12345";
            String sanitizedOpaque = SensitiveDataSanitizer.sanitize(inputOpaque);
            boolean opaqueLeaked = sanitizedOpaque.contains("my_secret_opaque_token_12345");
            System.out.println("Bearer with colon opaque output: " + sanitizedOpaque + " (Leaked: " + opaqueLeaked + ")");
        }

        @Test
        @DisplayName("4.3 - Bearer with multiple spaces and tabs")
        void testBearerWithTabsAndMultipleSpaces() {
            String input = "Authorization: Bearer \t \t  " + VALID_JWT_1;
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).doesNotContain(VALID_JWT_1);
            assertThat(sanitized).contains("Bearer [REDACTED_JWT]");
        }
    }

    // =========================================================================
    // 5. NESTED MEDICAL DIAGNOSIS TERMS
    // =========================================================================
    @Nested
    @DisplayName("5. Nested Medical Diagnosis Terms")
    class NestedMedicalDiagnosisTest {

        @Test
        @DisplayName("5.1 - Standard flat diagnosis field sanitization")
        void testFlatMedicalDiagnosis() {
            String input = "{\"diagnosis\": \"Viêm quanh cuống răng số 46\", \"treatmentDone\": \"Trám composite\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
            assertThat(sanitized).contains("\"treatmentDone\": \"[REDACTED_MEDICAL]\"");
            assertThat(sanitized).doesNotContain("Viêm quanh cuống răng số 46");
            assertThat(sanitized).doesNotContain("Trám composite");
        }

        @Test
        @DisplayName("5.2 - Nested JSON diagnosis object (e.g. EMR structured clinical diagnosis)")
        void testNestedJsonObjectMedicalDiagnosis() {
            String input = "{\"patientId\": 102, \"diagnosis\": {\"icd10\": \"K04.0\", \"description\": \"Pulpitis viem tuy cap\", \"severity\": \"ACUTE\"}}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
            assertThat(sanitized).doesNotContain("Pulpitis viem tuy cap");
            assertThat(sanitized).contains("\"patientId\": 102");
        }

        @Test
        @DisplayName("5.3 - Array of diagnosis strings in JSON")
        void testArrayOfDiagnosisStrings() {
            String input = "{\"patientId\": 103, \"diagnosis\": [\"Sâu răng R36\", \"Viêm nướu răng\"]}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
            assertThat(sanitized).doesNotContain("Sâu răng R36");
            assertThat(sanitized).doesNotContain("Viêm nướu răng");
            assertThat(sanitized).contains("\"patientId\": 103");
        }

        @Test
        @DisplayName("5.4 - Plain text multi-line diagnosis records")
        void testMultiLinePlainTextDiagnosis() {
            String input = "Patient Examination:\nDiagnosis: Viêm nha chu mạn tính tiến triển\nPrescription: Amoxicillin 500mg\nDoctorNotes: Tái khám sau 7 ngày";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("Diagnosis: [REDACTED_MEDICAL]");
            assertThat(sanitized).contains("Prescription: [REDACTED_MEDICAL]");
            assertThat(sanitized).contains("DoctorNotes: [REDACTED_MEDICAL]");
            assertThat(sanitized).doesNotContain("Viêm nha chu mạn tính tiến triển");
            assertThat(sanitized).doesNotContain("Amoxicillin 500mg");
        }
    }

    // =========================================================================
    // 6. SQL INJECTION STRINGS
    // =========================================================================
    @Nested
    @DisplayName("6. SQL Injection Strings")
    class SqlInjectionTest {

        @Test
        @DisplayName("6.1 - Classic SQL injection payload inside JSON password")
        void testSqlInjectionInJsonPassword() {
            String input = "{\"username\": \"admin\", \"password\": \"' OR '1'='1; DROP TABLE it_agent_profile; --\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
            assertThat(sanitized).doesNotContain("DROP TABLE");
            assertThat(sanitized).doesNotContain("'1'='1");
        }

        @Test
        @DisplayName("6.2 - SQL injection containing escaped double quotes in JSON password: sanitized properly")
        void testSqlInjectionWithEscapedQuotesInPassword() {
            String input = "{\"password\": \"admin\\\" OR 1=1 --\", \"role\": \"ADMIN\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
            assertThat(sanitized).doesNotContain("OR 1=1 --");
            assertThat(sanitized).contains("\"role\": \"ADMIN\"");
        }

        @Test
        @DisplayName("6.3 - SQL injection inside URL form-encoded password parameter")
        void testSqlInjectionInFormUrlEncodedPassword() {
            String input = "action=login&password=admin' OR '1'='1&remember=true";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            // Analysis: FORM_PASSWORD_PATTERN uses [^&\\s\"',;]+ which stops at the single quote '.
            // Therefore 'password=admin' is redacted to 'password=[REDACTED]', but the remaining
            // SQL injection payload ' OR '1'='1 remains unredacted!
            boolean sqlFragmentLeaked = sanitized.contains("' OR '1'='1");
            System.out.println("SQLi in form param output: " + sanitized + " (SQLi payload survived: " + sqlFragmentLeaked + ")");
        }

        @Test
        @DisplayName("6.4 - Raw SQL statement containing password field: SELECT * WHERE password='secretPass'")
        void testRawSqlStatementWithPassword() {
            String input = "DEBUG QUERY: SELECT * FROM users WHERE username='owner' AND password='SuperSecretDbPassword123'";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            // Notice: FORM_PASSWORD_PATTERN requires [^&\\s\"',;]+ immediately after '='.
            // When followed by a single quote 'SuperSecretDbPassword123', it matches 0 characters!
            boolean passwordLeaked = sanitized.contains("SuperSecretDbPassword123");
            System.out.println("Raw SQL statement output: " + sanitized + " (Password leaked: " + passwordLeaked + ")");
        }
    }

    // =========================================================================
    // 7. IDEMPOTENCY
    // =========================================================================
    @Nested
    @DisplayName("7. Idempotency Verification")
    class IdempotencyTest {

        @Test
        @DisplayName("7.1 - Triple-pass sanitization must produce identical output")
        void testTriplePassIdempotency() {
            String raw = "{\"password\": \"mySecretPass123\", \"token\": \"" + VALID_JWT_1
                    + "\", \"diagnosis\": \"Sâu răng\", \"patientPhone\": \"0981234567\"}";

            String pass1 = SensitiveDataSanitizer.sanitize(raw);
            String pass2 = SensitiveDataSanitizer.sanitize(pass1);
            String pass3 = SensitiveDataSanitizer.sanitize(pass2);

            assertThat(pass2).isEqualTo(pass1);
            assertThat(pass3).isEqualTo(pass1);
        }

        @Test
        @DisplayName("7.2 - Sanitizing already redacted markers does not mutate or nest redaction tags")
        void testAlreadyRedactedStrings() {
            String alreadyRedacted = "Authorization: Bearer [REDACTED_JWT]\n"
                    + "Basic [REDACTED]\n"
                    + "\"password\": \"[REDACTED]\"\n"
                    + "password=[REDACTED]\n"
                    + "Cookie: [REDACTED]\n"
                    + "\"diagnosis\": \"[REDACTED_MEDICAL]\"\n"
                    + "Diagnosis: [REDACTED_MEDICAL]\n"
                    + "CCCD: [REDACTED_ID]";

            String sanitized = SensitiveDataSanitizer.sanitize(alreadyRedacted);

            assertThat(sanitized).isEqualTo(alreadyRedacted);
            assertThat(sanitized).doesNotContain("[[REDACTED");
            assertThat(sanitized).doesNotContain("[REDACTED_JWT]_JWT");
        }
    }

    // =========================================================================
    // 8. JPA PRE-PERSISTENCE HOOKS & MISSING GUARDRAILS
    // =========================================================================
    @Nested
    @DisplayName("8. JPA Pre-Persistence Hooks & Missing Guardrails")
    class JpaPrePersistenceHookTest {

        @Test
        @DisplayName("8.1 - ITBrowserTabRecord: Missing pre-persistence sanitization hook on urlRoute and tabTitle!")
        void testBrowserTabRecordMissingSanitization() {
            String dirtyUrl = "http://localhost:8080/api/auth/reset?password=SecretPass123&token=" + VALID_JWT_1;
            String dirtyTitle = "Admin Password Reset (password=SecretPass123)";

            ITBrowserTabRecord tab = new ITBrowserTabRecord(
                    1L,
                    "backend",
                    dirtyTitle,
                    dirtyUrl,
                    "EXTERNAL",
                    "OPEN"
            );

            // Trigger prePersist callback
            tab.prePersist();

            // Check if URL or Title are sanitized
            boolean urlLeaked = tab.getUrlRoute().contains("SecretPass123") || tab.getUrlRoute().contains(VALID_JWT_1);
            boolean titleLeaked = tab.getTabTitle().contains("SecretPass123");

            System.out.println("ITBrowserTabRecord URL after prePersist: " + tab.getUrlRoute() + " (Leaked: " + urlLeaked + ")");
            System.out.println("ITBrowserTabRecord Title after prePersist: " + tab.getTabTitle() + " (Leaked: " + titleLeaked + ")");

            // In R1 (§R1 lines 26), storing passwords/JWTs in tab records is strictly prohibited!
            assertThat(urlLeaked).as("ITBrowserTabRecord urlRoute must not leak sensitive data after pre-persist sanitizer").isFalse();
            assertThat(titleLeaked).as("ITBrowserTabRecord tabTitle must not leak sensitive data after pre-persist sanitizer").isFalse();
            assertThat(tab.getUrlRoute()).contains("password=[REDACTED]");
            assertThat(tab.getUrlRoute()).contains("[REDACTED_JWT]");
        }

        @Test
        @DisplayName("8.2 - ITApiRunLog: prePersist re-sanitizes updated dirty payloads")
        void testApiRunLogPrePersistReSanitization() {
            ITApiRunLog log = new ITApiRunLog();
            log.setEndpoint("/api/v1/auth/login");
            log.setHttpMethod("POST");
            log.setStatusCode(200);
            log.setExecutionDurationMs(12L);
            // Setter does not sanitize directly:
            log.setRequestPayload("{\"password\": \"DirtySecret999\"}");
            log.setResponsePayload("{\"token\": \"" + VALID_JWT_1 + "\"}");

            // PrePersist hook sanitizes:
            log.prePersist();

            assertThat(log.getRequestPayload()).contains("\"password\": \"[REDACTED]\"");
            assertThat(log.getRequestPayload()).doesNotContain("DirtySecret999");
            assertThat(log.getResponsePayload()).contains("\"token\": \"[REDACTED_JWT]\"");
            assertThat(log.getResponsePayload()).doesNotContain(VALID_JWT_1);
        }

        @Test
        @DisplayName("8.3 - ITAgentMemory: prePersist hook re-sanitizes modified memory content")
        void testAgentMemoryPrePersistReSanitization() {
            ITAgentMemory memory = new ITAgentMemory();
            memory.setAgentCode("backend");
            memory.setMemoryKey("API_KEYS");
            memory.setMemoryContent("Set api_key=sk-secret-live-999888777 in production");

            memory.prePersist();

            assertThat(memory.getMemoryContent()).contains("api_key=[REDACTED]");
            assertThat(memory.getMemoryContent()).doesNotContain("sk-secret-live-999888777");
        }

        @Test
        @DisplayName("8.4 - ITAgentMessage: prePersist hook re-sanitizes modified message body")
        void testAgentMessagePrePersistReSanitization() {
            ITAgentMessage message = new ITAgentMessage();
            message.setSenderName("owner");
            message.setMessageBody("Check patient with Cookie: JSESSIONID=SECRET987654321");

            message.prePersist();

            assertThat(message.getMessageBody()).contains("Cookie: [REDACTED]");
            assertThat(message.getMessageBody()).doesNotContain("SECRET987654321");
        }

        @Test
        @DisplayName("8.5 - ITAgentActivity: prePersist hook re-sanitizes description and result summary")
        void testAgentActivityPrePersistReSanitization() {
            ITAgentActivity activity = new ITAgentActivity();
            activity.setActionType("MENTIONED");
            activity.setDescription("Tagged #it-backend with password=PlainPassword123");
            activity.setResultSummary("Processed with Bearer " + VALID_JWT_2);

            activity.prePersist();

            assertThat(activity.getDescription()).contains("password=[REDACTED]");
            assertThat(activity.getDescription()).doesNotContain("PlainPassword123");
            assertThat(activity.getResultSummary()).contains("Bearer [REDACTED_JWT]");
            assertThat(activity.getResultSummary()).doesNotContain(VALID_JWT_2);
        }
    }
}
