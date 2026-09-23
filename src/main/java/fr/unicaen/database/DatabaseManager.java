package fr.unicaen.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:stylometrie.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        String createAuthorsTable = """
            CREATE TABLE IF NOT EXISTS authors (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            );
        """;

        String createTextsTable = """
            CREATE TABLE IF NOT EXISTS texts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                author_id INTEGER,
                title TEXT NOT NULL,
                FOREIGN KEY(author_id) REFERENCES authors(id)
            );
        """;

        String createMetricsTable = """
            CREATE TABLE IF NOT EXISTS metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text_id INTEGER,
                metric_name TEXT NOT NULL,
                metric_value REAL NOT NULL,
                FOREIGN KEY(text_id) REFERENCES texts(id)
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createAuthorsTable);
            stmt.execute(createTextsTable);
            stmt.execute(createMetricsTable);
            System.out.println("[Database] Base SQLite initialisée.");
        } catch (SQLException e) {
            System.err.println("[Database] Erreur d'initialisation : " + e.getMessage());
        }
    }
}