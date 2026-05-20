package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;

public class UserService {

    private final UserRepository userRepository = new UserRepository();
    private User currentUser;

    public boolean register(String username, String password) {
        if (username == null || username.isBlank()) {
            return false;
        }

        if (password == null || password.isBlank()) {
            return false;
        }

        if (userRepository.findByUsername(username) != null) {
            return false;
        }

        userRepository.createUser(username, password);
        currentUser = userRepository.findByUsername(username);

        return true;
    }

    public boolean login(String username, String password) {
        String savedPassword = userRepository.getPasswordHash(username);

        if (savedPassword == null) {
            return false;
        }

        if (!savedPassword.equals(password)) {
            return false;
        }

        currentUser = userRepository.findByUsername(username);
        return true;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}