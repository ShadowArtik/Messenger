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

    public void resetChatName(int chatId, int userId) {
        chatRepository.resetChatName(chatId, userId);
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
            int targetUserId
    ) {
        return chatRepository.createPrivateChat(
                chatName,
                currentUserId,
                targetUserId
        );
    }

    public Chat createGroupChat(String groupName, int creatorId, List<Integer> memberIds) {
        if (groupName == null || groupName.isBlank()) {
            return null;
        }

        if (memberIds == null || memberIds.isEmpty()) {
            return null;
        }

        return chatRepository.createGroupChat(
                groupName.trim(),
                creatorId,
                memberIds
        );
    }

    public void updateChatActivity(int chatId) {
        chatRepository.updateChatActivity(chatId);
    }
}
