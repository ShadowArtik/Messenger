package com.example.model;

import com.example.service.MessageService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

public class MessengerModel {

    private final ObservableList<String> contacts;
    private final Map<String, ObservableList<Message>> chatMessages;
    private final MessageService messageService;

    public MessengerModel() {
        contacts = FXCollections.observableArrayList();
        chatMessages = new HashMap<>();
        messageService = new MessageService();

        addContact("Anna");
        addContact("Victor");
        addContact("Bot");
    }

    public void addContact(String contactName) {
        if (chatMessages.containsKey(contactName)) {
            return;
        }

        contacts.add(contactName);

        ObservableList<Message> messages =
                FXCollections.observableArrayList(messageService.getMessages(contactName));

        chatMessages.put(contactName, messages);
    }

    public boolean hasContact(String contactName) {
        return chatMessages.containsKey(contactName);
    }

    public void renameChat(String oldName, String newName) {
        int contactIndex = contacts.indexOf(oldName);

        if (contactIndex == -1 || chatMessages.containsKey(newName)) {
            return;
        }

        ObservableList<Message> messages = chatMessages.remove(oldName);
        chatMessages.put(newName, messages);
        contacts.set(contactIndex, newName);

        // Пока переименование меняет только UI.
        // Позже можно добавить update contact_name в базе.
    }

    public void deleteChat(String contactName) {
        contacts.remove(contactName);
        chatMessages.remove(contactName);
        messageService.clearChat(contactName);
    }

    public void clearChat(String contactName) {
        chatMessages.get(contactName).clear();
        messageService.clearChat(contactName);
    }

    public ObservableList<String> getContacts() {
        return contacts;
    }

    public ObservableList<Message> getMessagesForContact(String contactName) {
        return chatMessages.get(contactName);
    }

    public void addMessage(String contactName, Message message) {
        chatMessages.get(contactName).add(message);
        messageService.saveMessage(contactName, message);
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