package fr.unicaen.model.algo.tool.gate.composant;

import fr.unicaen.model.algo.FrequencyAnalyzer;
import fr.unicaen.model.algo.TextFeatures;
import fr.unicaen.service.GatePipelineService;
import gate.*;
import gate.creole.*;
import gate.util.GateException;
import gate.Factory;
import gate.ProcessingResource;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Processeur de texte basé sur GATE Embedded.
 *
 * <p>Fournit des méthodes de haut niveau pour :
 * <ul>
 *   <li>Tokeniser un texte brut en liste de tokens</li>
 *   <li>Extraire les annotations POS (Part-of-Speech)</li>
 *   <li>Calculer les {@link TextFeatures} complètes d'un texte</li>
 *   <li>Traiter un fichier texte directement depuis son chemin</li>
 * </ul>
 * </p>
 *
 * <p><strong>Dépendance :</strong> GATE doit être initialisé avant utilisation.
 * Appeler {@link #ensureGateInitialized()} ou utiliser le constructeur qui
 * accepte un {@link GatePipelineService} déjà initialisé.</p>
 */
public class GateTextProcessor {

    private final GatePipelineService gateService;
    private CorpusController pipeline;

    // ── Constructeurs ────────────────────────────────────────────────────────────

    /**
     * Constructeur par défaut : crée et initialise GATE automatiquement.
     */
    public GateTextProcessor() {
        this.gateService = new GatePipelineService();
        ensureGateInitialized();
    }

    /**
     * Constructeur avec un service GATE déjà initialisé (injection de dépendance).
     *
     * @param gateService service GATE déjà appelé avec {@code init()}
     */
    public GateTextProcessor(GatePipelineService gateService) {
        this.gateService = gateService;
        ensureGateInitialized();
    }

    // ── Initialisation GATE ──────────────────────────────────────────────────────

    private void ensureGateInitialized() {
        if (!gateService.isInitialized()) {
            gateService.init();
        }
    }

    // ── Méthodes publiques ───────────────────────────────────────────────────────

    /**
     * Tokenise un texte brut.
     *
     * <p>Retourne la liste des tokens dans l'ordre d'apparition.
     * Utilise le tokeniseur GATE (DefaultTokeniser) via un pipeline minimal,
     * avec fallback regex si GATE n'est pas disponible.</p>
     *
     * @param rawText texte brut à tokeniser
     * @return liste ordonnée de tokens (jamais null, peut être vide)
     */
    public List<String> tokenize(String rawText) {
        if (rawText == null || rawText.isBlank()) return Collections.emptyList();

        if (gateService.isInitialized()) {
            try {
                return tokenizeWithGate(rawText);
            } catch (Exception e) {
                System.err.println("[GateTextProcessor] Fallback regex (erreur GATE): " + e.getMessage());
            }
        }
        return tokenizeWithRegex(rawText);
    }

    /**
     * Tokenise le contenu d'un fichier texte.
     *
     * @param filePath chemin absolu du fichier
     * @return liste ordonnée de tokens
     * @throws IOException si le fichier n'existe pas ou n'est pas lisible
     */
    public List<String> tokenizeFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            throw new IOException("Fichier inaccessible : " + filePath);
        }
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        return tokenize(content);
    }

    /**
     * Extrait les tags POS d'un texte via GATE.
     *
     * <p>Retourne une Map token → tag POS (ex. "NOM", "VER", "ADJ"…).
     * Si GATE n'est pas disponible, retourne une Map vide.</p>
     *
     * @param rawText texte brut
     * @return Map token → étiquette POS
     */
    public Map<String, String> extractPosTags(String rawText) {
        if (!gateService.isInitialized() || rawText == null || rawText.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return extractPosTagsWithGate(rawText);
        } catch (Exception e) {
            System.err.println("[GateTextProcessor] Impossible d'extraire les POS : " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Calcule le vecteur complet de features stylistiques d'un texte.
     *
     * <p>Pipeline :
     * <ol>
     *   <li>Tokenisation (GATE ou regex)</li>
     *   <li>Extraction des mots seuls (filtrage des tokens non-alphabétiques)</li>
     *   <li>Détection des phrases (séparateur « . ? ! »)</li>
     *   <li>Calcul TTR, hapax, longueur de mots</li>
     *   <li>Fréquences des mots-fonctions français</li>
     *   <li>Distribution POS (si GATE disponible)</li>
     * </ol>
     * </p>
     *
     * @param rawText  texte brut
     * @param textId   identifiant du texte en base (0 si inconnu)
     * @param authorId identifiant de l'auteur (0 si inconnu)
     * @param title    titre du texte
     * @return features calculées
     */
    public TextFeatures computeFeatures(String rawText, int textId, int authorId, String title) {
        TextFeatures features = new TextFeatures(textId, title, authorId);
        if (rawText == null || rawText.isBlank()) return features;

        // ── 1. Tokenisation ──────────────────────────────────────────────────
        List<String> tokens = tokenize(rawText);
        features.setTotalTokens(tokens.size());

        // ── 2. Mots seuls (alphabétiques) ────────────────────────────────────
        List<String> words = tokens.stream()
                .filter(t -> t.matches("[\\p{L}\\-']+"))
                .map(String::toLowerCase)
                .toList();
        features.setTotalWords(words.size());

        // ── 3. Phrases ───────────────────────────────────────────────────────
        String[] sentences = rawText.split("[.!?]+");
        int sentCount = (int) Arrays.stream(sentences)
                .filter(s -> !s.isBlank())
                .count();
        features.setTotalSentences(Math.max(1, sentCount));
        features.setAvgSentenceLength(words.isEmpty() ? 0.0
                : (double) words.size() / features.getTotalSentences());

        // ── 4. Métriques lexicales ───────────────────────────────────────────
        features.setTypeTokenRatio(FrequencyAnalyzer.typeTokenRatio(words));
        features.setHapaxRatio(FrequencyAnalyzer.hapaxRatio(words));
        features.setAvgWordLength(FrequencyAnalyzer.avgWordLength(words));

        // ── 5. Mots-fonctions ────────────────────────────────────────────────
        features.setFunctionWordFrequencies(FrequencyAnalyzer.functionWordFrequencies(words));

        // ── 6. Distribution POS ──────────────────────────────────────────────
        if (gateService.isInitialized()) {
            try {
                Map<String, String> posMap = extractPosTagsWithGate(rawText);
                features.setPosDistribution(computePosDistribution(posMap));
            } catch (Exception e) {
                System.err.println("[GateTextProcessor] POS skipped: " + e.getMessage());
            }
        }

        return features;
    }

    /**
     * Calcule les features d'un texte directement depuis un chemin de fichier.
     */
    public TextFeatures computeFeaturesFromFile(String filePath, int textId, int authorId, String title)
            throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("Fichier introuvable : " + filePath);
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        return computeFeatures(content, textId, authorId, title);
    }

    // ── Méthodes privées GATE ────────────────────────────────────────────────────

    /**
     * Tokenisation interne via GATE DefaultTokeniser.
     * Crée un Document GATE temporaire, applique le tokeniseur et
     * retourne la liste des strings de chaque annotation Token.
     */
    private List<String> tokenizeWithGate(String rawText) throws GateException {
        Document doc = Factory.newDocument(rawText);
        try {
            // Pipeline minimaliste : tokeniseur seulement
            SerialAnalyserController tokenPipeline =
                    (SerialAnalyserController) Factory.createResource(
                            "gate.creole.SerialAnalyserController");

            ProcessingResource tokeniser = (ProcessingResource) Factory.createResource(
                    "gate.creole.tokeniser.DefaultTokeniser");

            tokenPipeline.add(tokeniser);
            tokenPipeline.setCorpus(Factory.newCorpus("tmp"));
            tokenPipeline.getCorpus().add(doc);
            tokenPipeline.execute();

            List<String> tokens = new ArrayList<>();
            AnnotationSet annotations = doc.getAnnotations().get("Token");
            List<Annotation> sorted = new ArrayList<>(annotations);
            sorted.sort(Comparator.comparingLong(a -> a.getStartNode().getOffset()));

            for (Annotation ann : sorted) {
                String tokenStr = gate.Utils.stringFor(doc, ann);
                if (tokenStr != null && !tokenStr.isBlank()) {
                    tokens.add(tokenStr);
                }
            }
            return tokens;
        } finally {
            Factory.deleteResource(doc);
        }
    }

    /**
     * Extraction POS interne via GATE (tokeniseur + POSTagger).
     */
    private Map<String, String> extractPosTagsWithGate(String rawText) throws GateException {
        Document doc = Factory.newDocument(rawText);
        Map<String, String> result = new LinkedHashMap<>();
        try {
            SerialAnalyserController pipeline =
                    (SerialAnalyserController) Factory.createResource(
                            "gate.creole.SerialAnalyserController");

            pipeline.add((ProcessingResource) Factory.createResource(
                    "gate.creole.tokeniser.DefaultTokeniser"));
            pipeline.add((ProcessingResource) Factory.createResource(
                    "gate.creole.gazetteer.DefaultGazetteer"));
            pipeline.add((ProcessingResource) Factory.createResource(
                    "gate.creole.splitter.SentenceSplitter"));
            pipeline.add((ProcessingResource) Factory.createResource(
                    "gate.creole.POSTagger"));

            Corpus corpus = Factory.newCorpus("tmp");
            corpus.add(doc);
            pipeline.setCorpus(corpus);
            pipeline.execute();

            AnnotationSet tokens = doc.getAnnotations().get("Token");
            for (Annotation ann : tokens) {
                String word = gate.Utils.stringFor(doc, ann);
                FeatureMap fm = ann.getFeatures();
                String pos = fm.get("category") != null
                        ? fm.get("category").toString()
                        : "UNK";
                result.put(word, pos);
            }
        } finally {
            Factory.deleteResource(doc);
        }
        return result;
    }

    /**
     * Calcule la distribution normalisée des étiquettes POS.
     */
    private Map<String, Double> computePosDistribution(Map<String, String> posMap) {
        if (posMap == null || posMap.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> counts = new HashMap<>();
        posMap.values().forEach(pos -> counts.merge(pos, 1, Integer::sum));
        int total = posMap.size();
        Map<String, Double> dist = new LinkedHashMap<>();
        counts.forEach((pos, count) -> dist.put(pos, (double) count / total));
        return dist;
    }

    // ── Fallback regex ───────────────────────────────────────────────────────────

    /**
     * Tokenisation de secours sans GATE : split sur les espaces et ponctuation.
     */
    private List<String> tokenizeWithRegex(String rawText) {
        String[] parts = rawText.split("\\s+|(?<=[\\p{Punct}])|(?=[\\p{Punct}])");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) tokens.add(p.trim());
        }
        return tokens;
    }
}
