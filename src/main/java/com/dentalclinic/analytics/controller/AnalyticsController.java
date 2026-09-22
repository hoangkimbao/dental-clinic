package com.dentalclinic.analytics.controller;

import com.dentalclinic.analytics.dto.AnalyticsEventDto;
import com.dentalclinic.analytics.dto.AnalyticsSummaryDto;
import com.dentalclinic.analytics.service.AnalyticsService;
import com.dentalclinic.common.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for public analytics event ingestion and RBAC-protected metrics aggregation.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Tracking", description = "Endpoints for user behavior ingestion and funnel analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public AnalyticsController(AnalyticsService analyticsService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingestion endpoint for single or batched analytics events.
     * Always returns HTTP 202 Accepted immediately, delegating persistence to non-blocking async executor.
     * Supports application/json, text/plain (Blob from sendBeacon), and */*.
     */
    @PostMapping(value = "/events", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "*/*"})
    @Operation(summary = "Ingest single or batch analytics events", description = "Non-blocking ingestion returning HTTP 202 Accepted")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestEvents(
            @RequestBody String rawPayload,
            HttpServletRequest request) {

        List<AnalyticsEventDto> eventList = new ArrayList<>();

        if (rawPayload != null && !rawPayload.isBlank()) {
            try {
                JsonNode rootNode = objectMapper.readTree(rawPayload);
                if (rootNode.isArray()) {
                    for (JsonNode item : rootNode) {
                        AnalyticsEventDto dto = objectMapper.convertValue(item, AnalyticsEventDto.class);
                        if (dto != null) {
                            eventList.add(dto);
                        }
                    }
                } else if (rootNode.isObject()) {
                    AnalyticsEventDto dto = objectMapper.convertValue(rootNode, AnalyticsEventDto.class);
                    if (dto != null) {
                        eventList.add(dto);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse analytics payload: {}. Payload sample: {}", e.getMessage(),
                        rawPayload.length() > 200 ? rawPayload.substring(0, 200) + "..." : rawPayload);
            }
        }

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (!eventList.isEmpty()) {
            analyticsService.processAndSaveBatchAsync(eventList, clientIp, userAgent);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "ACCEPTED");
        responseData.put("receivedCount", eventList.size());
        responseData.put("async", true);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Events accepted for ingestion", responseData));
    }

    /**
     * Metrics and funnel analytics summary for Owner and Admin dashboards.
     * Protected by Role-Based Access Control (ROLE_ADMIN, ROLE_OWNER).
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Get analytics metrics and conversion funnel summary", description = "Requires ROLE_ADMIN or ROLE_OWNER")
    public ResponseEntity<ApiResponse<AnalyticsSummaryDto>> getSummary() {
        AnalyticsSummaryDto summary = analyticsService.getAnalyticsSummary();
        return ResponseEntity.ok(ApiResponse.success("Thống kê hành vi người dùng và phễu chuyển đổi", summary));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
