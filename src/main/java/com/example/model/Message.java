package com.example.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Message {

    private static final String SYSTEM_PREFIX = "[SYSTEM] ";

    private final Integer senderId;
    private final String senderUsername;
    private final String senderDisplayName;
    private final String text;
    private final String time;

    public Message(String senderUsername, String text) {
        this(null, senderUsername, senderUsername, text);
    }

    public Message(Integer senderId, String senderUsername, String senderDisplayName, String text) {
        this(
                senderId,
                senderUsername,
                senderDisplayName,
                text,
                ZonedDateTime
                .now(ZoneId.of("Europe/Kyiv"))
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    public Message(String senderUsername, String text, String time) {
        this(null, senderUsername, senderUsername, text, time);
    }

    public Message(
            Integer senderId,
            String senderUsername,
            String senderDisplayName,
            String text,
            String time
    ) {
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.text = text;
        this.time = time;
    }

    public String getSender() {
        return senderUsername;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getText() {
        if (isStoredSystemText(text)) {
            return text.substring(SYSTEM_PREFIX.length());
        }

        return text;
    }

    public String getStorageText() {
        return text;
    }

    public String getFormattedTime() {
        return time;
    }

    public boolean isSystem() {
        return "system".equalsIgnoreCase(senderUsername)
                || isStoredSystemText(text);
    }

    public static Message system(String text) {
        return new Message(
                null,
                "system",
                "System",
                SYSTEM_PREFIX + text
        );
    }

    public static String cleanSystemText(String text) {
        if (isStoredSystemText(text)) {
            return text.substring(SYSTEM_PREFIX.length());
        }

        return text;
    }

    private static boolean isStoredSystemText(String text) {
        return text != null && text.startsWith(SYSTEM_PREFIX);
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + senderUsername + ": " + text;
    }
}
