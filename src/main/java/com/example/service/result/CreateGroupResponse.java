package com.example.service.result;

import com.example.model.Chat;

import java.util.List;

public class CreateGroupResponse {

    private final CreateGroupResult result;
    private final Chat chat;
    private final String failedUsername;
    private final List<Integer> memberIds;

    public CreateGroupResponse(CreateGroupResult result, Chat chat, String failedUsername) {
        this(result, chat, failedUsername, null);
    }

    public CreateGroupResponse(
            CreateGroupResult result,
            Chat chat,
            String failedUsername,
            List<Integer> memberIds
    ) {
        this.result = result;
        this.chat = chat;
        this.failedUsername = failedUsername;
        this.memberIds = memberIds;
    }

    public static CreateGroupResponse success(Chat chat) {
        return new CreateGroupResponse(CreateGroupResult.SUCCESS, chat, null);
    }

    public static CreateGroupResponse success(Chat chat, List<Integer> memberIds) {
        return new CreateGroupResponse(CreateGroupResult.SUCCESS, chat, null, memberIds);
    }

    public static CreateGroupResponse error(CreateGroupResult result) {
        return new CreateGroupResponse(result, null, null);
    }

    public static CreateGroupResponse userNotFound(String username) {
        return new CreateGroupResponse(CreateGroupResult.USER_NOT_FOUND, null, username);
    }

    public CreateGroupResult getResult() {
        return result;
    }

    public Chat getChat() {
        return chat;
    }

    public String getFailedUsername() {
        return failedUsername;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }
}
