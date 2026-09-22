package com.dentalclinic.itteam.model;

import com.dentalclinic.common.BaseEntity;
import com.dentalclinic.itteam.service.SensitiveDataSanitizer;
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
        this.messageBody = messageBody != null ? SensitiveDataSanitizer.sanitize(messageBody) : null;
        this.parsedHashtags = parsedHashtags;
        this.isRead = false;
        this.parentMessageId = parentMessageId;
        this.sentAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.messageBody != null) {
            this.messageBody = SensitiveDataSanitizer.sanitize(this.messageBody);
        }
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
