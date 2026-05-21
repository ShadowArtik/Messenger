package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.User;
import com.example.util.PasswordHasher;

import java.sql.*;

public class UserRepository {

    public void createUser(String username, String displayName, String passwordHash) {
        String sql = """
                INSERT INTO users (username, display_name, password_hash)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, displayName);
            statement.setString(3, passwordHash);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findByUsername(String username) {
        String sql = """
                SELECT id, username, display_name
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new User(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("display_name")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getPasswordHash(String username) {
        String sql = """
                SELECT password_hash
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("password_hash");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public User createSystemUserIfNotExists(String username, String displayName) {
        User existingUser = findByUsername(username);

        if (existingUser != null) {
            return existingUser;
        }

        createUser(
                username,
                displayName,
                PasswordHasher.hashPassword("system")
        );

        return findByUsername(username);
    }
}