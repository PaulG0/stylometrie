package fr.unicaen.model.algo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO représentant le vecteur de caractéristiques stylistiques
 * extrait d'un texte.
 *
 * <p>Contient les métriques calculées par les algorithmes du package :
 * fréquences de mots-fonctions, distribution POS, longueur de phrases,
 * richesse lexicale (TTR), etc.</p>
 */
public class TextFeatures {

    /** Identifiant du texte en base SQLite (0 si inconnu). */
    private int textId;

    /** Titre du texte source. */
    private String title;

    /** Identifiant de l'auteur propriétaire du texte (0 si inconnu). */
    private int authorId;

    // ── Métriques brutes ────────────────────────────────────────────────────────

    /** Nombre total de tokens (mots + ponctuation). */
    private int totalTokens;

    /** Nombre total de mots (tokens alphabétiques uniquement). */
    private int totalWords;

    /** Nombre de phrases détectées. */
    private int totalSentences;

    /** Longueur moyenne des phrases en nombre de mots. */
    private double avgSentenceLength;

    /** Type-Token Ratio : diversité lexicale (0.0 – 1.0). */
    private double typeTokenRatio;

    /** Fréquences normalisées des 50 mots-fonctions les plus fréquents. */
    private Map<String, Double> functionWordFrequencies = new HashMap<>();

    /** Distribution des étiquettes POS (ex. NOM→0.32, VER→0.25 …). */
    private Map<String, Double> posDistribution = new HashMap<>();

    /** Longueur moyenne des mots (en caractères). */
    private double avgWordLength;

    /** Hapax Legomena Ratio : proportion de mots n'apparaissant qu'une fois. */
    private double hapaxRatio;

    // ── Constructeurs ────────────────────────────────────────────────────────────

    public TextFeatures() {}

    public TextFeatures(int textId, String title, int authorId) {
        this.textId   = textId;
        this.title    = title;
        this.authorId = authorId;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────────

    public int    getTextId()          { return textId; }
    public void   setTextId(int v)     { this.textId = v; }

    public String getTitle()           { return title; }
    public void   setTitle(String v)   { this.title = v; }

    public int    getAuthorId()        { return authorId; }
    public void   setAuthorId(int v)   { this.authorId = v; }

    public int    getTotalTokens()            { return totalTokens; }
    public void   setTotalTokens(int v)       { this.totalTokens = v; }

    public int    getTotalWords()             { return totalWords; }
    public void   setTotalWords(int v)        { this.totalWords = v; }

    public int    getTotalSentences()         { return totalSentences; }
    public void   setTotalSentences(int v)    { this.totalSentences = v; }

    public double getAvgSentenceLength()      { return avgSentenceLength; }
    public void   setAvgSentenceLength(double v) { this.avgSentenceLength = v; }

    public double getTypeTokenRatio()         { return typeTokenRatio; }
    public void   setTypeTokenRatio(double v) { this.typeTokenRatio = v; }

    public Map<String, Double> getFunctionWordFrequencies() {
        return Collections.unmodifiableMap(functionWordFrequencies);
    }
    public void setFunctionWordFrequencies(Map<String, Double> m) {
        this.functionWordFrequencies = m != null ? m : new HashMap<>();
    }

    public Map<String, Double> getPosDistribution() {
        return Collections.unmodifiableMap(posDistribution);
    }
    public void setPosDistribution(Map<String, Double> m) {
        this.posDistribution = m != null ? m : new HashMap<>();
    }

    public double getAvgWordLength()          { return avgWordLength; }
    public void   setAvgWordLength(double v)  { this.avgWordLength = v; }

    public double getHapaxRatio()             { return hapaxRatio; }
    public void   setHapaxRatio(double v)     { this.hapaxRatio = v; }

    /**
     * Exporte toutes les métriques dans une Map plate (nom → valeur),
     * utilisable directement comme vecteur numérique.
     */
    public Map<String, Double> toFlatVector() {
        Map<String, Double> v = new HashMap<>();
        v.put("total_tokens",        (double) totalTokens);
        v.put("total_words",         (double) totalWords);
        v.put("total_sentences",     (double) totalSentences);
        v.put("avg_sentence_length", avgSentenceLength);
        v.put("type_token_ratio",    typeTokenRatio);
        v.put("avg_word_length",     avgWordLength);
        v.put("hapax_ratio",         hapaxRatio);
        functionWordFrequencies.forEach((k, val) -> v.put("fw_" + k, val));
        posDistribution.forEach((k, val)          -> v.put("pos_" + k, val));
        return v;
    }

    @Override
    public String toString() {
        return "TextFeatures{textId=" + textId +
               ", title='" + title + '\'' +
               ", words=" + totalWords +
               ", sentences=" + totalSentences +
               ", TTR=" + String.format("%.3f", typeTokenRatio) + "}";
    }
}
