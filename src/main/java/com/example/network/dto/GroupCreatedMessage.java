package com.example.network.dto;

import java.util.List;

public class GroupCreatedMessage {

    private final String type = "GROUP_CREATED";
    private final int chatId;
    private final List<Integer> memberIds;

    public GroupCreatedMessage(int chatId, List<Integer> memberIds) {
        this.chatId = chatId;
        this.memberIds = memberIds;
    }

    public String getType() {
        return type;
    }

    public int getChatId() {
        return chatId;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }
}
