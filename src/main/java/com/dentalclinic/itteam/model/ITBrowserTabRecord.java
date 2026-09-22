package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
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
    private String agentCode; // e.g. "backend", "it-backend"

    @Column(name = "tab_title", nullable = false, length = 200)
    private String tabTitle;

    @Column(name = "url_route", nullable = false, length = 500)
    private String urlRoute;

    @Column(name = "tab_category", length = 50)
    private String tabCategory; // "DOCS", "MONITORING", "DATABASE", "PORTAL", "EXTERNAL", "API_TESTER"

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
        this.tabTitle = tabTitle != null ? SensitiveDataSanitizer.sanitize(tabTitle) : null;
        this.urlRoute = urlRoute != null ? SensitiveDataSanitizer.sanitize(urlRoute) : null;
        this.tabCategory = tabCategory;
        this.status = (status != null) ? status : "OPEN";
        this.openedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.tabTitle != null) {
            this.tabTitle = SensitiveDataSanitizer.sanitize(this.tabTitle);
        }
        if (this.urlRoute != null) {
            this.urlRoute = SensitiveDataSanitizer.sanitize(this.urlRoute);
        }
        if (this.openedAt == null) {
            this.openedAt = LocalDateTime.now();
        }
    }

    public void preUpdate() {
        prePersist();
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
