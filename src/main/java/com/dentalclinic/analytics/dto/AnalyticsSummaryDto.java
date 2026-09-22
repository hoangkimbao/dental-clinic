package com.dentalclinic.analytics.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated analytics metrics and funnel breakdown for Admin & Owner dashboard.
 */
public class AnalyticsSummaryDto {

    private long totalEvents;
    private Map<String, Long> eventsByType = new HashMap<>();
    private Map<String, Long> bookingFunnelCounts = new HashMap<>();
    private Map<String, Long> orderFunnelCounts = new HashMap<>();
    private Map<String, Long> topPages = new HashMap<>();
    private long activeSessionsCount;
    private List<RecentEventItem> recentEvents;
    private LocalDateTime generatedAt;

    public AnalyticsSummaryDto() {
        this.generatedAt = LocalDateTime.now();
    }

    public static class RecentEventItem {
        private Long id;
        private String eventType;
        private String eventName;
        private String pageUrl;
        private String clientSessionId;
        private String userRole;
        private LocalDateTime occurredAt;

        public RecentEventItem() {
        }

        public RecentEventItem(Long id, String eventType, String eventName, String pageUrl, String clientSessionId, String userRole, LocalDateTime occurredAt) {
            this.id = id;
            this.eventType = eventType;
            this.eventName = eventName;
            this.pageUrl = pageUrl;
            this.clientSessionId = clientSessionId;
            this.userRole = userRole;
            this.occurredAt = occurredAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        public String getPageUrl() { return pageUrl; }
        public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
        public String getClientSessionId() { return clientSessionId; }
        public void setClientSessionId(String clientSessionId) { this.clientSessionId = clientSessionId; }
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public Map<String, Long> getEventsByType() {
        return eventsByType;
    }

    public void setEventsByType(Map<String, Long> eventsByType) {
        this.eventsByType = eventsByType;
    }

    public Map<String, Long> getBookingFunnelCounts() {
        return bookingFunnelCounts;
    }

    public void setBookingFunnelCounts(Map<String, Long> bookingFunnelCounts) {
        this.bookingFunnelCounts = bookingFunnelCounts;
    }

    public Map<String, Long> getOrderFunnelCounts() {
        return orderFunnelCounts;
    }

    public void setOrderFunnelCounts(Map<String, Long> orderFunnelCounts) {
        this.orderFunnelCounts = orderFunnelCounts;
    }

    public Map<String, Long> getTopPages() {
        return topPages;
    }

    public void setTopPages(Map<String, Long> topPages) {
        this.topPages = topPages;
    }

    public long getActiveSessionsCount() {
        return activeSessionsCount;
    }

    public void setActiveSessionsCount(long activeSessionsCount) {
        this.activeSessionsCount = activeSessionsCount;
    }

    public List<RecentEventItem> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<RecentEventItem> recentEvents) {
        this.recentEvents = recentEvents;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
