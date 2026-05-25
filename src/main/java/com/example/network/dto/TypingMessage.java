package com.example.network.dto;

import java.util.List;

public class TypingMessage {

    private final String type = "TYPING";
    private final int chatId;
    private final int senderId;
    private final List<Integer> receiverIds;
    private final String senderDisplayName;

    public TypingMessage(
            int chatId,
            int senderId,
            List<Integer> receiverIds,
            String senderDisplayName
    ) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverIds = receiverIds;
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

    public List<Integer> getReceiverIds() {
        return receiverIds;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }
}
