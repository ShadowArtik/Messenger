package org.example.messengerserver.dto;

public class ActiveSessionResponse {

    // =================== Fields & accessors ===================

    private final int userId;

    public ActiveSessionResponse(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
