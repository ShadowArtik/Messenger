package com.example.network.dto;

import java.util.List;

public class IncomingMessage {

    private String type;
    private int chatId;
    private int senderId;
    private int receiverId;
    private int userId;
    private List<Integer> userIds;
    private String senderUsername;
    private String senderDisplayName;
    private String displayName;
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

    public String getDisplayName() {
        return displayName;
    }

    public int getUserId() {
        return userId;
    }

    public List<Integer> getUserIds() {
        return userIds;
    }

    public boolean isPrivateMessage() {
        return "PRIVATE_MESSAGE".equalsIgnoreCase(type);
    }

    public boolean isTyping() {
        return "TYPING".equalsIgnoreCase(type);
    }

    public boolean isConnectSuccess() {
        return "CONNECT_SUCCESS".equalsIgnoreCase(type);
    }

    public boolean isUserOnline() {
        return "USER_ONLINE".equalsIgnoreCase(type);
    }

    public boolean isUserOffline() {
        return "USER_OFFLINE".equalsIgnoreCase(type);
    }

    public boolean isOnlineUsers() {
        return "ONLINE_USERS".equalsIgnoreCase(type);
    }

    public boolean isUserProfileUpdated() {
        return "USER_PROFILE_UPDATED".equalsIgnoreCase(type);
    }
}
