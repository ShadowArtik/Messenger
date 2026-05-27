package com.example.network.dto;

import java.util.List;

public class GroupRenamedMessage {

    private final String type = "GROUP_RENAMED";
    private final int senderId;
    private final int chatId;
    private final String oldName;
    private final String newName;
    private final List<Integer> memberIds;
    private final String systemMessageText;

    public GroupRenamedMessage(
            int senderId,
            int chatId,
            String oldName,
            String newName,
            List<Integer> memberIds,
            String systemMessageText
    ) {
        this.senderId = senderId;
        this.chatId = chatId;
        this.oldName = oldName;
        this.newName = newName;
        this.memberIds = memberIds;
        this.systemMessageText = systemMessageText;
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

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }

    public String getSystemMessageText() {
        return systemMessageText;
    }
}
