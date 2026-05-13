package com.example.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Message {

    private final String sender;
    private final String text;
    private final String time;

    public Message(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public Message(String sender, String text, String time) {
        this.sender = sender;
        this.text = text;
        this.time = time;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public String getFormattedTime() {
        return time;
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + sender + ": " + text;
    }
}