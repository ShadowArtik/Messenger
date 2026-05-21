package com.example.service;

import com.example.model.Message;
import com.example.repository.MessageRepository;

import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository =
            new MessageRepository();

    public void saveMessage(
            int chatId,
            int senderId,
            Message message
    ) {
        messageRepository.saveMessage(
                chatId,
                senderId,
                message
        );
    }

    public List<Message> getMessages(int chatId) {
        return messageRepository.getMessages(chatId);
    }

    public void clearChat(int chatId) {
        messageRepository.deleteMessages(chatId);
    }
}