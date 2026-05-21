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
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """;

        String chatsSql = """
        CREATE TABLE IF NOT EXISTS chats (
            id SERIAL PRIMARY KEY,
            chat_name VARCHAR(100) NOT NULL,
            chat_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """;

        String chatMembersSql = """
        CREATE TABLE IF NOT EXISTS chat_members (
            chat_id INTEGER NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(usersSql);
            statement.execute(chatsSql);
            statement.execute(chatMembersSql);
            statement.execute(messagesSql);

            System.out.println("Table users is ready");
            System.out.println("Table chats is ready");
            System.out.println("Table chat_members is ready");
            System.out.println("Table messages is ready");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
