package com.dentalclinic.itteam;

import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveDataSanitizer Unit Tests")
public class SensitiveDataSanitizerTest {

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

    @Test
    @DisplayName("R1.4 - Should sanitize Basic authentication header to Basic [REDACTED]")
    void testSanitizeBasicAuth() {
        String input = "Authorization: Basic dXNlcjpwYXNzd29yZDEyMw==";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Authorization: Basic [REDACTED]");
        assertThat(sanitized).doesNotContain("dXNlcjpwYXNzd29yZDEyMw==");
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "passwd", "pwd", "secret", "client_secret", "apiKey", "api_key", "refreshToken", "accessToken"})
    @DisplayName("R1.5 - Should sanitize all password and credential keys in JSON to [REDACTED]")
    void testSanitizePasswordFieldsInJson(String key) {
        String input = String.format("{\"username\": \"owner\", \"%s\": \"SuperSecretP@ssw0rd!\"}", key);
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains(String.format("\"%s\": \"[REDACTED]\"", key));
        assertThat(sanitized).doesNotContain("SuperSecretP@ssw0rd!");
        assertThat(sanitized).contains("\"username\": \"owner\"");
    }

    @Test
    @DisplayName("R1.6 - Should sanitize unquoted numeric password in JSON to [REDACTED]")
    void testSanitizeUnquotedNumericPasswordInJson() {
        String input = "{\"username\": \"admin\", \"password\": 123456}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("{\"username\": \"admin\", \"password\": \"[REDACTED]\"}");
    }

    @Test
    @DisplayName("R1.7 - Should sanitize query parameter and form passwords to [REDACTED]")
    void testSanitizeFormAndQueryPasswords() {
        String input = "POST /api/login?password=mysecret&user=admin\nBody: password=bodysecret&remember=true";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("password=[REDACTED]");
        assertThat(sanitized).doesNotContain("mysecret");
        assertThat(sanitized).doesNotContain("bodysecret");
    }

    @Test
    @DisplayName("R1.8 - Should sanitize plain text password key-values")
    void testSanitizePlainTextPassword() {
        String input = "Current configuration: Password: myRootPassword123 for service admin";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Current configuration: Password: [REDACTED] for service admin");
        assertThat(sanitized).doesNotContain("myRootPassword123");
    }

    @Test
    @DisplayName("R1.9 - Should sanitize HTTP Cookie and Set-Cookie headers")
    void testSanitizeCookieHeaders() {
        String input = "GET /api/test\nCookie: JSESSIONID=ABCD1234EFGH; Path=/; HttpOnly\nSet-Cookie: token=secret; Secure";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("Cookie: [REDACTED]");
        assertThat(sanitized).contains("Set-Cookie: [REDACTED]");
        assertThat(sanitized).doesNotContain("ABCD1234EFGH");
    }

    @Test
    @DisplayName("R1.10 - Should sanitize JSON Cookie properties")
    void testSanitizeJsonCookie() {
        String input = "{\"headers\": {\"Cookie\": \"session=12345\"}}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"Cookie\": \"[REDACTED]\"");
        assertThat(sanitized).doesNotContain("12345");
    }

    @Test
    @DisplayName("R1.11 - Should sanitize standalone session cookies")
    void testSanitizeSessionCookies() {
        String input = "Request params: JSESSIONID=xyz98765&remember-me=true";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("JSESSIONID=[REDACTED]");
        assertThat(sanitized).doesNotContain("xyz98765");
    }

    @ParameterizedTest
    @ValueSource(strings = {"diagnosis", "prescription", "treatmentDone", "doctorNotes", "medicalHistory", "symptoms"})
    @DisplayName("R1.12 - Should sanitize medical EMR fields in JSON to [REDACTED_MEDICAL]")
    void testSanitizeMedicalEmrFieldsInJson(String key) {
        String input = String.format("{\"patientId\": 101, \"%s\": \"Viêm tủy cấp tính, chỉ định chữa tủy\"}", key);
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains(String.format("\"%s\": \"[REDACTED_MEDICAL]\"", key));
        assertThat(sanitized).doesNotContain("Viêm tủy cấp tính");
        assertThat(sanitized).contains("\"patientId\": 101");
    }

    @Test
    @DisplayName("R1.13 - Should sanitize plain text clinical diagnosis and prescriptions")
    void testSanitizePlainTextMedical() {
        String input = "Record note: Diagnosis: Sâu răng hàm số 6 kèm áp xe nướu";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Record note: Diagnosis: [REDACTED_MEDICAL]");
        assertThat(sanitized).doesNotContain("Sâu răng hàm số 6");
    }

    @Test
    @DisplayName("R1.14 - Should redact Vietnamese Citizen ID (CCCD/CMND) to [REDACTED_ID]")
    void testSanitizeVietnameseCitizenId() {
        String input = "Patient CCCD: 001201012345, Old CMND: 123456789 verified.";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Patient CCCD: [REDACTED_ID], Old CMND: [REDACTED_ID] verified.");
        assertThat(sanitized).doesNotContain("001201012345");
        assertThat(sanitized).doesNotContain("123456789");
    }

    @Test
    @DisplayName("R1.15 - Should mask patient phone numbers preserving first and last 3 digits")
    void testMaskPatientPhone() {
        String input = "{\"patientName\": \"Nguyen Van A\", \"patientPhone\": \"0981234567\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"patientPhone\": \"098****567\"");
        assertThat(sanitized).doesNotContain("0981234567");
    }

    @Test
    @DisplayName("R1.16 - Idempotency Verification: Repeated sanitization yields identical result")
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
    @DisplayName("R1.17 - Boundary Handling: Null, empty, and clean strings")
    void testBoundaryHandling() {
        assertThat(SensitiveDataSanitizer.sanitize(null)).isNull();
        assertThat(SensitiveDataSanitizer.sanitize("")).isEmpty();
        assertThat(SensitiveDataSanitizer.sanitize("   ")).isEqualTo("   ");

        String harmlessJson = "{\"status\": 200, \"message\": \"Thành công\", \"agent\": \"#it-backend\"}";
        assertThat(SensitiveDataSanitizer.sanitize(harmlessJson)).isEqualTo(harmlessJson);
    }

    @Test
    @DisplayName("R1.18 - Sensitive Data Detection check")
    void testContainsUnsanitizedSensitiveData() {
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Authorization: Bearer " + SAMPLE_JWT)).isTrue();
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"password\": \"secret\"}")).isTrue();
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"diagnosis\": \"Sâu răng\"}")).isTrue();
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"status\": \"OK\"}")).isFalse();
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData(null)).isFalse();
    }
}
