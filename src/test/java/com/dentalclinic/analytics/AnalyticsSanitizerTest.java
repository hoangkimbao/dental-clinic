package com.dentalclinic.analytics;

import com.dentalclinic.analytics.service.AnalyticsDataSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsSanitizerTest {

    private AnalyticsDataSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new AnalyticsDataSanitizer();
    }

    @Test
    @DisplayName("Should redact Bearer JWT and standalone JWT tokens")
    void testRedactJwt() {
        String input = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozG4m1e_someSignature";
        String sanitized = sanitizer.sanitizeText(input);

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
        assertTrue(sanitized.contains("[REDACTED_JWT]"));
    }

    @Test
    @DisplayName("Should redact passwords, API keys, and secrets in JSON payloads")
    void testRedactSecretsInJson() {
        String json = "{\"username\": \"dr_nguyen\", \"password\": \"SuperSecret123!\", \"apiKey\": \"ak_live_999888\"}";
        String sanitized = sanitizer.sanitizeText(json);

        assertFalse(sanitized.contains("SuperSecret123!"));
        assertFalse(sanitized.contains("ak_live_999888"));
        assertTrue(sanitized.contains("\"password\": \"[REDACTED]\""));
        assertTrue(sanitized.contains("\"apiKey\": \"[REDACTED]\""));
        assertTrue(sanitized.contains("\"username\": \"dr_nguyen\""));
    }

    @Test
    @DisplayName("Should redact Vietnamese CCCD (12 digits) and CMND (9 digits)")
    void testRedactCitizenIds() {
        String payload = "Khách hàng có CCCD 079198000123 và CMND: 025689145 cần tái khám";
        String sanitized = sanitizer.sanitizeText(payload);

        assertFalse(sanitized.contains("079198000123"));
        assertFalse(sanitized.contains("025689145"));
        assertTrue(sanitized.contains("[REDACTED_ID]"));
    }

    @Test
    @DisplayName("Should redact credit card numbers")
    void testRedactCreditCardNumbers() {
        String payload = "Thanh toán bằng thẻ 4111 2222 3333 4444 hoặc 5500-0000-0000-0004";
        String sanitized = sanitizer.sanitizeText(payload);

        assertFalse(sanitized.contains("4111 2222 3333 4444"));
        assertFalse(sanitized.contains("5500-0000-0000-0004"));
        assertTrue(sanitized.contains("[REDACTED_CARD]"));
    }

    @Test
    @DisplayName("Should redact clinical dental terms and EMR diagnosis fields")
    void testRedactClinicalTermsAndEmr() {
        String json = "{\"diagnosis\": \"Sâu răng độ 3 và viêm tủy cấp\", \"notes\": \"kê đơn amoxicillin 500mg và ibuprofen\"}";
        String sanitized = sanitizer.sanitizeText(json);

        assertFalse(sanitized.contains("Sâu răng độ 3"));
        assertFalse(sanitized.contains("viêm tủy cấp"));
        assertFalse(sanitized.contains("amoxicillin"));
        assertTrue(sanitized.contains("[REDACTED_MEDICAL]") || sanitized.contains("[REDACTED_CLINICAL]"));
    }

    @Test
    @DisplayName("Should mask phone numbers correctly")
    void testMaskPhoneNumber() {
        String masked1 = sanitizer.maskPhone("0977224504");
        assertEquals("09****4504", masked1);

        String masked2 = sanitizer.maskPhone("+84912345678");
        assertTrue(masked2.startsWith("+8") || masked2.startsWith("84"));
        assertTrue(masked2.contains("****"));

        assertNull(sanitizer.maskPhone(null));
        assertEquals("***", sanitizer.maskPhone("123"));
    }

    @Test
    @DisplayName("Should anonymize IPv4 and IPv6 addresses according to GDPR")
    void testAnonymizeIp() {
        assertEquals("192.168.1.xxx", sanitizer.anonymizeIp("192.168.1.100"));
        assertEquals("10.0.0.xxx", sanitizer.anonymizeIp("10.0.0.1"));
        assertEquals("172.16.20.xxx", sanitizer.anonymizeIp("172.16.20.55, 10.0.0.1")); // handles X-Forwarded-For list
        assertTrue(sanitizer.anonymizeIp("2001:0db8:85a3:0000:0000:8a2e:0370:7334").contains("xxxx"));
    }
}
