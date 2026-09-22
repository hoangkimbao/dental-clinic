package com.dentalclinic.itteam;

import com.dentalclinic.itteam.config.ITTeamDataInitializer;
import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.repository.*;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("IT Team Milestone 1 Empirical Challenger Stress Test Suite")
public class ITTeamMilestone1EmpiricalStressTest {

    @Autowired
    private ITAgentProfileRepository profileRepository;

    @Autowired
    private ITAgentMemoryRepository memoryRepository;

    @Autowired
    private ITAgentMessageRepository messageRepository;

    @Autowired
    private ITAgentActivityRepository activityRepository;

    @Autowired
    private ITBrowserTabRecordRepository tabRepository;

    @Autowired
    private ITApiRunLogRepository apiRunLogRepository;

    @Autowired
    private ITTeamDataInitializer dataInitializer;

    @Autowired
    private EntityManager entityManager;

    // =========================================================================
    // 1. ITTeamDataInitializer Idempotency & Failure Mode Stress Tests
    // =========================================================================

    @Test
    @DisplayName("ST-1.1 - Double invocation of ITTeamDataInitializer does not duplicate records in standard state")
    void testStandardDoubleInvocation() throws Exception {
        long pCount1 = profileRepository.count();
        long mCount1 = memoryRepository.count();
        long tCount1 = tabRepository.count();

        // 2nd run
        dataInitializer.run();

        assertThat(profileRepository.count()).isEqualTo(pCount1);
        assertThat(memoryRepository.count()).isEqualTo(mCount1);
        assertThat(tabRepository.count()).isEqualTo(tCount1);
    }

    @Test
    @DisplayName("ST-1.2 - Partial State Reconciliation: If profiles exist but memories/tabs are deleted, re-run re-seeds child data")
    void testPartialStateReconciliation() throws Exception {
        // Given profiles exist (seeded on startup)
        assertThat(profileRepository.count()).isEqualTo(5);

        // Suppose memories were wiped or failed during initial deployment
        memoryRepository.deleteAll();
        memoryRepository.flush();
        assertThat(memoryRepository.count()).isZero();

        // When dataInitializer is run again
        dataInitializer.run();

        // After remediation: Seeder reconciles and re-seeds memories even when profiles already exist
        assertThat(memoryRepository.count()).isEqualTo(12);

        // Test tabs reconciliation as well
        tabRepository.deleteAll();
        tabRepository.flush();
        assertThat(tabRepository.count()).isZero();

        dataInitializer.run();
        assertThat(tabRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("ST-1.3 - Database Unique Constraint on (agent_code, memory_key) prevents duplicate memory keys")
    void testDuplicateMemoryKeyConstraint() {
        ITAgentMemory mem1 = new ITAgentMemory(1L, "backend", "CUSTOM_KEY", "Content 1", "MEDIUM");
        ITAgentMemory mem2 = new ITAgentMemory(1L, "backend", "CUSTOM_KEY", "Content 2", "HIGH");

        memoryRepository.saveAndFlush(mem1);

        // With unique constraint, saving duplicate (agent_code, memory_key) throws DataIntegrityViolationException
        assertThatThrownBy(() -> memoryRepository.saveAndFlush(mem2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================================
    // 2. Model Field Validation, Nullability & Size Limit Stress Tests
    // =========================================================================

    @Test
    @DisplayName("ST-2.1 - Orphaned Memory Allowed: agentCode and agentId both null can be persisted")
    void testOrphanedMemoryPersisted() {
        ITAgentMemory orphaned = new ITAgentMemory(null, null, "ORPHAN_KEY", "Orphaned content", "LOW");
        ITAgentMemory saved = memoryRepository.saveAndFlush(orphaned);
        entityManager.clear();

        ITAgentMemory reloaded = memoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAgentCode()).isNull();
        assertThat(reloaded.getAgentId()).isNull();
    }

    @Test
    @DisplayName("ST-2.2 - Null memoryContent violates nullable = false")
    void testNullMemoryContentFails() {
        ITAgentMemory invalid = new ITAgentMemory(1L, "backend", "KEY_X", null, "LOW");
        assertThatThrownBy(() -> memoryRepository.saveAndFlush(invalid))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("ST-2.3 - ITAgentActivity description supports large text (>1000 chars) via TEXT columnDefinition")
    void testActivityDescriptionExceeds1000() {
        // Large stack trace or audit log entry (1050 chars)
        String longDescription = "API failure stack trace: " + "a".repeat(1050);
        ITAgentActivity activity = new ITAgentActivity(
                1L, "backend", "API_ERROR", longDescription, "Failed", "/api/error"
        );

        ITAgentActivity saved = activityRepository.saveAndFlush(activity);
        entityManager.clear();

        ITAgentActivity reloaded = activityRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo(longDescription);
    }

    @Test
    @DisplayName("ST-2.4 - ITApiRunLog supports null statusCode for connection timeouts/errors")
    void testApiRunLogNullStatusCodeSupported() {
        // When HTTP request fails before handshake (timeout/connect refused), statusCode is null
        ITApiRunLog connectionFailedLog = new ITApiRunLog(
                "/api/external/timeout", "GET", null, 5000L, null, "Connection timed out", "ROLE_ADMIN"
        );

        ITApiRunLog saved = apiRunLogRepository.saveAndFlush(connectionFailedLog);
        entityManager.clear();

        ITApiRunLog reloaded = apiRunLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getStatusCode()).isNull();
        assertThat(reloaded.getResponsePayload()).isEqualTo("Connection timed out");
    }

    // =========================================================================
    // 3. SensitiveDataSanitizer Empirical Vulnerability Stress Tests
    // =========================================================================

    @Test
    @DisplayName("ST-3.1 - Escaped quotes in JSON string are fully redacted without leaking medical data or corrupting JSON")
    void testEscapedQuotesLeakMedicalData() {
        String input = "{\"diagnosis\": \"Bệnh nhân bị \\\"sâu răng nặng\\\" ở hàm trên\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(sanitized).doesNotContain("sâu răng nặng");
    }

    @Test
    @DisplayName("ST-3.2 - OAuth2 snake_case access_token and refresh_token are redacted")
    void testSnakeCaseOAuthTokensRedacted() {
        String oauthPayload = "{\"access_token\": \"gho_16C7e42F292c6912E7710c838347Ae178B4a\", \"refresh_token\": \"rfr_SecretOauthRefreshToken9988\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(oauthPayload);

        assertThat(sanitized).contains("\"access_token\": \"[REDACTED]\"");
        assertThat(sanitized).contains("\"refresh_token\": \"[REDACTED]\"");
        assertThat(sanitized).doesNotContain("gho_16C7e42F292c6912E7710c838347Ae178B4a");
        assertThat(sanitized).doesNotContain("rfr_SecretOauthRefreshToken9988");
    }

    @Test
    @DisplayName("ST-3.3 - containsUnsanitizedSensitiveData detects all sensitive data patterns")
    void testContainsUnsanitizedSensitiveDataOmissions() {
        // 1. Basic Auth detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Authorization: Basic dXNlcjpwYXNz"))
                .as("Basic auth header should be detected").isTrue();

        // 2. Plain text password detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Password: SuperSecretPassword123"))
                .as("Plaintext password should be detected").isTrue();

        // 3. Unquoted numeric password detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("{\"password\": 987654}"))
                .as("Unquoted numeric password should be detected").isTrue();

        // 4. Standalone session cookie detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Session param JSESSIONID=SECRET98765"))
                .as("Session cookie should be detected").isTrue();

        // 5. Plaintext medical diagnosis detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("Diagnosis: Viêm tủy răng cấp"))
                .as("Plaintext medical diagnosis should be detected").isTrue();

        // 6. CCCD detected
        assertThat(SensitiveDataSanitizer.containsUnsanitizedSensitiveData("CCCD: 001201012345"))
                .as("CCCD should be detected").isTrue();
    }

    @Test
    @DisplayName("ST-3.4 - 9-digit payment amount is preserved without false positive CCCD/CMND redaction")
    void testNineDigitAmountFalselyRedacted() {
        String payment = "{\"amount\": 100000000, \"currency\": \"VND\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(payment);

        assertThat(sanitized).contains("\"amount\": 100000000");
        assertThat(sanitized).doesNotContain("[REDACTED_ID]");
    }
}
