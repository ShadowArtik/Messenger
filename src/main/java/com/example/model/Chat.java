package com.example.model;

public class Chat {

    private final int id;
    private final String name;
    private final String type;
    private final String lastMessageText;
    private final String lastMessageTime;

    public Chat(int id, String name, String type) {
        this(id, name, type, null, null);
    }

    public Chat(int id, String name, String type, String lastMessageText, String lastMessageTime) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
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

    public boolean isBot() {
        return "BOT".equalsIgnoreCase(type);
    }

    @Override
    public String toString() {
        return name;
    }
}