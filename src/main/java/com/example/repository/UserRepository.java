package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.User;

import java.sql.*;

public class UserRepository {

    public void createUser(String username, String passwordHash) {

        String sql = """
                INSERT INTO users (username, password_hash)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findByUsername(String username) {

        String sql = """
                SELECT id, username
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {

                return new User(
                        resultSet.getInt("id"),
                        resultSet.getString("username")
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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
}