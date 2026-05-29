package com.example.model;

import com.example.service.ChatService;
import com.example.service.MessageService;
import com.example.service.UserService;
import com.example.service.result.CreateGroupResponse;
import com.example.service.result.CreateGroupResult;
import com.example.service.result.CreateChatResponse;
import com.example.service.result.CreateChatResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessengerModel {

    private final ObservableList<Chat> chats;
    private final Map<Integer, ObservableList<Message>> chatMessages;

    private final MessageService messageService;
    private final ChatService chatService;
    private final UserService userService;
    private final Set<Integer> onlineUserIds;

    private User helperBotUser;

    public MessengerModel() {
        chats = FXCollections.observableArrayList();
        chatMessages = new HashMap<>();
        onlineUserIds = new HashSet<>();

        messageService = new MessageService();
        chatService = new ChatService();
        userService = new UserService();

        helperBotUser = userService.createSystemUserIfNotExists(
                "helper_bot",
                "Helper"
        );

        loadChats();
    }

    private void loadChats() {
        int currentUserId = Session.getCurrentUser().getId();

        List<Chat> savedChats = chatService.getChatsForUser(currentUserId);

        for (Chat chat : savedChats) {
            addChatToMemory(chat);
        }

        if (!userService.isHelperInitialized(currentUserId)) {
            Chat helperChat = chatService.createBotChat("Helper", currentUserId);

            if (helperChat != null) {
                addChatToMemory(helperChat);
            }

            userService.markHelperInitialized(currentUserId);
        }
    }

    private void addChatToMemory(Chat chat) {
        if (chat == null || chatMessages.containsKey(chat.getId())) {
            return;
        }

        chats.add(chat);

        ObservableList<Message> messages =
                FXCollections.observableArrayList(
                        messageService.getMessages(chat.getId())
                );

        chatMessages.put(chat.getId(), messages);
    }

    public CreateChatResponse addChat(String username) {
        User targetUser = userService.findByUsername(username);

        if (targetUser == null) {
            return new CreateChatResponse(
                    CreateChatResult.USER_NOT_FOUND,
                    null
            );
        }

        if (targetUser.getId() == Session.getCurrentUser().getId()) {
            return new CreateChatResponse(
                    CreateChatResult.SELF_CHAT,
                    null
            );
        }

        for (Chat existingChat : chats) {
            if (existingChat.getCompanionUserId() != null
                    && existingChat.getCompanionUserId().equals(targetUser.getId())) {

                return new CreateChatResponse(
                        CreateChatResult.CHAT_ALREADY_EXISTS,
                        null
                );
            }
        }

        Chat chat = chatService.createPrivateChat(
                targetUser.getDisplayName(),
                Session.getCurrentUser().getId(),
                targetUser.getId()
        );

        if (chat == null) {
            return new CreateChatResponse(
                    CreateChatResult.DATABASE_ERROR,
                    null
            );
        }

        addChatToMemory(chat);

        return new CreateChatResponse(
                CreateChatResult.SUCCESS,
                chat
        );
    }

    public CreateGroupResponse addGroupChat(String groupName, List<String> usernames) {
        if (groupName == null || groupName.isBlank()) {
            return CreateGroupResponse.error(CreateGroupResult.EMPTY_GROUP_NAME);
        }

        if (usernames == null || usernames.isEmpty()) {
            return CreateGroupResponse.error(CreateGroupResult.EMPTY_MEMBERS);
        }

        int currentUserId = Session.getCurrentUser().getId();

        List<Integer> memberIds = new ArrayList<>();

        for (String username : usernames) {
            if (username == null || username.isBlank()) {
                continue;
            }

            String cleanUsername = username.trim();

            User user = userService.findByUsername(cleanUsername);

            if (user == null) {
                return CreateGroupResponse.userNotFound(cleanUsername);
            }

            if (user.getId() == currentUserId) {
                continue;
            }

            if (!memberIds.contains(user.getId())) {
                memberIds.add(user.getId());
            }
        }

        if (memberIds.isEmpty()) {
            return CreateGroupResponse.error(CreateGroupResult.ONLY_SELF_SELECTED);
        }

        Chat groupChat = chatService.createGroupChat(
                groupName.trim(),
                currentUserId,
                memberIds
        );

        if (groupChat == null) {
            return CreateGroupResponse.error(CreateGroupResult.DATABASE_ERROR);
        }

        addChatToMemory(groupChat);

        List<Integer> notificationMemberIds = new ArrayList<>();
        notificationMemberIds.add(currentUserId);
        notificationMemberIds.addAll(memberIds);

        return CreateGroupResponse.success(groupChat, notificationMemberIds);
    }

    public CreateGroupResponse createGroupChat(String groupName, List<Integer> memberIds) {
        if (groupName == null || groupName.isBlank()) {
            return CreateGroupResponse.error(CreateGroupResult.EMPTY_GROUP_NAME);
        }

        if (memberIds == null || memberIds.isEmpty()) {
            return CreateGroupResponse.error(CreateGroupResult.EMPTY_MEMBERS);
        }

        int currentUserId = Session.getCurrentUser().getId();

        Chat groupChat = chatService.createGroupChat(
                groupName.trim(),
                currentUserId,
                memberIds
        );

        if (groupChat == null) {
            return CreateGroupResponse.error(CreateGroupResult.DATABASE_ERROR);
        }

        addChatToMemory(groupChat);

        List<Integer> notificationMemberIds = new ArrayList<>();
        notificationMemberIds.add(currentUserId);

        for (Integer memberId : memberIds) {
            if (memberId != null && !notificationMemberIds.contains(memberId)) {
                notificationMemberIds.add(memberId);
            }
        }

        return CreateGroupResponse.success(groupChat, notificationMemberIds);
    }

    public boolean isBotChat(Chat chat) {
        return chat != null && chat.isBot();
    }

    public boolean isGroupChat(Chat chat) {
        return chat != null && "GROUP".equalsIgnoreCase(chat.getType());
    }

    public boolean isPrivateChat(Chat chat) {
        return chat != null && "PRIVATE".equalsIgnoreCase(chat.getType());
    }

    public boolean isChatOnline(Chat chat) {
        if (chat == null || isBotChat(chat)) {
            return false;
        }

        if ("PRIVATE".equalsIgnoreCase(chat.getType())) {
            return isUserOnline(chat.getCompanionUserId());
        }

        if (isGroupChat(chat)) {
            for (Integer userId : getGroupReceiverIds(chat)) {
                if (isUserOnline(userId)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<Integer> getGroupReceiverIds(Chat chat) {
        if (chat == null || !isGroupChat(chat)) {
            return List.of();
        }

        return chatService.getChatMemberIdsExcept(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public List<Integer> getReceiverIdsForChat(Chat chat) {
        if (chat == null) {
            return List.of();
        }

        if ("PRIVATE".equalsIgnoreCase(chat.getType())) {
            if (chat.getCompanionUserId() == null) {
                return List.of();
            }

            return List.of(chat.getCompanionUserId());
        }

        if (isGroupChat(chat)) {
            return getGroupReceiverIds(chat);
        }

        return List.of();
    }

    public List<User> getGroupMembers(Chat chat) {
        if (chat == null || !isGroupChat(chat)) {
            return List.of();
        }

        return chatService.getGroupMembers(chat.getId());
    }

    public String getCurrentUserGroupRole(Chat chat) {
        if (chat == null || !isGroupChat(chat) || Session.getCurrentUser() == null) {
            return null;
        }

        return chatService.getMemberRole(
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

        return chatService.transferOwnership(
                chat.getId(),
                Session.getCurrentUser().getId(),
                member.getId()
        );
    }

    public boolean updateGroupMemberRole(Chat chat, User member, String role) {
        if (chat == null || member == null || Session.getCurrentUser() == null) {
            return false;
        }

        return chatService.updateMemberRole(
                chat.getId(),
                Session.getCurrentUser().getId(),
                member.getId(),
                role
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

        return chatService.kickMemberFromGroup(
                chat.getId(),
                Session.getCurrentUser().getId(),
                member.getId()
        );
    }

    public List<User> getUsersNotInChat(Chat chat) {
        if (chat == null || !isGroupChat(chat) || Session.getCurrentUser() == null) {
            return List.of();
        }

        return chatService.getUsersNotInChat(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public void addMembersToGroup(Chat chat, List<User> users) {
        if (chat == null || users == null || users.isEmpty()) {
            return;
        }

        List<Integer> userIds = users.stream()
                .map(User::getId)
                .toList();

        chatService.addMembersToGroup(chat.getId(), userIds);
    }

    public Chat renameChat(Chat chat, String newName) {
        if (chat == null) {
            return null;
        }

        for (Chat existingChat : chats) {
            if (existingChat.getName().equalsIgnoreCase(newName)
                    && existingChat.getId() != chat.getId()) {
                return null;
            }
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return null;
        }

        boolean groupChat = isGroupChat(chat);

        if (groupChat && !canManageGroup(chat)) {
            return null;
        }

        if (groupChat) {
            chatService.renameGroupChat(chat.getId(), newName);
        } else {
            chatService.renameChat(
                    chat.getId(),
                    Session.getCurrentUser().getId(),
                    newName
            );
        }

        Chat renamedChat = new Chat(
                chat.getId(),
                newName,
                chat.getType(),
                chat.getLastMessageText(),
                chat.getLastMessageTime(),
                chat.getCompanionUserId(),
                chat.getUnreadCount(),
                !groupChat
        );

        chats.set(index, renamedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(renamedChat.getId(), messages);

        return renamedChat;
    }

    public Chat resetChatName(Chat chat) {
        if (chat == null) {
            return null;
        }

        int chatId = chat.getId();

        chatService.resetChatName(
                chatId,
                Session.getCurrentUser().getId()
        );

        reloadChats();

        return findChatById(chatId);
    }

    public void deleteChat(Chat chat) {
        if (chat == null) {
            return;
        }

        chats.remove(chat);
        chatMessages.remove(chat.getId());

        chatService.deleteChatForUser(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public boolean leaveGroup(Chat chat) {
        if (chat == null || !isGroupChat(chat)) {
            return false;
        }

        boolean success = chatService.leaveGroup(
                chat.getId(),
                Session.getCurrentUser().getId()
        );

        if (!success) {
            return false;
        }

        chats.remove(chat);
        chatMessages.remove(chat.getId());

        return true;
    }

    public Chat clearChat(Chat chat) {
        if (chat == null) {
            return null;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages != null) {
            messages.clear();
        } else {
            messages = FXCollections.observableArrayList();
        }

        messageService.clearChat(chat.getId());

        Chat updatedChat = new Chat(
                chat.getId(),
                chat.getName(),
                chat.getType(),
                null,
                null,
                chat.getCompanionUserId(),
                0,
                chat.hasCustomName()
        );

        int index = chats.indexOf(chat);

        if (index != -1) {
            chats.set(index, updatedChat);
        }

        chatMessages.remove(chat.getId());
        chatMessages.put(updatedChat.getId(), messages);

        return updatedChat;
    }

    public ObservableList<Chat> getChats() {
        return chats;
    }

    public ObservableList<Message> getMessagesForChat(Chat chat) {
        if (chat == null) {
            return FXCollections.observableArrayList();
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages == null) {
            return FXCollections.observableArrayList();
        }

        return messages;
    }

    public Chat addMessage(Chat chat, Message message) {
        if (chat == null || message == null) {
            return null;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages == null) {
            return null;
        }

        if (isPrivateChat(chat) && chat.getCompanionUserId() != null) {
            chatService.ensureChatMember(
                    chat.getId(),
                    chat.getCompanionUserId()
            );
        }

        messages.add(message);

        messageService.saveMessage(
                chat.getId(),
                Session.getCurrentUser().getId(),
                message
        );

        return updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );
    }

    public Chat addBotMessage(Chat chat, Message message) {
        if (chat == null || message == null || helperBotUser == null) {
            return null;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages == null) {
            return null;
        }

        messages.add(message);

        messageService.saveMessage(
                chat.getId(),
                helperBotUser.getId(),
                message
        );

        return updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );
    }

    public Chat addSystemMessage(Chat chat, String text, int senderId) {
        if (chat == null || text == null || text.isBlank()) {
            return null;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages == null) {
            messages = FXCollections.observableArrayList();
            chatMessages.put(chat.getId(), messages);
        }

        Message message = Message.system(text);

        messages.add(message);

        messageService.saveMessage(
                chat.getId(),
                senderId,
                message
        );

        return updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );
    }

    private Chat updateChatAfterNewMessage(
            Chat chat,
            String lastMessageText,
            String lastMessageTime
    ) {
        if (chat == null) {
            return null;
        }

        chatService.updateChatActivity(chat.getId());

        int index = chats.indexOf(chat);

        if (index == -1) {
            return null;
        }

        Chat updatedChat = new Chat(
                chat.getId(),
                chat.getName(),
                chat.getType(),
                lastMessageText,
                lastMessageTime,
                chat.getCompanionUserId(),
                chat.getUnreadCount(),
                chat.hasCustomName()
        );

        chats.remove(index);
        chats.add(0, updatedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(updatedChat.getId(), messages);

        return updatedChat;
    }

    public Message generateBotResponse(String userText) {
        String response;

        if (userText.equalsIgnoreCase("hello")) {
            response = "Hello! How are you?";
        } else if (userText.equalsIgnoreCase("how are you")) {
            response = "I am fine :)";
        } else {
            response = "I don't understand";
        }

        return new Message("Bot", response);
    }

    public Chat findChatById(int chatId) {
        for (Chat chat : chats) {
            if (chat.getId() == chatId) {
                return chat;
            }
        }

        return null;
    }

    public Chat addIncomingMessage(Chat chat, Message message) {
        if (chat == null || message == null) {
            return null;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages == null) {
            messages = FXCollections.observableArrayList();
            chatMessages.put(chat.getId(), messages);
        }

        messages.add(message);

        return updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );
    }

    public void reloadChats() {
        chats.clear();
        chatMessages.clear();
        loadChats();
    }

    public void setUserOnline(int userId) {
        onlineUserIds.add(userId);
    }

    public void setOnlineUsers(List<Integer> userIds, int currentUserId) {
        onlineUserIds.clear();

        if (userIds == null) {
            return;
        }

        for (Integer userId : userIds) {
            if (userId != null && userId != currentUserId) {
                onlineUserIds.add(userId);
            }
        }
    }

    public void setUserOffline(int userId) {
        onlineUserIds.remove(userId);
    }

    public boolean isUserOnline(Integer userId) {
        return userId != null && onlineUserIds.contains(userId);
    }

    public Chat updateCompanionDisplayName(int companionUserId, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }

        for (int i = 0; i < chats.size(); i++) {
            Chat chat = chats.get(i);

            if (chat.getCompanionUserId() == null
                    || chat.hasCustomName()
                    || chat.getCompanionUserId() != companionUserId) {
                continue;
            }

            Chat updatedChat = new Chat(
                    chat.getId(),
                    displayName,
                    chat.getType(),
                    chat.getLastMessageText(),
                    chat.getLastMessageTime(),
                    chat.getCompanionUserId(),
                    chat.getUnreadCount(),
                    chat.hasCustomName()
            );

            chats.set(i, updatedChat);

            ObservableList<Message> messages = chatMessages.remove(chat.getId());
            chatMessages.put(updatedChat.getId(), messages);

            return updatedChat;
        }

        return null;
    }

    public Chat increaseUnreadCount(Chat chat) {
        if (chat == null) {
            return null;
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return null;
        }

        Chat updatedChat = new Chat(
                chat.getId(),
                chat.getName(),
                chat.getType(),
                chat.getLastMessageText(),
                chat.getLastMessageTime(),
                chat.getCompanionUserId(),
                chat.getUnreadCount() + 1,
                chat.hasCustomName()
        );

        chats.set(index, updatedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(updatedChat.getId(), messages);

        return updatedChat;
    }

    public Chat resetUnreadCount(Chat chat) {
        if (chat == null) {
            return null;
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return null;
        }

        if (chat.getUnreadCount() == 0) {
            return chat;
        }

        Chat updatedChat = new Chat(
                chat.getId(),
                chat.getName(),
                chat.getType(),
                chat.getLastMessageText(),
                chat.getLastMessageTime(),
                chat.getCompanionUserId(),
                0,
                chat.hasCustomName()
        );

        chats.set(index, updatedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(updatedChat.getId(), messages);

        return updatedChat;
    }
}
