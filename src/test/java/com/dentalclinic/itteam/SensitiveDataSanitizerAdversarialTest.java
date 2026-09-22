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
