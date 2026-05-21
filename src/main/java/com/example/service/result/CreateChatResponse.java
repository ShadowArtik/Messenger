package com.example.service.result;

import com.example.model.Chat;

public class CreateChatResponse {

    private final CreateChatResult result;
    private final Chat chat;

    public CreateChatResponse(
            CreateChatResult result,
            Chat chat
    ) {
        this.result = result;
        this.chat = chat;
    }

    public CreateChatResult getResult() {
        return result;
    }

    public Chat getChat() {
        return chat;
    }
}