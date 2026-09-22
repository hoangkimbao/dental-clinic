package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity recording agent audit events and actions, such as MENTIONED triggers,
 * memory operations, API tests, and configuration adjustments.
 */
@Entity
@Table(name = "it_agent_activity", indexes = {
    @Index(name = "idx_act_agent_id", columnList = "agent_id"),
    @Index(name = "idx_act_agent_code", columnList = "agent_code"),
    @Index(name = "idx_act_action_type", columnList = "action_type"),
    @Index(name = "idx_act_timestamp", columnList = "timestamp")
})
public class ITAgentActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agent_code", length = 50)
    private String agentCode; // e.g. "backend", "it-backend"

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // "MENTIONED", "API_RUN", "MEMORY_UPDATE", "STATUS_CHANGE", "TAB_OPEN"

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "result_summary", length = 500)
    private String resultSummary;

    @Column(name = "related_entity_link", length = 255)
    private String relatedEntityLink; // e.g. "message:42", "api_run:15", "tab:7"

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public ITAgentActivity() {
    }

    public ITAgentActivity(Long agentId, String agentCode, String actionType,
                           String description, String resultSummary,
                           String relatedEntityLink) {
        this.agentId = agentId;
        this.agentCode = agentCode;
        this.actionType = actionType;
        this.description = description != null ? SensitiveDataSanitizer.sanitize(description) : null;
        this.resultSummary = resultSummary != null ? SensitiveDataSanitizer.sanitize(resultSummary) : null;
        this.relatedEntityLink = relatedEntityLink;
        this.timestamp = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.description != null) {
            this.description = SensitiveDataSanitizer.sanitize(this.description);
        }
        if (this.resultSummary != null) {
            this.resultSummary = SensitiveDataSanitizer.sanitize(this.resultSummary);
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getRelatedEntityLink() {
        return relatedEntityLink;
    }

    public void setRelatedEntityLink(String relatedEntityLink) {
        this.relatedEntityLink = relatedEntityLink;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
