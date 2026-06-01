package org.example.messengerserver.controller;

import org.example.messengerserver.dto.ChatResponse;
import org.example.messengerserver.dto.UserResponse;
import org.example.messengerserver.repository.ChatRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatRepository chatRepository;

    public ChatController(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    // =================== Request bodies ===================

    public record CreateChatRequest(String name, String type, int userId) {
    }

    public record CreatePrivateChatRequest(String chatName, int currentUserId, int targetUserId) {
    }

    public record CreateGroupChatRequest(String groupName, int creatorId, List<Integer> memberIds) {
    }

    public record MemberRequest(int userId) {
    }

    public record CustomNameRequest(int userId, String name) {
    }

    public record GroupNameRequest(String name) {
    }

    public record RoleRequest(String role) {
    }

    public record TransferOwnershipRequest(int currentOwnerId, int newOwnerId) {
    }

    @GetMapping
    // =================== Endpoints ===================

    public List<ChatResponse> getChatsForUser(@RequestParam int userId) {
        return chatRepository.getChatsForUser(userId);
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@RequestBody CreateChatRequest request) {
        ChatResponse chat = chatRepository.createChat(request.name(), request.type(), request.userId());
        return chat == null ? ResponseEntity.status(500).build() : ResponseEntity.ok(chat);
    }

    @PostMapping("/private")
    public ResponseEntity<ChatResponse> createPrivateChat(@RequestBody CreatePrivateChatRequest request) {
        ChatResponse chat = chatRepository.createPrivateChat(
                request.chatName(), request.currentUserId(), request.targetUserId()
        );
        return chat == null ? ResponseEntity.status(500).build() : ResponseEntity.ok(chat);
    }

    @PostMapping("/group")
    public ResponseEntity<ChatResponse> createGroupChat(@RequestBody CreateGroupChatRequest request) {
        ChatResponse chat = chatRepository.createGroupChat(
                request.groupName(), request.creatorId(), request.memberIds()
        );
        return chat == null ? ResponseEntity.status(500).build() : ResponseEntity.ok(chat);
    }

    @PostMapping("/{chatId}/members")
    public void addMember(@PathVariable int chatId, @RequestBody MemberRequest request) {
        chatRepository.addMember(chatId, request.userId());
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    public void removeMember(@PathVariable int chatId, @PathVariable int userId) {
        chatRepository.removeMember(chatId, userId);
    }

    @DeleteMapping("/{chatId}/me")
    public void hideChatForUser(@PathVariable int chatId, @RequestParam int userId) {
        chatRepository.hideChatForUser(chatId, userId);
    }

    @GetMapping("/{chatId}/member-ids")
    public List<Integer> getChatMemberIdsExcept(
            @PathVariable int chatId,
            @RequestParam int excludeUserId
    ) {
        return chatRepository.getChatMemberIdsExcept(chatId, excludeUserId);
    }

    @GetMapping("/{chatId}/members")
    public List<UserResponse> getGroupMembers(@PathVariable int chatId) {
        return chatRepository.getGroupMembers(chatId);
    }

    @PutMapping("/{chatId}/custom-name")
    public void renameCustomName(@PathVariable int chatId, @RequestBody CustomNameRequest request) {
        chatRepository.renameCustomName(chatId, request.userId(), request.name());
    }

    @DeleteMapping("/{chatId}/custom-name")
    public void resetCustomName(@PathVariable int chatId, @RequestParam int userId) {
        chatRepository.resetCustomName(chatId, userId);
    }

    @PutMapping("/{chatId}/name")
    public void renameGroupChat(@PathVariable int chatId, @RequestBody GroupNameRequest request) {
        chatRepository.renameGroupChat(chatId, request.name());
    }

    @GetMapping("/{chatId}/members/{userId}/role")
    public ResponseEntity<String> getMemberRole(@PathVariable int chatId, @PathVariable int userId) {
        String role = chatRepository.getMemberRole(chatId, userId);
        return role == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(role);
    }

    @PutMapping("/{chatId}/members/{userId}/role")
    public boolean updateMemberRole(
            @PathVariable int chatId,
            @PathVariable int userId,
            @RequestBody RoleRequest request
    ) {
        return chatRepository.updateMemberRole(chatId, userId, request.role());
    }

    @PostMapping("/{chatId}/transfer-ownership")
    public boolean transferOwnership(
            @PathVariable int chatId,
            @RequestBody TransferOwnershipRequest request
    ) {
        return chatRepository.transferOwnership(chatId, request.currentOwnerId(), request.newOwnerId());
    }

    @PutMapping("/{chatId}/activity")
    public void updateChatActivity(@PathVariable int chatId) {
        chatRepository.updateChatActivity(chatId);
    }
}
