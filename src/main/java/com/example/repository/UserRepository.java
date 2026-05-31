package com.example.repository;

import com.example.model.User;
import com.example.network.ServerApi;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-side gateway for the user domain. All data access goes through the
 * MessengerServer REST API; the client no longer talks to PostgreSQL directly.
 */
public class UserRepository {

    private final ServerApi api = new ServerApi();

    public User findByUsername(String username) {
        HttpResponse<String> response = api.get("/api/users/by-username/" + ServerApi.encode(username));

        if (response.statusCode() != 200) {
            return null;
        }

        return toUser(api.readTree(response.body()));
    }

    public User login(String username, String password) {
        HttpResponse<String> response = api.post(
                "/api/users/login",
                Map.of("username", username, "password", password)
        );

        if (response.statusCode() != 200) {
            return null;
        }

        return toUser(api.readTree(response.body()));
    }

    /**
     * @return the created user, or {@code null} if the username is already taken.
     */
    public User register(String username, String displayName, String password) {
        HttpResponse<String> response = api.post(
                "/api/users/register",
                Map.of("username", username, "displayName", displayName, "password", password)
        );

        if (response.statusCode() != 200) {
            return null;
        }

        return toUser(api.readTree(response.body()));
    }

    public User createSystemUserIfNotExists(String username, String displayName) {
        HttpResponse<String> response = api.post(
                "/api/users/system",
                Map.of("username", username, "displayName", displayName)
        );

        if (response.statusCode() != 200) {
            return null;
        }

        return toUser(api.readTree(response.body()));
    }

    public boolean isHelperInitialized(int userId) {
        HttpResponse<String> response = api.get("/api/users/" + userId + "/helper-initialized");

        return response.statusCode() == 200 && Boolean.parseBoolean(response.body().trim());
    }

    public void markHelperInitialized(int userId) {
        api.post("/api/users/" + userId + "/helper-initialized", null);
    }

    public boolean updateDisplayName(int userId, String newDisplayName) {
        HttpResponse<String> response = api.put(
                "/api/users/" + userId + "/display-name",
                Map.of("displayName", newDisplayName)
        );

        return response.statusCode() == 200;
    }

    public List<User> getUsersNotInChat(int chatId, int currentUserId) {
        HttpResponse<String> response = api.get(
                "/api/users/non-members?chatId=" + chatId + "&currentUserId=" + currentUserId
        );

        List<User> users = new ArrayList<>();

        if (response.statusCode() != 200) {
            return users;
        }

        for (JsonNode node : api.readTree(response.body())) {
            users.add(toUser(node));
        }

        return users;
    }

    /** @return companion's last-seen time as epoch millis, or {@code null} if unknown. */
    public Long getLastSeen(int userId) {
        HttpResponse<String> response = api.get("/api/users/" + userId + "/last-seen");

        if (response.statusCode() != 200) {
            return null;
        }

        try {
            return Long.parseLong(response.body().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private User toUser(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String memberRole = node.hasNonNull("memberRole")
                ? node.get("memberRole").asText()
                : null;

        return new User(
                node.get("id").asInt(),
                node.get("username").asText(),
                node.get("displayName").asText(),
                memberRole
        );
    }
}
