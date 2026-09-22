package com.dentalclinic.analytics.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise Medical & Privacy Data Sanitizer for Analytics Ingestion.
 * Enforces strict PII protection: redacts credentials, JWTs, credit cards,
 * national IDs, phone numbers, and clinical EMR medical diagnosis text.
 */
@Component
public class AnalyticsDataSanitizer {

    // 1. Bearer JWT & Generic Bearer
    private static final Pattern BEARER_JWT_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+ey[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-+]+"
    );
    private static final Pattern STANDALONE_JWT_PATTERN = Pattern.compile(
            "\\beyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-+]{10,}\\b"
    );
    private static final Pattern BEARER_GENERIC_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+(?!\\[REDACTED)[^\\s\"',;}{]+"
    );

    // 2. Passwords, Secrets, API Keys in JSON
    private static final Pattern JSON_SECRET_STRING_PATTERN = Pattern.compile(
            "(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|token|authToken|auth_token|accessToken|access_token|refreshToken|refresh_token|idToken|id_token)\"\\s*:\\s*\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\""
    );
    private static final Pattern JSON_SECRET_UNQUOTED_PATTERN = Pattern.compile(
            "(?i)\"(password|passwd|pwd|pass|secret|client_secret|apiKey|api_key|token|authToken|auth_token|accessToken|access_token|refreshToken|refresh_token)\"\\s*:\\s*(?!\\[REDACTED)[^,\\}\\]\\s\"]+"
    );

    // 3. Query string / Form passwords
    private static final Pattern FORM_PASSWORD_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|api_key|apikey|access_token|refresh_token|auth_token)=(?!\\[REDACTED)[^&\\s\"',;]+"
    );
    private static final Pattern TEXT_PASSWORD_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret)\\s*[:=]\\s*(?!\\[REDACTED)[^\\s\"',;]+"
    );

    // 4. Cookies & Session tokens
    private static final Pattern COOKIE_PATTERN = Pattern.compile(
            "(?i)\\b(Set-Cookie|Cookie)\\s*:\\s*(?!\\[REDACTED)[^\\r\\n;]+"
    );
    private static final Pattern SESSION_COOKIE_KEY_PATTERN = Pattern.compile(
            "(?i)\\b(JSESSIONID|remember-me|dental_token|sessionid|authToken)=(?!\\[REDACTED)[^;\\s\"',&]+"
    );

    // 5. Financial Data (Credit Card Numbers: 13 to 19 digits with optional spaces/dashes)
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})\\b"
    );
    private static final Pattern GENERIC_CARD_CHUNKED_PATTERN = Pattern.compile(
            "\\b(?:\\d{4}[-\\s]){3}\\d{4}\\b"
    );

    // 6. National IDs: CCCD (12 digits starting with 0) & Contextual CMND (9 digits)
    private static final Pattern CCCD_PATTERN = Pattern.compile(
            "(?<!\\d)0\\d{11}(?!\\d)"
    );
    private static final Pattern CMND_CONTEXT_PATTERN = Pattern.compile(
            "(?i)\\b(CMND|CCCD|citizenId|citizen_id|idCard|id_card|nationalId|national_id)\\b(\\s*[:=]?\\s*[\"']?)\\d{9}(?!\\d)"
    );

    // 7. Clinical EMR Fields in JSON (diagnosis, prescription, treatmentDone, etc.)
    private static final Pattern JSON_MEDICAL_PATTERN = Pattern.compile(
            "(?i)\"(diagnosis|prescription|treatmentDone|treatment_done|notes|medicalHistory|medical_history|symptoms|doctorNotes|doctor_notes|treatmentPlan|treatment_plan|emr|clinicalFindings|clinical_findings)\"\\s*:\\s*(?:\"(?!\\[REDACTED)(?:\\\\\"|[^\"])*\"|\\{[^}]*\\}|\\[[^\\]]*\\])"
    );

    // 8. Plaintext Medical / EMR terms & Dental conditions
    private static final Pattern CLINICAL_TERMS_PATTERN = Pattern.compile(
            "(?i)\\b(sâu\\s*răng(?:\\s*(?:độ|cấp)?\\s*[0-4])?|viêm\\s*(?:tủy|tuỷ|nướu|nha\\s*chu|chóp)|áp\\s*xe|abcess|răng\\s*khôn\\s*(?:mọc\\s*lệch|mọc\\s*ngầm)|khớp\\s*cắn\\s*ngược|hoại\\s*tử\\s*(?:tủy|tuỷ)|chẩn\\s*đoán\\s*y\\s*khoa|đơn\\s*thuốc\\s*(?:điều\\s*trị)?|bệnh\\s*án\\s*nội\\s*trú|amoxicillin|ibuprofen|paracetamol|lidocaine|kháng\\s*sinh)\\b"
    );

    // 9. Phone number pattern inside strings or JSON
    private static final Pattern PHONE_JSON_PATTERN = Pattern.compile(
            "(?i)\"(patientPhone|phone|phoneNumber|customerPhone|userPhone)\"\\s*:\\s*\"(\\d{2,4})\\d{3,4}(\\d{3,4})\""
    );

    /**
     * Sanitizes general text or JSON payload by redacting all sensitive PII,
     * secrets, medical clinical terms, and financial data.
     */
    public String sanitizeText(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // Step 1: Redact Bearer & JWT
        result = BEARER_JWT_PATTERN.matcher(result).replaceAll("Bearer [REDACTED_JWT]");
        result = STANDALONE_JWT_PATTERN.matcher(result).replaceAll("[REDACTED_JWT]");
        result = BEARER_GENERIC_PATTERN.matcher(result).replaceAll("Bearer [REDACTED]");

        // Step 2: Redact Passwords / Secrets
        result = JSON_SECRET_STRING_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED]\"");
        result = JSON_SECRET_UNQUOTED_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED]\"");
        result = FORM_PASSWORD_PATTERN.matcher(result).replaceAll("$1=[REDACTED]");
        result = TEXT_PASSWORD_PATTERN.matcher(result).replaceAll("$1: [REDACTED]");

        // Step 3: Redact Cookies
        result = COOKIE_PATTERN.matcher(result).replaceAll("$1: [REDACTED]");
        result = SESSION_COOKIE_KEY_PATTERN.matcher(result).replaceAll("$1=[REDACTED]");

        // Step 4: Redact Credit Cards
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[REDACTED_CARD]");
        result = GENERIC_CARD_CHUNKED_PATTERN.matcher(result).replaceAll("[REDACTED_CARD]");

        // Step 5: Redact National IDs
        result = CCCD_PATTERN.matcher(result).replaceAll("[REDACTED_ID]");
        result = CMND_CONTEXT_PATTERN.matcher(result).replaceAll("$1$2[REDACTED_ID]");

        // Step 6: Redact Medical EMR Fields & Clinical terms
        result = JSON_MEDICAL_PATTERN.matcher(result).replaceAll("\"$1\": \"[REDACTED_MEDICAL]\"");
        result = CLINICAL_TERMS_PATTERN.matcher(result).replaceAll("[REDACTED_CLINICAL]");

        // Step 7: Mask Phone numbers inside payload
        result = PHONE_JSON_PATTERN.matcher(result).replaceAll("\"$1\": \"$2****$3\"");

        return result;
    }

    /**
     * Masks standalone phone number to format 09****5678.
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String cleanPhone = phone.replaceAll("[^0-9+]", "");
        if (cleanPhone.length() >= 10) {
            String prefix = cleanPhone.substring(0, 2);
            String suffix = cleanPhone.substring(cleanPhone.length() - 4);
            return prefix + "****" + suffix;
        }
        if (cleanPhone.length() >= 7) {
            return cleanPhone.substring(0, 2) + "***" + cleanPhone.substring(cleanPhone.length() - 2);
        }
        return "***";
    }

    /**
     * Anonymizes IP address by masking the host part according to GDPR privacy guidelines.
     * e.g. 192.168.1.55 -> 192.168.1.xxx
     */
    public String anonymizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "0.0.0.xxx";
        }
        String cleanIp = ip.split(",")[0].trim();
        if (cleanIp.contains(".")) {
            // IPv4
            int lastDot = cleanIp.lastIndexOf('.');
            if (lastDot > 0) {
                return cleanIp.substring(0, lastDot) + ".xxx";
            }
        } else if (cleanIp.contains(":")) {
            // IPv6
            int colonIndex = cleanIp.indexOf(':');
            int secondColon = (colonIndex >= 0) ? cleanIp.indexOf(':', colonIndex + 1) : -1;
            if (secondColon > 0) {
                return cleanIp.substring(0, secondColon) + ":xxxx:xxxx";
            }
            return "xxxx:xxxx:xxxx:xxxx";
        }
        return cleanIp;
    }

    /**
     * Safe string truncation helper.
     */
    public String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
