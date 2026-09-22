package com.dentalclinic.analytics.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Domain entity representing an ingested user behavior tracking event.
 * Stores sanitized interactions across Web, Mobile, and PC Desktop applications.
 */
@Entity
@Table(name = "analytics_events", indexes = {
        @Index(name = "idx_analytics_event_type", columnList = "event_type"),
        @Index(name = "idx_analytics_event_name", columnList = "event_name"),
        @Index(name = "idx_analytics_session_id", columnList = "client_session_id"),
        @Index(name = "idx_analytics_occurred_at", columnList = "occurred_at")
})
public class AnalyticsEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(name = "event_name", nullable = false, length = 128)
    private String eventName;

    @Column(name = "page_url", length = 1024)
    private String pageUrl;

    @Column(name = "referrer", length = 1024)
    private String referrer;

    @Column(name = "client_session_id", length = 128)
    private String clientSessionId;

    @Column(name = "user_phone", length = 32)
    private String userPhone;

    @Column(name = "user_role", length = 32)
    private String userRole;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public AnalyticsEvent() {
    }

    public AnalyticsEvent(EventType eventType,
                          String eventName,
                          String pageUrl,
                          String referrer,
                          String clientSessionId,
                          String userPhone,
                          String userRole,
                          String metadataJson,
                          String ipAddress,
                          String userAgent,
                          LocalDateTime occurredAt) {
        this.eventType = eventType;
        this.eventName = eventName;
        this.pageUrl = pageUrl;
        this.referrer = referrer;
        this.clientSessionId = clientSessionId;
        this.userPhone = userPhone;
        this.userRole = userRole;
        this.metadataJson = metadataJson;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.occurredAt = (occurredAt != null) ? occurredAt : LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getClientSessionId() {
        return clientSessionId;
    }

    public void setClientSessionId(String clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
