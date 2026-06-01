package org.example.messengerserver.repository;

import org.example.messengerserver.dto.ChatResponse;
import org.example.messengerserver.dto.UserResponse;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ChatRepository {

    private final DataSource dataSource;

    public ChatRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =================== Helpers ===================

    @FunctionalInterface
    private interface Setter {
        void set(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    private interface Mapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private void execute(String sql, Setter setter) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean executeAndCheck(String sql, Setter setter) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private <T> T queryOne(String sql, Setter setter, Mapper<T> mapper) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper.map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> List<T> queryList(String sql, Setter setter, Mapper<T> mapper) {
        List<T> results = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // =================== Members (used by WebSocket handler) ===================

    public List<Integer> getChatMemberIds(int chatId) {
        return queryList(
                "SELECT user_id FROM chat_members WHERE chat_id = ?",
                ps -> ps.setInt(1, chatId),
                rs -> rs.getInt("user_id")
        );
    }

    public void updateChatActivity(int chatId) {
        execute("UPDATE chats SET updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                ps -> ps.setInt(1, chatId));
    }

    // =================== Chat list ===================

    public List<ChatResponse> getChatsForUser(int userId) {
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
                       TO_CHAR(last_msg.created_at, 'HH24:MI') AS last_message_time,
                       unread.unread_count
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

                LEFT JOIN LATERAL (
                    SELECT COUNT(*) AS unread_count
                    FROM messages m
                    WHERE m.chat_id = c.id
                      AND m.sender_id <> cm.user_id
                      AND m.is_read = FALSE
                      AND c.chat_type <> 'BOT'
                ) unread ON true

                WHERE cm.user_id = ?
                  AND cm.hidden = FALSE
                ORDER BY c.updated_at DESC, c.id DESC
                """;

        return queryList(
                sql,
                ps -> ps.setInt(1, userId),
                rs -> new ChatResponse(
                        rs.getInt("id"),
                        rs.getString("chat_name"),
                        rs.getString("chat_type"),
                        rs.getString("last_message_text"),
                        rs.getString("last_message_time"),
                        (Integer) rs.getObject("companion_user_id"),
                        rs.getBoolean("has_custom_name"),
                        rs.getInt("unread_count")
                )
        );
    }

    // =================== Create ===================

    public ChatResponse createChat(String name, String type, int userId) {
        String insertChatSql = """
                INSERT INTO chats (chat_name, chat_type)
                VALUES (?, ?)
                RETURNING id, chat_name, chat_type
                """;

        String insertMemberSql = "INSERT INTO chat_members (chat_id, user_id) VALUES (?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement chatStatement = connection.prepareStatement(insertChatSql)) {
                chatStatement.setString(1, name);
                chatStatement.setString(2, type);

                try (ResultSet resultSet = chatStatement.executeQuery()) {
                    if (resultSet.next()) {
                        int chatId = resultSet.getInt("id");

                        try (PreparedStatement memberStatement = connection.prepareStatement(insertMemberSql)) {
                            memberStatement.setInt(1, chatId);
                            memberStatement.setInt(2, userId);
                            memberStatement.executeUpdate();
                        }

                        connection.commit();

                        return new ChatResponse(
                                chatId,
                                resultSet.getString("chat_name"),
                                resultSet.getString("chat_type")
                        );
                    }
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

    public ChatResponse createPrivateChat(String chatName, int currentUserId, int targetUserId) {
        Integer existingChatId = findPrivateChatId(currentUserId, targetUserId);
        if (existingChatId != null) {
            addMember(existingChatId, currentUserId);
            return new ChatResponse(
                    existingChatId, chatName, "PRIVATE", null, null, targetUserId, false, 0
            );
        }

        String insertChatSql = """
                INSERT INTO chats (chat_name, chat_type)
                VALUES (?, 'PRIVATE')
                RETURNING id, chat_name, chat_type
                """;

        String insertMemberSql = "INSERT INTO chat_members (chat_id, user_id) VALUES (?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement chatStatement = connection.prepareStatement(insertChatSql)) {
                chatStatement.setString(1, chatName);

                try (ResultSet resultSet = chatStatement.executeQuery()) {
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

                        return new ChatResponse(
                                chatId,
                                chatName,
                                resultSet.getString("chat_type"),
                                null,
                                null,
                                targetUserId,
                                false,
                                0
                        );
                    }
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

    public ChatResponse createGroupChat(String groupName, int creatorId, List<Integer> memberIds) {
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

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement chatStatement = connection.prepareStatement(insertChatSql)) {
                chatStatement.setString(1, groupName);

                try (ResultSet resultSet = chatStatement.executeQuery()) {
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

                        return new ChatResponse(
                                chatId,
                                resultSet.getString("chat_name"),
                                resultSet.getString("chat_type")
                        );
                    }
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

    // =================== Membership ===================

    public void addMember(int chatId, int userId) {
        execute("INSERT INTO chat_members (chat_id, user_id, member_role) VALUES (?, ?, 'MEMBER') "
                        + "ON CONFLICT (chat_id, user_id) DO UPDATE SET hidden = FALSE",
                ps -> { ps.setInt(1, chatId); ps.setInt(2, userId); });
    }

    public void hideChatForUser(int chatId, int userId) {
        execute("UPDATE chat_members SET hidden = TRUE WHERE chat_id = ? AND user_id = ?",
                ps -> { ps.setInt(1, chatId); ps.setInt(2, userId); });
    }

    public Integer findPrivateChatId(int userA, int userB) {
        List<Integer> ids = queryList(
                "SELECT cm1.chat_id FROM chat_members cm1 "
                        + "JOIN chat_members cm2 ON cm1.chat_id = cm2.chat_id "
                        + "JOIN chats c ON c.id = cm1.chat_id "
                        + "WHERE c.chat_type = 'PRIVATE' AND cm1.user_id = ? AND cm2.user_id = ? LIMIT 1",
                ps -> { ps.setInt(1, userA); ps.setInt(2, userB); },
                rs -> rs.getInt("chat_id")
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    public void removeMember(int chatId, int userId) {
        execute("DELETE FROM chat_members WHERE chat_id = ? AND user_id = ?",
                ps -> { ps.setInt(1, chatId); ps.setInt(2, userId); });
    }

    public void deleteChat(int chatId) {
        execute("DELETE FROM chats WHERE id = ?",
                ps -> ps.setInt(1, chatId));
    }

    public List<Integer> getChatMemberIdsExcept(int chatId, int excludedUserId) {
        return queryList(
                "SELECT user_id FROM chat_members WHERE chat_id = ? AND user_id <> ? ORDER BY user_id ASC",
                ps -> { ps.setInt(1, chatId); ps.setInt(2, excludedUserId); },
                rs -> rs.getInt("user_id")
        );
    }

    public List<UserResponse> getGroupMembers(int chatId) {
        return queryList(
                """
                SELECT u.id, u.username, u.display_name, cm.member_role
                FROM users u
                JOIN chat_members cm ON u.id = cm.user_id
                WHERE cm.chat_id = ?
                ORDER BY CASE cm.member_role WHEN 'OWNER' THEN 1 WHEN 'ADMIN' THEN 2 ELSE 3 END, u.display_name
                """,
                ps -> ps.setInt(1, chatId),
                rs -> new UserResponse(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("member_role")
                )
        );
    }

    // =================== Rename ===================

    public void renameCustomName(int chatId, int userId, String newName) {
        execute("UPDATE chat_members SET custom_chat_name = ? WHERE chat_id = ? AND user_id = ?",
                ps -> { ps.setString(1, newName); ps.setInt(2, chatId); ps.setInt(3, userId); });
    }

    public void renameGroupChat(int chatId, String newName) {
        execute("UPDATE chats SET chat_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND chat_type = 'GROUP'",
                ps -> { ps.setString(1, newName); ps.setInt(2, chatId); });
    }

    public void resetCustomName(int chatId, int userId) {
        execute("UPDATE chat_members SET custom_chat_name = NULL WHERE chat_id = ? AND user_id = ?",
                ps -> { ps.setInt(1, chatId); ps.setInt(2, userId); });
    }

    // =================== Roles ===================

    public String getMemberRole(int chatId, int userId) {
        return queryOne(
                "SELECT member_role FROM chat_members WHERE chat_id = ? AND user_id = ?",
                ps -> { ps.setInt(1, chatId); ps.setInt(2, userId); },
                rs -> rs.getString("member_role")
        );
    }

    public boolean updateMemberRole(int chatId, int userId, String role) {
        return executeAndCheck(
                "UPDATE chat_members SET member_role = ? WHERE chat_id = ? AND user_id = ? AND member_role <> 'OWNER'",
                ps -> { ps.setString(1, role); ps.setInt(2, chatId); ps.setInt(3, userId); }
        );
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

        try (Connection connection = dataSource.getConnection()) {
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
}
