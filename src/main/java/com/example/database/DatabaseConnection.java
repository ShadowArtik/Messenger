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
        String sql = """
        CREATE TABLE IF NOT EXISTS messages (
            id SERIAL PRIMARY KEY,
            contact_name VARCHAR(100) NOT NULL,
            sender VARCHAR(50) NOT NULL,
            text TEXT NOT NULL,
            message_time VARCHAR(10) NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            System.out.println("Table messages is ready");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}