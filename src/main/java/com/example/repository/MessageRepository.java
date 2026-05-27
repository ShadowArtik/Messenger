package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.Message;

import java.sql.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void saveMessage(int chatId, int senderId, Message message) {
        String sql = """
                INSERT INTO messages (chat_id, sender_id, text)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, senderId);
            statement.setString(3, message.getStorageText());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Message> getMessages(int chatId) {
        List<Message> messages = new ArrayList<>();

        String sql = """
                SELECT u.id AS sender_id,
                       u.username,
                       u.display_name,
                       m.text,
                       m.created_at
                FROM messages m
                JOIN users u ON m.sender_id = u.id
                WHERE m.chat_id = ?
                ORDER BY m.created_at ASC, m.id ASC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Timestamp createdAt = resultSet.getTimestamp("created_at");

                String time = createdAt.toInstant()
                        .atZone(ZoneId.of("Europe/Kyiv"))
                        .format(DateTimeFormatter.ofPattern("HH:mm"));

                messages.add(new Message(
                        resultSet.getInt("sender_id"),
                        resultSet.getString("username"),
                        resultSet.getString("display_name"),
                        resultSet.getString("text"),
                        time
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public void deleteMessages(int chatId) {
        String sql = "DELETE FROM messages WHERE chat_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
