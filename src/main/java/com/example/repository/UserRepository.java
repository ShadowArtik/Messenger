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
}
