package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.util.PasswordHasher;

public class UserService {

    private final UserRepository userRepository = new UserRepository();
    private User currentUser;

    public boolean register(String username, String displayName, String password) {
        if (username == null || username.isBlank()) {
            return false;
        }

        if (displayName == null || displayName.isBlank()) {
            return false;
        }

        if (password == null || password.isBlank()) {
            return false;
        }

        if (userRepository.findByUsername(username) != null) {
            return false;
        }

        String hashedPassword = PasswordHasher.hashPassword(password);

        userRepository.createUser(
                username,
                displayName,
                hashedPassword
        );

        currentUser = userRepository.findByUsername(username);

        return true;
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
}
