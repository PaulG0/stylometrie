package fr.unicaen.model.algo.tool;

import fr.unicaen.model.algo.FrequencyAnalyzer;
import fr.unicaen.model.algo.TextFeatures;
import fr.unicaen.model.algo.tool.gate.pipeline.GateAnalysisPipeline;
import fr.unicaen.model.algo.tool.gate.pipeline.GateProcessingResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Façade haut niveau pour extraire des caractéristiques stylistiques
 * depuis un texte en utilisant les pipelines GATE.
 */
public class GateTextProcessor {

    private final GateAnalysisPipeline gateAnalysisPipeline;

    public GateTextProcessor() {
        this.gateAnalysisPipeline = new GateAnalysisPipeline();
    }

    public List<String> tokenize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        return gateAnalysisPipeline.analyzeWithTokeniser(rawText).getTokens();
    }

    public List<String> tokenizeFile(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.canRead()) {
            throw new IOException("Fichier inaccessible : " + filePath);
        }

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return tokenize(content);
    }

    public Map<String, String> extractPosTags(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyMap();
        }

        return gateAnalysisPipeline.analyzeWithFullPipeline(rawText).getPosTags();
    }

    public TextFeatures computeFeatures(String rawText, int textId, int authorId, String title) {
        TextFeatures features = new TextFeatures(textId, title, authorId);

        if (rawText == null || rawText.isBlank()) {
            return features;
        }

        GateProcessingResult result = gateAnalysisPipeline.analyzeWithFullPipeline(rawText);

        List<String> tokens = result.getTokens();
        List<String> words = tokens.stream()
                .filter(token -> token.matches("[\\p{L}\\-']+"))
                .map(String::toLowerCase)
                .toList();

        features.setTotalTokens(tokens.size());
        features.setTotalWords(words.size());
        features.setTotalSentences(Math.max(1, result.getSentenceCount()));
        features.setAvgSentenceLength(words.isEmpty() ? 0.0 : (double) words.size() / features.getTotalSentences());
        features.setTypeTokenRatio(FrequencyAnalyzer.typeTokenRatio(words));
        features.setHapaxRatio(FrequencyAnalyzer.hapaxRatio(words));
        features.setAvgWordLength(FrequencyAnalyzer.avgWordLength(words));
        features.setFunctionWordFrequencies(FrequencyAnalyzer.functionWordFrequencies(words));
        features.setPosDistribution(computePosDistribution(result.getPosTags()));

        return features;
    }

    public TextFeatures computeFeaturesFromFile(String filePath, int textId, int authorId, String title)
            throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.canRead()) {
            throw new IOException("Fichier introuvable ou illisible : " + filePath);
        }

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return computeFeatures(content, textId, authorId, title);
    }

    private Map<String, Double> computePosDistribution(Map<String, String> posMap) {
        if (posMap == null || posMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> counts = new HashMap<>();
        posMap.values().forEach(pos -> counts.merge(pos, 1, Integer::sum));

        int total = posMap.size();
        Map<String, Double> distribution = new HashMap<>();
        counts.forEach((pos, count) -> distribution.put(pos, (double) count / total));

        return distribution;
    }
}
