package com.example.model;

import com.example.network.WebSocketClient;
import com.example.repository.ChatRepository;
import com.example.service.GroupService;
import com.example.service.UserService;
import com.example.service.result.CreateGroupResponse;
import com.example.service.result.CreateChatResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class MessengerModel {

    private final ObservableList<Chat> chats;
    private final MessageStore messageStore = new MessageStore();

    private boolean reordering = false;

    private WebSocketClient webSocketClient;
    private final ChatRepository chatRepository;
    private final UserService userService;
    private final GroupService groupService;
    private final PresenceTracker presence = new PresenceTracker();

    private User helperBotUser;

    public MessengerModel() {
        chats = FXCollections.observableArrayList();

        chatRepository = new ChatRepository();
        userService = new UserService();
        groupService = new GroupService();

        helperBotUser = userService.createSystemUserIfNotExists(
                "helper_bot",
                "Helper"
        );

        loadChats();
    }

    public GroupService groupService() {
        return groupService;
    }

    private void loadChats() {
        int currentUserId = Session.getCurrentUser().getId();

        List<Chat> savedChats = chatRepository.getChatsForUser(currentUserId);

        for (Chat chat : savedChats) {
            addChatToMemory(chat);
        }

        if (!userService.isHelperInitialized(currentUserId)) {
            Chat helperChat = chatRepository.createChat("Helper", "BOT", currentUserId);

            if (helperChat != null) {
                addChatToMemory(helperChat);
            }

            userService.markHelperInitialized(currentUserId);
        }
    }

    private void addChatToMemory(Chat chat) {
        addChatToMemory(chat, false);
    }

    private void addChatToMemory(Chat chat, boolean atTop) {
        if (chat == null || messageStore.has(chat.getId())) {
            return;
        }

        // Loading appends in the server's activity order; a freshly created chat
        // goes to the top so it shows up first for its creator.
        if (atTop) {
            chats.add(0, chat);
        } else {
            chats.add(chat);
        }

        messageStore.ensure(chat.getId());
    }

    public CreateChatResponse addChat(String username) {
        User targetUser = userService.findByUsername(username);

        if (targetUser == null) {
            return new CreateChatResponse(
                    CreateChatResponse.Result.USER_NOT_FOUND,
                    null
            );
        }

        if (targetUser.getId() == Session.getCurrentUser().getId()) {
            return new CreateChatResponse(
                    CreateChatResponse.Result.SELF_CHAT,
                    null
            );
        }

        for (Chat existingChat : chats) {
            if (existingChat.getCompanionUserId() != null
                    && existingChat.getCompanionUserId().equals(targetUser.getId())) {

                return new CreateChatResponse(
                        CreateChatResponse.Result.CHAT_ALREADY_EXISTS,
                        null
                );
            }
        }

        Chat chat = chatRepository.createPrivateChat(
                targetUser.getDisplayName(),
                Session.getCurrentUser().getId(),
                targetUser.getId()
        );

        if (chat == null) {
            return new CreateChatResponse(
                    CreateChatResponse.Result.DATABASE_ERROR,
                    null
            );
        }

        addChatToMemory(chat, true);

        return new CreateChatResponse(
                CreateChatResponse.Result.SUCCESS,
                chat
        );
    }

    public CreateGroupResponse createGroupChat(String groupName, List<Integer> memberIds) {
        if (groupName == null || groupName.isBlank()) {
            return CreateGroupResponse.error(CreateGroupResponse.Result.EMPTY_GROUP_NAME);
        }

        if (memberIds == null || memberIds.isEmpty()) {
            return CreateGroupResponse.error(CreateGroupResponse.Result.EMPTY_MEMBERS);
        }

        int currentUserId = Session.getCurrentUser().getId();

        Chat groupChat = chatRepository.createGroupChat(
                groupName.trim(),
                currentUserId,
                memberIds
        );

        if (groupChat == null) {
            return CreateGroupResponse.error(CreateGroupResponse.Result.DATABASE_ERROR);
        }

        addChatToMemory(groupChat, true);

        List<Integer> notificationMemberIds = new ArrayList<>();
        notificationMemberIds.add(currentUserId);

        for (Integer memberId : memberIds) {
            if (memberId != null && !notificationMemberIds.contains(memberId)) {
                notificationMemberIds.add(memberId);
            }
        }

        return CreateGroupResponse.success(groupChat, notificationMemberIds);
    }

    // =================== Chat type / online ===================

    public boolean isBotChat(Chat chat) {
        return chat != null && chat.isBot();
    }

    public boolean isGroupChat(Chat chat) {
        return chat != null && chat.isGroup();
    }

    public boolean isChatOnline(Chat chat) {
        if (chat == null || chat.isBot()) {
            return false;
        }

        if (chat.isPrivate()) {
            return isUserOnline(chat.getCompanionUserId());
        }

        if (chat.isGroup()) {
            for (Integer userId : getGroupReceiverIds(chat)) {
                if (isUserOnline(userId)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<Integer> getReceiverIdsForChat(Chat chat) {
        if (chat == null) {
            return List.of();
        }

        if (chat.isPrivate()) {
            if (chat.getCompanionUserId() == null) {
                return List.of();
            }

            return List.of(chat.getCompanionUserId());
        }

        if (chat.isGroup()) {
            return getGroupReceiverIds(chat);
        }

        return List.of();
    }

    // =================== Group facade (logic lives in GroupService) ===================

    public List<Integer> getGroupReceiverIds(Chat chat) {
        return groupService.getGroupReceiverIds(chat);
    }

    public List<User> getGroupMembers(Chat chat) {
        return groupService.getGroupMembers(chat);
    }

    public boolean canManageGroup(Chat chat) {
        return groupService.canManageGroup(chat);
    }

    // =================== Chat mutations ===================

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

        boolean groupChat = chat.isGroup();

        if (groupChat && !canManageGroup(chat)) {
            return null;
        }

        if (groupChat) {
            chatRepository.renameGroupChat(chat.getId(), newName);
        } else {
            chatRepository.renameChat(
                    chat.getId(),
                    Session.getCurrentUser().getId(),
                    newName
            );
        }

        Chat renamedChat = chat.withName(newName, !groupChat);
        chats.set(index, renamedChat);

        return renamedChat;
    }

    public Chat resetChatName(Chat chat) {
        if (chat == null) {
            return null;
        }

        int chatId = chat.getId();

        chatRepository.resetChatName(
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
        messageStore.remove(chat.getId());

        chatRepository.deleteChatForUser(
                chat.getId(),
                Session.getCurrentUser().getId()
        );
    }

    public boolean leaveGroup(Chat chat) {
        if (chat == null || !chat.isGroup()) {
            return false;
        }

        chatRepository.leaveGroup(chat.getId(), Session.getCurrentUser().getId());
        chats.remove(chat);
        messageStore.remove(chat.getId());
        return true;
    }

    /** Owner action: ask the server to delete the whole group for every member. */
    public void deleteGroupForAll(Chat chat) {
        if (chat == null || !chat.isGroup() || webSocketClient == null) {
            return;
        }

        webSocketClient.sendDeleteGroup(chat.getId(), Session.getCurrentUser().getId());
    }

    /** Remove a chat from memory only (the server already deleted it). */
    public void removeChatLocally(int chatId) {
        Chat chat = findChatById(chatId);
        if (chat != null) {
            chats.remove(chat);
        }
        messageStore.remove(chatId);
    }

    public Chat clearChat(Chat chat) {
        if (chat == null) {
            return null;
        }

        messageStore.clear(chat.getId());

        if (webSocketClient != null) {
            webSocketClient.sendClearChat(chat.getId(), Session.getCurrentUser().getId());
        }

        Chat updatedChat = chat.withLastMessage(null, null).withUnreadCount(0);

        int index = chats.indexOf(chat);

        if (index != -1) {
            chats.set(index, updatedChat);
        }

        return updatedChat;
    }

    public Chat applyRemoteClear(int chatId) {
        Chat chat = findChatById(chatId);

        if (chat == null) {
            return null;
        }

        messageStore.clear(chatId);

        int index = chats.indexOf(chat);
        Chat updatedChat = chat.withLastMessage(null, null).withUnreadCount(0);

        if (index != -1) {
            chats.set(index, updatedChat);
        }

        return updatedChat;
    }

    public boolean markOutgoingMessagesRead(int chatId) {
        if (Session.getCurrentUser() == null) {
            return false;
        }

        return messageStore.markOutgoingRead(chatId, Session.getCurrentUser().getId());
    }

    public boolean deleteMessageLocal(int chatId, String clientId) {
        if (clientId == null) {
            return false;
        }

        boolean removed = messageStore.deleteByClientId(chatId, clientId);

        if (removed) {
            refreshChatPreview(chatId);
        }

        return removed;
    }

    public boolean editMessageLocal(int chatId, String clientId, String newText) {
        if (clientId == null || newText == null) {
            return false;
        }

        boolean changed = messageStore.editByClientId(chatId, clientId, newText);

        if (changed) {
            refreshChatPreview(chatId);
        }

        return changed;
    }

    public void refreshChatPreviewFromServer(int chatId) {
        if (Session.getCurrentUser() == null) {
            return;
        }

        Chat local = findChatById(chatId);

        if (local == null) {
            return;
        }

        List<Chat> serverChats = chatRepository.getChatsForUser(Session.getCurrentUser().getId());

        for (Chat serverChat : serverChats) {
            if (serverChat.getId() == chatId) {
                int index = chats.indexOf(local);

                if (index != -1) {
                    Chat updated = local.withLastMessage(
                            serverChat.getLastMessageText(),
                            serverChat.getLastMessageTime()
                    );
                    chats.set(index, updated);
                }

                return;
            }
        }
    }

    private void refreshChatPreview(int chatId) {
        Chat chat = findChatById(chatId);

        if (chat == null) {
            return;
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return;
        }

        Message last = messageStore.last(chatId);

        Chat updatedChat = last == null
                ? chat.withLastMessage(null, null)
                : chat.withLastMessage(last.getText(), last.getFormattedTime());

        chats.set(index, updatedChat);
    }

    public void setWebSocketClient(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    public void setMessagesForChat(int chatId, List<Message> messages) {
        messageStore.setMessages(chatId, messages);
    }

    public ObservableList<Chat> getChats() {
        return chats;
    }

    public ObservableList<Message> getMessagesForChat(Chat chat) {
        if (chat == null) {
            return FXCollections.observableArrayList();
        }

        return messageStore.getOrEmpty(chat.getId());
    }

    // =================== Messages ===================

    public Chat addMessage(Chat chat, Message message) {
        if (chat == null || message == null) {
            return null;
        }

        if (!messageStore.has(chat.getId())) {
            return null;
        }

        if (chat.isPrivate() && chat.getCompanionUserId() != null) {
            chatRepository.ensureChatMember(
                    chat.getId(),
                    chat.getCompanionUserId()
            );
        }

        messageStore.add(chat.getId(), message);

        if (chat.isBot() && webSocketClient != null && message.getSenderId() != null) {
            webSocketClient.sendSaveMessage(
                    chat.getId(),
                    message.getSenderId(),
                    message.getStorageText()
            );
        }

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

        if (!messageStore.has(chat.getId())) {
            return null;
        }

        messageStore.add(chat.getId(), message);

        if (webSocketClient != null) {
            webSocketClient.sendSaveMessage(chat.getId(), helperBotUser.getId(), message.getStorageText());
        }
        chatRepository.updateChatActivity(chat.getId());

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

        Message message = Message.system(text);

        messageStore.add(chat.getId(), message);

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

        int index = chats.indexOf(chat);

        if (index == -1) {
            return null;
        }

        Chat updatedChat = chat.withLastMessage(lastMessageText, lastMessageTime);

        reordering = true;
        try {
            chats.remove(index);
            chats.add(0, updatedChat);
        } finally {
            reordering = false;
        }

        return updatedChat;
    }

    public boolean isReordering() {
        return reordering;
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

        messageStore.add(chat.getId(), message);

        return updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );
    }

    public void reloadChats() {
        chats.clear();
        messageStore.clearAll();
        loadChats();
    }

    // =================== Online presence ===================

    public void setUserOnline(int userId) {
        presence.setUserOnline(userId);
    }

    public void setOnlineUsers(List<Integer> userIds, int currentUserId) {
        presence.setOnlineUsers(userIds, currentUserId);
    }

    public void setUserOffline(int userId) {
        presence.setUserOffline(userId);
    }

    public boolean isUserOnline(Integer userId) {
        return presence.isUserOnline(userId);
    }

    // =================== Chat list updates ===================

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

            Chat updatedChat = chat.withName(displayName, chat.hasCustomName());
            chats.set(i, updatedChat);

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

        Chat updatedChat = chat.withUnreadCount(chat.getUnreadCount() + 1);
        chats.set(index, updatedChat);

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

        Chat updatedChat = chat.withUnreadCount(0);

        // Replacing the selected chat re-fires the ListView selection listener,
        // which would re-enter openChat (with unread already 0). Suppress that.
        reordering = true;
        try {
            chats.set(index, updatedChat);
        } finally {
            reordering = false;
        }

        return updatedChat;
    }
}
