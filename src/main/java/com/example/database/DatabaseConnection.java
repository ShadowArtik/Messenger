package com.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimeZone;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:postgresql://localhost:5433/messenger";

    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initDatabase() {

        String usersSql = """
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            username VARCHAR(100) NOT NULL UNIQUE,
            display_name VARCHAR(100) NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            helper_initialized BOOLEAN NOT NULL DEFAULT FALSE,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """;

        String chatsSql = """
        CREATE TABLE IF NOT EXISTS chats (
            id SERIAL PRIMARY KEY,
            chat_name VARCHAR(100) NOT NULL,
            chat_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """;

        String chatMembersSql = """
        CREATE TABLE IF NOT EXISTS chat_members (
            chat_id INTEGER NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            custom_chat_name VARCHAR(100),
            PRIMARY KEY (chat_id, user_id)
        );
        """;

        String messagesSql = """
        CREATE TABLE IF NOT EXISTS messages (
            id SERIAL PRIMARY KEY,
            chat_id INTEGER NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
            sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            text TEXT NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """;

        String addMemberRoleSql = """
        ALTER TABLE chat_members
        ADD COLUMN IF NOT EXISTS member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';
        """;

        String ensureGroupOwnerSql = """
        UPDATE chat_members cm
        SET member_role = 'OWNER'
        FROM (
            SELECT cm2.chat_id,
                   MIN(cm2.user_id) AS owner_id
            FROM chat_members cm2
            JOIN chats c ON c.id = cm2.chat_id
            WHERE c.chat_type = 'GROUP'
              AND NOT EXISTS (
                  SELECT 1
                  FROM chat_members owner_check
                  WHERE owner_check.chat_id = cm2.chat_id
                    AND owner_check.member_role = 'OWNER'
              )
            GROUP BY cm2.chat_id
        ) owners
        WHERE cm.chat_id = owners.chat_id
          AND cm.user_id = owners.owner_id;
        """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(usersSql);
            statement.execute(chatsSql);
            statement.execute(chatMembersSql);
            statement.execute(messagesSql);
            statement.execute(addMemberRoleSql);
            statement.execute(ensureGroupOwnerSql);

            System.out.println("Table users is ready");
            System.out.println("Table chats is ready");
            System.out.println("Table chat_members is ready");
            System.out.println("Table messages is ready");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
