package com.dentalclinic.itteam.dto;

public class DispatchMessageRequest {
    private String messageBody;
    private Long parentMessageId;
    private String recipientHashtag;

    public DispatchMessageRequest() {
    }

    public DispatchMessageRequest(String messageBody, Long parentMessageId, String recipientHashtag) {
        this.messageBody = messageBody;
        this.parentMessageId = parentMessageId;
        this.recipientHashtag = recipientHashtag;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public Long getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(Long parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public String getRecipientHashtag() {
        return recipientHashtag;
    }

    public void setRecipientHashtag(String recipientHashtag) {
        this.recipientHashtag = recipientHashtag;
    }

    public boolean isValid() {
        return messageBody != null && !messageBody.trim().isEmpty();
    }
}
