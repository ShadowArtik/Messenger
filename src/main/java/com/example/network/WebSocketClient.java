package com.example.network;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WebSocketClient {

    private WebSocket webSocket;
    private Consumer<String> messageHandler;

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

                                String connectMessage = String.format(
                                        """
                                        {
                                          "type": "CONNECT",
                                          "userId": %d,
                                          "username": "%s",
                                          "displayName": "%s"
                                        }
                                        """,
                                        userId,
                                        username,
                                        displayName
                                );

                                webSocket.sendText(connectMessage, true);
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

    public void sendMessage(String text) {
        if (webSocket == null) {
            System.out.println("WebSocket is not connected");
            return;
        }

        webSocket.sendText(text, true);
    }

    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closed");
        }
    }
}