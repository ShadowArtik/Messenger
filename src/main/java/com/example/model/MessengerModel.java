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

        chatService.renameChat(chat.getId(), Session.getCurrentUser().getId(), newName);

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

    public Chat clearChat(Chat chat) {
        if (chat == null) {
            return null;
        }

        ObservableList<Message> messages = chatMessages.get(chat.getId());

        if (messages != null) {
            messages.clear();
        }

        messageService.clearChat(chat.getId());

        Chat updatedChat = new Chat(
                chat.getId(),
                chat.getName(),
                chat.getType(),
                null,
                null
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

        return chatMessages.get(chat.getId());
    }

    public Chat addMessage(Chat chat, Message message) {
        if (chat == null) {
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

        updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );

        return chats.get(0);
    }

    public Chat addBotMessage(Chat chat, Message message) {
        if (chat == null || helperBotUser == null) {
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

        updateChatAfterNewMessage(
                chat,
                message.getText(),
                message.getFormattedTime()
        );

        return chats.get(0);
    }

    private void updateChatAfterNewMessage(Chat chat, String lastMessageText, String lastMessageTime) {
        if (chat == null) {
            return;
        }

        int index = chats.indexOf(chat);

        if (index == -1) {
            return;
        }

        Chat updatedChat = new Chat(
                chat.getId(),
                chat.getName(),
                chat.getType(),
                lastMessageText,
                lastMessageTime
        );

        chats.remove(index);
        chats.add(0, updatedChat);

        ObservableList<Message> messages = chatMessages.remove(chat.getId());
        chatMessages.put(updatedChat.getId(), messages);
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

    private void moveChatToTop(Chat chat) {
        int index = chats.indexOf(chat);

        if (index > 0) {
            chats.remove(chat);
            chats.add(0, chat);
        }
    }
}
