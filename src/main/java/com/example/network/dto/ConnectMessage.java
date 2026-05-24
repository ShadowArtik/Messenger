package com.example.network.dto;

public class ConnectMessage {

    private final String type = "CONNECT";
    private final int userId;
    private final String username;
    private final String displayName;

    public ConnectMessage(int userId, String username, String displayName) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }
}