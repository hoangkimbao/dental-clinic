package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "dental_community_posts", indexes = {
    @Index(name = "idx_post_category", columnList = "category"),
    @Index(name = "idx_post_approved", columnList = "approved")
})
public class DentalCommunityPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    private String authorName;

    private String authorPhone;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityPostCategory category = CommunityPostCategory.EXPERIENCE;

    @Column(nullable = false)
    private Integer likesCount = 0;

    @Column(nullable = false)
    private boolean approved = true;

    public DentalCommunityPost() {}

    public DentalCommunityPost(User author, String authorName, String authorPhone, String title,
                               String content, CommunityPostCategory category, Integer likesCount, boolean approved) {
        this.author = author;
        this.authorName = authorName;
        this.authorPhone = authorPhone;
        this.title = title;
        this.content = content;
        this.category = category;
        this.likesCount = likesCount != null ? likesCount : 0;
        this.approved = approved;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorPhone() { return authorPhone; }
    public void setAuthorPhone(String authorPhone) { this.authorPhone = authorPhone; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public CommunityPostCategory getCategory() { return category; }
    public void setCategory(CommunityPostCategory category) { this.category = category; }

    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
