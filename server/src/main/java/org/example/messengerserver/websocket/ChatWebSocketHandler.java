package org.example.messengerserver.websocket;

import org.example.messengerserver.repository.ChatRepository;
import org.example.messengerserver.repository.MessageRepository;
import org.example.messengerserver.repository.UserRepository;
import org.example.messengerserver.storage.ConnectedUserStorage;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ConnectedUserStorage connectedUserStorage;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    private final Object sendLock = new Object();

    public ChatWebSocketHandler(
            ConnectedUserStorage connectedUserStorage,
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository
    ) {
        this.connectedUserStorage = connectedUserStorage;
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    @Override
    // =================== Lifecycle ===================

    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("WebSocket connection opened: " + session.getId());
    }

    @Override
    // =================== Message dispatch ===================

    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        XmlMessage xmlMessage = XmlProtocol.parse(payload);
        String type = xmlMessage.getType();

        if (type.equals("CONNECT")) {
            int userId = xmlMessage.getUserId();

            connectedUserStorage.addUser(userId, session);

            send(session, XmlProtocol.connectSuccess());
            send(session, XmlProtocol.onlineUsers(connectedUserStorage.getOnlineUserIds()));

            broadcastUserStatusExcept("USER_ONLINE", userId);

            System.out.println("User connected: " + userId);
            return;
        }

        if (type.equals("PRIVATE_MESSAGE")) {
            int chatId = xmlMessage.getChatId();

            chatRepository.addMember(chatId, xmlMessage.getSenderId());
            chatRepository.addMember(chatId, xmlMessage.getReceiverId());

            messageRepository.saveMessage(chatId, xmlMessage.getSenderId(), xmlMessage.getText(), xmlMessage.getMsgId());
            chatRepository.updateChatActivity(chatId);
            sendToUser(xmlMessage.getReceiverId(), payload);
            send(session, payload);
            return;
        }

        if (type.equals("GROUP_MESSAGE")) {
            int chatId = xmlMessage.getChatId();
            int senderId = xmlMessage.getSenderId();

            List<Integer> memberIds = chatRepository.getChatMemberIds(chatId);

            if (!memberIds.contains(senderId)) {
                return;
            }

            messageRepository.saveMessage(chatId, senderId, xmlMessage.getText(), xmlMessage.getMsgId());
            chatRepository.updateChatActivity(chatId);

            for (Integer memberId : memberIds) {
                if (memberId == senderId) {
                    continue;
                }
                sendToUser(memberId, payload);
            }

            send(session, payload);

            return;
        }

        if (type.equals("LOAD_HISTORY")) {
            int chatId = xmlMessage.getChatId();
            send(session, XmlProtocol.history(chatId, messageRepository.getMessages(chatId)));
            return;
        }

        if (type.equals("CLEAR_CHAT")) {
            int chatId = xmlMessage.getChatId();
            int senderId = xmlMessage.getSenderId();

            messageRepository.deleteMessages(chatId);
            chatRepository.updateChatActivity(chatId);

            for (Integer memberId : chatRepository.getChatMemberIds(chatId)) {
                if (memberId == senderId) {
                    continue;
                }
                sendToUser(memberId, payload);
            }

            return;
        }

        if (type.equals("MESSAGE_READ")) {
            int chatId = xmlMessage.getChatId();
            int readerId = xmlMessage.getSenderId();

            messageRepository.markRead(chatId, readerId);

            for (Integer memberId : chatRepository.getChatMemberIds(chatId)) {
                if (memberId == readerId) {
                    continue;
                }
                sendToUser(memberId, XmlProtocol.messagesRead(chatId));
            }

            return;
        }

        if (type.equals("DELETE_MESSAGE")) {
            int chatId = xmlMessage.getChatId();
            int senderId = xmlMessage.getSenderId();
            String msgId = xmlMessage.getMsgId();

            messageRepository.deleteByClientId(msgId);
            chatRepository.updateChatActivity(chatId);

            for (Integer memberId : chatRepository.getChatMemberIds(chatId)) {
                sendToUser(memberId, XmlProtocol.messageDeleted(chatId, msgId));
            }

            return;
        }

        if (type.equals("EDIT_MESSAGE")) {
            int chatId = xmlMessage.getChatId();
            int senderId = xmlMessage.getSenderId();
            String msgId = xmlMessage.getMsgId();
            String newText = xmlMessage.getText();

            messageRepository.editByClientId(msgId, newText);
            chatRepository.updateChatActivity(chatId);

            for (Integer memberId : chatRepository.getChatMemberIds(chatId)) {
                sendToUser(memberId, XmlProtocol.messageEdited(chatId, msgId, newText));
            }

            return;
        }

        if (type.equals("DELETE_GROUP")) {
            int chatId = xmlMessage.getChatId();
            int senderId = xmlMessage.getSenderId();

            // Only the group owner may delete the whole group.
            if (!"OWNER".equalsIgnoreCase(chatRepository.getMemberRole(chatId, senderId))) {
                return;
            }

            List<Integer> memberIds = chatRepository.getChatMemberIds(chatId);
            chatRepository.deleteChat(chatId);

            for (Integer memberId : memberIds) {
                sendToUser(memberId, XmlProtocol.groupDeleted(chatId));
            }

            return;
        }

        if (type.equals("SAVE_MESSAGE")) {
            messageRepository.saveMessage(
                    xmlMessage.getChatId(),
                    xmlMessage.getSenderId(),
                    xmlMessage.getText()
            );
            return;
        }

        if (type.equals("TYPING")) {
            sendToUsers(xmlMessage.getReceiverIds(), payload);
            return;
        }

        if (type.equals("USER_PROFILE_UPDATED")) {
            broadcastExcept(xmlMessage.getUserId(), payload);
            return;
        }

        if (type.equals("GROUP_CREATED")) {
            sendToUsersExcept(xmlMessage.getMemberIds(), xmlMessage.getSenderId(), payload);
            return;
        }

        if (type.equals("GROUP_MEMBERS_UPDATED") || type.equals("GROUP_RENAMED")) {
            int chatId = xmlMessage.getChatId();
            int senderId = xmlMessage.getSenderId();
            String systemText = xmlMessage.getSystemMessageText();

            if (systemText != null && !systemText.isBlank()) {
                messageRepository.saveSystemMessage(chatId, senderId, systemText);
                chatRepository.updateChatActivity(chatId);
            }

            java.util.Set<Integer> recipients =
                    new java.util.HashSet<>(chatRepository.getChatMemberIds(chatId));
            if (xmlMessage.getMemberIds() != null) {
                recipients.addAll(xmlMessage.getMemberIds());
            }

            for (Integer memberId : recipients) {
                if (memberId == senderId) {
                    continue;
                }
                sendToUser(memberId, payload);
            }

            return;
        }

        System.out.println("Unknown message type: " + type);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer disconnectedUserId = connectedUserStorage.removeUser(session);

        if (disconnectedUserId != null) {
            userRepository.updateLastSeen(disconnectedUserId);
            broadcastUserStatus("USER_OFFLINE", disconnectedUserId);
        }

        System.out.println("WebSocket connection closed: " + session.getId());
    }

    // =================== Send helpers ===================

    private void send(WebSocketSession session, String payload) throws Exception {
        if (session == null || !session.isOpen()) {
            return;
        }

        synchronized (sendLock) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private void sendToUser(int userId, String payload) throws Exception {
        send(connectedUserStorage.getSession(userId), payload);
    }

    private void sendToUsers(Iterable<Integer> userIds, String payload) throws Exception {
        for (Integer userId : userIds) {
            sendToUser(userId, payload);
        }
    }

    private void sendToUsersExcept(
            Iterable<Integer> userIds,
            int excludedUserId,
            String payload
    ) throws Exception {
        for (Integer userId : userIds) {
            if (userId == excludedUserId) {
                continue;
            }

            sendToUser(userId, payload);
        }
    }

    private void broadcastExcept(int excludedUserId, String payload) throws Exception {
        for (Integer onlineUserId : connectedUserStorage.getOnlineUserIds()) {
            if (onlineUserId == excludedUserId) {
                continue;
            }

            sendToUser(onlineUserId, payload);
        }
    }

    private void broadcastUserStatusExcept(String type, int userId) throws Exception {
        String statusMessage = XmlProtocol.userStatus(type, userId);

        for (Integer onlineUserId : connectedUserStorage.getOnlineUserIds()) {
            if (onlineUserId == userId) {
                continue;
            }

            sendToUser(onlineUserId, statusMessage);
        }
    }

    private void broadcastUserStatus(String type, int userId) {
        String statusMessage = XmlProtocol.userStatus(type, userId);

        for (WebSocketSession clientSession : connectedUserStorage.getAllSessions()) {
            try {
                send(clientSession, statusMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
