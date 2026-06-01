package org.example.messengerserver.dto;

public class UserResponse {

    // =================== Fields & accessors ===================

    private final int id;
    private final String username;
    private final String displayName;
    private final String memberRole;

    public UserResponse(int id, String username, String displayName) {
        this(id, username, displayName, null);
    }

    public UserResponse(int id, String username, String displayName, String memberRole) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.memberRole = memberRole;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMemberRole() {
        return memberRole;
    }
}
