package com.example.model;

public class Chat {

    private final int id;
    private final String name;
    private final String type;
    private final String lastMessageText;
    private final String lastMessageTime;
    private final Integer companionUserId;
    private final int unreadCount;

    public Chat(int id, String name, String type) {
        this(id, name, type, null, null, null, 0);
    }

    public Chat(int id, String name, String type, String lastMessageText, String lastMessageTime) {
        this(id, name, type, lastMessageText, lastMessageTime, null, 0);
    }

    public Chat(
            int id,
            String name,
            String type,
            String lastMessageText,
            String lastMessageTime,
            Integer companionUserId
    ) {
        this(id, name, type, lastMessageText, lastMessageTime, companionUserId, 0);
    }

    public Chat(
            int id,
            String name,
            String type,
            String lastMessageText,
            String lastMessageTime,
            Integer companionUserId,
            int unreadCount
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.companionUserId = companionUserId;
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

    public int getUnreadCount() {
        return unreadCount;
    }

    public boolean isBot() {
        return "BOT".equalsIgnoreCase(type);
    }

    @Override
    public String toString() {
        return name;
    }
}