package org.example.messengerserver.controller;

import org.example.messengerserver.dto.ActiveSessionResponse;
import org.example.messengerserver.storage.ConnectedUserStorage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SessionController {

    // =================== Endpoints ===================

    private final ConnectedUserStorage connectedUserStorage;

    public SessionController(ConnectedUserStorage connectedUserStorage) {
        this.connectedUserStorage = connectedUserStorage;
    }

    @GetMapping("/api/sessions")
    public List<ActiveSessionResponse> getActiveSessions() {
        return connectedUserStorage.getOnlineUserIds()
                .stream()
                .map(userId -> new ActiveSessionResponse(userId))
                .toList();
    }
}
