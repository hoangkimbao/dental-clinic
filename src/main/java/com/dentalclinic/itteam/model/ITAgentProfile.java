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
    private String agentCode; // e.g. "backend", "it-backend"

    @Column(name = "hashtag", unique = true, nullable = false, length = 50)
    private String hashtag; // e.g. "#it-backend"

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName; // e.g. "Alex Rivera (Backend Architect)"

    @Column(name = "role", nullable = false, length = 100)
    private String role; // e.g. "Backend Architect"

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ONLINE"; // "ONLINE", "ACTIVE", "IDLE", "BUSY", "OFFLINE"

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
        this.status = (status != null) ? status : "ONLINE";
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
