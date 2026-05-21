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
            SELECT
                c.id,
                c.chat_type,
                CASE
                    WHEN c.chat_type = 'PRIVATE' THEN other_user.display_name
                    ELSE c.chat_name
                END AS display_name
            FROM chats c
            JOIN chat_members cm_current
                ON c.id = cm_current.chat_id
            LEFT JOIN chat_members cm_other
                ON c.id = cm_other.chat_id
                AND cm_other.user_id <> ?
            LEFT JOIN users other_user
                ON cm_other.user_id = other_user.id
            WHERE cm_current.user_id = ?
            ORDER BY c.id ASC
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                chats.add(new Chat(
                        resultSet.getInt("id"),
                        resultSet.getString("display_name"),
                        resultSet.getString("chat_type")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return chats;
    }

    public void renameChat(int chatId, String newName) {
        String sql = """
                UPDATE chats
                SET chat_name = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newName);
            statement.setInt(2, chatId);
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

        public Chat createPrivateChat(String chatName, int firstUserId, int secondUserId) {
            String insertChatSql = """
            INSERT INTO chats (chat_name, chat_type)
            VALUES (?, 'PRIVATE')
            RETURNING id, chat_name, chat_type
            """;

            String insertMemberSql = """
            INSERT INTO chat_members (chat_id, user_id)
            VALUES (?, ?)
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
                            memberStatement.setInt(2, firstUserId);
                            memberStatement.executeUpdate();

                            memberStatement.setInt(1, chatId);
                            memberStatement.setInt(2, secondUserId);
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
}