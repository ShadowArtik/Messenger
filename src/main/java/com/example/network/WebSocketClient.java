package com.example.network;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WebSocketClient {

    private WebSocket webSocket;
    private Consumer<String> messageHandler;
    private final StringBuilder messageBuffer = new StringBuilder();

    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    // =================== Connection ===================

    public void connect(int userId, String username, String displayName) {
        HttpClient client = HttpClient.newHttpClient();

        client.newWebSocketBuilder()
                .buildAsync(
                        URI.create("ws://localhost:8080/ws/chat"),
                        new WebSocket.Listener() {

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                WebSocketClient.this.webSocket = webSocket;

                                System.out.println("Connected to WebSocket server");

                                sendMessage(XmlProtocol.connect(userId, username, displayName));

                                webSocket.request(1);
                            }

                            @Override
                            public CompletionStage<?> onText(
                                    WebSocket webSocket,
                                    CharSequence data,
                                    boolean last
                            ) {
                                messageBuffer.append(data);

                                if (last) {
                                    String message = messageBuffer.toString();
                                    messageBuffer.setLength(0);

                                    if (messageHandler != null) {
                                        Platform.runLater(() -> messageHandler.accept(message));
                                    }
                                }

                                webSocket.request(1);
                                return null;
                            }

                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                System.out.println("WebSocket error:");
                                error.printStackTrace();
                            }
                        }
                );
    }

    // =================== Senders ===================

    public void sendLoadHistory(int chatId) {
        sendMessage(XmlProtocol.loadHistory(chatId));
    }

    public void sendClearChat(int chatId, int senderId) {
        sendMessage(XmlProtocol.clearChat(chatId, senderId));
    }

    public void sendMessageRead(int chatId, int readerId) {
        sendMessage(XmlProtocol.messageRead(chatId, readerId));
    }

    public void sendSaveMessage(int chatId, int senderId, String text) {
        sendMessage(XmlProtocol.saveMessage(chatId, senderId, text));
    }

    public void sendPrivateMessage(
            int chatId,
            int senderId,
            int receiverId,
            String senderUsername,
            String senderDisplayName,
            String text,
            String msgId
    ) {
        String privateMessage = XmlProtocol.privateMessage(
                chatId,
                senderId,
                receiverId,
                senderUsername,
                senderDisplayName,
                text,
                msgId
        );

        sendMessage(privateMessage);
    }

    public void sendGroupMessage(
            int chatId,
            int senderId,
            String senderUsername,
            String senderDisplayName,
            String text,
            String msgId
    ) {
        sendMessage(XmlProtocol.groupMessage(
                chatId,
                senderId,
                senderUsername,
                senderDisplayName,
                text,
                msgId
        ));
    }

    public void sendDeleteMessage(int chatId, int senderId, String msgId) {
        sendMessage(XmlProtocol.deleteMessage(chatId, senderId, msgId));
    }

    public void sendEditMessage(int chatId, int senderId, String msgId, String newText) {
        sendMessage(XmlProtocol.editMessage(chatId, senderId, msgId, newText));
    }

    public void sendTypingMessage(
            int chatId,
            int senderId,
            List<Integer> receiverIds,
            String senderDisplayName
    ) {
        String typingMessage = XmlProtocol.typing(
                chatId,
                senderId,
                receiverIds,
                senderDisplayName
        );

        sendMessage(typingMessage);
    }

    public void sendProfileUpdatedMessage(int userId, String displayName) {
        sendMessage(XmlProtocol.profileUpdated(userId, displayName));
    }

    public void sendGroupCreatedMessage(
            int senderId,
            int chatId,
            String chatName,
            List<Integer> memberIds
    ) {
        sendMessage(XmlProtocol.groupCreated(senderId, chatId, chatName, memberIds));
    }

    public void sendGroupMembersUpdatedMessage(
            int senderId,
            int chatId,
            List<Integer> memberIds,
            String systemMessageText
    ) {
        sendMessage(XmlProtocol.groupMembersUpdated(
                senderId,
                chatId,
                memberIds,
                systemMessageText
        ));
    }

    public void sendGroupRenamedMessage(
            int senderId,
            int chatId,
            String oldName,
            String newName,
            List<Integer> memberIds,
            String systemMessageText
    ) {
        sendMessage(XmlProtocol.groupRenamed(
                senderId,
                chatId,
                oldName,
                newName,
                memberIds,
                systemMessageText
        ));
    }

    // =================== Low-level send ===================

    public void sendMessage(String text) {
        if (webSocket == null) {
            System.out.println("WebSocket is not connected");
            return;
        }

        webSocket.sendText(text, true);
    }

    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client logged out");
            webSocket = null;
        }
    }
}
