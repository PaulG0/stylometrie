package fr.unicaen.model.algo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vecteur de style moyen d'un auteur, calculé à partir de l'ensemble
 * de ses {@link TextFeatures}.
 *
 * <p>Utilisé par les algorithmes de distance (cosinus, delta de Burrows)
 * pour comparer le style d'un texte candidat à la signature moyenne
 * de chaque auteur du corpus.</p>
 */
public class StyleVector {

    /** Identifiant de l'auteur en base SQLite. */
    private final int authorId;

    /** Nom de l'auteur (pour l'affichage). */
    private final String authorName;

    /** Vecteur de style moyen (nom_feature → valeur_moyenne). */
    private final Map<String, Double> features;

    /** Nombre de textes utilisés pour calculer cette moyenne. */
    private final int textCount;

    // ── Constructeur ─────────────────────────────────────────────────────────────

    public StyleVector(int authorId, String authorName, Map<String, Double> features, int textCount) {
        this.authorId   = authorId;
        this.authorName = authorName;
        this.features   = features != null ? Collections.unmodifiableMap(new HashMap<>(features)) : Collections.emptyMap();
        this.textCount  = textCount;
    }

    // ── Méthode de construction depuis une liste de TextFeatures ─────────────────

    /**
     * Construit un {@code StyleVector} en moyennant les vecteurs plats
     * d'une liste de {@link TextFeatures}.
     *
     * @param authorId   identifiant auteur
     * @param authorName nom auteur
     * @param textList   liste non vide de features
     * @return vecteur de style moyenné, ou un vecteur vide si la liste est nulle/vide
     */
    public static StyleVector fromTextFeatures(int authorId, String authorName, List<TextFeatures> textList) {
        if (textList == null || textList.isEmpty()) {
            return new StyleVector(authorId, authorName, Collections.emptyMap(), 0);
        }

        Map<String, Double> sum   = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();

        for (TextFeatures tf : textList) {
            tf.toFlatVector().forEach((key, val) -> {
                sum.merge(key, val, Double::sum);
                count.merge(key, 1, Integer::sum);
            });
        }

        Map<String, Double> avg = new HashMap<>();
        sum.forEach((key, total) -> avg.put(key, total / count.get(key)));

        return new StyleVector(authorId, authorName, avg, textList.size());
    }

    // ── Getters ──────────────────────────────────────────────────────────────────

    public int               getAuthorId()   { return authorId; }
    public String            getAuthorName() { return authorName; }
    public Map<String, Double> getFeatures() { return features; }
    public int               getTextCount()  { return textCount; }

    /**
     * Valeur d'une feature précise (0.0 si absente).
     */
    public double get(String featureName) {
        return features.getOrDefault(featureName, 0.0);
    }

    @Override
    public String toString() {
        return "StyleVector{author='" + authorName + "', texts=" + textCount +
               ", features=" + features.size() + "}";
    }
}
