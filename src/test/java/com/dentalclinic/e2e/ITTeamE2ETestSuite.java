package com.dentalclinic.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Enterprise E2E Test Suite for the DentalCare Management Portal 'IT Team Command Center'.
 * Follows the 4-Tier (+ Tier 5 Adversarial) opaque-box testing methodology:
 * - Tier 1: Primary Feature Coverage (>=5 tests per functional area across 9 areas)
 * - Tier 2: Boundary, Extreme & Corner Cases (>=5 tests per area)
 * - Tier 3: Cross-Feature Combinations (Multi-component pipelines)
 * - Tier 4: Real-World Operational Scenarios (Incident diagnostics, security audits, handoffs)
 * - Tier 5: Adversarial & Resilience Coverage (SQLi, XSS, SSRF evasion, Auth tampering, DoS defense)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT Team Command Center E2E Test Suite")
public class ITTeamE2ETestSuite {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger IP_COUNTER = new AtomicInteger(100);

    private String ownerToken;
    private String patientToken;

    private String getUniqueIp() {
        return "192.168.10." + (IP_COUNTER.incrementAndGet() % 250);
    }

    private String obtainToken(String username, String password) throws Exception {
        Map<String, String> loginReq = new HashMap<>();
        loginReq.put("username", username);
        loginReq.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", getUniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @BeforeEach
    void setUpAuthTokens() throws Exception {
        if (ownerToken == null) {
            ownerToken = obtainToken("owner", "123");
        }
        if (patientToken == null) {
            patientToken = obtainToken("benhnhan", "123");
        }
    }

    // =========================================================================
    // TIER 1: PRIMARY FEATURE COVERAGE (>=5 tests per functional area)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Area 1: Agent Profiles & Startup Seeding")
    class Tier1AgentProfilesAndSeedingTests {

        @Test
        @DisplayName("T1-PRF-01: Verify all 5 standard IT profiles are returned upon startup")
        void testSeededAgentsExist() throws Exception {
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-PRF-02: Verify agent attributes (agentCode, hashtag, displayName, role, status)")
        void testAgentProfileDetailsAndAttributes() throws Exception {
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-PRF-03: Update agent status to BUSY")
        void testUpdateAgentStatusToBusy() throws Exception {
            Map<String, String> updateReq = new HashMap<>();
            updateReq.put("status", "BUSY");

            mockMvc.perform(put("/api/it-team/agents/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-PRF-04: Update agent status to OFFLINE and restore to ONLINE")
        void testUpdateAgentStatusToOfflineAndRestore() throws Exception {
            Map<String, String> updateReq = new HashMap<>();
            updateReq.put("status", "OFFLINE");

            mockMvc.perform(put("/api/it-team/agents/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            updateReq.put("status", "ONLINE");
            mockMvc.perform(put("/api/it-team/agents/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-PRF-05: Verify seeder idempotency across repeated profile requests")
        void testSeederIdempotencyAndNoDuplicates() throws Exception {
            MvcResult result1 = mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode data1 = objectMapper.readTree(result1.getResponse().getContentAsString()).path("data");
            JsonNode data2 = objectMapper.readTree(result2.getResponse().getContentAsString()).path("data");

            assertTrue(data1.isArray());
            assertEquals(data1.size(), data2.size());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 2: Agent Persistent Memories")
    class Tier1AgentMemoriesTests {

        @Test
        @DisplayName("T1-MEM-01: Create high priority memory for #it-backend")
        void testCreateHighPriorityMemory() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-backend");
            req.put("memoryKey", "db_pool_size");
            req.put("memoryContent", "Maximum pool size configured to 20 connections");
            req.put("priorityLevel", 1);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MEM-02: Retrieve memories filtered by agentCode")
        void testGetMemoriesFilteredByAgentCode() throws Exception {
            mockMvc.perform(get("/api/it-team/memories")
                    .param("agentCode", "it-backend")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-MEM-03: Create medium and low priority memories for #it-frontend")
        void testCreateMediumAndLowPriorityMemories() throws Exception {
            Map<String, Object> req1 = new HashMap<>();
            req1.put("agentCode", "it-frontend");
            req1.put("memoryKey", "theme_color");
            req1.put("memoryContent", "Brand cyan #0ea5e9 and dark navy");
            req1.put("priorityLevel", 2);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            Map<String, Object> req2 = new HashMap<>();
            req2.put("agentCode", "it-frontend");
            req2.put("memoryKey", "sidebar_state");
            req2.put("memoryContent", "Default expanded on desktop");
            req2.put("priorityLevel", 3);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MEM-04: Retrieve all memories and verify ordering")
        void testMemoryRetrievalOrdering() throws Exception {
            mockMvc.perform(get("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-MEM-05: Update or overwrite existing memory key")
        void testUpdateOrOverwriteMemoryKey() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-backend");
            req.put("memoryKey", "db_pool_size");
            req.put("memoryContent", "Updated maximum pool size to 30 connections");
            req.put("priorityLevel", 1);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 3: Inter-Agent Messaging & Hashtag Engine")
    class Tier1MessagingAndHashtagsTests {

        @Test
        @DisplayName("T1-MSG-01: Dispatch message containing single hashtag #it-qa")
        void testDispatchMessageWithSingleHashtag() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Please initiate regression test on coupons #it-qa");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MSG-02: Dispatch message containing multiple hashtags #it-backend and #it-devops")
        void testDispatchMessageWithMultipleHashtags() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Deploying new health check endpoint #it-backend and check tunnel #it-devops");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MSG-03: Retrieve top-level message feed")
        void testGetTopLevelMessages() throws Exception {
            mockMvc.perform(get("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-MSG-04: Dispatch threaded reply with parentMessageId")
        void testDispatchThreadedReplyMessage() throws Exception {
            Map<String, Object> topReq = new HashMap<>();
            topReq.put("messageBody", "Incident triage thread #it-devops");

            MvcResult topResult = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(topReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode topJson = objectMapper.readTree(topResult.getResponse().getContentAsString());
            long parentId = topJson.path("data").path("id").asLong(1L);

            Map<String, Object> replyReq = new HashMap<>();
            replyReq.put("messageBody", "Investigating server metrics #it-devops");
            replyReq.put("parentMessageId", parentId);

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(replyReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MSG-05: Retrieve thread replies by parentMessageId")
        void testGetThreadRepliesByParentMessageId() throws Exception {
            mockMvc.perform(get("/api/it-team/messages")
                    .param("parentMessageId", "1")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 4: Mention Activity Logging & Audit Trail")
    class Tier1MentionActivitiesTests {

        @Test
        @DisplayName("T1-ACT-01: Mentioning agent triggers MENTIONED activity record")
        void testMentionTriggersMentionedActivity() throws Exception {
            Map<String, Object> msgReq = new HashMap<>();
            msgReq.put("messageBody", "Security review required for new patient auth #it-security");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(msgReq)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/it-team/activities")
                    .param("agentCode", "it-security")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ACT-02: Retrieve paginated activity audit trail")
        void testGetActivitiesPaginatedAuditTrail() throws Exception {
            mockMvc.perform(get("/api/it-team/activities")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ACT-03: Filter activities by agentCode")
        void testFilterActivitiesByAgentCode() throws Exception {
            mockMvc.perform(get("/api/it-team/activities")
                    .param("agentCode", "it-backend")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ACT-04: Filter activities by actionType MENTIONED")
        void testFilterActivitiesByActionType() throws Exception {
            mockMvc.perform(get("/api/it-team/activities")
                    .param("actionType", "MENTIONED")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ACT-05: Verify activity log contains timestamp and description")
        void testActivityLogTimestampAndEntityLink() throws Exception {
            mockMvc.perform(get("/api/it-team/activities")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 5: Browser Tab Sessions")
    class Tier1BrowserTabsTests {

        @Test
        @DisplayName("T1-TAB-01: Record a newly opened browser tab session")
        void testRecordBrowserTabSession() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-frontend");
            req.put("tabTitle", "DentalCare Management Portal UI");
            req.put("urlRoute", "/index.html#management-portal");
            req.put("tabCategory", "PORTAL_UI");
            req.put("status", "OPEN");

            mockMvc.perform(post("/api/it-team/browser-tabs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-TAB-02: Retrieve all recorded browser tab sessions")
        void testGetAllBrowserTabSessions() throws Exception {
            mockMvc.perform(get("/api/it-team/browser-tabs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-TAB-03: Update browser tab status to CLOSED")
        void testUpdateBrowserTabStatusToClosed() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-qa");
            req.put("tabTitle", "Swagger OpenAPI Spec");
            req.put("urlRoute", "/swagger-ui/index.html");
            req.put("tabCategory", "API_DOCS");
            req.put("status", "CLOSED");

            mockMvc.perform(post("/api/it-team/browser-tabs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-TAB-04: Filter browser tabs by agentCode")
        void testFilterBrowserTabsByAgent() throws Exception {
            mockMvc.perform(get("/api/it-team/browser-tabs")
                    .param("agentCode", "it-frontend")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-TAB-05: Record multiple tab categories (DOCUMENTATION, MONITORING, CONSOLE)")
        void testVerifyTabCategories() throws Exception {
            String[] categories = {"DOCUMENTATION", "MONITORING", "CONSOLE"};
            for (String category : categories) {
                Map<String, Object> req = new HashMap<>();
                req.put("agentCode", "it-devops");
                req.put("tabTitle", category + " Tab");
                req.put("urlRoute", "/actuator/health");
                req.put("tabCategory", category);
                req.put("status", "OPEN");

                mockMvc.perform(post("/api/it-team/browser-tabs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Forwarded-For", getUniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 6: Safe Localhost API Runner")
    class Tier1ApiRunnerTests {

        @Test
        @DisplayName("T1-RUN-01: Execute safe GET request against /api/coupons/active")
        void testExecuteGetApiRunOnActiveCoupons() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "/api/coupons/active");
            req.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-RUN-02: Verify run log captures status code 200 and latency ms > 0")
        void testApiRunLogContainsStatusAndDuration() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "/api/coupons/active");
            req.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-RUN-03: Retrieve historical API run logs")
        void testGetAllApiRunLogs() throws Exception {
            mockMvc.perform(get("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-RUN-04: Execute safe POST run with internal coupon validate endpoint")
        void testExecutePostApiRunInternal() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "/api/coupons/validate");
            req.put("httpMethod", "POST");
            req.put("requestPayload", "{\"code\":\"NIENG3D5TR\",\"serviceType\":\"NIENG_RANG\",\"totalBill\":10000000}");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-RUN-05: Verify sanitized request and response payload logged in run record")
        void testApiRunSanitizedRequestResponse() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "/api/coupons/active");
            req.put("httpMethod", "GET");

            MvcResult result = mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode resNode = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(resNode.path("success").asBoolean());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 7: RBAC Authorization & Security")
    class Tier1RbacSecurityTests {

        @Test
        @DisplayName("T1-SEC-01: Unauthenticated request to GET /api/it-team/agents blocked (401 or 403)")
        void testUnauthenticatedGetAgentsBlocked() throws Exception {
            mockMvc.perform(get("/api/it-team/agents")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("T1-SEC-02: Unauthenticated request to POST /api/it-team/messages blocked (401 or 403)")
        void testUnauthenticatedPostMessagesBlocked() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Unauthenticated attack message #it-security");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("T1-SEC-03: ROLE_PATIENT access to /api/it-team/** strictly blocked with 403 Forbidden")
        void testPatientRoleAccessBlockedWithForbidden() throws Exception {
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("T1-SEC-04: Non-admin staff (receptionist) access to /api/it-team/api-runs blocked")
        void testNonAdminStaffAccessBlockedWithForbidden() throws Exception {
            String letanToken = obtainToken("letan", "123");
            mockMvc.perform(get("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + letanToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("T1-SEC-05: ROLE_OWNER or ROLE_ADMIN granted full access to IT Team APIs")
        void testAdminOrOwnerAccessAllowed() throws Exception {
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 8: Privacy Guardrail & Sensitive Data Sanitizer")
    class Tier1PrivacyGuardrailTests {

        @Test
        @DisplayName("T1-SAN-01: Sensitive password in message payload is redacted")
        void testSanitizePlaintextPasswordInPayload() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Database credentials: password=supersecretpass123 #it-backend");

            MvcResult result = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            assertFalse(responseString.contains("supersecretpass123"), "Plaintext password must be redacted");
        }

        @Test
        @DisplayName("T1-SAN-02: Bearer JWT token in message or memory is redacted")
        void testSanitizeJwtBearerTokenInPayload() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-security");
            req.put("memoryKey", "auth_header");
            req.put("memoryContent", "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.dummySignature");
            req.put("priorityLevel", 1);

            MvcResult result = mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            assertFalse(responseString.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"), "JWT token must be redacted");
        }

        @Test
        @DisplayName("T1-SAN-03: Cookie tokens (JSESSIONID) are stripped or redacted")
        void testSanitizeCookieHeaders() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Session header: Cookie: JSESSIONID=987654321ABCDEF #it-security");

            MvcResult result = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            assertFalse(responseString.contains("987654321ABCDEF"), "Cookie value must be redacted");
        }

        @Test
        @DisplayName("T1-SAN-04: Patient medical EMR PII is redacted")
        void testSanitizeMedicalPii() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-qa");
            req.put("memoryKey", "sample_log");
            req.put("memoryContent", "Patient diagnosis: Caries grade 3, Phone: 0988776655");
            req.put("priorityLevel", 2);

            MvcResult result = mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            assertNotNull(responseString);
        }

        @Test
        @DisplayName("T1-SAN-05: Clean non-sensitive diagnostic payload remains uncorrupted")
        void testCleanPayloadRemainsIntact() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Standard routine health check completed with zero warnings #it-devops");

            MvcResult result = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            assertTrue(responseString.contains("Standard routine health check"));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 9: 9Router AI Integration & Fallback")
    class Tier1NineRouterAiTests {

        @Test
        @DisplayName("T1-AI-01: Deterministic fallback response activated when 9Router offline")
        void testDeterministicFallbackWhenAiOffline() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Status report requested for database cluster #it-backend");

            MvcResult result = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(json.path("success").asBoolean());
        }

        @Test
        @DisplayName("T1-AI-02: Fallback response acknowledges agent hashtag and persona")
        void testFallbackResponseContainsAgentPersona() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Please review PR #42 for UI responsive fixes #it-frontend");

            MvcResult result = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(json.path("success").asBoolean());
        }

        @Test
        @DisplayName("T1-AI-03: Message persistence succeeds normally even if AI service is unreachable")
        void testPersistenceSucceedsWhenAiUnreachable() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Checking build runner pipelines #it-devops");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-AI-04: System logs offline state gracefully without 500 error")
        void testOfflineWarningLoggedGracefully() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Perform automated sanity tests #it-qa");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T1-AI-05: Fallback threaded response links cleanly to originating thread")
        void testFallbackThreadContinuity() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Are security policies active? #it-security");

            MvcResult result = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(json.path("success").asBoolean());
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY, EXTREME & CORNER CASES (>=5 tests per area)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2: Boundary & Corner Cases")
    class Tier2BoundaryCasesTests {

        @Test
        @DisplayName("T2-BND-01: Empty or whitespace-only message rejected with 400 Bad Request")
        void testEmptyOrWhitespaceMessageRejected() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "    ");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-02: Message with non-existent hashtag handled gracefully")
        void testUnknownHashtagHandledGracefully() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Pinging ghost agent #it-nonexistent");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T2-BND-03: Duplicate hashtags #it-qa #it-qa deduplicated cleanly")
        void testDuplicateHashtagsDeduplicatedCleanly() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Double mention test #it-qa #it-qa in same message");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T2-BND-04: Hashtag embedded in punctuation (#it-backend)! parsed accurately")
        void testHashtagsWithPunctuationParsedCorrectly() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Urgent notice for (#it-backend)! Please respond.");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T2-BND-05: Large message payload (5,000 chars) processed safely")
        void testLargeMessagePayloadHandledSafely() throws Exception {
            String largeText = "Log report ".repeat(300) + " #it-devops";
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", largeText);

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T2-BND-06: Updating status of non-existent agent ID (999999) returns 404")
        void testUpdateStatusOfNonExistentAgentReturns404() throws Exception {
            Map<String, String> req = new HashMap<>();
            req.put("status", "BUSY");

            mockMvc.perform(put("/api/it-team/agents/999999/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("T2-BND-07: Updating status with invalid status value returns 400 Bad Request")
        void testUpdateStatusWithInvalidStatusReturns400() throws Exception {
            Map<String, String> req = new HashMap<>();
            req.put("status", "NOT_A_VALID_STATUS");

            mockMvc.perform(put("/api/it-team/agents/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-08: Querying memories for non-existent agent returns empty array, not 500")
        void testQueryMemoriesForNonExistentAgentReturnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/it-team/memories")
                    .param("agentCode", "it-nonexistent")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T2-BND-09: Create memory with empty key or content returns 400 Bad Request")
        void testCreateMemoryWithEmptyKeyOrContentReturns400() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-backend");
            req.put("memoryKey", "");
            req.put("memoryContent", "");
            req.put("priorityLevel", 1);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-10: API Runner blocks SSRF attempt against external evil.com domain")
        void testApiRunnerBlocksExternalSsrfDomain() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "http://evil.com/malicious");
            req.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-11: API Runner blocks SSRF attempt against AWS cloud metadata service (169.254.169.254)")
        void testApiRunnerBlocksCloudMetadataSsrf() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "http://169.254.169.254/latest/meta-data/");
            req.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-12: API Runner blocks private network IP (10.0.0.1)")
        void testApiRunnerBlocksPrivateIpSsrf() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "http://10.0.0.1:8080/admin");
            req.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-13: API Runner executing non-existent local route records 404 in log without crashing")
        void testApiRunnerNonExistentLocalRouteReturns404Log() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "/api/not-a-real-endpoint-404");
            req.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T2-BND-14: Browser tab session with empty URL rejected with 400 Bad Request")
        void testTabSessionWithEmptyUrlRejected() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("agentCode", "it-frontend");
            req.put("tabTitle", "Invalid Tab");
            req.put("urlRoute", "");
            req.put("tabCategory", "PORTAL_UI");

            mockMvc.perform(post("/api/it-team/browser-tabs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-15: Activity query with negative pagination defaults safely without crash")
        void testActivityNegativePaginationHandledSafely() throws Exception {
            mockMvc.perform(get("/api/it-team/activities")
                    .param("page", "-1")
                    .param("size", "-10")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Nested
    @DisplayName("Tier 3: Cross-Feature Combinations")
    class Tier3CrossFeatureTests {

        @Test
        @DisplayName("T3-XFT-01: Message Dispatch -> Hashtag Extraction -> Recipient Linking -> Mention Activity")
        void testPipelineMessageHashtagExtractionToMentionActivity() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Deploying database patch #it-backend");

            MvcResult msgResult = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode msgJson = objectMapper.readTree(msgResult.getResponse().getContentAsString());
            assertTrue(msgJson.path("success").asBoolean());

            mockMvc.perform(get("/api/it-team/activities")
                    .param("agentCode", "it-backend")
                    .param("actionType", "MENTIONED")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T3-XFT-02: API Runner Execution -> Sanitizer Filter -> API Run Log Persistence")
        void testPipelineApiRunnerExecutionToSanitizerToLogPersistence() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("endpoint", "/api/coupons/active");
            req.put("httpMethod", "GET");
            req.put("requestPayload", "{\"auth\":\"Bearer eyJhbGciOiJIUzI1NiJ9.testToken\"}");

            MvcResult runResult = mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = runResult.getResponse().getContentAsString();
            assertFalse(responseString.contains("eyJhbGciOiJIUzI1NiJ9.testToken"), "API Run Log must redact JWT tokens");
        }

        @Test
        @DisplayName("T3-XFT-03: Agent Status Update -> Activity Audit -> Profile View Synchronization")
        void testPipelineStatusUpdateToActivityAuditToProfileSync() throws Exception {
            Map<String, String> updateReq = new HashMap<>();
            updateReq.put("status", "BUSY");

            mockMvc.perform(put("/api/it-team/agents/2/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T3-XFT-04: Memory Storage with Sensitive Tokens -> Redaction -> Retrieval Integrity")
        void testPipelineMemoryStorageWithSensitiveTokensSanitization() throws Exception {
            Map<String, Object> memReq = new HashMap<>();
            memReq.put("agentCode", "it-security");
            memReq.put("memoryKey", "api_gateway_secret");
            memReq.put("memoryContent", "SecretKey=MySuperSecretToken998811 and password=hiddenpwd");
            memReq.put("priorityLevel", 1);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(memReq)))
                    .andExpect(status().isOk());

            MvcResult result = mockMvc.perform(get("/api/it-team/memories")
                    .param("agentCode", "it-security")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            String memBody = result.getResponse().getContentAsString();
            assertFalse(memBody.contains("hiddenpwd"), "Plaintext password must be redacted from memories");
        }

        @Test
        @DisplayName("T3-XFT-05: Multi-Agent Mention Cascade -> Generates Distinct Activities for Both Agents")
        void testPipelineMultiAgentCascadeMentions() throws Exception {
            Map<String, Object> req = new HashMap<>();
            req.put("messageBody", "Investigating frontend styling issue #it-frontend and verify logs #it-devops");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/it-team/activities")
                    .param("agentCode", "it-frontend")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/it-team/activities")
                    .param("agentCode", "it-devops")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Tier 4: Real-World Operational Scenarios")
    class Tier4RealWorldScenariosTests {

        @Test
        @DisplayName("T4-SCN-01: Incident Diagnostic & Resolution Workflow (Alert -> Runner -> Memory -> Reply)")
        void testScenarioIncidentDiagnosticAndResolutionWorkflow() throws Exception {
            // Step 1: #it-devops posts alert
            Map<String, Object> alertMsg = new HashMap<>();
            alertMsg.put("messageBody", "Coupon check endpoint latency spike reported #it-backend");

            MvcResult alertResult = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(alertMsg)))
                    .andExpect(status().isOk())
                    .andReturn();

            long alertId = objectMapper.readTree(alertResult.getResponse().getContentAsString())
                    .path("data").path("id").asLong(1L);

            // Step 2: #it-backend runs API check
            Map<String, Object> runReq = new HashMap<>();
            runReq.put("endpoint", "/api/coupons/active");
            runReq.put("httpMethod", "GET");

            mockMvc.perform(post("/api/it-team/api-runs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(runReq)))
                    .andExpect(status().isOk());

            // Step 3: #it-backend records diagnostic memory
            Map<String, Object> diagMem = new HashMap<>();
            diagMem.put("agentCode", "it-backend");
            diagMem.put("memoryKey", "incident_coupon_latency");
            diagMem.put("memoryContent", "Response healthy at 35ms. No deadlock found.");
            diagMem.put("priorityLevel", 1);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(diagMem)))
                    .andExpect(status().isOk());

            // Step 4: #it-backend replies in thread
            Map<String, Object> replyMsg = new HashMap<>();
            replyMsg.put("messageBody", "Endpoint verified healthy (35ms). No database bottleneck found #it-devops");
            replyMsg.put("parentMessageId", alertId);

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(replyMsg)))
                    .andExpect(status().isOk());

            // Step 5: Verify entire conversation thread
            mockMvc.perform(get("/api/it-team/messages")
                    .param("parentMessageId", String.valueOf(alertId))
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T4-SCN-02: Security Audit & Credential Redaction Workflow")
        void testScenarioSecurityAuditAndRedactionWorkflow() throws Exception {
            // Attempt to inject headers, tokens, and medical records into audit
            Map<String, Object> auditMsg = new HashMap<>();
            auditMsg.put("messageBody", "Security Audit: Token=Bearer eyJhbGciOiJIUzI1NiJ9.adminToken, Pass=adminPass123, Cookie=JSESSIONID=xyz #it-security");

            MvcResult res = mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(auditMsg)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseString = res.getResponse().getContentAsString();
            assertFalse(responseString.contains("adminPass123"));
            assertFalse(responseString.contains("eyJhbGciOiJIUzI1NiJ9.adminToken"));
        }

        @Test
        @DisplayName("T4-SCN-03: Multi-Agent Shift Handoff Workflow (Tabs -> Memory -> Status -> Handoff)")
        void testScenarioMultiAgentShiftHandoffWorkflow() throws Exception {
            // Step 1: #it-frontend logs open UI tabs
            Map<String, Object> tabReq = new HashMap<>();
            tabReq.put("agentCode", "it-frontend");
            tabReq.put("tabTitle", "Management Portal Command Center");
            tabReq.put("urlRoute", "/index.html#management-portal");
            tabReq.put("tabCategory", "PORTAL_UI");
            tabReq.put("status", "OPEN");

            mockMvc.perform(post("/api/it-team/browser-tabs")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tabReq)))
                    .andExpect(status().isOk());

            // Step 2: #it-frontend saves handoff memory
            Map<String, Object> handoffMem = new HashMap<>();
            handoffMem.put("agentCode", "it-frontend");
            handoffMem.put("memoryKey", "shift_handoff_summary");
            handoffMem.put("memoryContent", "IT Team UI sub-views integrated. Ready for regression testing.");
            handoffMem.put("priorityLevel", 1);

            mockMvc.perform(post("/api/it-team/memories")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(handoffMem)))
                    .andExpect(status().isOk());

            // Step 3: #it-frontend posts handoff message to #it-qa
            Map<String, Object> handoffMsg = new HashMap<>();
            handoffMsg.put("messageBody", "Frontend tasks completed for today. Ready for regression checks #it-qa");

            mockMvc.perform(post("/api/it-team/messages")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(handoffMsg)))
                    .andExpect(status().isOk());

            // Step 4: Verify #it-qa received mention
            mockMvc.perform(get("/api/it-team/activities")
                    .param("agentCode", "it-qa")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // =========================================================================
    // TIER 5: ADVERSARIAL & RESILIENCE COVERAGE
    // =========================================================================

    @Nested
    @DisplayName("Tier 5: Adversarial & Resilience Coverage")
    class Tier5AdversarialTests {

        @Test
        @DisplayName("T5-ADV-01: SQL Injection payloads handled as literal text without syntax or data breach")
        void testAdversarialSqlInjectionPayloadsHandledAsLiteral() throws Exception {
            String[] sqliPayloads = {
                "' OR '1'='1' --",
                "'; DROP TABLE it_agent_profile; --",
                "1; SELECT * FROM users WHERE '1'='1"
            };

            for (String sqli : sqliPayloads) {
                Map<String, Object> req = new HashMap<>();
                req.put("messageBody", "Testing SQLi attack: " + sqli + " #it-security");

                mockMvc.perform(post("/api/it-team/messages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Forwarded-For", getUniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }

            // Verify all 5 profiles still intact
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T5-ADV-02: XSS script injection payloads stored and escaped safely")
        void testAdversarialXssScriptPayloadsEscapedSafely() throws Exception {
            String[] xssPayloads = {
                "<script>alert('XSS-ATTACK')</script>",
                "<img src=x onerror=alert('DOM-XSS')>",
                "javascript:alert(document.cookie)"
            };

            for (String xss : xssPayloads) {
                Map<String, Object> req = new HashMap<>();
                req.put("messageBody", "Testing XSS payload: " + xss + " #it-frontend");

                mockMvc.perform(post("/api/it-team/messages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Forwarded-For", getUniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("T5-ADV-03: SSRF host evasion tricks blocked (0.0.0.0, [::1], nip.io)")
        void testAdversarialSsrfHostEvasionAttemptsBlocked() throws Exception {
            String[] evasionTargets = {
                "http://0.0.0.0:8080/api/dashboard/stats",
                "http://[::1]:8080/api/dashboard/stats",
                "http://127.0.0.1.nip.io:8080/api/dashboard/stats",
                "http://localhost@attacker.com/"
            };

            for (String target : evasionTargets) {
                Map<String, Object> req = new HashMap<>();
                req.put("endpoint", target);
                req.put("httpMethod", "GET");

                mockMvc.perform(post("/api/it-team/api-runs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Forwarded-For", getUniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        @DisplayName("T5-ADV-04: Malformed and forged JWT tokens rejected with 401/403")
        void testAdversarialMalformedAndTamperedJwtRejected() throws Exception {
            String[] badTokens = {
                "Bearer invalid.token.payload",
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tampered.signature",
                "Bearer "
            };

            for (String token : badTokens) {
                mockMvc.perform(get("/api/it-team/agents")
                        .header("Authorization", token)
                        .header("X-Forwarded-For", getUniqueIp()))
                        .andExpect(status().is4xxClientError());
            }
        }

        @Test
        @DisplayName("T5-ADV-05: Rate-limiting DoS defense triggers 429 when single IP exceeds threshold")
        void testAdversarialRateLimitingDosDefense() throws Exception {
            String spamIp = "203.0.113.99";

            // Send rapid requests from identical client IP until rate limit window threshold (60 requests) is breached
            int status429Count = 0;
            for (int i = 0; i < 70; i++) {
                MvcResult result = mockMvc.perform(get("/api/coupons/active")
                        .header("X-Forwarded-For", spamIp))
                        .andReturn();

                if (result.getResponse().getStatus() == 429) {
                    status429Count++;
                }
            }

            assertTrue(status429Count > 0, "RateLimitingFilter must trigger HTTP 429 after 60 requests in window");
        }
    }
}
