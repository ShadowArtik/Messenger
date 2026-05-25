package com.example.network;

import com.example.network.dto.ConnectMessage;
import com.example.network.dto.PrivateMessage;
import com.example.network.dto.ProfileUpdatedMessage;
import com.example.network.dto.TypingMessage;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WebSocketClient {

    private WebSocket webSocket;
    private Consumer<String> messageHandler;

    private final Gson gson = new Gson();

    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

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

                                ConnectMessage connectMessage =
                                        new ConnectMessage(userId, username, displayName);

                                sendMessage(gson.toJson(connectMessage));

                                webSocket.request(1);
                            }

                            @Override
                            public CompletionStage<?> onText(
                                    WebSocket webSocket,
                                    CharSequence data,
                                    boolean last
                            ) {
                                String message = data.toString();

                                System.out.println("Message from server: " + message);

                                if (messageHandler != null) {
                                    Platform.runLater(() -> messageHandler.accept(message));
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

    public void sendPrivateMessage(
            int chatId,
            int senderId,
            int receiverId,
            String senderUsername,
            String senderDisplayName,
            String text
    ) {
        PrivateMessage privateMessage = new PrivateMessage(
                chatId,
                senderId,
                receiverId,
                senderUsername,
                senderDisplayName,
                text
        );

        sendMessage(gson.toJson(privateMessage));
    }

    public void sendTypingMessage(
            int chatId,
            int senderId,
            int receiverId,
            String senderDisplayName
    ) {
        TypingMessage typingMessage = new TypingMessage(
                chatId,
                senderId,
                receiverId,
                senderDisplayName
        );

        sendMessage(gson.toJson(typingMessage));
    }

    public void sendProfileUpdatedMessage(int userId, String displayName) {
        ProfileUpdatedMessage profileUpdatedMessage =
                new ProfileUpdatedMessage(userId, displayName);

        sendMessage(gson.toJson(profileUpdatedMessage));
    }

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
