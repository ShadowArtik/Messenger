package com.example.model;

public class Chat {

    private final int id;
    private final String name;
    private final String type;

    public Chat(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
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

    public boolean isBot() {
        return "BOT".equals(type);
    }
}