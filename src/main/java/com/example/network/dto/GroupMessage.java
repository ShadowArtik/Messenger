package com.example.network.dto;

import java.util.List;

public class GroupMessage {

    private final String type = "GROUP_MESSAGE";
    private final int chatId;
    private final int senderId;
    private final List<Integer> receiverIds;
    private final String senderUsername;
    private final String senderDisplayName;
    private final String text;

    public GroupMessage(
            int chatId,
            int senderId,
            List<Integer> receiverIds,
            String senderUsername,
            String senderDisplayName,
            String text
    ) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverIds = receiverIds;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.text = text;
    }
}
