package fr.unicaen.model.algo;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Calcul de la similarité cosinus entre deux vecteurs de style.
 *
 * <p>La similarité cosinus mesure l'angle entre deux vecteurs dans un
 * espace à N dimensions. Une valeur de 1.0 indique des styles identiques,
 * une valeur de 0.0 des styles complètement orthogonaux.</p>
 *
 * <p>Formule :
 * <pre>
 *   cos(A, B) = Σ(A_i × B_i) / (||A|| × ||B||)
 * </pre></p>
 */
public class CosineSimilarity {

    /** Constructeur privé — classe utilitaire statique. */
    private CosineSimilarity() {}

    /**
     * Calcule la similarité cosinus entre deux vecteurs de features.
     *
     * @param a premier vecteur (Map feature → valeur)
     * @param b second vecteur
     * @return valeur entre 0.0 et 1.0 (1.0 = identique)
     */
    public static double compute(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;

        // Union des clés présentes dans les deux vecteurs
        Set<String> keys = new HashSet<>(a.keySet());
        keys.addAll(b.keySet());

        double dotProduct = 0.0;
        double normA      = 0.0;
        double normB      = 0.0;

        for (String key : keys) {
            double valA = a.getOrDefault(key, 0.0);
            double valB = b.getOrDefault(key, 0.0);
            dotProduct += valA * valB;
            normA      += valA * valA;
            normB      += valB * valB;
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dotProduct / denom;
    }

    /**
     * Calcule la similarité cosinus entre deux {@link StyleVector}.
     */
    public static double compute(StyleVector a, StyleVector b) {
        return compute(a.getFeatures(), b.getFeatures());
    }

    /**
     * Calcule la similarité cosinus entre un {@link TextFeatures} (texte candidat)
     * et un {@link StyleVector} (profil de l'auteur).
     */
    public static double compute(TextFeatures candidate, StyleVector authorProfile) {
        return compute(candidate.toFlatVector(), authorProfile.getFeatures());
    }

    /**
     * Convertit la similarité cosinus en distance (0 = identique, 1 = opposé).
     */
    public static double toDistance(double similarity) {
        return 1.0 - similarity;
    }
}
