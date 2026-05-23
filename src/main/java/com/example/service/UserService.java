package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.util.PasswordHasher;
import com.example.service.result.RegisterResult;

public class UserService {

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

        String hashedPassword =
                PasswordHasher.hashPassword(password);

        userRepository.createUser(
                username,
                displayName,
                hashedPassword
        );

        currentUser =
                userRepository.findByUsername(username);

        return RegisterResult.SUCCESS;
    }

    public boolean login(String username, String password) {
        String savedPassword = userRepository.getPasswordHash(username);

        if (savedPassword == null) {
            return false;
        }

        if (!PasswordHasher.checkPassword(password, savedPassword)) {
            return false;
        }

        currentUser = userRepository.findByUsername(username);
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
}
