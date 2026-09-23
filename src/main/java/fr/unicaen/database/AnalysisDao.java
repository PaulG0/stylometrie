package fr.unicaen.database;

import fr.unicaen.model.Author;
import fr.unicaen.model.Text;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalysisDao {

    public void addAuthor(String name) throws SQLException {
        String sql = "INSERT INTO authors(name) VALUES(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public List<Author> getAllAuthors() throws SQLException {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT id, name FROM authors ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                authors.add(new Author(rs.getInt("id"), rs.getString("name")));
            }
        }
        return authors;
    }

    public void addText(int authorId, String title) throws SQLException {
        String sql = "INSERT INTO texts(author_id, title) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, authorId);
            pstmt.setString(2, title);
            pstmt.executeUpdate();
        }
    }

    public List<Text> getAllTexts() throws SQLException {
        List<Text> texts = new ArrayList<>();
        String sql = """
            SELECT t.id, t.author_id, t.title, a.name AS author_name 
            FROM texts t 
            JOIN authors a ON t.author_id = a.id 
            ORDER BY t.id DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                texts.add(new Text(
                        rs.getInt("id"),
                        rs.getInt("author_id"),
                        rs.getString("title"),
                        rs.getString("author_name")
                ));
            }
        }
        return texts;
    }
}