package com.example.network.dto;

import java.util.List;

public class GroupMembersUpdatedMessage {

    private final String type = "GROUP_MEMBERS_UPDATED";
    private final int senderId;
    private final int chatId;
    private final List<Integer> memberIds;

    public GroupMembersUpdatedMessage(
            int senderId,
            int chatId,
            List<Integer> memberIds
    ) {
        this.senderId = senderId;
        this.chatId = chatId;
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

    public List<Integer> getMemberIds() {
        return memberIds;
    }
}
