package com.example.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

public class MessengerModel {

    private final ObservableList<String> contacts;
    private final Map<String, ObservableList<Message>> chatMessages;

    public MessengerModel() {
        contacts = FXCollections.observableArrayList();
        chatMessages = new HashMap<>();

        addContact("Anna");
        addContact("Victor");
        addContact("Bot");
    }

    private void addContact(String contactName) {
        contacts.add(contactName);
        chatMessages.put(contactName, FXCollections.observableArrayList());
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
    }

    public void deleteChat(String contactName) {
        contacts.remove(contactName);
        chatMessages.remove(contactName);
    }

    public void clearChat(String contactName) {
        chatMessages.get(contactName).clear();
    }

    public ObservableList<String> getContacts() {
        return contacts;
    }

    public ObservableList<Message> getMessagesForContact(String contactName) {
        return chatMessages.get(contactName);
    }

    public void addMessage(String contactName, Message message) {
        chatMessages.get(contactName).add(message);
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
