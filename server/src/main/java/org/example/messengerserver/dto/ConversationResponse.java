package org.example.messengerserver.dto;

public class ConversationResponse {

    // =================== Fields & accessors ===================

    private final int chatId;
    private final String chatName;
    private final String chatType;
    private final String members;
    private final String lastMessageSender;
    private final String lastMessage;
    private final String lastMessageTime;
    private final int messagesCount;

    public ConversationResponse(
            int chatId,
            String chatName,
            String chatType,
            String members,
            String lastMessageSender,
            String lastMessage,
            String lastMessageTime,
            int messagesCount
    ) {
        this.chatId = chatId;
        this.chatName = chatName;
        this.chatType = chatType;
        this.members = members;
        this.lastMessageSender = lastMessageSender;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.messagesCount = messagesCount;
    }

    public int getChatId() {
        return chatId;
    }

    public String getChatName() {
        return chatName;
    }

    public String getChatType() {
        return chatType;
    }

    public String getMembers() {
        return members;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public int getMessagesCount() {
        return messagesCount;
    }
}
