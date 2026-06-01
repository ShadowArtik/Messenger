package org.example.messengerserver.repository;

import org.example.messengerserver.dto.ConversationResponse;
import org.example.messengerserver.dto.MessageHistoryResponse;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AdminDashboardRepository {

    private final DataSource dataSource;

    public AdminDashboardRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =================== Queries ===================

    public List<ConversationResponse> getAllConversations() {
        List<ConversationResponse> conversations = new ArrayList<>();

        String sql = """
                SELECT
                    c.id AS chat_id,
                    c.chat_name,
                    c.chat_type,
                    COALESCE(
                        STRING_AGG(DISTINCT u.display_name || ' (@' || u.username || ')', ', '),
                        'No members'
                    ) AS members,
                    COALESCE(
                        CASE
                            WHEN last_msg.text LIKE '[SYSTEM] %'
                            THEN 'System'
                            ELSE sender.display_name
                        END,
                        ''
                    ) AS last_message_sender,
                    COALESCE(
                        CASE
                            WHEN last_msg.text LIKE '[SYSTEM] %'
                            THEN SUBSTRING(last_msg.text FROM 10)
                            ELSE last_msg.text
                        END,
                        'No messages yet'
                    ) AS last_message,
                    COALESCE(TO_CHAR(last_msg.created_at + INTERVAL '3 hours', 'HH24:MI'), '') AS last_message_time,
                    COUNT(DISTINCT m.id) AS messages_count
                FROM chats c
                LEFT JOIN chat_members cm ON c.id = cm.chat_id
                LEFT JOIN users u ON cm.user_id = u.id
                LEFT JOIN messages m ON c.id = m.chat_id
                LEFT JOIN LATERAL (
                    SELECT text, created_at, sender_id
                    FROM messages
                    WHERE chat_id = c.id
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                ) last_msg ON true
                LEFT JOIN users sender ON last_msg.sender_id = sender.id
                GROUP BY
                    c.id,
                    c.chat_name,
                    c.chat_type,
                    last_msg.text,
                    last_msg.created_at,
                    sender.display_name
                ORDER BY c.updated_at DESC, c.id DESC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                conversations.add(new ConversationResponse(
                        resultSet.getInt("chat_id"),
                        resultSet.getString("chat_name"),
                        resultSet.getString("chat_type"),
                        resultSet.getString("members"),
                        resultSet.getString("last_message_sender"),
                        resultSet.getString("last_message"),
                        resultSet.getString("last_message_time"),
                        resultSet.getInt("messages_count")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conversations;
    }

    public List<MessageHistoryResponse> getMessagesForChat(int chatId) {
        List<MessageHistoryResponse> messages = new ArrayList<>();

        String sql = """
                SELECT
                    CASE
                        WHEN m.text LIKE '[SYSTEM] %'
                        THEN 'System'
                        ELSE COALESCE(u.display_name, 'Unknown user')
                    END AS sender_name,
                    COALESCE(u.username, '') AS sender_username,
                    CASE
                        WHEN m.text LIKE '[SYSTEM] %'
                        THEN SUBSTRING(m.text FROM 10)
                        ELSE m.text
                    END AS message_text,
                    TO_CHAR(m.created_at + INTERVAL '3 hours', 'HH24:MI') AS created_at,
                    m.text LIKE '[SYSTEM] %' AS system_message
                FROM messages m
                LEFT JOIN users u ON m.sender_id = u.id
                WHERE m.chat_id = ?
                ORDER BY m.created_at ASC, m.id ASC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new MessageHistoryResponse(
                            resultSet.getString("sender_name"),
                            resultSet.getString("sender_username"),
                            resultSet.getString("message_text"),
                            resultSet.getString("created_at"),
                            resultSet.getBoolean("system_message")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }
}
