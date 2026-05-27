package com.example.service;

import com.example.model.Chat;
import com.example.model.User;
import com.example.repository.ChatRepository;
import com.example.repository.UserRepository;

import java.util.List;

public class ChatService {

    private final ChatRepository chatRepository = new ChatRepository();
    private final UserRepository userRepository = new UserRepository();

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

    public void renameGroupChat(int chatId, String newName) {
        chatRepository.renameGroupChat(chatId, newName);
    }

    public void resetChatName(int chatId, int userId) {
        chatRepository.resetChatName(chatId, userId);
    }

    public void deleteChat(int chatId) {
        chatRepository.deleteChat(chatId);
    }

    public boolean leaveGroup(int chatId, int userId) {
        if (!chatRepository.isGroupChat(chatId)) {
            return false;
        }

        chatRepository.leaveGroup(chatId, userId);
        return true;
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

    public List<Integer> getChatMemberIdsExcept(int chatId, int excludedUserId) {
        return chatRepository.getChatMemberIdsExcept(chatId, excludedUserId);
    }

    public List<User> getGroupMembers(int chatId) {
        return chatRepository.getGroupMembers(chatId);
    }

    public String getMemberRole(int chatId, int userId) {
        return chatRepository.getMemberRole(chatId, userId);
    }

    public boolean updateMemberRole(int chatId, int currentUserId, int targetUserId, String role) {
        if (!chatRepository.isGroupChat(chatId)) {
            return false;
        }

        if (!"OWNER".equalsIgnoreCase(chatRepository.getMemberRole(chatId, currentUserId))) {
            return false;
        }

        if (!"ADMIN".equalsIgnoreCase(role) && !"MEMBER".equalsIgnoreCase(role)) {
            return false;
        }

        return chatRepository.updateMemberRole(chatId, targetUserId, role.toUpperCase());
    }

    public boolean transferOwnership(int chatId, int currentOwnerId, int newOwnerId) {
        if (!chatRepository.isGroupChat(chatId) || currentOwnerId == newOwnerId) {
            return false;
        }

        if (!"OWNER".equalsIgnoreCase(chatRepository.getMemberRole(chatId, currentOwnerId))) {
            return false;
        }

        String newOwnerRole = chatRepository.getMemberRole(chatId, newOwnerId);

        if (newOwnerRole == null || "OWNER".equalsIgnoreCase(newOwnerRole)) {
            return false;
        }

        boolean transferred = chatRepository.transferOwnership(
                chatId,
                currentOwnerId,
                newOwnerId
        );

        if (transferred) {
            chatRepository.updateChatActivity(chatId);
        }

        return transferred;
    }

    public boolean kickMemberFromGroup(int chatId, int currentUserId, int targetUserId) {
        if (!chatRepository.isGroupChat(chatId) || currentUserId == targetUserId) {
            return false;
        }

        String currentUserRole = chatRepository.getMemberRole(chatId, currentUserId);
        String targetUserRole = chatRepository.getMemberRole(chatId, targetUserId);

        if (currentUserRole == null || targetUserRole == null) {
            return false;
        }

        if ("OWNER".equalsIgnoreCase(targetUserRole)) {
            return false;
        }

        boolean canKick = "OWNER".equalsIgnoreCase(currentUserRole)
                || ("ADMIN".equalsIgnoreCase(currentUserRole)
                && "MEMBER".equalsIgnoreCase(targetUserRole));

        if (!canKick) {
            return false;
        }

        chatRepository.removeMemberFromGroup(chatId, targetUserId);
        chatRepository.updateChatActivity(chatId);

        return true;
    }

    public List<User> getUsersNotInChat(int chatId, int currentUserId) {
        return userRepository.getUsersNotInChat(chatId, currentUserId);
    }

    public void addMembersToGroup(int chatId, List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Integer userId : userIds) {
            if (userId != null) {
                chatRepository.addMemberToChat(chatId, userId);
            }
        }

        chatRepository.updateChatActivity(chatId);
    }

    public void updateChatActivity(int chatId) {
        chatRepository.updateChatActivity(chatId);
    }
}
