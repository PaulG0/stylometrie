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

    /**
     * Initialise la base de données SQLite et crée les tables si elles n'existent pas.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Table des auteurs (avec métadonnées enrichies)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS authors (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    wikidata_uri TEXT UNIQUE,
                    birth_date TEXT,
                    movement TEXT
                );
            """);

            // 2. Table des textes / ouvrages de référence (colonne filepath ajoutée)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS texts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    author_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    filepath TEXT,
                    FOREIGN KEY(author_id) REFERENCES authors(id) ON DELETE CASCADE
                );
            """);

            // Altérations au cas où la base de données existe déjà avec un ancien schéma
            try { stmt.executeUpdate("ALTER TABLE authors ADD COLUMN wikidata_uri TEXT UNIQUE;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE authors ADD COLUMN birth_date TEXT;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE authors ADD COLUMN movement TEXT;"); } catch (SQLException ignored) {}

            // Migration pour ajouter la colonne filepath si elle manque dans la table texts existante
            try { stmt.executeUpdate("ALTER TABLE texts ADD COLUMN filepath TEXT;"); } catch (SQLException ignored) {}

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation de la base SQLite : " + e.getMessage());
        }
    }
}