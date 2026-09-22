package com.dentalclinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String category;
    
    private String authorName;
    
    private String authorAvatar;

    private String readTime;

    private String icon;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private Integer viewCount = 0;

    private Boolean isPublished = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Article() {
        this.viewCount = 0;
        this.isPublished = true;
    }

    public Article(String title, String slug, String category, String summary, String content, String authorName, String authorAvatar, Integer readTimeMinutes) {
        this.title = title;
        this.slug = slug;
        this.category = category;
        this.summary = summary;
        this.content = content;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.readTime = readTimeMinutes != null ? (readTimeMinutes + " phút đọc") : "5 phút đọc";
        this.icon = "fa-tooth";
        this.viewCount = 0;
        this.isPublished = true;
    }

    public Article(Long id, String title, String slug, String category, String authorName, String authorAvatar, String readTime, String icon, String summary, String content, Integer viewCount, Boolean isPublished, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.category = category;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.readTime = readTime;
        this.icon = icon;
        this.summary = summary;
        this.content = content;
        this.viewCount = viewCount != null ? viewCount : 0;
        this.isPublished = isPublished != null ? isPublished : true;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0;
        if (isPublished == null) isPublished = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getReadTime() { return readTime; }
    public void setReadTime(String readTime) { this.readTime = readTime; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
