package com.dentalclinic.itteam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entity PrePersist Sanitization Unit Tests")
public class EntityPrePersistenceSanitizationTest {

    private static final String SAMPLE_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    @DisplayName("R2.1 - ITApiRunLog prePersist hook automatically sanitizes request and response payloads")
    void testApiRunLogPrePersistHook() {
        String rawRequest = "{\"username\": \"owner\", \"password\": \"UnsanitizedPass123\"}";
        String rawResponse = "{\"token\": \"" + SAMPLE_JWT + "\", \"diagnosis\": \"Sâu răng số 7\"}";

        ITApiRunLog log = new ITApiRunLog(
                "/api/test",
                "POST",
                200,
                45L,
                rawRequest,
                rawResponse,
                "ROLE_ADMIN:test"
        );

        // Act: trigger prePersist callback
        log.prePersist();

        // Assert: payloads sanitized prior to persistence
        assertThat(log.getRequestPayload()).contains("\"password\": \"[REDACTED]\"");
        assertThat(log.getRequestPayload()).doesNotContain("UnsanitizedPass123");

        assertThat(log.getResponsePayload()).contains("\"token\": \"[REDACTED_JWT]\"");
        assertThat(log.getResponsePayload()).contains("\"diagnosis\": \"[REDACTED_MEDICAL]\"");
        assertThat(log.getResponsePayload()).doesNotContain(SAMPLE_JWT);
        assertThat(log.getResponsePayload()).doesNotContain("Sâu răng số 7");
    }

    @Test
    @DisplayName("R2.2 - ITAgentMemory prePersist hook automatically sanitizes memoryContent")
    void testAgentMemoryPrePersistHook() {
        String rawMemory = "Configured admin password=DatabaseRootSecret! with API key=sk-secret-12345";
        ITAgentMemory memory = new ITAgentMemory(
                1L,
                "it-backend",
                "db-credentials",
                rawMemory,
                "CRITICAL"
        );

        // Act
        memory.prePersist();

        // Assert
        assertThat(memory.getMemoryContent()).contains("password=[REDACTED]");
        assertThat(memory.getMemoryContent()).contains("key=[REDACTED]");
        assertThat(memory.getMemoryContent()).doesNotContain("DatabaseRootSecret!");
        assertThat(memory.getMemoryContent()).doesNotContain("sk-secret-12345");
    }

    @Test
    @DisplayName("R2.3 - ITAgentActivity prePersist hook automatically sanitizes description")
    void testAgentActivityPrePersistHook() {
        String rawDescription = "User logged in with Cookie: JSESSIONID=SECRET98765 and Bearer " + SAMPLE_JWT;
        ITAgentActivity activity = new ITAgentActivity(
                null,
                "system",
                "SECURITY_AUDIT",
                rawDescription,
                "SUCCESS",
                "auth:login"
        );

        // Act
        activity.prePersist();

        // Assert
        assertThat(activity.getDescription()).contains("Cookie: [REDACTED]");
        assertThat(activity.getDescription()).contains("Bearer [REDACTED_JWT]");
        assertThat(activity.getDescription()).doesNotContain("SECRET98765");
        assertThat(activity.getDescription()).doesNotContain(SAMPLE_JWT);
    }

    @Test
    @DisplayName("R2.4 - ITAgentMessage prePersist hook automatically sanitizes messageBody")
    void testAgentMessagePrePersistHook() {
        String rawBody = "Message with password=secret123 and diagnosis: Viêm lợi";
        ITAgentMessage message = new ITAgentMessage(
                1L,
                "Admin",
                "USER",
                2L,
                "#it-backend",
                rawBody,
                "#it-backend",
                null
        );

        // Act
        message.prePersist();

        // Assert
        assertThat(message.getMessageBody()).contains("password=[REDACTED]");
        assertThat(message.getMessageBody()).contains("diagnosis: [REDACTED_MEDICAL]");
        assertThat(message.getMessageBody()).doesNotContain("secret123");
        assertThat(message.getMessageBody()).doesNotContain("Viêm lợi");
    }
}
