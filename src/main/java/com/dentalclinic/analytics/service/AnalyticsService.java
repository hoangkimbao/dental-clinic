package com.dentalclinic.analytics.service;

import com.dentalclinic.analytics.dto.AnalyticsEventDto;
import com.dentalclinic.analytics.dto.AnalyticsSummaryDto;
import com.dentalclinic.analytics.model.AnalyticsEvent;
import com.dentalclinic.analytics.model.EventType;
import com.dentalclinic.analytics.repository.AnalyticsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for non-blocking asynchronous ingestion, strict sanitization,
 * batch persistence, and summary metrics retrieval of analytics events.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsDataSanitizer sanitizer;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsRepository analyticsRepository,
                            AnalyticsDataSanitizer sanitizer,
                            ObjectMapper objectMapper) {
        this.analyticsRepository = analyticsRepository;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
    }

    /**
     * Non-blocking asynchronous processing and batch persistence.
     * Guaranteed to process on background thread pool without blocking HTTP request thread.
     */
    @Async("analyticsTaskExecutor")
    @Transactional
    public CompletableFuture<Integer> processAndSaveBatchAsync(List<AnalyticsEventDto> dtoList,
                                                              String requestIp,
                                                              String requestUserAgent) {
        try {
            List<AnalyticsEvent> savedEntities = processAndSaveBatchSync(dtoList, requestIp, requestUserAgent);
            log.debug("Asynchronously ingested and saved {} analytics events", savedEntities.size());
            return CompletableFuture.completedFuture(savedEntities.size());
        } catch (Exception ex) {
            log.error("Failed to asynchronously process analytics batch: {}", ex.getMessage(), ex);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Core batch processing logic with strict sanitization and validation.
     */
    @Transactional
    public List<AnalyticsEvent> processAndSaveBatchSync(List<AnalyticsEventDto> dtoList,
                                                       String requestIp,
                                                       String requestUserAgent) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }

        List<AnalyticsEvent> entitiesToSave = new ArrayList<>(dtoList.size());

        for (AnalyticsEventDto dto : dtoList) {
            if (dto == null) continue;

            EventType eventType = EventType.fromString(dto.getEventType());
            String eventName = sanitizer.truncate(
                    sanitizer.sanitizeText(dto.getEventName() != null ? dto.getEventName() : "unknown_event"),
                    128
            );

            String pageUrl = sanitizer.truncate(
                    sanitizer.sanitizeText(dto.getPageUrl()),
                    1024
            );

            String referrer = sanitizer.truncate(
                    sanitizer.sanitizeText(dto.getReferrer()),
                    1024
            );

            String clientSessionId = sanitizer.truncate(
                    sanitizer.sanitizeText(dto.getClientSessionId()),
                    128
            );

            String userPhone = sanitizer.maskPhone(dto.getUserPhone());
            String userRole = sanitizer.truncate(sanitizer.sanitizeText(dto.getUserRole()), 32);

            // Handle metadata: prioritize metadata JsonNode serialization or metadataJson
            String metadataJson = null;
            if (dto.getMetadata() != null && !dto.getMetadata().isNull()) {
                try {
                    String raw = objectMapper.writeValueAsString(dto.getMetadata());
                    metadataJson = sanitizer.sanitizeText(raw);
                } catch (Exception e) {
                    log.warn("Could not serialize analytics event metadata: {}", e.getMessage());
                }
            } else if (dto.getMetadataJson() != null && !dto.getMetadataJson().isBlank()) {
                metadataJson = sanitizer.sanitizeText(dto.getMetadataJson());
            }

            // Client IP: prefer request IP, fallback to DTO IP
            String ipToUse = (requestIp != null && !requestIp.isBlank()) ? requestIp : dto.getIpAddress();
            String anonymizedIp = sanitizer.truncate(sanitizer.anonymizeIp(ipToUse), 64);

            // User Agent: prefer request User-Agent header, fallback to DTO
            String uaToUse = (requestUserAgent != null && !requestUserAgent.isBlank()) ? requestUserAgent : dto.getUserAgent();
            String sanitizedUa = sanitizer.truncate(sanitizer.sanitizeText(uaToUse), 512);

            LocalDateTime occurredAt = parseOccurredAt(dto.getOccurredAt());

            AnalyticsEvent entity = new AnalyticsEvent(
                    eventType,
                    eventName,
                    pageUrl,
                    referrer,
                    clientSessionId,
                    userPhone,
                    userRole,
                    metadataJson,
                    anonymizedIp,
                    sanitizedUa,
                    occurredAt
            );

            entitiesToSave.add(entity);
        }

        if (entitiesToSave.isEmpty()) {
            return List.of();
        }

        return analyticsRepository.saveAll(entitiesToSave);
    }

    /**
     * Aggregates summary statistics for Admin and Owner.
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getAnalyticsSummary() {
        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();

        summary.setTotalEvents(analyticsRepository.count());

        // Events grouped by Type
        List<Object[]> typeCounts = analyticsRepository.countGroupedByEventType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeCounts) {
            if (row != null && row.length >= 2 && row[0] != null) {
                typeMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        summary.setEventsByType(typeMap);

        // Booking Funnel counts
        List<Object[]> bookingFunnel = analyticsRepository.countGroupedByEventName(EventType.BOOKING_FUNNEL);
        Map<String, Long> bookingMap = new HashMap<>();
        for (Object[] row : bookingFunnel) {
            if (row != null && row.length >= 2 && row[0] != null) {
                bookingMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        summary.setBookingFunnelCounts(bookingMap);

        // Order Funnel counts
        List<Object[]> orderFunnel = analyticsRepository.countGroupedByEventName(EventType.ORDER_FUNNEL);
        Map<String, Long> orderMap = new HashMap<>();
        for (Object[] row : orderFunnel) {
            if (row != null && row.length >= 2 && row[0] != null) {
                orderMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        summary.setOrderFunnelCounts(orderMap);

        // Top pages
        List<Object[]> topPages = analyticsRepository.countTopPages(PageRequest.of(0, 10));
        Map<String, Long> pagesMap = new HashMap<>();
        for (Object[] row : topPages) {
            if (row != null && row.length >= 2 && row[0] != null) {
                pagesMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        summary.setTopPages(pagesMap);

        // Distinct active sessions
        summary.setActiveSessionsCount(analyticsRepository.countDistinctSessions());

        // Recent 20 events
        List<AnalyticsEvent> recent = analyticsRepository.findTop20ByOrderByOccurredAtDesc();
        List<AnalyticsSummaryDto.RecentEventItem> recentItems = new ArrayList<>();
        for (AnalyticsEvent event : recent) {
            recentItems.add(new AnalyticsSummaryDto.RecentEventItem(
                    event.getId(),
                    event.getEventType() != null ? event.getEventType().name() : null,
                    event.getEventName(),
                    event.getPageUrl(),
                    event.getClientSessionId(),
                    event.getUserRole(),
                    event.getOccurredAt()
            ));
        }
        summary.setRecentEvents(recentItems);

        return summary;
    }

    private LocalDateTime parseOccurredAt(Object raw) {
        if (raw == null) {
            return LocalDateTime.now();
        }

        if (raw instanceof LocalDateTime ldt) {
            return ldt;
        }

        if (raw instanceof Number number) {
            long epochMs = number.longValue();
            if (epochMs > 0) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
            }
        }

        String str = raw.toString().trim();
        if (str.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // Check if numeric string timestamp
            if (str.matches("^\\d{10,13}$")) {
                long epoch = Long.parseLong(str);
                if (str.length() == 10) epoch *= 1000;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
            }

            // ISO Instant (e.g. 2026-09-23T00:25:35.000Z)
            if (str.endsWith("Z") || str.contains("+")) {
                return LocalDateTime.ofInstant(Instant.parse(str), ZoneId.systemDefault());
            }

            // Standard ISO Local Date Time
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            log.debug("Unable to parse event occurredAt timestamp '{}', falling back to now()", str);
            return LocalDateTime.now();
        }
    }
}
