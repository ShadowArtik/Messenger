package com.example.network.dto;

public class ProfileUpdatedMessage {

    private final String type = "USER_PROFILE_UPDATED";
    private final int userId;
    private final String displayName;

    public ProfileUpdatedMessage(int userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }
}
