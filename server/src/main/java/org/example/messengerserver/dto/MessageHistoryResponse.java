package org.example.messengerserver.dto;

public class MessageHistoryResponse {

    // =================== Fields & accessors ===================

    private final String senderName;
    private final String senderUsername;
    private final String text;
    private final String createdAt;
    private final boolean systemMessage;

    public MessageHistoryResponse(
            String senderName,
            String senderUsername,
            String text,
            String createdAt,
            boolean systemMessage
    ) {
        this.senderName = senderName;
        this.senderUsername = senderUsername;
        this.text = text;
        this.createdAt = createdAt;
        this.systemMessage = systemMessage;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getText() {
        return text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isSystemMessage() {
        return systemMessage;
    }
}
