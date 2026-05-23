package com.example.service;

import com.example.model.Chat;
import com.example.repository.ChatRepository;

import java.util.List;

public class ChatService {

    private final ChatRepository chatRepository = new ChatRepository();

    public Chat createChat(String name, int userId) {
        return chatRepository.createChat(name, "PRIVATE", userId);
    }

    public Chat createBotChat(String name, int userId) {
        return chatRepository.createChat(name, "BOT", userId);
    }

    public List<Chat> getChatsForUser(int userId) {
        return chatRepository.getChatsForUser(userId);
    }

    public void renameChat(int chatId, int userId, String newName) {
        chatRepository.renameChat(chatId, userId, newName);
    }

    public void deleteChat(int chatId) {
        chatRepository.deleteChat(chatId);
    }

    public boolean isBotChat(int chatId) {
        return chatRepository.isBotChat(chatId);
    }

    public Chat createPrivateChat(
            String chatName,
            int currentUserId,
            int targetUserId,
            String currentUserChatName,
            String targetUserChatName
    ) {
        return chatRepository.createPrivateChat(
                chatName,
                currentUserId,
                targetUserId,
                currentUserChatName,
                targetUserChatName
        );
    }

    public void updateChatActivity(int chatId) {
        chatRepository.updateChatActivity(chatId);
    }
}