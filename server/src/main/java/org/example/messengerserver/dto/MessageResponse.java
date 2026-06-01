package org.example.messengerserver.dto;

public class MessageResponse {

    // =================== Fields & accessors ===================

    private final int senderId;
    private final String senderUsername;
    private final String senderDisplayName;
    private final String text;
    private final String time;
    private final String date;
    private final boolean read;
    private final String clientId;
    private final boolean edited;

    public MessageResponse(
            int senderId,
            String senderUsername,
            String senderDisplayName,
            String text,
            String time,
            String date,
            boolean read,
            String clientId,
            boolean edited
    ) {
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.text = text;
        this.time = time;
        this.date = date;
        this.read = read;
        this.clientId = clientId;
        this.edited = edited;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isEdited() {
        return edited;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public boolean isRead() {
        return read;
    }
}
