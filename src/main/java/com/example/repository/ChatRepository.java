package com.example.repository;

import com.example.database.DatabaseConnection;
import com.example.model.Chat;
import com.example.model.User;

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
                       CASE
                           WHEN c.chat_type = 'PRIVATE'
                           THEN COALESCE(cm.custom_chat_name, companion.display_name)
                           ELSE c.chat_name
                       END AS chat_name,
                       c.chat_type,
                       CASE
                           WHEN c.chat_type = 'PRIVATE'
                           THEN companion.id
                           ELSE NULL
                       END AS companion_user_id,
                       cm.custom_chat_name IS NOT NULL AS has_custom_name,
                       CASE
                           WHEN last_msg.last_message_text LIKE '[SYSTEM] %'
                           THEN SUBSTRING(last_msg.last_message_text FROM 10)
                           ELSE last_msg.last_message_text
                       END AS last_message_text,
                       TO_CHAR(last_msg.created_at, 'HH24:MI') AS last_message_time
                FROM chats c
                JOIN chat_members cm ON c.id = cm.chat_id

                LEFT JOIN LATERAL (
                    SELECT u.id,
                           u.display_name
                    FROM chat_members companion_member
                    JOIN users u ON u.id = companion_member.user_id
                    WHERE companion_member.chat_id = c.id
                      AND companion_member.user_id <> cm.user_id
                      AND c.chat_type = 'PRIVATE'
                    LIMIT 1
                ) companion ON true

                LEFT JOIN LATERAL (
                    SELECT m.text AS last_message_text,
                           m.created_at
                    FROM messages m
                    WHERE m.chat_id = c.id
                    ORDER BY m.created_at DESC, m.id DESC
                    LIMIT 1
                ) last_msg ON true

                WHERE cm.user_id = ?
                ORDER BY c.updated_at DESC, c.id DESC
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                chats.add(new Chat(
                        resultSet.getInt("id"),
                        resultSet.getString("chat_name"),
                        resultSet.getString("chat_type"),
                        resultSet.getString("last_message_text"),
                        resultSet.getString("last_message_time"),
                        (Integer) resultSet.getObject("companion_user_id"),
                        0,
                        resultSet.getBoolean("has_custom_name")
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

    public void renameGroupChat(int chatId, String newName) {
        String sql = """
            UPDATE chats
            SET chat_name = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND chat_type = 'GROUP'
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

    public void resetChatName(int chatId, int userId) {
        String sql = """
            UPDATE chat_members
            SET custom_chat_name = NULL
            WHERE chat_id = ? AND user_id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, userId);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteChatForUser(int chatId, int userId) {
        String sql = """
            DELETE FROM chat_members
            WHERE chat_id = ?
              AND user_id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, userId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isGroupChat(int chatId) {
        String sql = """
                SELECT chat_type
                FROM chats
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return "GROUP".equalsIgnoreCase(
                        resultSet.getString("chat_type")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void leaveGroup(int chatId, int userId) {
        String sql = """
            DELETE FROM chat_members
            WHERE chat_id = ?
                  AND user_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, userId);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMemberFromGroup(int chatId, int userId) {
        leaveGroup(chatId, userId);
    }

    public void ensureChatMember(int chatId, int userId) {
        String sql = """
            INSERT INTO chat_members (chat_id, user_id)
            VALUES (?, ?)
            ON CONFLICT (chat_id, user_id) DO NOTHING
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, userId);
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
            int targetUserId
    ) {
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
                        memberStatement.setInt(2, currentUserId);
                        memberStatement.executeUpdate();

                        memberStatement.setInt(1, chatId);
                        memberStatement.setInt(2, targetUserId);
                        memberStatement.executeUpdate();
                    }

                    connection.commit();

                    return new Chat(
                            chatId,
                            chatName,
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

    public Chat createGroupChat(String groupName, int creatorId, List<Integer> memberIds) {
        String insertChatSql = """
            INSERT INTO chats (chat_name, chat_type)
            VALUES (?, 'GROUP')
            RETURNING id, chat_name, chat_type
            """;

        String insertMemberSql = """
            INSERT INTO chat_members (chat_id, user_id, member_role)
            VALUES (?, ?, ?)
            ON CONFLICT (chat_id, user_id) DO NOTHING
            """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement chatStatement = connection.prepareStatement(insertChatSql)) {
                chatStatement.setString(1, groupName);

                ResultSet resultSet = chatStatement.executeQuery();

                if (resultSet.next()) {
                    int chatId = resultSet.getInt("id");

                    try (PreparedStatement memberStatement = connection.prepareStatement(insertMemberSql)) {
                        memberStatement.setInt(1, chatId);
                        memberStatement.setInt(2, creatorId);
                        memberStatement.setString(3, "OWNER");
                        memberStatement.executeUpdate();

                        for (Integer memberId : memberIds) {
                            if (memberId == null) {
                                continue;
                            }

                            memberStatement.setInt(1, chatId);
                            memberStatement.setInt(2, memberId);
                            memberStatement.setString(3, "MEMBER");
                            memberStatement.executeUpdate();
                        }
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

    public List<Integer> getChatMemberIdsExcept(int chatId, int excludedUserId) {
        List<Integer> memberIds = new ArrayList<>();

        String sql = """
            SELECT user_id
            FROM chat_members
            WHERE chat_id = ?
              AND user_id <> ?
            ORDER BY user_id ASC
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, excludedUserId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                memberIds.add(resultSet.getInt("user_id"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return memberIds;
    }

    public List<User> getGroupMembers(int chatId) {
        List<User> members = new ArrayList<>();

        String sql = """
            SELECT u.id,
                   u.username,
                   u.display_name,
                   cm.member_role
            FROM users u
            JOIN chat_members cm ON u.id = cm.user_id
            WHERE cm.chat_id = ?
            ORDER BY
                CASE cm.member_role
                    WHEN 'OWNER' THEN 1
                    WHEN 'ADMIN' THEN 2
                    ELSE 3
                END,
                u.display_name
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                members.add(new User(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("display_name"),
                        resultSet.getString("member_role")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return members;
    }

    public void addMemberToChat(int chatId, int userId) {
        String sql = """
            INSERT INTO chat_members (chat_id, user_id, member_role)
            VALUES (?, ?, 'MEMBER')
            ON CONFLICT (chat_id, user_id) DO NOTHING
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, userId);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getMemberRole(int chatId, int userId) {
        String sql = """
            SELECT member_role
            FROM chat_members
            WHERE chat_id = ?
              AND user_id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, chatId);
            statement.setInt(2, userId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("member_role");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateMemberRole(int chatId, int userId, String role) {
        String sql = """
            UPDATE chat_members
            SET member_role = ?
            WHERE chat_id = ?
              AND user_id = ?
              AND member_role <> 'OWNER'
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, role);
            statement.setInt(2, chatId);
            statement.setInt(3, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean transferOwnership(int chatId, int currentOwnerId, int newOwnerId) {
        String demoteCurrentOwnerSql = """
            UPDATE chat_members
            SET member_role = 'ADMIN'
            WHERE chat_id = ?
              AND user_id = ?
              AND member_role = 'OWNER'
            """;

        String promoteNewOwnerSql = """
            UPDATE chat_members
            SET member_role = 'OWNER'
            WHERE chat_id = ?
              AND user_id = ?
              AND member_role <> 'OWNER'
            """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement demoteStatement = connection.prepareStatement(demoteCurrentOwnerSql);
                 PreparedStatement promoteStatement = connection.prepareStatement(promoteNewOwnerSql)) {

                demoteStatement.setInt(1, chatId);
                demoteStatement.setInt(2, currentOwnerId);

                int demotedRows = demoteStatement.executeUpdate();

                promoteStatement.setInt(1, chatId);
                promoteStatement.setInt(2, newOwnerId);

                int promotedRows = promoteStatement.executeUpdate();

                if (demotedRows == 0 || promotedRows == 0) {
                    connection.rollback();
                    return false;
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
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
