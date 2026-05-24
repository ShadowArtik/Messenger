package com.example.network.dto;

public class IncomingMessage {

    private String type;
    private int chatId;
    private int senderId;
    private int receiverId;
    private String senderUsername;
    private String senderDisplayName;
    private String text;

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

    public boolean isPrivateMessage() {
        return "PRIVATE_MESSAGE".equalsIgnoreCase(type);
    }

    public boolean isConnectSuccess() {
        return "CONNECT_SUCCESS".equalsIgnoreCase(type);
    }
}