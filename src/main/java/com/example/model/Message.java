package com.example.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Message {

    // =================== State & accessors ===================

    private static final String SYSTEM_PREFIX = "[SYSTEM] ";

    private final Integer senderId;
    private final String senderUsername;
    private final String senderDisplayName;

    private String text;
    private final String time;

    private String clientId = UUID.randomUUID().toString();

    private boolean edited;

    private boolean read;

    private LocalDate date = LocalDate.now(ZoneId.of("Europe/Kyiv"));

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

    public void setText(String text) {
        this.text = text;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public String getFormattedTime() {
        return time;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    private static boolean isStoredSystemText(String text) {
        return text != null && text.startsWith(SYSTEM_PREFIX);
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + senderUsername + ": " + text;
    }
}
