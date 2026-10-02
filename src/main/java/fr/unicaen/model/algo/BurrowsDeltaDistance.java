package fr.unicaen.model.algo;

import java.util.*;

/**
 * Implémentation du Delta de Burrows (Burrows' Delta).
 *
 * <p>C'est l'algorithme de référence en stylométrie computationnelle, publié
 * par John Burrows en 2002. Il mesure la distance entre le style d'un texte
 * candidat et le style moyen d'un auteur, normalisée par l'écart-type du
 * corpus complet.</p>
 *
 * <h3>Formule :</h3>
 * <pre>
 *   Δ(C, A) = (1/n) × Σ | (z_C_i − z_A_i) |
 *
 *   avec z_i = (valeur_i − μ_i) / σ_i   (score Z normalisé sur le corpus)
 * </pre>
 *
 * <p>Plus le delta est petit, plus le style du texte candidat est proche de
 * l'auteur. Un delta inférieur à 1.0 est généralement considéré comme une
 * attribution fiable.</p>
 */
public class BurrowsDeltaDistance {

    /** Nombre de mots-fonctions les plus fréquents à utiliser (MFW). */
    private static final int DEFAULT_MFW = 150;

    /** Constructeur privé — classe utilitaire statique. */
    private BurrowsDeltaDistance() {}

    /**
     * Calcule le Delta de Burrows entre un texte candidat et un auteur.
     *
     * @param candidate     features du texte à attribuer
     * @param authorProfile vecteur moyen de l'auteur
     * @param corpusMeans   moyenne de chaque feature sur tout le corpus
     * @param corpusStdDevs écart-type de chaque feature sur tout le corpus
     * @return valeur du Delta (≥ 0, plus petit = plus proche)
     */
    public static double compute(
            TextFeatures        candidate,
            StyleVector         authorProfile,
            Map<String, Double> corpusMeans,
            Map<String, Double> corpusStdDevs
    ) {
        if (candidate == null || authorProfile == null ||
            corpusMeans == null || corpusStdDevs == null) return Double.MAX_VALUE;

        Map<String, Double> candVec   = candidate.toFlatVector();
        Map<String, Double> authVec   = authorProfile.getFeatures();

        // On travaille uniquement sur les features présentes dans les stats corpus
        Set<String> features = new HashSet<>(corpusMeans.keySet());
        features.retainAll(corpusStdDevs.keySet());

        if (features.isEmpty()) return Double.MAX_VALUE;

        double delta = 0.0;
        int    n     = 0;

        for (String feat : features) {
            double stdDev = corpusStdDevs.getOrDefault(feat, 0.0);
            if (stdDev == 0.0) continue;   // feature constante → ignorée

            double mean   = corpusMeans.getOrDefault(feat, 0.0);
            double zCand  = (candVec.getOrDefault(feat,   0.0) - mean) / stdDev;
            double zAuth  = (authVec.getOrDefault(feat,   0.0) - mean) / stdDev;

            delta += Math.abs(zCand - zAuth);
            n++;
        }

        return n == 0 ? Double.MAX_VALUE : delta / n;
    }

    /**
     * Calcule les moyennes et écarts-types de toutes les features
     * à partir d'un corpus de vecteurs.
     *
     * @param corpus liste de vecteurs plats issus de TextFeatures.toFlatVector()
     * @return tableau [means, stdDevs] de deux Maps<String, Double>
     */
    public static Map<String, Double>[] computeCorpusStats(List<Map<String, Double>> corpus) {
        if (corpus == null || corpus.isEmpty()) {
            //noinspection unchecked
            return new Map[]{Collections.emptyMap(), Collections.emptyMap()};
        }

        // Somme des valeurs par feature
        Map<String, Double>  sumMap   = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        for (Map<String, Double> vec : corpus) {
            vec.forEach((k, v) -> {
                sumMap.merge(k, v, Double::sum);
                countMap.merge(k, 1, Integer::sum);
            });
        }

        // Moyennes
        Map<String, Double> means = new HashMap<>();
        sumMap.forEach((k, s) -> means.put(k, s / countMap.get(k)));

        // Somme des carrés des écarts pour les écarts-types
        Map<String, Double> sqDiffSum = new HashMap<>();
        for (Map<String, Double> vec : corpus) {
            vec.forEach((k, v) -> {
                double diff = v - means.getOrDefault(k, 0.0);
                sqDiffSum.merge(k, diff * diff, Double::sum);
            });
        }

        // Écarts-types (population)
        Map<String, Double> stdDevs = new HashMap<>();
        sqDiffSum.forEach((k, sq) -> stdDevs.put(k, Math.sqrt(sq / countMap.get(k))));

        //noinspection unchecked
        return new Map[]{means, stdDevs};
    }
}
