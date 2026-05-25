package com.example.network.dto;

import java.util.List;

public class GroupCreatedMessage {

    private final String type = "GROUP_CREATED";
    private final int senderId;
    private final int chatId;
    private final String chatName;
    private final List<Integer> memberIds;

    public GroupCreatedMessage(
            int senderId,
            int chatId,
            String chatName,
            List<Integer> memberIds
    ) {
        this.senderId = senderId;
        this.chatId = chatId;
        this.chatName = chatName;
        this.memberIds = memberIds;
    }

    public String getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getChatId() {
        return chatId;
    }

    public String getChatName() {
        return chatName;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }
}
