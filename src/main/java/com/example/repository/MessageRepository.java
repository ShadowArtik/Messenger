package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void saveMessage(String contactName, Message message) {
        String sql = """
                INSERT INTO messages (contact_name, sender, text, message_time)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, contactName);
            statement.setString(2, message.getSender());
            statement.setString(3, message.getText());
            statement.setString(4, message.getFormattedTime());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Message> getMessages(String contactName) {
        List<Message> messages = new ArrayList<>();

        String sql = """
                SELECT sender, text, message_time
                FROM messages
                WHERE contact_name = ?
                ORDER BY created_at ASC, id ASC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, contactName);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                messages.add(new Message(
                        resultSet.getString("sender"),
                        resultSet.getString("text"),
                        resultSet.getString("message_time")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public void deleteMessages(String contactName) {
        String sql = "DELETE FROM messages WHERE contact_name = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, contactName);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}