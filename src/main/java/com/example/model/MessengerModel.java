package com.example.model;

import com.example.service.ChatService;
import com.example.service.MessageService;
import com.example.service.UserService;
import com.example.service.result.CreateChatResponse;
import com.example.service.result.CreateChatResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessengerModel {

    private final ObservableList<Chat> chats;
    private final Map<Integer, ObservableList<Message>> chatMessages;

    private final MessageService messageService;
    private final ChatService chatService;
    private final UserService userService;
    private User helperBotUser;

    public MessengerModel() {
        chats = FXCollections.observableArrayList();
        chatMessages = new HashMap<>();

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

        if (savedChats.isEmpty()) {
            Chat helperChat = chatService.createBotChat("Helper", currentUserId);

            if (helperChat != null) {
                addChatToMemory(helperChat);
            }

            return;
        }

        for (Chat chat : savedChats) {
            addChatToMemory(chat);
        }
    }

    private void addChatToMemory(Chat chat) {
        if (chatMessages.containsKey(chat.getId())) {
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
            if (existingChat.getName()
                    .equalsIgnoreCase(
                            targetUser.getDisplayName()
                    )) {

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

    public boolean isBotChat(Chat chat) {
        return chat != null && chat.isBot();
    }

    public void renameChat(Chat chat, String newName) {
        if (chat == null) {
            return;
        }

        for (Chat existingChat : chats) {
            if (existingChat.getName()
                    .equalsIgnoreCase(newName)
                    && existingChat.getId() != chat.getId()) {
                return;
            }
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return;
        }

        chatService.renameChat(chat.getId(), newName);

        Chat renamedChat = new Chat(
                chat.getId(),
                newName,
                chat.getType()
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

    public void clearChat(Chat chat) {
        if (chat == null) {
            return;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages != null) {
            messages.clear();
        }

        messageService.clearChat(chat.getId());
    }

    public ObservableList<Chat> getChats() {
        return chats;
    }

    public ObservableList<Message> getMessagesForChat(Chat chat) {
        if (chat == null) {
            return FXCollections.observableArrayList();
        }

        return chatMessages.get(chat.getId());
    }

    public void addMessage(Chat chat, Message message) {
        if (chat == null) {
            return;
        }

        chatMessages.get(chat.getId()).add(message);

        messageService.saveMessage(
                chat.getId(),
                Session.getCurrentUser().getId(),
                message
        );
    }

    public void addBotMessage(Chat chat, Message message) {
        if (chat == null || helperBotUser == null) {
            return;
        }

        chatMessages.get(chat.getId()).add(message);

        messageService.saveMessage(
                chat.getId(),
                helperBotUser.getId(),
                message
        );
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
}
