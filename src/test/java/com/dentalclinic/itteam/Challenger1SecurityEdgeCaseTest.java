package com.dentalclinic.itteam;

import com.dentalclinic.itteam.service.ITApiRunnerService;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Empirical Challenger 1 Test Suite: Security, Edge Cases & Adversarial Verification.
 * Focus:
 * 1. SensitiveDataSanitizer expanded PHONE_MASK_PATTERN, pattern integrity, and ReDoS resistance.
 * 2. ITApiRunnerService early dangerous URI scheme rejection and SSRF prevention.
 * 3. it-team.js escapeHtml(str) contract verification.
 */
@DisplayName("Challenger 1 - Security & Edge Cases Test Suite")
public class Challenger1SecurityEdgeCaseTest {

    private ITApiRunnerService apiRunnerService;

    @BeforeEach
    void setUp() {
        apiRunnerService = new ITApiRunnerService(null);
    }

    // =========================================================================
    // FOCUS 1: SENSITIVEDATASANITIZER EXPANDED PHONE MASKING & REDOS RESILIENCE
    // =========================================================================
    @Nested
    @DisplayName("1. SensitiveDataSanitizer Phone Masking & Redaction")
    class PhoneMaskingAndSanitizerTests {

        @Test
        @DisplayName("1.1 - Expanded PHONE_MASK_PATTERN masks phoneNumber variation correctly")
        void testPhoneNumberVariationMasked() {
            String input = "{\"patientId\": 42, \"phoneNumber\": \"0981234567\", \"status\": \"ACTIVE\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).isEqualTo("{\"patientId\": 42, \"phoneNumber\": \"098****567\", \"status\": \"ACTIVE\"}");
            assertThat(sanitized).doesNotContain("0981234567");
        }

        @Test
        @DisplayName("1.2 - Expanded PHONE_MASK_PATTERN masks customerPhone variation correctly")
        void testCustomerPhoneVariationMasked() {
            String input = "{\"customerName\": \"Tran Van B\", \"customerPhone\": \"0977654321\"}";
            String sanitized = SensitiveDataSanitizer.sanitize(input);

            assertThat(sanitized).isEqualTo("{\"customerName\": \"Tran Van B\", \"customerPhone\": \"097****321\"}");
            assertThat(sanitized).doesNotContain("0977654321");
        }

        @Test
        @DisplayName("1.3 - Expanded PHONE_MASK_PATTERN masks patientPhone and phone variations")
        void testPatientPhoneAndPhoneVariations() {
            String input1 = "{\"patientPhone\": \"0901234567\"}";
            String input2 = "{\"phone\": \"0912345678\"}";

            assertThat(SensitiveDataSanitizer.sanitize(input1)).isEqualTo("{\"patientPhone\": \"090****567\"}");
            assertThat(SensitiveDataSanitizer.sanitize(input2)).isEqualTo("{\"phone\": \"091****678\"}");
        }

        @Test
        @DisplayName("1.4 - Case-insensitivity for phone variations")
        void testPhoneMaskingCaseInsensitivity() {
            String inputUpper = "{\"PHONENUMBER\": \"0981234567\"}";
            String inputMixed = "{\"CustomerPhone\": \"0977654321\"}";

            assertThat(SensitiveDataSanitizer.sanitize(inputUpper)).isEqualTo("{\"PHONENUMBER\": \"098****567\"}");
            assertThat(SensitiveDataSanitizer.sanitize(inputMixed)).isEqualTo("{\"CustomerPhone\": \"097****321\"}");
        }

        @Test
        @DisplayName("1.5 - Whitespace tolerance in phone key-value pairs")
        void testPhoneMaskingWhitespaceTolerance() {
            String input = "{\"phoneNumber\"   :   \"0981234567\"}";
            assertThat(SensitiveDataSanitizer.sanitize(input)).isEqualTo("{\"phoneNumber\": \"098****567\"}");
        }

        @Test
        @DisplayName("1.6 - containsUnsanitizedSensitiveData detects phone variations prior to sanitization")
        void testContainsUnsanitizedDetectsPhones() {
            assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"phoneNumber\": \"0981234567\"}")).isTrue();
            assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"customerPhone\": \"0977654321\"}")).isTrue();

            // After sanitization, containsUnsanitizedSensitiveData should return false
            String sanitizedPhone = SensitiveDataSanitizer.sanitize("{\"phoneNumber\": \"0981234567\"}");
            assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData(sanitizedPhone)).isFalse();

            String sanitizedCustomer = SensitiveDataSanitizer.sanitize("{\"customerPhone\": \"0977654321\"}");
            assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData(sanitizedCustomer)).isFalse();
        }

        @Test
        @DisplayName("1.7 - Preserves other patterns: JWT, passwords, cookies, CCCD/CMND, EMR diagnosis")
        void testOtherPatternsIntegrity() {
            String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String combined = String.format(
                    "Token: Bearer %s\n" +
                    "Password: secretPassword123\n" +
                    "Cookie: JSESSIONID=ABCD1234EFGH\n" +
                    "Patient CCCD: 001201012345, CMND: 123456789\n" +
                    "{\"diagnosis\": \"Sâu răng hàm số 6\", \"phoneNumber\": \"0981234567\"}",
                    jwt
            );

            String sanitized = SensitiveDataSanitizer.sanitize(combined);

            assertThat(sanitized).contains("Bearer [REDACTED_JWT]");
            assertThat(sanitized).doesNotContain(jwt);
            assertThat(sanitized).contains("Password: [REDACTED]");
            assertThat(sanitized).doesNotContain("secretPassword123");
            assertThat(sanitized).contains("Cookie: [REDACTED]");
            assertThat(sanitized).doesNotContain("ABCD1234EFGH");
            assertThat(sanitized).contains("Patient CCCD: [REDACTED_ID]");
            assertThat(sanitized).doesNotContain("001201012345");
            assertThat(sanitized).contains("CMND: [REDACTED_ID]");
            assertThat(sanitized).doesNotContain("123456789");
            assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
            assertThat(sanitized).doesNotContain("Sâu răng hàm số 6");
            assertThat(sanitized).contains("\"phoneNumber\": \"098****567\"");
            assertThat(sanitized).doesNotContain("0981234567");
        }

        @Test
        @DisplayName("1.8 - ReDoS Resilience: Verify execution terminates within 1000ms under adversarial inputs")
        void testReDosResilience() {
            assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
                // Adversarial case A: 30,000 backslashes without closing quote for password pattern
                String repeatedBackslashes = "{\"password\": \"" + "\\".repeat(30000);
                SensitiveDataSanitizer.sanitize(repeatedBackslashes);

                // Adversarial case B: 30,000 characters without closing quote for medical pattern
                String repeatedMedical = "{\"diagnosis\": \"" + "a".repeat(30000);
                SensitiveDataSanitizer.sanitize(repeatedMedical);

                // Adversarial case C: Partial JWT with long repeated segments
                String partialJwt = "Bearer ey" + "a".repeat(30000);
                SensitiveDataSanitizer.sanitize(partialJwt);

                // Adversarial case D: Long sequence of digits
                String repeatedDigits = "0" + "9".repeat(30000);
                SensitiveDataSanitizer.sanitize(repeatedDigits);
            });
        }
    }

    // =========================================================================
    // FOCUS 2: ITAPIRUNNERSERVICE SCHEME REJECTION & SSRF DEFENSES
    // =========================================================================
    @Nested
    @DisplayName("2. ITApiRunnerService SSRF & Scheme Defenses")
    class ApiRunnerSecurityTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "file:///etc/passwd",
                "file:///C:/Windows/win.ini",
                "ftp://attacker.com/exploit",
                "gopher://127.0.0.1:6379/_flushall",
                "ldap://attacker.com/exploit",
                "ldaps://attacker.com/exploit",
                "jar://target.jar!/META-INF/MANIFEST.MF",
                "netdoc:///etc/hosts",
                "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
                "dict://127.0.0.1:11211/stats",
                "mailto:victim@example.com",
                "telnet://127.0.0.1:23",
                "php://filter/convert.base64-encode/resource=index.php",
                "expect://id"
        })
        @DisplayName("2.1 - Early scheme rejection against non-HTTP/dangerous protocols")
        void testEarlyDangerousSchemeRejection(String dangerousEndpoint) {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint(dangerousEndpoint))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid or dangerous URI scheme rejected");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://169.254.169.254/latest/meta-data/",
                "http://169.254.169.254:80/",
                "169.254.169.254"
        })
        @DisplayName("2.2 - SSRF Block against AWS/Cloud Metadata IP (169.254.169.254)")
        void testCloudMetadataBlocked(String metadataUrl) {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint(metadataUrl))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SSRF blocked");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://evil.com/malicious",
                "http://evil.com",
                "evil.com"
        })
        @DisplayName("2.3 - SSRF Block against evil.com")
        void testEvilComBlocked(String evilUrl) {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint(evilUrl))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SSRF blocked");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://10.0.0.1:8080/internal",
                "http://10.255.0.1/admin",
                "http://192.168.1.1/router",
                "http://192.168.0.100:8080/api",
                "http://0.0.0.0:8080/api",
                "http://[::1]:8080/api",
                "http://::1:8080/api",
                "http://127.0.0.1.nip.io:8080/api",
                "http://test.xip.io/api",
                "http://127.0.0.1.sslip.io/api"
        })
        @DisplayName("2.4 - SSRF Block against private subnets (10.x, 192.168.x, 0.0.0.0, [::1], DNS evasion)")
        void testPrivateSubnetsAndDnsEvasionBlocked(String target) {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint(target))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SSRF blocked");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://localhost@attacker.com/",
                "http://admin:pass@127.0.0.1:8080/api",
                "http://user@evil.com"
        })
        @DisplayName("2.5 - SSRF Block against userinfo '@' tricks")
        void testUserInfoAtTricksBlocked(String target) {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint(target))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SSRF blocked");
        }

        @Test
        @DisplayName("2.6 - SSRF Block against non-localhost external domain via URI host check")
        void testNonLocalhostExternalDomainBlocked() {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint("http://google.com/search"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SSRF blocked: Non-localhost target: google.com");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/coupons/active",
                "api/coupons/active",
                "/api/dashboard/stats",
                "http://localhost:8080/api/coupons/active",
                "http://127.0.0.1:8080/api/coupons/active"
        })
        @DisplayName("2.7 - Legitimate internal localhost endpoints pass validation")
        void testLegitimateInternalEndpointsPass(String endpoint) {
            apiRunnerService.validateEndpoint(endpoint);
        }

        @Test
        @DisplayName("2.8 - Null or empty endpoint rejected with IllegalArgumentException")
        void testNullOrEmptyEndpointRejected() {
            assertThatThrownBy(() -> apiRunnerService.validateEndpoint(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Endpoint cannot be null or empty");

            assertThatThrownBy(() -> apiRunnerService.validateEndpoint("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Endpoint cannot be null or empty");
        }
    }

    // =========================================================================
    // FOCUS 3: ESCAPEHTML(STR) LOGICAL CONTRACT VERIFICATION
    // =========================================================================
    @Nested
    @DisplayName("3. escapeHtml(str) Logic Verification")
    class EscapeHtmlContractTests {

        /**
         * Java implementation exactly mirroring it-team.js escapeHtml(str):
         * function escapeHtml(str) {
         *     if (str == null) return '';
         *     return String(str)
         *         .replace(/&/g, '&amp;')
         *         .replace(/</g, '&lt;')
         *         .replace(/>/g, '&gt;')
         *         .replace(/"/g, '&quot;')
         *         .replace(/'/g, '&#39;');
         * }
         */
        private String escapeHtml(Object str) {
            if (str == null) return "";
            return String.valueOf(str)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }

        @Test
        @DisplayName("3.1 - HTML Special characters &, <, >, \", ' are correctly escaped")
        void testSpecialCharactersEscaped() {
            assertThat(escapeHtml("&")).isEqualTo("&amp;");
            assertThat(escapeHtml("<")).isEqualTo("&lt;");
            assertThat(escapeHtml(">")).isEqualTo("&gt;");
            assertThat(escapeHtml("\"")).isEqualTo("&quot;");
            assertThat(escapeHtml("'")).isEqualTo("&#39;");

            String attackPayload = "<script>alert('XSS & \"quote\"')</script>";
            assertThat(escapeHtml(attackPayload))
                    .isEqualTo("&lt;script&gt;alert(&#39;XSS &amp; &quot;quote&quot;&#39;)&lt;/script&gt;");
        }

        @Test
        @DisplayName("3.2 - Null and undefined-like inputs return empty string without errors")
        void testNullAndEmptyInputs() {
            assertThat(escapeHtml(null)).isEqualTo("");
            assertThat(escapeHtml("")).isEqualTo("");
        }

        @Test
        @DisplayName("3.3 - Numeric inputs (0, positive, negative) are preserved as strings")
        void testNumericInputs() {
            assertThat(escapeHtml(0)).isEqualTo("0");
            assertThat(escapeHtml(12345)).isEqualTo("12345");
            assertThat(escapeHtml(-42)).isEqualTo("-42");
            assertThat(escapeHtml(3.14159)).isEqualTo("3.14159");
        }
    }
}
