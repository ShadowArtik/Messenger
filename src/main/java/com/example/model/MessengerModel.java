package com.example.model;

import com.example.service.ChatService;
import com.example.service.MessageService;
import com.example.service.UserService;
import com.example.service.result.CreateChatResponse;
import com.example.service.result.CreateChatResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
                targetUser.getId(),
                targetUser.getDisplayName(),
                Session.getCurrentUser().getDisplayName()
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

    public boolean isBotChat(Chat chat) {
        return chat != null && chat.isBot();
    }

    public void renameChat(Chat chat, String newName) {
        if (chat == null) {
            return;
        }

        for (Chat existingChat : chats) {
            if (existingChat.getName().equalsIgnoreCase(newName)
                    && existingChat.getId() != chat.getId()) {
                return;
            }
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return;
        }

        chatService.renameChat(
                chat.getId(),
                Session.getCurrentUser().getId(),
                newName
        );

        Chat renamedChat = new Chat(
                chat.getId(),
                newName,
                chat.getType(),
                chat.getLastMessageText(),
                chat.getLastMessageTime(),
                chat.getCompanionUserId(),
                chat.getUnreadCount()
        );

        chats.set(index, renamedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(renamedChat.getId(), messages);
    }

    public void deleteChat(Chat chat) {
        if (chat == null) {
            return;
        }

        chats.remove(chat);
        chatMessages.remove(chat.getId());

        chatService.deleteChat(chat.getId());
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
                0
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
                chat.getUnreadCount()
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
                chat.getUnreadCount() + 1
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
                0
        );

        chats.set(index, updatedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(updatedChat.getId(), messages);

        return updatedChat;
    }
}