package com.dentalclinic.dto;

import com.dentalclinic.model.CommunityPostCategory;

public class CreateForumPostRequestDto {
    private String authorName;
    private String authorPhone;
    private String title;
    private String content;
    private CommunityPostCategory category = CommunityPostCategory.EXPERIENCE;

    public CreateForumPostRequestDto() {}

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
}
