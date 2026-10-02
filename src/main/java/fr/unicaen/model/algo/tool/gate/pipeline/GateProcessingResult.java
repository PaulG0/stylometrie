package fr.unicaen.model.algo.tool.gate.pipeline;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Résultat structuré produit après exécution d'un pipeline GATE.
 */
public class GateProcessingResult {

    private final String originalText;
    private final List<String> tokens;
    private final List<String> sentences;
    private final Map<String, String> posTags;
    private final Map<String, Integer> annotationCounts;

    public GateProcessingResult(
            String originalText,
            List<String> tokens,
            List<String> sentences,
            Map<String, String> posTags,
            Map<String, Integer> annotationCounts
    ) {
        this.originalText = originalText != null ? originalText : "";
        this.tokens = tokens != null ? List.copyOf(tokens) : Collections.emptyList();
        this.sentences = sentences != null ? List.copyOf(sentences) : Collections.emptyList();
        this.posTags = posTags != null ? Collections.unmodifiableMap(new LinkedHashMap<>(posTags)) : Collections.emptyMap();
        this.annotationCounts = annotationCounts != null ? Collections.unmodifiableMap(new LinkedHashMap<>(annotationCounts)) : Collections.emptyMap();
    }

    public String getOriginalText() {
        return originalText;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public List<String> getSentences() {
        return sentences;
    }

    public Map<String, String> getPosTags() {
        return posTags;
    }

    public Map<String, Integer> getAnnotationCounts() {
        return annotationCounts;
    }

    public int getTokenCount() {
        return tokens.size();
    }

    public int getSentenceCount() {
        return sentences.size();
    }

    public int getWordCount() {
        return (int) tokens.stream()
                .filter(token -> token.matches("[\\p{L}\\-']+"))
                .count();
    }

    @Override
    public String toString() {
        return "GateProcessingResult{" +
                "tokens=" + tokens.size() +
                ", sentences=" + sentences.size() +
                ", posTags=" + posTags.size() +
                ", annotationCounts=" + annotationCounts +
                '}';
    }
}