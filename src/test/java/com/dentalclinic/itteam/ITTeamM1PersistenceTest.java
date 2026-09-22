package com.dentalclinic.itteam;

import com.dentalclinic.itteam.config.ITTeamDataInitializer;
import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("IT Team Milestone 1 Persistence & Seeder Tests")
public class ITTeamM1PersistenceTest {

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

    private static final String SAMPLE_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    @DisplayName("M1.1 - DataInitializer should seed 5 standard IT agent profiles on startup")
    void testSeededAgentProfiles() {
        long count = profileRepository.count();
        assertThat(count).isEqualTo(5);

        List<String> expectedCodes = List.of("backend", "frontend", "qa", "devops", "security");
        for (String code : expectedCodes) {
            Optional<ITAgentProfile> profileOpt = profileRepository.findByAgentCode(code);
            assertThat(profileOpt).isPresent();
            ITAgentProfile profile = profileOpt.get();
            assertThat(profile.getHashtag()).isEqualTo("#it-" + code);
            assertThat(profile.getStatus()).isIn("ONLINE", "ACTIVE");
            assertThat(profile.getRole()).isNotBlank();
            assertThat(profile.getExpertise()).isNotBlank();
        }
    }

    @Test
    @DisplayName("M1.2 - DataInitializer idempotency: multiple runs must not duplicate records")
    void testSeederIdempotency() throws Exception {
        long initialProfileCount = profileRepository.count();
        long initialMemoryCount = memoryRepository.count();
        long initialTabCount = tabRepository.count();
        long initialMsgCount = messageRepository.count();

        // Run data initializer again
        dataInitializer.run();

        assertThat(profileRepository.count()).isEqualTo(initialProfileCount);
        assertThat(memoryRepository.count()).isEqualTo(initialMemoryCount);
        assertThat(tabRepository.count()).isEqualTo(initialTabCount);
        assertThat(messageRepository.count()).isEqualTo(initialMsgCount);
    }

    @Test
    @DisplayName("M1.3 - Repository query methods should return expected data")
    void testRepositoryQueryMethods() {
        // 1. Profile queries
        Optional<ITAgentProfile> backend = profileRepository.findByHashtag("#it-backend");
        assertThat(backend).isPresent();
        assertThat(backend.get().getAgentCode()).isEqualTo("backend");

        boolean existsHashtag = profileRepository.existsByHashtag("#it-frontend");
        assertThat(existsHashtag).isTrue();

        // 2. Memory queries
        List<ITAgentMemory> backendMemories = memoryRepository.findByAgentCodeOrderByLastUpdatedDesc("backend");
        assertThat(backendMemories).isNotEmpty();
        assertThat(backendMemories.get(0).getAgentCode()).isEqualTo("backend");

        Optional<ITAgentMemory> archMem = memoryRepository.findByAgentCodeAndMemoryKey("backend", "ARCHITECTURE_OVERVIEW");
        assertThat(archMem).isPresent();

        // 3. Tab queries
        List<ITBrowserTabRecord> frontendTabs = tabRepository.findByAgentCodeAndStatus("frontend", "OPEN");
        assertThat(frontendTabs).isNotEmpty();
        assertThat(frontendTabs.get(0).getUrlRoute()).contains("index.html");

        // 4. Message queries
        List<ITAgentMessage> rootMessages = messageRepository.findByParentMessageIdIsNullOrderBySentAtAsc();
        assertThat(rootMessages).isNotEmpty();

        // 5. Activity queries
        List<ITAgentActivity> topActivities = activityRepository.findTop20ByOrderByTimestampDesc();
        assertThat(topActivities).isNotEmpty();
    }

    @Test
    @DisplayName("M1.4 - Database Persistence with Pre-Persist Sanitization for ITApiRunLog")
    void testApiRunLogPrePersistSanitization() {
        String dirtyRequest = "{\"username\": \"owner\", \"password\": \"RawPassword123\"}";
        String dirtyResponse = "{\"token\": \"" + SAMPLE_JWT + "\", \"diagnosis\": \"Sâu răng số 7\"}";

        ITApiRunLog log = new ITApiRunLog(
                "/api/test-endpoint",
                "POST",
                200,
                35L,
                dirtyRequest,
                dirtyResponse,
                "ROLE_ADMIN:test"
        );

        ITApiRunLog saved = apiRunLogRepository.saveAndFlush(log);

        // Clear 1st-level cache to force database fetch
        entityManager.clear();

        ITApiRunLog reloaded = apiRunLogRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getRequestPayload()).contains("\"password\": \"[REDACTED]\"");
        assertThat(reloaded.getRequestPayload()).doesNotContain("RawPassword123");

        assertThat(reloaded.getResponsePayload()).contains("\"token\": \"[REDACTED_JWT]\"");
        assertThat(reloaded.getResponsePayload()).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(reloaded.getResponsePayload()).doesNotContain(SAMPLE_JWT);
        assertThat(reloaded.getResponsePayload()).doesNotContain("Sâu răng số 7");

        assertThat(reloaded.getIsSuccess()).isTrue();
        assertThat(reloaded.getRunTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("M1.5 - Database Persistence with Pre-Persist Sanitization for ITAgentMemory")
    void testAgentMemoryPrePersistSanitization() {
        String rawSecretMemory = "Server config with password=SuperAdminKey9988 and token=" + SAMPLE_JWT;

        ITAgentMemory memory = new ITAgentMemory(
                1L,
                "backend",
                "TEST_CREDENTIALS",
                rawSecretMemory,
                "HIGH"
        );

        ITAgentMemory saved = memoryRepository.saveAndFlush(memory);
        entityManager.clear();

        ITAgentMemory reloaded = memoryRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getMemoryContent()).contains("password=[REDACTED]");
        assertThat(reloaded.getMemoryContent()).contains("[REDACTED_JWT]");
        assertThat(reloaded.getMemoryContent()).doesNotContain("SuperAdminKey9988");
        assertThat(reloaded.getMemoryContent()).doesNotContain(SAMPLE_JWT);
        assertThat(reloaded.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("M1.6 - Database Persistence with Pre-Persist Sanitization for ITAgentMessage")
    void testAgentMessagePrePersistSanitization() {
        String rawMessageBody = "Please check patient records with Cookie: JSESSIONID=SECRETCOOKIE and password=MySecret";

        ITAgentMessage message = new ITAgentMessage(
                1L,
                "Dr. Thang",
                "USER",
                2L,
                "#it-security",
                rawMessageBody,
                "#it-security",
                null
        );

        ITAgentMessage saved = messageRepository.saveAndFlush(message);
        entityManager.clear();

        ITAgentMessage reloaded = messageRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getMessageBody()).contains("Cookie: [REDACTED]");
        assertThat(reloaded.getMessageBody()).contains("password=[REDACTED]");
        assertThat(reloaded.getMessageBody()).doesNotContain("SECRETCOOKIE");
        assertThat(reloaded.getMessageBody()).doesNotContain("MySecret");
        assertThat(reloaded.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("M1.7 - Database Persistence with Pre-Persist Sanitization for ITAgentActivity")
    void testAgentActivityPrePersistSanitization() {
        String rawDescription = "Agent executed task with Authorization: Bearer " + SAMPLE_JWT;
        String rawSummary = "Result note: Diagnosis: Viêm tủy cấp";

        ITAgentActivity activity = new ITAgentActivity(
                1L,
                "qa",
                "SECURITY_TEST",
                rawDescription,
                rawSummary,
                "/api/it-team/test"
        );

        ITAgentActivity saved = activityRepository.saveAndFlush(activity);
        entityManager.clear();

        ITAgentActivity reloaded = activityRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getDescription()).contains("Authorization: Bearer [REDACTED_JWT]");
        assertThat(reloaded.getDescription()).doesNotContain(SAMPLE_JWT);

        assertThat(reloaded.getResultSummary()).contains("Diagnosis: [REDACTED_MEDICAL]");
        assertThat(reloaded.getResultSummary()).doesNotContain("Viêm tủy cấp");
        assertThat(reloaded.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("M1.8 - Database Persistence with Pre-Persist Sanitization for ITBrowserTabRecord")
    void testBrowserTabRecordPrePersistSanitization() {
        String dirtyUrl = "http://localhost:8080/api/auth/reset?password=SecretPass123&token=" + SAMPLE_JWT;
        String dirtyTitle = "Reset Password (password=SecretPass123)";

        ITBrowserTabRecord tab = new ITBrowserTabRecord(
                1L,
                "backend",
                dirtyTitle,
                dirtyUrl,
                "SECURITY",
                "OPEN"
        );

        ITBrowserTabRecord saved = tabRepository.saveAndFlush(tab);
        entityManager.clear();

        ITBrowserTabRecord reloaded = tabRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getUrlRoute()).contains("password=[REDACTED]");
        assertThat(reloaded.getUrlRoute()).contains("[REDACTED_JWT]");
        assertThat(reloaded.getUrlRoute()).doesNotContain("SecretPass123");
        assertThat(reloaded.getUrlRoute()).doesNotContain(SAMPLE_JWT);

        assertThat(reloaded.getTabTitle()).contains("password=[REDACTED]");
        assertThat(reloaded.getTabTitle()).doesNotContain("SecretPass123");
        assertThat(reloaded.getOpenedAt()).isNotNull();
    }
}
