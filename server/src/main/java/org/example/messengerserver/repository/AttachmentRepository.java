package org.example.messengerserver.repository;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class AttachmentRepository {

    // =================== Queries ===================

    private final DataSource dataSource;

    public AttachmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record Attachment(String filename, String contentType, byte[] data) {
    }

    public int save(String filename, String contentType, byte[] data) {
        String sql = "INSERT INTO attachments (filename, content_type, data) VALUES (?, ?, ?) RETURNING id";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, filename);
            statement.setString(2, contentType);
            statement.setBytes(3, data);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public Attachment get(int id) {
        String sql = "SELECT filename, content_type, data FROM attachments WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Attachment(
                            rs.getString("filename"),
                            rs.getString("content_type"),
                            rs.getBytes("data")
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
