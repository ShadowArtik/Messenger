package com.example.service.result;

import com.example.model.Chat;

import java.util.List;

public class CreateGroupResponse {

    public enum Result {
        SUCCESS,
        EMPTY_GROUP_NAME,
        EMPTY_MEMBERS,
        USER_NOT_FOUND,
        ONLY_SELF_SELECTED,
        DATABASE_ERROR
    }

    private final Result result;
    private final Chat chat;
    private final List<Integer> memberIds;

    private CreateGroupResponse(Result result, Chat chat, List<Integer> memberIds) {
        this.result = result;
        this.chat = chat;
        this.memberIds = memberIds;
    }

    public static CreateGroupResponse success(Chat chat, List<Integer> memberIds) {
        return new CreateGroupResponse(Result.SUCCESS, chat, memberIds);
    }

    public static CreateGroupResponse error(Result result) {
        return new CreateGroupResponse(result, null, null);
    }

    public Result getResult() {
        return result;
    }

    public Chat getChat() {
        return chat;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }
}
