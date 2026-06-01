package org.example.messengerserver.dto;

public class ChatResponse {

    // =================== Fields & accessors ===================

    private final int id;
    private final String name;
    private final String type;
    private final String lastMessageText;
    private final String lastMessageTime;
    private final Integer companionUserId;
    private final boolean customName;
    private final int unreadCount;

    public ChatResponse(int id, String name, String type) {
        this(id, name, type, null, null, null, false, 0);
    }

    public ChatResponse(
            int id,
            String name,
            String type,
            String lastMessageText,
            String lastMessageTime,
            Integer companionUserId,
            boolean customName,
            int unreadCount
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.companionUserId = companionUserId;
        this.customName = customName;
        this.unreadCount = unreadCount;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public Integer getCompanionUserId() {
        return companionUserId;
    }

    public boolean isCustomName() {
        return customName;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
