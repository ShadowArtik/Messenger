package org.example.messengerserver.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectedUserStorage {

    // =================== Online users ===================

    private final Map<Integer, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    public void addUser(int userId, WebSocketSession session) {
        onlineUsers.put(userId, session);
    }

    public WebSocketSession getSession(int userId) {
        return onlineUsers.get(userId);
    }

    public Set<Integer> getOnlineUserIds() {
        return onlineUsers.keySet();
    }

    public Collection<WebSocketSession> getAllSessions() {
        return onlineUsers.values();
    }

    public Integer removeUser(WebSocketSession session) {
        Integer removedUserId = null;

        for (Map.Entry<Integer, WebSocketSession> entry : onlineUsers.entrySet()) {
            if (entry.getValue().equals(session)) {
                removedUserId = entry.getKey();
                break;
            }
        }

        if (removedUserId != null) {
            onlineUsers.remove(removedUserId);
        }

        return removedUserId;
    }
}
