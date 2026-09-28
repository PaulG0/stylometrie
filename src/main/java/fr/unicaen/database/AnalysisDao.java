package fr.unicaen.database;

import fr.unicaen.model.Author;
import fr.unicaen.model.Text;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalysisDao {

    public boolean insertOrUpdateAuthor(String name, String wikidataUri, String birthDate, String movement) {
        String checkSql = "SELECT id FROM authors WHERE LOWER(name) = LOWER(?)";
        String insertSql = "INSERT INTO authors(name, wikidata_uri, birth_date, movement) VALUES(?, ?, ?, ?)";
        String updateSql = "UPDATE authors SET wikidata_uri = ?, birth_date = ?, movement = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, name.trim());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int existingId = rs.getInt("id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, wikidataUri);
                            updateStmt.setString(2, birthDate);
                            updateStmt.setString(3, movement);
                            updateStmt.setInt(4, existingId);
                            updateStmt.executeUpdate();
                            return true;
                        }
                    } else {
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, name.trim());
                            insertStmt.setString(2, wikidataUri);
                            insertStmt.setString(3, birthDate);
                            insertStmt.setString(4, movement);
                            insertStmt.executeUpdate();
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur UPSERT auteur : " + e.getMessage());
        }
        return false;
    }

    public List<Author> getAllAuthors() {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT a.id, a.name, a.wikidata_uri, a.birth_date, a.movement, COUNT(t.id) AS works_count " +
                "FROM authors a " +
                "LEFT JOIN texts t ON a.id = t.author_id " +
                "GROUP BY a.id, a.name, a.wikidata_uri, a.birth_date, a.movement " +
                "ORDER BY a.name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                authors.add(new Author(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("wikidata_uri"),
                        rs.getString("birth_date"),
                        rs.getString("movement"),
                        rs.getInt("works_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération auteurs : " + e.getMessage());
        }
        return authors;
    }

    public Author getAuthorByName(String name) {
        String sql = "SELECT a.id, a.name, a.wikidata_uri, a.birth_date, a.movement, COUNT(t.id) AS works_count " +
                "FROM authors a " +
                "LEFT JOIN texts t ON a.id = t.author_id " +
                "WHERE LOWER(a.name) = LOWER(?) " +
                "GROUP BY a.id, a.name, a.wikidata_uri, a.birth_date, a.movement";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Author(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("wikidata_uri"),
                            rs.getString("birth_date"),
                            rs.getString("movement"),
                            rs.getInt("works_count")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche auteur : " + e.getMessage());
        }
        return null;
    }

    public Author getAuthorById(int id) {
        String sql = "SELECT a.id, a.name, a.wikidata_uri, a.birth_date, a.movement, COUNT(t.id) AS works_count " +
                "FROM authors a " +
                "LEFT JOIN texts t ON a.id = t.author_id " +
                "WHERE a.id = ? " +
                "GROUP BY a.id, a.name, a.wikidata_uri, a.birth_date, a.movement";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Author(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("wikidata_uri"),
                            rs.getString("birth_date"),
                            rs.getString("movement"),
                            rs.getInt("works_count")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche auteur par ID : " + e.getMessage());
        }
        return null;
    }

    public boolean updateAuthorMetadata(int authorId, String birthDate, String movement) {
        String sql = "UPDATE authors SET birth_date = ?, movement = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, birthDate);
            pstmt.setString(2, movement);
            pstmt.setInt(3, authorId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour métadonnées auteur : " + e.getMessage());
        }
        return false;
    }

    public int insertAuthor(Author author) {
        String sql = "INSERT INTO authors(name, wikidata_uri, birth_date, movement) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, author.getName());
            pstmt.setString(2, author.getWikidataUri());
            pstmt.setString(3, author.getBirthDate());
            pstmt.setString(4, author.getMovement());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur insertion auteur : " + e.getMessage());
        }
        return -1;
    }

    public List<Text> getTextsByAuthorId(int authorId) {
        List<Text> texts = new ArrayList<>();
        String sql = "SELECT id, author_id, title, filepath FROM texts WHERE author_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, authorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    texts.add(new Text(
                            rs.getInt("id"),
                            rs.getInt("author_id"),
                            rs.getString("title"),
                            rs.getString("filepath")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération textes : " + e.getMessage());
        }
        return texts;
    }

    public int insertText(Text text) {
        String sql = "INSERT INTO texts(author_id, title, filepath) VALUES(?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, text.getAuthorId());
            pstmt.setString(2, text.getTitle());
            pstmt.setString(3, text.getFilePath());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur insertion texte : " + e.getMessage());
        }
        return -1;
    }

    public int getTotalTextsCount() {
        String sql = "SELECT COUNT(*) FROM texts";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur comptage : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Récupère tous les textes enregistrés pour un auteur spécifique.
     */
    public List<Text> getTextsByAuthor(int authorId) {
        List<Text> texts = new ArrayList<>();
        String sql = "SELECT id, author_id, title, filepath FROM texts WHERE author_id = ? ORDER BY title ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, authorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    texts.add(new Text(
                            rs.getInt("id"),
                            rs.getInt("author_id"),
                            rs.getString("title"),
                            rs.getString("filepath")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération textes de l'auteur : " + e.getMessage());
        }
        return texts;
    }

    /**
     * Récupère l'auteur associé à un texte donné.
     */
    public Author getAuthorByTextId(int textId) {
        String sql = "SELECT a.id, a.name, a.wikidata_uri, a.birth_date, a.movement " +
                "FROM authors a INNER JOIN texts t ON a.id = t.author_id WHERE t.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, textId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Author(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("wikidata_uri"),
                            rs.getString("birth_date"),
                            rs.getString("movement")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche auteur de l'œuvre : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère une liste paginée des textes avec le nom de l'auteur associé via une jointure SQL.
     */
    public List<Text> getPaginatedTexts(int limit, int offset, String sortBy, String order) {
        List<Text> texts = new ArrayList<>();
        String validSortBy = "title".equals(sortBy) ? "t.title" : ("author".equals(sortBy) ? "a.name" : "t.id");
        String validOrder = "DESC".equalsIgnoreCase(order) ? "DESC" : "ASC";

        String sql = "SELECT t.id, t.author_id, t.title, t.filepath, a.name AS author_name " +
                "FROM texts t " +
                "LEFT JOIN authors a ON t.author_id = a.id " +
                "ORDER BY " + validSortBy + " " + validOrder + " " +
                "LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Text text = new Text(
                            rs.getInt("id"),
                            rs.getInt("author_id"),
                            rs.getString("title"),
                            rs.getString("filepath")
                    );
                    text.setAuthorName(rs.getString("author_name"));
                    texts.add(text);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur pagination textes avec auteurs : " + e.getMessage());
        }
        return texts;
    }
}