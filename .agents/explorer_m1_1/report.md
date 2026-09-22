# Milestone 1: Domain Model & Database Persistence
## JPA Entities Architecture & Specification Report

**Author**: Explorer 1 (`explorer_m1_1`)  
**Date**: 2026-09-12  
**Target Package**: `com.dentalclinic.itteam.model`  
**Base Entity**: `com.dentalclinic.common.BaseEntity`  
**Target Tables**: `it_agent_profile`, `it_agent_memory`, `it_agent_message`, `it_agent_activity`, `it_browser_tab_record`, `it_api_run_log`

---

## 1. Executive Summary

This report establishes the complete, production-ready Java code structure for the 6 JPA entities supporting the **IT Team Command Center** module of the DentalCare Management Portal.

### Verified Architectural Constraints:
1. **Inheritance**: All 6 entities extend `com.dentalclinic.common.BaseEntity`, leveraging Spring Data JPA auditing (`@EntityListeners(AuditingEntityListener.class)`) for automatic population of `createdAt` and `updatedAt`.
2. **Zero Lombok**: Pure Java 17 POJOs with explicit default constructors, full/convenience constructors, getters, and setters.
3. **Database Portability**: Tested against H2 in-file database (`jdbc:h2:file:./data/dentaldb`) and production PostgreSQL. Uses `columnDefinition = "TEXT"` for large text payloads rather than `@Lob` to prevent JDBC blob streaming complexity.
4. **Schema & Indexing**: Table names and column names strictly adhere to `PROJECT.md` § M1 and `ORIGINAL_REQUEST.md` R1/R2/Acceptance Criteria. Indexes are defined for foreign references, hashtags, status, agent codes, and query timestamps.

---

## 2. Entity Mapping & Schema Overview

| Entity Class | Target Table | Primary Keys & Indexes | Key Attributes |
|---|---|---|---|
| `ITAgentProfile` | `it_agent_profile` | PK: `id`<br>Unique: `agent_code`, `hashtag`<br>Idx: `status` | `agentCode`, `hashtag`, `displayName`, `role`, `status`, `expertise`, `avatar`, `systemPrompt` |
| `ITAgentMemory` | `it_agent_memory` | PK: `id`<br>Idx: `agent_id`, `agent_code`, `memory_key`, `priority` | `agentId`, `agentCode`, `memoryKey`, `memoryContent`, `priority`, `lastUpdated` |
| `ITAgentMessage` | `it_agent_message` | PK: `id`<br>Idx: `sender_id`, `recipient_id`, `parent_message_id`, `sent_at`, `is_read` | `senderId`, `senderName`, `senderType`, `recipientId`, `recipientHashtag`, `messageBody`, `parsedHashtags`, `isRead`, `parentMessageId`, `sentAt` |
| `ITAgentActivity` | `it_agent_activity` | PK: `id`<br>Idx: `agent_id`, `agent_code`, `action_type`, `timestamp` | `agentId`, `agentCode`, `actionType`, `description`, `resultSummary`, `relatedEntityLink`, `timestamp` |
| `ITBrowserTabRecord` | `it_browser_tab_record` | PK: `id`<br>Idx: `agent_id`, `agent_code`, `status`, `tab_category` | `agentId`, `agentCode`, `tabTitle`, `urlRoute`, `tabCategory`, `status`, `openedAt`, `closedAt` |
| `ITApiRunLog` | `it_api_run_log` | PK: `id`<br>Idx: `endpoint`, `http_method`, `status_code`, `run_timestamp` | `endpoint`, `httpMethod`, `statusCode`, `executionDurationMs`, `requestPayload`, `responsePayload`, `runTimestamp`, `initiatedBy`, `isSuccess` |

---

## 3. Detailed Entity Implementation Code

### 3.1. `ITAgentProfile.java`
**File Location**: `src/main/java/com/dentalclinic/itteam/model/ITAgentProfile.java`

```java
package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;

/**
 * Entity representing an IT Team agent profile.
 * Pre-seeded with 5 standard agents:
 * #it-backend, #it-frontend, #it-qa, #it-devops, #it-security.
 */
@Entity
@Table(name = "it_agent_profile", indexes = {
    @Index(name = "idx_agent_code", columnList = "agent_code", unique = true),
    @Index(name = "idx_agent_hashtag", columnList = "hashtag", unique = true),
    @Index(name = "idx_agent_status", columnList = "status")
})
public class ITAgentProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_code", unique = true, nullable = false, length = 50)
    private String agentCode; // e.g. "it-backend"

    @Column(name = "hashtag", unique = true, nullable = false, length = 50)
    private String hashtag; // e.g. "#it-backend"

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName; // e.g. "Backend Specialist"

    @Column(name = "role", nullable = false, length = 100)
    private String role; // e.g. "Senior Backend Engineer"

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE"; // "ACTIVE", "IDLE", "BUSY", "OFFLINE"

    @Column(name = "expertise", length = 1000)
    private String expertise; // e.g. "Spring Boot, Database, Security, REST APIs"

    @Column(name = "avatar", length = 500)
    private String avatar; // Avatar image path or font-awesome icon identifier

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt; // Context persona for 9Router AI engine

    public ITAgentProfile() {
    }

    public ITAgentProfile(String agentCode, String hashtag, String displayName, String role,
                          String status, String expertise, String avatar, String systemPrompt) {
        this.agentCode = agentCode;
        this.hashtag = hashtag;
        this.displayName = displayName;
        this.role = role;
        this.status = (status != null) ? status : "ACTIVE";
        this.expertise = expertise;
        this.avatar = avatar;
        this.systemPrompt = systemPrompt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
```

---

### 3.2. `ITAgentMemory.java`
**File Location**: `src/main/java/com/dentalclinic/itteam/model/ITAgentMemory.java`

```java
package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity storing long-term key-value memory for an IT agent.
 * Sensitive data must be sanitized prior to persistence.
 */
@Entity
@Table(name = "it_agent_memory", indexes = {
    @Index(name = "idx_mem_agent_id", columnList = "agent_id"),
    @Index(name = "idx_mem_agent_code", columnList = "agent_code"),
    @Index(name = "idx_mem_key", columnList = "memory_key"),
    @Index(name = "idx_mem_priority", columnList = "priority")
})
public class ITAgentMemory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agent_code", length = 50)
    private String agentCode; // e.g. "it-backend"

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
        this.memoryContent = memoryContent;
        this.priority = (priority != null) ? priority : "MEDIUM";
        this.lastUpdated = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.lastUpdated == null) {
            this.lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
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
```

---

### 3.3. `ITAgentMessage.java`
**File Location**: `src/main/java/com/dentalclinic/itteam/model/ITAgentMessage.java`

```java
package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity storing inter-agent or user-to-agent messages with hashtag routing,
 * read receipts, and threaded reply grouping.
 */
@Entity
@Table(name = "it_agent_message", indexes = {
    @Index(name = "idx_msg_sender_id", columnList = "sender_id"),
    @Index(name = "idx_msg_recipient_id", columnList = "recipient_id"),
    @Index(name = "idx_msg_parent_id", columnList = "parent_message_id"),
    @Index(name = "idx_msg_sent_at", columnList = "sent_at"),
    @Index(name = "idx_msg_read", columnList = "is_read")
})
public class ITAgentMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id")
    private Long senderId; // User ID or Agent ID

    @Column(name = "sender_name", length = 100)
    private String senderName; // e.g. "Admin", "Dr. Nguyen", "it-backend"

    @Column(name = "sender_type", length = 30)
    private String senderType = "USER"; // "USER", "AGENT", "SYSTEM"

    @Column(name = "recipient_id")
    private Long recipientId; // ITAgentProfile ID or User ID (nullable if broadcast)

    @Column(name = "recipient_hashtag", length = 50)
    private String recipientHashtag; // Primary recipient hashtag e.g. "#it-backend"

    @Column(name = "message_body", columnDefinition = "TEXT", nullable = false)
    private String messageBody;

    @Column(name = "parsed_hashtags", length = 255)
    private String parsedHashtags; // Comma-separated list e.g. "#it-backend,#it-qa"

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "parent_message_id")
    private Long parentMessageId; // Null for root messages; Thread ID for replies

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public ITAgentMessage() {
    }

    public ITAgentMessage(Long senderId, String senderName, String senderType,
                           Long recipientId, String recipientHashtag, String messageBody,
                           String parsedHashtags, Long parentMessageId) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderType = (senderType != null) ? senderType : "USER";
        this.recipientId = recipientId;
        this.recipientHashtag = recipientHashtag;
        this.messageBody = messageBody;
        this.parsedHashtags = parsedHashtags;
        this.isRead = false;
        this.parentMessageId = parentMessageId;
        this.sentAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientHashtag() {
        return recipientHashtag;
    }

    public void setRecipientHashtag(String recipientHashtag) {
        this.recipientHashtag = recipientHashtag;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getParsedHashtags() {
        return parsedHashtags;
    }

    public void setParsedHashtags(String parsedHashtags) {
        this.parsedHashtags = parsedHashtags;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Long getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(Long parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
```

---

### 3.4. `ITAgentActivity.java`
**File Location**: `src/main/java/com/dentalclinic/itteam/model/ITAgentActivity.java`

```java
package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
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
    private String agentCode; // e.g. "it-backend"

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // "MENTIONED", "API_RUN", "MEMORY_UPDATE", "STATUS_CHANGE", "TAB_OPEN"

    @Column(name = "description", length = 1000, nullable = false)
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
        this.description = description;
        this.resultSummary = resultSummary;
        this.relatedEntityLink = relatedEntityLink;
        this.timestamp = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
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
```

---

### 3.5. `ITBrowserTabRecord.java`
**File Location**: `src/main/java/com/dentalclinic/itteam/model/ITBrowserTabRecord.java`

```java
package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking browser navigation and session records of simulated agents.
 * Privacy guardrails guarantee that URLs and parameters do not store sensitive tokens/PII.
 */
@Entity
@Table(name = "it_browser_tab_record", indexes = {
    @Index(name = "idx_tab_agent_id", columnList = "agent_id"),
    @Index(name = "idx_tab_agent_code", columnList = "agent_code"),
    @Index(name = "idx_tab_status", columnList = "status"),
    @Index(name = "idx_tab_category", columnList = "tab_category")
})
public class ITBrowserTabRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agent_code", length = 50)
    private String agentCode;

    @Column(name = "tab_title", nullable = false, length = 200)
    private String tabTitle;

    @Column(name = "url_route", nullable = false, length = 500)
    private String urlRoute;

    @Column(name = "tab_category", length = 50)
    private String tabCategory; // "DOCS", "MONITORING", "DATABASE", "PORTAL", "EXTERNAL"

    @Column(name = "status", nullable = false, length = 30)
    private String status = "OPEN"; // "OPEN", "CLOSED", "BACKGROUND"

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public ITBrowserTabRecord() {
    }

    public ITBrowserTabRecord(Long agentId, String agentCode, String tabTitle,
                              String urlRoute, String tabCategory, String status) {
        this.agentId = agentId;
        this.agentCode = agentCode;
        this.tabTitle = tabTitle;
        this.urlRoute = urlRoute;
        this.tabCategory = tabCategory;
        this.status = (status != null) ? status : "OPEN";
        this.openedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.openedAt == null) {
            this.openedAt = LocalDateTime.now();
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

    public String getTabTitle() {
        return tabTitle;
    }

    public void setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
    }

    public String getUrlRoute() {
        return urlRoute;
    }

    public void setUrlRoute(String urlRoute) {
        this.urlRoute = urlRoute;
    }

    public String getTabCategory() {
        return tabCategory;
    }

    public void setTabCategory(String tabCategory) {
        this.tabCategory = tabCategory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
```

---

### 3.6. `ITApiRunLog.java`
**File Location**: `src/main/java/com/dentalclinic/itteam/model/ITApiRunLog.java`

```java
package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
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

    @Column(name = "status_code", nullable = false)
    private Integer statusCode; // e.g. 200, 401, 403, 500

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
    private Boolean isSuccess;

    public ITApiRunLog() {
    }

    public ITApiRunLog(String endpoint, String httpMethod, Integer statusCode,
                       Long executionDurationMs, String requestPayload,
                       String responsePayload, String initiatedBy) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.executionDurationMs = executionDurationMs;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.initiatedBy = initiatedBy;
        this.isSuccess = (statusCode != null && statusCode >= 200 && statusCode < 400);
        this.runTimestamp = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.runTimestamp == null) {
            this.runTimestamp = LocalDateTime.now();
        }
        if (this.isSuccess == null && this.statusCode != null) {
            this.isSuccess = (this.statusCode >= 200 && this.statusCode < 400);
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
```

---

## 4. Key Design Decisions & Analysis

### 4.1. Decoupled Foreign References vs. Direct `@ManyToOne`
- **Decision**: In `ITAgentMemory`, `ITAgentActivity`, and `ITBrowserTabRecord`, we store `agentId` (Long) and `agentCode` (String) directly rather than an eager `@ManyToOne ITAgentProfile`.
- **Rationale**:
  1. REST responses in M3 (`/api/it-team/memories`, `/api/it-team/activities`) return entity payloads directly wrapped in `ApiResponse<T>`. Flat properties prevent Jackson infinite recursion and avoid triggering lazy initialization exceptions when accessed outside transactions.
  2. Filtering by `agentCode` (e.g., `GET /api/it-team/memories?agentCode=it-backend`) is instant via the direct B-tree index on `agent_code` without requiring SQL `JOIN` overhead.
  3. Seamless compatibility with decoupled micro-agent execution where agents log activities independently by code.

### 4.2. BaseEntity Integration
- `BaseEntity` in `com.dentalclinic.common.BaseEntity` supplies:
  ```java
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  ```
- Because `com.dentalclinic.config.JpaAuditingConfig` enables `@EnableJpaAuditing`, all 6 entities automatically benefit from Spring Data JPA auditing upon persist/update.
- Each entity also provides its domain-specific lifecycle timestamp (`lastUpdated` for memory, `sentAt` for message, `timestamp` for activity, `openedAt` for browser tabs, `runTimestamp` for API logs) initialized via `@PrePersist` to ensure deterministic query semantics even in un-audited test scenarios.

### 4.3. String Constants for Enums
- Enums like status, priority, and category are persisted as `VARCHAR` rather than ordinal integers.
- This allows flexible additions (e.g. new action types in M2 or M3) without migrating existing database tables or risking deserialization breakages.

---

## 5. Downstream Integration Guidance

1. **For Explorer 2 (Repositories & Seeder)**:
   - Repositories can leverage standard Spring Data derived query methods:
     - `ITAgentProfileRepository`: `Optional<ITAgentProfile> findByAgentCode(String agentCode);`, `Optional<ITAgentProfile> findByHashtag(String hashtag);`
     - `ITAgentMemoryRepository`: `List<ITAgentMemory> findByAgentCodeOrderByLastUpdatedDesc(String agentCode);`, `List<ITAgentMemory> findByAgentIdOrderByLastUpdatedDesc(Long agentId);`
     - `ITAgentMessageRepository`: `List<ITAgentMessage> findByParentMessageIdOrderBySentAtAsc(Long parentMessageId);`, `List<ITAgentMessage> findByParentMessageIdIsNullOrderBySentAtDesc();`, `List<ITAgentMessage> findByParsedHashtagsContainingOrderBySentAtDesc(String hashtag);`
     - `ITAgentActivityRepository`: `Page<ITAgentActivity> findByAgentCodeOrderByTimestampDesc(String agentCode, Pageable pageable);`
     - `ITBrowserTabRecordRepository`: `List<ITBrowserTabRecord> findByStatusOrderByOpenedAtDesc(String status);`
     - `ITApiRunLogRepository`: `List<ITApiRunLog> findTop50ByOrderByRunTimestampDesc();`

2. **For Worker (Implementation)**:
   - Place all 6 classes under: `src/main/java/com/dentalclinic/itteam/model/`
   - Compile using `./mvnw compile -DskipTests` to verify clean compilation.
