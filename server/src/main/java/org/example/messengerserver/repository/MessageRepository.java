package org.example.messengerserver.repository;

import org.example.messengerserver.dto.MessageResponse;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MessageRepository {

    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =================== Queries ===================

    public List<MessageResponse> getMessages(int chatId) {
        List<MessageResponse> messages = new ArrayList<>();

        String sql = """
                SELECT u.id AS sender_id,
                       u.username,
                       u.display_name,
                       m.text,
                       m.created_at,
                       m.is_read,
                       m.client_id,
                       m.edited
                FROM messages m
                JOIN users u ON m.sender_id = u.id
                WHERE m.chat_id = ?
                ORDER BY m.created_at ASC, m.id ASC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                var moment = rs.getTimestamp("created_at")
                        .toInstant()
                        .atZone(ZoneId.of("Europe/Kyiv"));

                String time = moment.format(DateTimeFormatter.ofPattern("HH:mm"));
                String date = moment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                messages.add(new MessageResponse(
                        rs.getInt("sender_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("text"),
                        time,
                        date,
                        rs.getBoolean("is_read"),
                        rs.getString("client_id"),
                        rs.getBoolean("edited")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    // =================== Mutations ===================

    public void deleteMessages(int chatId) {
        String sql = "DELETE FROM messages WHERE chat_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveSystemMessage(int chatId, int senderId, String text) {
        saveMessage(chatId, senderId, "[SYSTEM] " + text, null);
    }

    public void deleteByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        String sql = "DELETE FROM messages WHERE client_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, clientId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void editByClientId(String clientId, String newText) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        String sql = "UPDATE messages SET text = ?, edited = TRUE WHERE client_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newText);
            statement.setString(2, clientId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markRead(int chatId, int readerId) {
        String sql = "UPDATE messages SET is_read = TRUE WHERE chat_id = ? AND sender_id <> ? AND is_read = FALSE";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, readerId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveMessage(int chatId, int senderId, String text) {
        saveMessage(chatId, senderId, text, null);
    }

    public void saveMessage(int chatId, int senderId, String text, String clientId) {
        String sql = "INSERT INTO messages (chat_id, sender_id, text, client_id) VALUES (?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, senderId);
            statement.setString(3, text);
            statement.setString(4, clientId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
