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
            "(?i)\"(patientPhone|phone|phoneNumber|customerPhone)\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{3})\""
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
