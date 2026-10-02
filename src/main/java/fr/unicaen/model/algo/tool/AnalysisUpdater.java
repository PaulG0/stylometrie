package fr.unicaen.model.algo.tool;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.AnalysisResult;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import fr.unicaen.model.algo.TextFeatures;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Met à jour les analyses stylistiques des textes liés à un auteur en base SQLite.
 *
 * <p>Ce service coordonne le calcul des features via {@link GateTextProcessor}
 * et leur persistance dans la table {@code analysis_results} de SQLite.</p>
 *
 * <h3>Table attendue :</h3>
 * <pre>
 * CREATE TABLE IF NOT EXISTS analysis_results (
 *     id           INTEGER PRIMARY KEY AUTOINCREMENT,
 *     text_id      INTEGER NOT NULL,
 *     metric_name  TEXT    NOT NULL,
 *     metric_value REAL    NOT NULL,
 *     computed_at  TEXT    DEFAULT (datetime('now')),
 *     FOREIGN KEY (text_id) REFERENCES texts(id) ON DELETE CASCADE,
 *     UNIQUE(text_id, metric_name)
 * );
 * </pre>
 *
 * <p>Si la table n'existe pas, {@link #ensureTableExists(Connection)} la crée
 * automatiquement.</p>
 */
public class AnalysisUpdater {

    private final AnalysisDao     analysisDao;
    private final GateTextProcessor processor;

    // ── Constructeurs ────────────────────────────────────────────────────────────

    /**
     * Constructeur par défaut avec initialisation GATE automatique.
     */
    public AnalysisUpdater() {
        this.analysisDao = new AnalysisDao();
        this.processor   = new GateTextProcessor();
    }

    /**
     * Constructeur avec injection des dépendances (pour les tests).
     */
    public AnalysisUpdater(AnalysisDao analysisDao, GateTextProcessor processor) {
        this.analysisDao = analysisDao;
        this.processor   = processor;
    }

    // ── API publique ─────────────────────────────────────────────────────────────

    /**
     * Calcule et persiste les features stylistiques de tous les textes d'un auteur.
     *
     * @param authorId identifiant de l'auteur en base
     * @return nombre de textes mis à jour avec succès
     */
    public int updateAllTextsForAuthor(int authorId) {
        List<Text> texts = analysisDao.getTextsByAuthor(authorId);
        if (texts == null || texts.isEmpty()) return 0;

        int updatedCount = 0;
        for (Text text : texts) {
            boolean ok = updateSingleText(text);
            if (ok) updatedCount++;
        }
        System.out.println("[AnalysisUpdater] " + updatedCount + "/" + texts.size()
                + " textes mis à jour pour l'auteur #" + authorId);
        return updatedCount;
    }

    /**
     * Calcule et persiste les features stylistiques d'un texte unique.
     *
     * @param textId identifiant du texte en base
     * @return true si la mise à jour a réussi
     */
    public boolean updateSingleTextById(int textId) {
        // Récupérer le texte via la DAO
        List<Text> all = analysisDao.getTextsByAuthor(0); // fallback
        // Cherche dans tous les auteurs — méthode directe via SQL
        Text target = getTextById(textId);
        if (target == null) {
            System.err.println("[AnalysisUpdater] Texte #" + textId + " introuvable.");
            return false;
        }
        return updateSingleText(target);
    }

    /**
     * Récupère les résultats d'analyse existants pour un texte.
     *
     * @param textId identifiant du texte
     * @return liste des {@link AnalysisResult} stockés
     */
    public List<AnalysisResult> getResultsForText(int textId) {
        List<AnalysisResult> results = new ArrayList<>();
        String sql = "SELECT id, text_id, metric_name, metric_value FROM analysis_results WHERE text_id = ?";

        try (Connection conn = fr.unicaen.database.DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ensureTableExists(conn);
            ps.setInt(1, textId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new AnalysisResult(
                            rs.getInt("id"),
                            rs.getInt("text_id"),
                            rs.getString("metric_name"),
                            rs.getDouble("metric_value")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisUpdater] Erreur lecture résultats : " + e.getMessage());
        }
        return results;
    }

    /**
     * Supprime toutes les analyses pour un texte donné.
     *
     * @param textId identifiant du texte
     * @return nombre de lignes supprimées
     */
    public int clearResultsForText(int textId) {
        String sql = "DELETE FROM analysis_results WHERE text_id = ?";
        try (Connection conn = fr.unicaen.database.DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ensureTableExists(conn);
            ps.setInt(1, textId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AnalysisUpdater] Erreur suppression : " + e.getMessage());
            return 0;
        }
    }

    // ── Méthodes privées ─────────────────────────────────────────────────────────

    /**
     * Traite un texte complet : calcul des features puis persistance.
     */
    private boolean updateSingleText(Text text) {
        if (text.getFilePath() == null || text.getFilePath().isBlank()) {
            System.err.println("[AnalysisUpdater] Chemin vide pour le texte #" + text.getId());
            return false;
        }

        try {
            // Calcul des features via GATE
            TextFeatures features = processor.computeFeaturesFromFile(
                    text.getFilePath(),
                    text.getId(),
                    text.getAuthorId(),
                    text.getTitle()
            );

            // Persistance en SQLite
            return persistFeatures(features);

        } catch (Exception e) {
            System.err.println("[AnalysisUpdater] Erreur traitement texte #"
                    + text.getId() + " : " + e.getMessage());
            return false;
        }
    }

    /**
     * Persiste un vecteur de features dans la table {@code analysis_results}.
     * Utilise INSERT OR REPLACE pour mettre à jour les valeurs existantes.
     */
    private boolean persistFeatures(TextFeatures features) {
        String sql = "INSERT OR REPLACE INTO analysis_results " +
                     "(text_id, metric_name, metric_value, computed_at) " +
                     "VALUES (?, ?, ?, datetime('now'))";

        Map<String, Double> flatVector = features.toFlatVector();
        if (flatVector.isEmpty()) return false;

        try (Connection conn = fr.unicaen.database.DatabaseManager.getConnection()) {
            ensureTableExists(conn);
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, Double> entry : flatVector.entrySet()) {
                    ps.setInt(1,    features.getTextId());
                    ps.setString(2, entry.getKey());
                    ps.setDouble(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                System.out.println("[AnalysisUpdater] ✅ Texte #" + features.getTextId()
                        + " → " + flatVector.size() + " métriques persistées.");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisUpdater] Erreur persistance features : " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère un texte par son ID directement en SQL (méthode interne).
     */
    private Text getTextById(int textId) {
        String sql = "SELECT id, author_id, title, filepath FROM texts WHERE id = ?";
        try (Connection conn = fr.unicaen.database.DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, textId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Text(
                            rs.getInt("id"),
                            rs.getInt("author_id"),
                            rs.getString("title"),
                            rs.getString("filepath")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisUpdater] Erreur getTextById : " + e.getMessage());
        }
        return null;
    }

    /**
     * Crée la table {@code analysis_results} si elle n'existe pas encore.
     */
    private void ensureTableExists(Connection conn) throws SQLException {
        String ddl = """
                CREATE TABLE IF NOT EXISTS analysis_results (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    text_id      INTEGER NOT NULL,
                    metric_name  TEXT    NOT NULL,
                    metric_value REAL    NOT NULL,
                    computed_at  TEXT    DEFAULT (datetime('now')),
                    FOREIGN KEY (text_id) REFERENCES texts(id) ON DELETE CASCADE,
                    UNIQUE(text_id, metric_name)
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }
    }
}
