package com.example.network.dto;

public class TypingMessage {

    private final String type = "TYPING";
    private final int chatId;
    private final int senderId;
    private final int receiverId;
    private final String senderDisplayName;

    public TypingMessage(
            int chatId,
            int senderId,
            int receiverId,
            String senderDisplayName
    ) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderDisplayName = senderDisplayName;
    }

    public String getType() {
        return type;
    }

    public int getChatId() {
        return chatId;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }
}
