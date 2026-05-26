package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.User;
import com.example.util.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public boolean isHelperInitialized(int userId) {
        String sql = """
            SELECT helper_initialized
            FROM users
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean("helper_initialized");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void markHelperInitialized(int userId) {
        String sql = """
            UPDATE users
            SET helper_initialized = TRUE
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean updateDisplayName(int userId, String newDisplayName) {
        String sql = """
                UPDATE users
                SET display_name = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newDisplayName);
            statement.setInt(2, userId);

            int updatedRows = statement.executeUpdate();

            return updatedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<User> getUsersNotInChat(int chatId, int currentUserId) {
        List<User> users = new ArrayList<>();

        String sql = """
            SELECT DISTINCT u.id,
                            u.username,
                            u.display_name
            FROM users u
            JOIN chat_members my_private_membership
                 ON my_private_membership.user_id = ?
            JOIN chats private_chat
                 ON private_chat.id = my_private_membership.chat_id
                AND private_chat.chat_type = 'PRIVATE'
            JOIN chat_members companion_membership
                 ON companion_membership.chat_id = private_chat.id
                AND companion_membership.user_id = u.id
                AND companion_membership.user_id <> ?
            WHERE u.username <> 'helper_bot'
              AND u.id NOT IN (
                  SELECT user_id
                  FROM chat_members
                  WHERE chat_id = ?
              )
            ORDER BY u.display_name
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, currentUserId);
            statement.setInt(2, currentUserId);
            statement.setInt(3, chatId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                users.add(new User(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("display_name")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
}
