package com.example.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.HashMap;
import java.util.Map;
import com.example.storage.ChatStorage;
import java.util.ArrayList;
import java.util.List;

public class MessengerModel {

    private final ObservableList<String> contacts;
    private final Map<String, ObservableList<Message>> chatMessages;

    public MessengerModel() {

        contacts = FXCollections.observableArrayList();
        chatMessages = new HashMap<>();

        Map<String, List<Message>> savedChats = ChatStorage.loadChats();

        if (savedChats.isEmpty()) {

            addContact("Anna");
            addContact("Victor");
            addContact("Bot");

        } else {

            for (String contact : savedChats.keySet()) {

                addContact(contact);

                chatMessages.get(contact).setAll(savedChats.get(contact));
            }
        }
    }

    public void addContact(String contactName) {
        contacts.add(contactName);
        chatMessages.put(contactName, FXCollections.observableArrayList());
        saveChats();
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

        saveChats();
    }

    public void deleteChat(String contactName) {
        contacts.remove(contactName);
        chatMessages.remove(contactName);
        saveChats();
    }

    public void clearChat(String contactName) {
        chatMessages.get(contactName).clear();
        saveChats();
    }

    public void saveChats() {
        Map<String, List<Message>> data = new HashMap<>();

        for (String contact : contacts) {
            data.put(contact, new ArrayList<>(chatMessages.get(contact)));
        }

        ChatStorage.saveChats(data);
    }

    public ObservableList<String> getContacts() {
        return contacts;
    }

    public ObservableList<Message> getMessagesForContact(String contactName) {
        return chatMessages.get(contactName);
    }

    public void addMessage(String contactName, Message message) {
        chatMessages.get(contactName).add(message);
        saveChats();
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
