package com.example.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Message {

    private final String sender;
    private final String text;
    private final LocalTime time;

    public Message(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.time = LocalTime.now();
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getFormattedTime() {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + sender + ": " + text;
    }
}