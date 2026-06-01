package org.example.messengerserver.repository;

import org.example.messengerserver.dto.UserResponse;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepository {

    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =================== Lookup ===================

    public UserResponse findByUsername(String username) {
        String sql = "SELECT id, username, display_name FROM users WHERE username = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new UserResponse(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("display_name")
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getPasswordHash(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =================== Mutations ===================

    public void createUser(String username, String displayName, String passwordHash) {
        String sql = "INSERT INTO users (username, display_name, password_hash) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, displayName);
            statement.setString(3, passwordHash);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean updateDisplayName(int userId, String newDisplayName) {
        String sql = "UPDATE users SET display_name = ? WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newDisplayName);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isHelperInitialized(int userId) {
        String sql = "SELECT helper_initialized FROM users WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("helper_initialized");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void markHelperInitialized(int userId) {
        String sql = "UPDATE users SET helper_initialized = TRUE WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateLastSeen(int userId) {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getLastSeen(int userId) {
        String sql = "SELECT last_seen FROM users WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Timestamp lastSeen = rs.getTimestamp("last_seen");
                    return lastSeen == null ? null : lastSeen.toInstant().toEpochMilli();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<UserResponse> getUsersNotInChat(int chatId, int currentUserId) {
        List<UserResponse> users = new ArrayList<>();

        String sql = """
                SELECT DISTINCT u.id,
                                u.username,
                                u.display_name
                FROM users u
                JOIN chat_members my_private_membership
                     ON my_private_membership.user_id = ?
                JOIN chats private_chat
                     ON private_chat.id = my_private_membership.chat_id
                    AND private_chat.chat_type = 'PRIVATE'
                JOIN chat_members companion_membership
                     ON companion_membership.chat_id = private_chat.id
                    AND companion_membership.user_id = u.id
                    AND companion_membership.user_id <> ?
                WHERE u.username <> 'helper_bot'
                  AND u.id NOT IN (
                      SELECT user_id
                      FROM chat_members
                      WHERE chat_id = ?
                  )
                ORDER BY u.display_name
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, currentUserId);
            statement.setInt(2, currentUserId);
            statement.setInt(3, chatId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(new UserResponse(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("display_name")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
}
