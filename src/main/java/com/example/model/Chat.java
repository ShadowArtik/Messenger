package com.example.model;

public class Chat {

    private final int id;
    private final String name;
    private final String type;
    private final String lastMessageText;
    private final String lastMessageTime;
    private final Integer companionUserId;
    private final int unreadCount;
    private final boolean customName;

    public Chat(
            int id,
            String name,
            String type,
            String lastMessageText,
            String lastMessageTime,
            Integer companionUserId,
            int unreadCount,
            boolean customName
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.companionUserId = companionUserId;
        this.unreadCount = unreadCount;
        this.customName = customName;
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

    public int getUnreadCount() {
        return unreadCount;
    }

    public boolean hasCustomName() {
        return customName;
    }

    public boolean isBot() {
        return "BOT".equalsIgnoreCase(type);
    }

    public boolean isGroup() {
        return "GROUP".equalsIgnoreCase(type);
    }

    public boolean isPrivate() {
        return "PRIVATE".equalsIgnoreCase(type);
    }

    // Withers: return a copy with one aspect changed (Chat is immutable).

    public Chat withName(String name, boolean customName) {
        return new Chat(id, name, type, lastMessageText, lastMessageTime, companionUserId, unreadCount, customName);
    }

    public Chat withLastMessage(String lastMessageText, String lastMessageTime) {
        return new Chat(id, name, type, lastMessageText, lastMessageTime, companionUserId, unreadCount, customName);
    }

    public Chat withUnreadCount(int unreadCount) {
        return new Chat(id, name, type, lastMessageText, lastMessageTime, companionUserId, unreadCount, customName);
    }

    @Override
    public String toString() {
        return name;
    }
}
