package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.Chat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    public Chat createChat(String name, String type, int userId) {
        String insertChatSql = """
                INSERT INTO chats (chat_name, chat_type)
                VALUES (?, ?)
                RETURNING id, chat_name, chat_type
                """;

        String insertMemberSql = """
                INSERT INTO chat_members (chat_id, user_id)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement chatStatement = connection.prepareStatement(insertChatSql)) {
                chatStatement.setString(1, name);
                chatStatement.setString(2, type);

                ResultSet resultSet = chatStatement.executeQuery();

                if (resultSet.next()) {
                    int chatId = resultSet.getInt("id");

                    try (PreparedStatement memberStatement = connection.prepareStatement(insertMemberSql)) {
                        memberStatement.setInt(1, chatId);
                        memberStatement.setInt(2, userId);
                        memberStatement.executeUpdate();
                    }

                    connection.commit();

                    return new Chat(
                            chatId,
                            resultSet.getString("chat_name"),
                            resultSet.getString("chat_type")
                    );
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Chat> getChatsForUser(int userId) {
        List<Chat> chats = new ArrayList<>();

        String sql = """
    SELECT c.id,
           COALESCE(cm.custom_chat_name, c.chat_name) AS chat_name,
           c.chat_type,
           (
               SELECT m.text
               FROM messages m
               WHERE m.chat_id = c.id
               ORDER BY m.created_at DESC, m.id DESC
               LIMIT 1
           ) AS last_message_text,
           (
               SELECT TO_CHAR(m.created_at, 'HH24:MI')
               FROM messages m
               WHERE m.chat_id = c.id
               ORDER BY m.created_at DESC, m.id DESC
               LIMIT 1
           ) AS last_message_time,
           (
               SELECT cm2.user_id
               FROM chat_members cm2
               WHERE cm2.chat_id = c.id
                 AND cm2.user_id <> ?
               LIMIT 1
           ) AS companion_user_id
    FROM chats c
    JOIN chat_members cm ON c.id = cm.chat_id
    WHERE cm.user_id = ?
    ORDER BY c.updated_at DESC, c.id DESC
    """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                chats.add(new Chat(
                        resultSet.getInt("id"),
                        resultSet.getString("chat_name"),
                        resultSet.getString("chat_type"),
                        resultSet.getString("last_message_text"),
                        resultSet.getString("last_message_time"),
                        (Integer) resultSet.getObject("companion_user_id")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return chats;
    }

    public void renameChat(int chatId, int userId, String newName) {
        String sql = """
            UPDATE chat_members
            SET custom_chat_name = ?
            WHERE chat_id = ? AND user_id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newName);
            statement.setInt(2, chatId);
            statement.setInt(3, userId);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteChat(int chatId) {
        String sql = "DELETE FROM chats WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isBotChat(int chatId) {
        String sql = """
                SELECT COUNT(*)
                FROM chats
                WHERE id = ? AND chat_type = 'BOT'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Chat createPrivateChat(
            String chatName,
            int currentUserId,
            int targetUserId,
            String currentUserChatName,
            String targetUserChatName
    ) {
        String insertChatSql = """
            INSERT INTO chats (chat_name, chat_type)
            VALUES (?, 'PRIVATE')
            RETURNING id, chat_name, chat_type
            """;

        String insertMemberSql = """
            INSERT INTO chat_members (chat_id, user_id, custom_chat_name)
            VALUES (?, ?, ?)
            """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement chatStatement = connection.prepareStatement(insertChatSql)) {
                chatStatement.setString(1, chatName);

                ResultSet resultSet = chatStatement.executeQuery();

                if (resultSet.next()) {
                    int chatId = resultSet.getInt("id");

                    try (PreparedStatement memberStatement = connection.prepareStatement(insertMemberSql)) {
                        memberStatement.setInt(1, chatId);
                        memberStatement.setInt(2, currentUserId);
                        memberStatement.setString(3, currentUserChatName);
                        memberStatement.executeUpdate();

                        memberStatement.setInt(1, chatId);
                        memberStatement.setInt(2, targetUserId);
                        memberStatement.setString(3, targetUserChatName);
                        memberStatement.executeUpdate();
                    }

                    connection.commit();

                    return new Chat(
                            chatId,
                            currentUserChatName,
                            resultSet.getString("chat_type"),
                            null,
                            null,
                            targetUserId
                    );
                }

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateChatActivity(int chatId) {
        String sql = """
            UPDATE chats
            SET updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}