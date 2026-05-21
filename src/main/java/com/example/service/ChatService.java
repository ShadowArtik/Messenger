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

    public void renameChat(int chatId, String newName) {
        chatRepository.renameChat(chatId, newName);
    }

    public void deleteChat(int chatId) {
        chatRepository.deleteChat(chatId);
    }

    public boolean isBotChat(int chatId) {
        return chatRepository.isBotChat(chatId);
    }

    public Chat createPrivateChat(String chatName, int firstUserId, int secondUserId) {
        return chatRepository.createPrivateChat(chatName, firstUserId, secondUserId);
    }
}