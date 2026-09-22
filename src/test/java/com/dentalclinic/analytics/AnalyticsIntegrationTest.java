package com.dentalclinic.analytics;

import com.dentalclinic.analytics.dto.AnalyticsEventDto;
import com.dentalclinic.analytics.model.AnalyticsEvent;
import com.dentalclinic.analytics.model.EventType;
import com.dentalclinic.analytics.repository.AnalyticsRepository;
import com.dentalclinic.analytics.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Test
    @DisplayName("POST /api/analytics/events with single event should return HTTP 202 Accepted")
    void testIngestSingleEvent() throws Exception {
        Map<String, Object> singleEvent = Map.of(
                "eventType", "PAGE_VIEW",
                "eventName", "view_pricing",
                "pageUrl", "https://dentalcare.vn/#pricing",
                "clientSessionId", "sess-test-001",
                "userRole", "ANONYMOUS"
        );

        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleEvent)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.receivedCount").value(1));
    }

    @Test
    @DisplayName("POST /api/analytics/events with batch array of events should return HTTP 202 Accepted")
    void testIngestBatchEvents() throws Exception {
        List<Map<String, Object>> batch = List.of(
                Map.of("eventType", "CLICK", "eventName", "btn_book_now", "clientSessionId", "sess-batch-1"),
                Map.of("eventType", "BOOKING_FUNNEL", "eventName", "funnel_step_1", "clientSessionId", "sess-batch-1"),
                Map.of("eventType", "ORDER_FUNNEL", "eventName", "order_step_checkout", "clientSessionId", "sess-batch-1")
        );

        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.receivedCount").value(3));
    }

    @Test
    @DisplayName("POST /api/analytics/events should accept text/plain Blob payload from navigator.sendBeacon")
    void testIngestSendBeaconBlobPayload() throws Exception {
        String payload = "[{\"eventType\":\"QR_SCAN\",\"eventName\":\"qr_scanned\",\"clientSessionId\":\"beacon-sess\"}]";

        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receivedCount").value(1));
    }

    @Test
    @DisplayName("GET /api/analytics/summary without authentication should be blocked (401 or 403)")
    void testSummaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    @DisplayName("GET /api/analytics/summary with ROLE_PATIENT should return 403 Forbidden")
    void testSummaryForbiddenForPatient() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/analytics/summary with ROLE_ADMIN should succeed and return metrics")
    void testSummaryAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEvents").isNumber())
                .andExpect(jsonPath("$.data.eventsByType").isMap())
                .andExpect(jsonPath("$.data.bookingFunnelCounts").isMap());
    }

    @Test
    @DisplayName("AnalyticsService should sanitize and persist batch data genuinely")
    void testServiceDirectBatchPersistenceAndSanitization() {
        AnalyticsEventDto dto = new AnalyticsEventDto();
        dto.setEventType("BOOKING_FUNNEL");
        dto.setEventName("step_3_select_time");
        dto.setPageUrl("https://dentalcare.vn/#booking?token=eySecretToken");
        dto.setUserPhone("0977224504");
        dto.setClientSessionId("test-sess-direct");
        dto.setMetadataJson("{\"password\": \"PlainPassword123\", \"diagnosis\": \"Sâu răng hàm số 6\", \"cccd\": \"079123456789\"}");
        dto.setIpAddress("192.168.1.150");

        List<AnalyticsEvent> saved = analyticsService.processAndSaveBatchSync(List.of(dto), "192.168.1.150", "Mozilla/5.0 Test");

        assertNotNull(saved);
        assertEquals(1, saved.size());

        AnalyticsEvent entity = saved.get(0);
        assertNotNull(entity.getId());
        assertEquals(EventType.BOOKING_FUNNEL, entity.getEventType());
        assertEquals("09****4504", entity.getUserPhone(), "Phone must be masked");
        assertEquals("192.168.1.xxx", entity.getIpAddress(), "IP must be anonymized");

        // Verify PII & Medical redaction in metadata
        String metadata = entity.getMetadataJson();
        assertNotNull(metadata);
        assertFalse(metadata.contains("PlainPassword123"), "Password must be redacted");
        assertFalse(metadata.contains("079123456789"), "CCCD must be redacted");
        assertFalse(metadata.contains("Sâu răng"), "Clinical dental condition must be redacted");
        assertTrue(metadata.contains("[REDACTED]"));
        assertTrue(metadata.contains("[REDACTED_ID]"));
    }
}
