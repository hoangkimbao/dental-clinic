package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity logging safe execution runs against internal API endpoints.
 * Payloads are sanitized with SensitiveDataSanitizer prior to persisting.
 */
@Entity
@Table(name = "it_api_run_log", indexes = {
    @Index(name = "idx_run_endpoint", columnList = "endpoint"),
    @Index(name = "idx_run_method", columnList = "http_method"),
    @Index(name = "idx_run_status", columnList = "status_code"),
    @Index(name = "idx_run_timestamp", columnList = "run_timestamp")
})
public class ITApiRunLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint; // e.g. "/api/dashboard/stats"

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod; // "GET", "POST", "PUT", "DELETE"

    @Column(name = "status_code")
    private Integer statusCode; // e.g. 200, 401, 403, 500, or null if connection failed pre-handshake

    @Column(name = "execution_duration_ms", nullable = false)
    private Long executionDurationMs;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload; // Sanitized request body/query

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload; // Sanitized response body

    @Column(name = "run_timestamp", nullable = false)
    private LocalDateTime runTimestamp;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy; // e.g. "ROLE_ADMIN:owner" or "it-qa"

    @Column(name = "is_success")
    private Boolean isSuccess = false;

    public ITApiRunLog() {
    }

    public ITApiRunLog(String endpoint, String httpMethod, Integer statusCode,
                       Long executionDurationMs, String requestPayload,
                       String responsePayload, String initiatedBy) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.executionDurationMs = executionDurationMs;
        this.requestPayload = requestPayload != null ? SensitiveDataSanitizer.sanitize(requestPayload) : null;
        this.responsePayload = responsePayload != null ? SensitiveDataSanitizer.sanitize(responsePayload) : null;
        this.initiatedBy = initiatedBy;
        this.isSuccess = (statusCode != null && statusCode >= 200 && statusCode < 400);
        this.runTimestamp = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.requestPayload != null) {
            this.requestPayload = SensitiveDataSanitizer.sanitize(this.requestPayload);
        }
        if (this.responsePayload != null) {
            this.responsePayload = SensitiveDataSanitizer.sanitize(this.responsePayload);
        }
        if (this.runTimestamp == null) {
            this.runTimestamp = LocalDateTime.now();
        }
        if (this.isSuccess == null) {
            this.isSuccess = (this.statusCode != null && this.statusCode >= 200 && this.statusCode < 400);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        this.isSuccess = (statusCode != null && statusCode >= 200 && statusCode < 400);
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public LocalDateTime getRunTimestamp() {
        return runTimestamp;
    }

    public void setRunTimestamp(LocalDateTime runTimestamp) {
        this.runTimestamp = runTimestamp;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
