package com.example.network.dto;

public class PrivateMessage {

    private final String type = "PRIVATE_MESSAGE";
    private final int chatId;
    private final int senderId;
    private final int receiverId;
    private final String senderUsername;
    private final String senderDisplayName;
    private final String text;

    public PrivateMessage(
            int chatId,
            int senderId,
            int receiverId,
            String senderUsername,
            String senderDisplayName,
            String text
    ) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public int getChatId() {
        return chatId;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
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
}