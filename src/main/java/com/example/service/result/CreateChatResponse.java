package com.example.service.result;

import com.example.model.Chat;

public class CreateChatResponse {

    public enum Result {
        SUCCESS,
        USER_NOT_FOUND,
        SELF_CHAT,
        CHAT_ALREADY_EXISTS,
        DATABASE_ERROR
    }

    private final Result result;
    private final Chat chat;

    public CreateChatResponse(Result result, Chat chat) {
        this.result = result;
        this.chat = chat;
    }

    public Result getResult() {
        return result;
    }

    public Chat getChat() {
        return chat;
    }
}
