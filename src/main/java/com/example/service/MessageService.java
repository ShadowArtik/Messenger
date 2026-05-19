package com.example.service;

import com.example.model.Message;
import com.example.repository.MessageRepository;

import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository = new MessageRepository();

    public void saveMessage(String contactName, Message message) {
        messageRepository.saveMessage(contactName, message);
    }

    public List<Message> getMessages(String contactName) {
        return messageRepository.getMessages(contactName);
    }

    public void clearChat(String contactName) {
        messageRepository.deleteMessages(contactName);
    }
}