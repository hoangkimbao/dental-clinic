package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity storing long-term key-value memory for an IT agent.
 * Sensitive data is automatically sanitized prior to persistence.
 */
@Entity
@Table(name = "it_agent_memory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_agent_memory_key", columnNames = {"agent_code", "memory_key"})
    },
    indexes = {
        @Index(name = "idx_mem_agent_id", columnList = "agent_id"),
        @Index(name = "idx_mem_agent_code", columnList = "agent_code"),
        @Index(name = "idx_mem_key", columnList = "memory_key"),
        @Index(name = "idx_mem_priority", columnList = "priority")
    }
)
public class ITAgentMemory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agent_code", length = 50)
    private String agentCode; // e.g. "backend", "it-backend"

    @Column(name = "memory_key", nullable = false, length = 150)
    private String memoryKey;

    @Column(name = "memory_content", columnDefinition = "TEXT", nullable = false)
    private String memoryContent;

    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM"; // "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public ITAgentMemory() {
    }

    public ITAgentMemory(Long agentId, String agentCode, String memoryKey,
                          String memoryContent, String priority) {
        this.agentId = agentId;
        this.agentCode = agentCode;
        this.memoryKey = memoryKey;
        this.memoryContent = memoryContent != null ? SensitiveDataSanitizer.sanitize(memoryContent) : null;
        this.priority = (priority != null) ? priority : "MEDIUM";
        this.lastUpdated = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.memoryContent != null) {
            this.memoryContent = SensitiveDataSanitizer.sanitize(this.memoryContent);
        }
        if (this.lastUpdated == null) {
            this.lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.memoryContent != null) {
            this.memoryContent = SensitiveDataSanitizer.sanitize(this.memoryContent);
        }
        this.lastUpdated = LocalDateTime.now();
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

    public String getMemoryKey() {
        return memoryKey;
    }

    public void setMemoryKey(String memoryKey) {
        this.memoryKey = memoryKey;
    }

    public String getMemoryContent() {
        return memoryContent;
    }

    public void setMemoryContent(String memoryContent) {
        this.memoryContent = memoryContent;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
