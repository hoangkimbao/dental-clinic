package com.dentalclinic.analytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing incoming analytics tracking events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsEventDto {

    private String eventType;

    private String eventName;

    private String pageUrl;

    private String referrer;

    @JsonAlias({"sessionId", "clientSessionId"})
    private String clientSessionId;

    @JsonAlias({"phone", "userPhone", "patientPhone"})
    private String userPhone;

    @JsonAlias({"role", "userRole"})
    private String userRole;

    private String metadataJson;

    @JsonAlias({"properties", "metadata", "params"})
    private JsonNode metadata;

    private String ipAddress;

    private String userAgent;

    @JsonAlias({"timestamp", "occurredAt", "eventTimestamp"})
    private Object occurredAt;

    public AnalyticsEventDto() {
    }

    public AnalyticsEventDto(String eventType,
                             String eventName,
                             String pageUrl,
                             String referrer,
                             String clientSessionId,
                             String userPhone,
                             String userRole,
                             String metadataJson,
                             String ipAddress,
                             String userAgent,
                             Object occurredAt) {
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
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
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

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
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

    public Object getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Object occurredAt) {
        this.occurredAt = occurredAt;
    }
}
