package com.example.service;

import com.example.model.Chat;
import com.example.model.Session;
import com.example.model.User;
import com.example.repository.ChatRepository;
import com.example.repository.UserRepository;

import java.util.List;

/**
 * Group membership and role logic. Stateless: reads/writes go through the
 * repositories (server REST) and the current {@link Session}; it does not hold
 * any UI state, so it is independent of {@link com.example.model.MessengerModel}.
 */
public class GroupService {

    private final ChatRepository chatRepository = new ChatRepository();
    private final UserRepository userRepository = new UserRepository();

    public List<Integer> getGroupReceiverIds(Chat chat) {
        if (chat == null || !chat.isGroup()) {
            return List.of();
        }

        return chatRepository.getChatMemberIdsExcept(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public List<User> getGroupMembers(Chat chat) {
        if (chat == null || !chat.isGroup()) {
            return List.of();
        }

        return chatRepository.getGroupMembers(chat.getId());
    }

    private String getCurrentUserGroupRole(Chat chat) {
        if (chat == null || !chat.isGroup() || Session.getCurrentUser() == null) {
            return null;
        }

        return chatRepository.getMemberRole(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public boolean canManageGroup(Chat chat) {
        String role = getCurrentUserGroupRole(chat);
        return "OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    public boolean canManageGroupRoles(Chat chat) {
        return "OWNER".equalsIgnoreCase(getCurrentUserGroupRole(chat));
    }

    public boolean canCurrentOwnerLeaveGroup(Chat chat) {
        if (!"OWNER".equalsIgnoreCase(getCurrentUserGroupRole(chat))) {
            return true;
        }

        return getGroupMembers(chat).size() <= 1;
    }

    public boolean canTransferOwnership(Chat chat, User member) {
        return chat != null
                && member != null
                && canManageGroupRoles(chat)
                && Session.getCurrentUser() != null
                && member.getId() != Session.getCurrentUser().getId()
                && !"OWNER".equalsIgnoreCase(member.getMemberRole());
    }

    public boolean transferOwnership(Chat chat, User member) {
        if (!canTransferOwnership(chat, member)) {
            return false;
        }

        boolean transferred = chatRepository.transferOwnership(
                chat.getId(),
                Session.getCurrentUser().getId(),
                member.getId()
        );

        if (transferred) {
            chatRepository.updateChatActivity(chat.getId());
        }

        return transferred;
    }

    public boolean updateGroupMemberRole(Chat chat, User member, String role) {
        if (chat == null || member == null || Session.getCurrentUser() == null) {
            return false;
        }

        return chatRepository.updateMemberRole(
                chat.getId(),
                member.getId(),
                role.toUpperCase()
        );
    }

    public boolean canKickGroupMember(Chat chat, User member) {
        if (chat == null || member == null || Session.getCurrentUser() == null) {
            return false;
        }

        if (member.getId() == Session.getCurrentUser().getId()) {
            return false;
        }

        String currentUserRole = getCurrentUserGroupRole(chat);
        String targetUserRole = member.getMemberRole();

        if ("OWNER".equalsIgnoreCase(targetUserRole)) {
            return false;
        }

        return "OWNER".equalsIgnoreCase(currentUserRole)
                || ("ADMIN".equalsIgnoreCase(currentUserRole)
                && "MEMBER".equalsIgnoreCase(targetUserRole));
    }

    public boolean kickGroupMember(Chat chat, User member) {
        if (chat == null || member == null || Session.getCurrentUser() == null) {
            return false;
        }

        chatRepository.removeMemberFromGroup(chat.getId(), member.getId());
        chatRepository.updateChatActivity(chat.getId());
        return true;
    }

    public List<User> getUsersNotInChat(Chat chat) {
        if (chat == null || !chat.isGroup() || Session.getCurrentUser() == null) {
            return List.of();
        }

        return userRepository.getUsersNotInChat(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public void addMembersToGroup(Chat chat, List<User> users) {
        if (chat == null || users == null || users.isEmpty()) {
            return;
        }

        for (User user : users) {
            if (user != null) {
                chatRepository.addMemberToChat(chat.getId(), user.getId());
            }
        }

        chatRepository.updateChatActivity(chat.getId());
    }
}
