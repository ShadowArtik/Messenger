package org.example.messengerserver.websocket;

import java.util.List;

public class XmlMessage {

    // =================== Fields & accessors ===================

    private String type;
    private int chatId;
    private int senderId;
    private int receiverId;
    private int userId;
    private List<Integer> receiverIds = List.of();
    private List<Integer> memberIds = List.of();
    private String text;
    private String systemMessageText;
    private String msgId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<Integer> getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(List<Integer> receiverIds) {
        this.receiverIds = receiverIds;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Integer> memberIds) {
        this.memberIds = memberIds;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSystemMessageText() {
        return systemMessageText;
    }

    public void setSystemMessageText(String systemMessageText) {
        this.systemMessageText = systemMessageText;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
