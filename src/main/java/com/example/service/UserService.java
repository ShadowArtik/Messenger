package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;

public class UserService {

    public enum RegisterResult {
        SUCCESS,
        INVALID_USERNAME,
        INVALID_DISPLAY_NAME,
        INVALID_PASSWORD,
        USER_ALREADY_EXISTS
    }

    private final UserRepository userRepository = new UserRepository();
    private User currentUser;

    public RegisterResult register(
            String username,
            String displayName,
            String password
    ) {

        if (username == null ||
                !username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            return RegisterResult.INVALID_USERNAME;
        }

        if (displayName == null ||
                displayName.isBlank() ||
                displayName.length() < 2 ||
                displayName.length() > 30) {

            return RegisterResult.INVALID_DISPLAY_NAME;
        }

        if (password == null ||
                password.length() < 6) {

            return RegisterResult.INVALID_PASSWORD;
        }

        if (userRepository.findByUsername(username) != null) {
            return RegisterResult.USER_ALREADY_EXISTS;
        }

        User createdUser = userRepository.register(username, displayName, password);

        if (createdUser == null) {
            return RegisterResult.USER_ALREADY_EXISTS;
        }

        currentUser = createdUser;

        return RegisterResult.SUCCESS;
    }

    public boolean login(String username, String password) {
        User user = userRepository.login(username, password);

        if (user == null) {
            return false;
        }

        currentUser = user;
        return true;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createSystemUserIfNotExists(String username, String displayName) {
        return userRepository.createSystemUserIfNotExists(username, displayName);
    }

    public boolean isHelperInitialized(int userId) {
        return userRepository.isHelperInitialized(userId);
    }

    public void markHelperInitialized(int userId) {
        userRepository.markHelperInitialized(userId);
    }

    public boolean updateDisplayName(int userId, String newDisplayName) {
        if (newDisplayName == null || newDisplayName.isBlank()) {
            return false;
        }

        if (newDisplayName.length() > 100) {
            return false;
        }

        return userRepository.updateDisplayName(
                userId,
                newDisplayName.trim()
        );
    }
}
