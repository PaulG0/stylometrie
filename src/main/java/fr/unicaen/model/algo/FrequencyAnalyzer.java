package fr.unicaen.model.algo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyse les fréquences de tokens/mots dans un texte.
 *
 * <p>Utilisé par {@link fr.unicaen.model.algo.tool.GateTextProcessor} pour
 * construire la distribution de mots-fonctions d'un texte, composante
 * centrale des algorithmes de stylométrie.</p>
 */
public class FrequencyAnalyzer {

    /**
     * Liste des mots-fonctions français les plus discriminants
     * pour la stylométrie (déterminants, prépositions, conjonctions, pronoms…).
     */
    private static final Set<String> FRENCH_FUNCTION_WORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "du", "de",
            "et", "en", "ou", "ni", "mais", "donc", "or", "car",
            "que", "qui", "quoi", "dont", "où", "quand", "comme", "si",
            "je", "tu", "il", "elle", "nous", "vous", "ils", "elles",
            "me", "te", "se", "lui", "leur", "y", "en",
            "ce", "cet", "cette", "ces", "mon", "ma", "mes",
            "ton", "ta", "tes", "son", "sa", "ses", "notre", "votre", "leur", "leurs",
            "à", "au", "aux", "par", "pour", "sur", "sous", "dans", "avec", "sans",
            "pas", "plus", "très", "bien", "tout", "même", "encore", "toujours"
    );

    /** Constructeur privé — classe utilitaire statique. */
    private FrequencyAnalyzer() {}

    /**
     * Compte les occurrences de chaque token dans une liste.
     *
     * @param tokens liste de tokens (en minuscules de préférence)
     * @return Map token → nombre d'occurrences
     */
    public static Map<String, Integer> countTokens(List<String> tokens) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                freq.merge(token.toLowerCase(), 1, Integer::sum);
            }
        }
        return freq;
    }

    /**
     * Normalise une Map de fréquences brutes en fréquences relatives
     * (nombre_occurrences / total_tokens).
     *
     * @param rawFreq   fréquences brutes
     * @param totalSize taille totale du corpus (nombre de tokens)
     * @return Map token → fréquence normalisée [0.0, 1.0]
     */
    public static Map<String, Double> normalize(Map<String, Integer> rawFreq, int totalSize) {
        if (totalSize == 0) return Collections.emptyMap();
        Map<String, Double> norm = new LinkedHashMap<>();
        rawFreq.forEach((k, v) -> norm.put(k, (double) v / totalSize));
        return norm;
    }

    /**
     * Extrait la distribution normalisée des {@code n} mots les plus fréquents.
     *
     * @param tokens liste de tokens
     * @param n      nombre de mots à conserver (Most Frequent Words)
     * @return Map (trié par fréquence décroissante) des n MFW normalisés
     */
    public static Map<String, Double> topNFrequencies(List<String> tokens, int n) {
        Map<String, Integer> raw = countTokens(tokens);
        int total = tokens.size();

        return raw.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(n)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (double) e.getValue() / total,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Extrait uniquement les mots-fonctions français et retourne
     * leurs fréquences normalisées.
     *
     * @param tokens liste de tokens (mots en minuscules)
     * @return Map mot_fonction → fréquence relative
     */
    public static Map<String, Double> functionWordFrequencies(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return Collections.emptyMap();
        int total = tokens.size();

        Map<String, Double> result = new LinkedHashMap<>();
        for (String fw : FRENCH_FUNCTION_WORDS) {
            long count = tokens.stream()
                    .filter(t -> fw.equalsIgnoreCase(t))
                    .count();
            result.put(fw, (double) count / total);
        }
        return result;
    }

    /**
     * Calcule le Type-Token Ratio (TTR) : richesse lexicale.
     *
     * <p>TTR = types_distincts / total_tokens. Plus proche de 1.0 → vocabulaire
     * riche et varié.</p>
     *
     * @param tokens liste de tokens
     * @return TTR entre 0.0 et 1.0
     */
    public static double typeTokenRatio(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return 0.0;
        long distinct = tokens.stream()
                .map(String::toLowerCase)
                .distinct()
                .count();
        return (double) distinct / tokens.size();
    }

    /**
     * Calcule le ratio d'hapax legomena (mots n'apparaissant qu'une seule fois).
     *
     * @param tokens liste de tokens
     * @return proportion d'hapax [0.0, 1.0]
     */
    public static double hapaxRatio(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return 0.0;
        Map<String, Integer> freq = countTokens(tokens);
        long hapaxCount = freq.values().stream().filter(c -> c == 1).count();
        return (double) hapaxCount / freq.size();
    }

    /**
     * Calcule la longueur moyenne des mots en caractères.
     *
     * @param wordTokens tokens alphabétiques uniquement (pas la ponctuation)
     * @return longueur moyenne
     */
    public static double avgWordLength(List<String> wordTokens) {
        if (wordTokens == null || wordTokens.isEmpty()) return 0.0;
        return wordTokens.stream()
                .mapToInt(String::length)
                .average()
                .orElse(0.0);
    }

    /**
     * @return l'ensemble des mots-fonctions français utilisés dans ce module.
     */
    public static Set<String> getFrenchFunctionWords() {
        return FRENCH_FUNCTION_WORDS;
    }
}
