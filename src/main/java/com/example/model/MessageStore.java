package com.example.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MessageStore {

    private final Map<Integer, ObservableList<Message>> chatMessages = new HashMap<>();

    // =================== Access ===================

    boolean has(int chatId) {
        return chatMessages.containsKey(chatId);
    }

    ObservableList<Message> getOrCreate(int chatId) {
        return chatMessages.computeIfAbsent(chatId, id -> FXCollections.observableArrayList());
    }

    ObservableList<Message> getOrEmpty(int chatId) {
        ObservableList<Message> messages = chatMessages.get(chatId);
        return messages != null ? messages : FXCollections.observableArrayList();
    }

    void ensure(int chatId) {
        getOrCreate(chatId);
    }

    // =================== Mutations ===================

    void setMessages(int chatId, List<Message> messages) {
        getOrCreate(chatId).setAll(messages != null ? messages : List.of());
    }

    void add(int chatId, Message message) {
        getOrCreate(chatId).add(message);
    }

    void remove(int chatId) {
        chatMessages.remove(chatId);
    }

    void clear(int chatId) {
        ObservableList<Message> messages = chatMessages.get(chatId);
        if (messages != null) {
            messages.clear();
        } else {
            getOrCreate(chatId);
        }
    }

    void clearAll() {
        chatMessages.clear();
    }

    Message last(int chatId) {
        ObservableList<Message> messages = chatMessages.get(chatId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    boolean deleteByClientId(int chatId, String clientId) {
        ObservableList<Message> messages = chatMessages.get(chatId);
        if (messages == null) {
            return false;
        }
        return messages.removeIf(m -> clientId.equals(m.getClientId()));
    }

    boolean editByClientId(int chatId, String clientId, String newText) {
        ObservableList<Message> messages = chatMessages.get(chatId);
        if (messages == null) {
            return false;
        }

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);

            if (clientId.equals(message.getClientId())) {
                message.setText(newText);
                message.setEdited(true);

                messages.set(i, message);
                return true;
            }
        }

        return false;
    }

    boolean markOutgoingRead(int chatId, int currentUserId) {
        ObservableList<Message> messages = chatMessages.get(chatId);
        if (messages == null) {
            return false;
        }

        boolean changed = false;
        for (Message message : messages) {
            if (message.getSenderId() != null
                    && message.getSenderId() == currentUserId
                    && !message.isRead()) {
                message.setRead(true);
                changed = true;
            }
        }
        return changed;
    }
}
