package org.example.messengerserver.controller;

import org.example.messengerserver.dto.UserResponse;
import org.example.messengerserver.repository.UserRepository;
import org.example.messengerserver.service.PasswordHasher;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserController(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    // =================== Request bodies ===================

    public record RegisterRequest(String username, String displayName, String password) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record DisplayNameRequest(String displayName) {
    }

    public record SystemUserRequest(String username, String displayName) {
    }

    @PostMapping("/register")
    // =================== Endpoints ===================

    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()) != null) {
            return ResponseEntity.status(409).build();
        }

        userRepository.createUser(
                request.username(),
                request.displayName(),
                passwordHasher.hash(request.password())
        );

        return ResponseEntity.ok(userRepository.findByUsername(request.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
        String storedHash = userRepository.getPasswordHash(request.username());

        if (!passwordHasher.matches(request.password(), storedHash)) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(userRepository.findByUsername(request.username()));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponse> findByUsername(@PathVariable String username) {
        UserResponse user = userRepository.findByUsername(username);

        return user == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/display-name")
    public ResponseEntity<Void> updateDisplayName(
            @PathVariable int id,
            @RequestBody DisplayNameRequest request
    ) {
        boolean updated = userRepository.updateDisplayName(id, request.displayName());

        return updated
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(500).build();
    }

    @PostMapping("/system")
    public UserResponse createSystemUser(@RequestBody SystemUserRequest request) {
        UserResponse existing = userRepository.findByUsername(request.username());

        if (existing != null) {
            return existing;
        }

        userRepository.createUser(
                request.username(),
                request.displayName(),
                passwordHasher.hash("system")
        );

        return userRepository.findByUsername(request.username());
    }

    @GetMapping("/{id}/helper-initialized")
    public boolean isHelperInitialized(@PathVariable int id) {
        return userRepository.isHelperInitialized(id);
    }

    @GetMapping("/{id}/last-seen")
    public ResponseEntity<Long> getLastSeen(@PathVariable int id) {
        Long lastSeen = userRepository.getLastSeen(id);

        return lastSeen == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(lastSeen);
    }

    @PostMapping("/{id}/helper-initialized")
    public void markHelperInitialized(@PathVariable int id) {
        userRepository.markHelperInitialized(id);
    }

    @GetMapping("/non-members")
    public List<UserResponse> getUsersNotInChat(
            @RequestParam int chatId,
            @RequestParam int currentUserId
    ) {
        return userRepository.getUsersNotInChat(chatId, currentUserId);
    }
}
