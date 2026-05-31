package com.example.repository;

import com.example.model.Chat;
import com.example.model.User;
import com.example.network.ServerApi;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-side gateway for the chat domain. All data access goes through the
 * MessengerServer REST API; the client no longer talks to PostgreSQL directly.
 */
public class ChatRepository {

    private final ServerApi api = new ServerApi();

    // =================== Chat list / create ===================

    public List<Chat> getChatsForUser(int userId) {
        HttpResponse<String> response = api.get("/api/chats?userId=" + userId);

        List<Chat> chats = new ArrayList<>();

        if (response.statusCode() != 200) {
            return chats;
        }

        for (JsonNode node : api.readTree(response.body())) {
            chats.add(toChat(node));
        }

        return chats;
    }

    public Chat createChat(String name, String type, int userId) {
        HttpResponse<String> response = api.post(
                "/api/chats",
                Map.of("name", name, "type", type, "userId", userId)
        );

        return response.statusCode() == 200 ? toChat(api.readTree(response.body())) : null;
    }

    public Chat createPrivateChat(String chatName, int currentUserId, int targetUserId) {
        HttpResponse<String> response = api.post(
                "/api/chats/private",
                Map.of("chatName", chatName, "currentUserId", currentUserId, "targetUserId", targetUserId)
        );

        return response.statusCode() == 200 ? toChat(api.readTree(response.body())) : null;
    }

    public Chat createGroupChat(String groupName, int creatorId, List<Integer> memberIds) {
        HttpResponse<String> response = api.post(
                "/api/chats/group",
                Map.of("groupName", groupName, "creatorId", creatorId, "memberIds", memberIds)
        );

        return response.statusCode() == 200 ? toChat(api.readTree(response.body())) : null;
    }

    // =================== Rename / reset ===================

    public void renameChat(int chatId, int userId, String newName) {
        api.put("/api/chats/" + chatId + "/custom-name", Map.of("userId", userId, "name", newName));
    }

    public void renameGroupChat(int chatId, String newName) {
        api.put("/api/chats/" + chatId + "/name", Map.of("name", newName));
    }

    public void resetChatName(int chatId, int userId) {
        api.delete("/api/chats/" + chatId + "/custom-name?userId=" + userId);
    }

    // =================== Membership ===================

    public void deleteChatForUser(int chatId, int userId) {
        removeMember(chatId, userId);
    }

    public void leaveGroup(int chatId, int userId) {
        removeMember(chatId, userId);
    }

    public void removeMemberFromGroup(int chatId, int userId) {
        removeMember(chatId, userId);
    }

    private void removeMember(int chatId, int userId) {
        api.delete("/api/chats/" + chatId + "/members/" + userId);
    }

    public void ensureChatMember(int chatId, int userId) {
        addMemberToChat(chatId, userId);
    }

    public void addMemberToChat(int chatId, int userId) {
        api.post("/api/chats/" + chatId + "/members", Map.of("userId", userId));
    }

    public List<Integer> getChatMemberIdsExcept(int chatId, int excludedUserId) {
        HttpResponse<String> response = api.get(
                "/api/chats/" + chatId + "/member-ids?excludeUserId=" + excludedUserId
        );

        List<Integer> ids = new ArrayList<>();

        if (response.statusCode() != 200) {
            return ids;
        }

        for (JsonNode node : api.readTree(response.body())) {
            ids.add(node.asInt());
        }

        return ids;
    }

    public List<User> getGroupMembers(int chatId) {
        HttpResponse<String> response = api.get("/api/chats/" + chatId + "/members");

        List<User> members = new ArrayList<>();

        if (response.statusCode() != 200) {
            return members;
        }

        for (JsonNode node : api.readTree(response.body())) {
            members.add(toUser(node));
        }

        return members;
    }

    // =================== Roles ===================

    public String getMemberRole(int chatId, int userId) {
        HttpResponse<String> response = api.get("/api/chats/" + chatId + "/members/" + userId + "/role");

        return response.statusCode() == 200 ? response.body().trim() : null;
    }

    public boolean updateMemberRole(int chatId, int userId, String role) {
        HttpResponse<String> response = api.put(
                "/api/chats/" + chatId + "/members/" + userId + "/role",
                Map.of("role", role)
        );

        return response.statusCode() == 200 && Boolean.parseBoolean(response.body().trim());
    }

    public boolean transferOwnership(int chatId, int currentOwnerId, int newOwnerId) {
        HttpResponse<String> response = api.post(
                "/api/chats/" + chatId + "/transfer-ownership",
                Map.of("currentOwnerId", currentOwnerId, "newOwnerId", newOwnerId)
        );

        return response.statusCode() == 200 && Boolean.parseBoolean(response.body().trim());
    }

    public void updateChatActivity(int chatId) {
        api.put("/api/chats/" + chatId + "/activity", null);
    }

    // =================== Mapping ===================

    private Chat toChat(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        Integer companionUserId = node.hasNonNull("companionUserId")
                ? node.get("companionUserId").asInt()
                : null;

        return new Chat(
                node.get("id").asInt(),
                node.get("name").asText(),
                node.get("type").asText(),
                node.hasNonNull("lastMessageText") ? node.get("lastMessageText").asText() : null,
                node.hasNonNull("lastMessageTime") ? node.get("lastMessageTime").asText() : null,
                companionUserId,
                node.hasNonNull("unreadCount") ? node.get("unreadCount").asInt() : 0,
                node.get("customName").asBoolean()
        );
    }

    private User toUser(JsonNode node) {
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
