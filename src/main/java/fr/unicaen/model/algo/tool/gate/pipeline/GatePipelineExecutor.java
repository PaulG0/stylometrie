package fr.unicaen.model.algo.tool.gate.pipeline;

import fr.unicaen.service.GatePipelineService;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exécuteur de pipelines GATE.
 */
public class GatePipelineExecutor {

    private final GatePipelineService gateService;

    public GatePipelineExecutor() {
        this.gateService = new GatePipelineService();
        ensureGateInitialized();
    }

    public GatePipelineExecutor(GatePipelineService gateService) {
        this.gateService = gateService;
        ensureGateInitialized();
    }

    private void ensureGateInitialized() {
        if (!gateService.isInitialized()) {
            gateService.init();
        }
    }

    public GateProcessingResult execute(String rawText, SerialAnalyserController pipeline) {
        if (rawText == null || rawText.isBlank()) {
            return emptyResult(rawText);
        }

        Document document = null;
        Corpus corpus = null;

        try {
            document = Factory.newDocument(rawText);
            corpus = Factory.newCorpus("stylometrie-temp-corpus");

            corpus.add(document);
            pipeline.setCorpus(corpus);
            pipeline.execute();

            return new GateProcessingResult(
                    rawText,
                    extractTokens(document),
                    extractSentences(document),
                    extractPosTags(document),
                    countAnnotations(document)
            );

        } catch (GateException e) {
            throw new GatePipelineException("Erreur pendant l'exécution du pipeline GATE.", e);
        } finally {
            if (document != null) {
                Factory.deleteResource(document);
            }
            if (corpus != null) {
                Factory.deleteResource(corpus);
            }
            if (pipeline != null) {
                Factory.deleteResource(pipeline);
            }
        }
    }

    private List<String> extractTokens(Document document) {
        AnnotationSet annotations = document.getAnnotations().get("Token");
        List<Annotation> sortedAnnotations = sortAnnotations(annotations);

        List<String> tokens = new ArrayList<>();

        for (Annotation annotation : sortedAnnotations) {
            String token = gate.Utils.stringFor(document, annotation);

            if (token != null && !token.isBlank()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private List<String> extractSentences(Document document) {
        AnnotationSet annotations = document.getAnnotations().get("Sentence");
        List<Annotation> sortedAnnotations = sortAnnotations(annotations);

        List<String> sentences = new ArrayList<>();

        for (Annotation annotation : sortedAnnotations) {
            String sentence = gate.Utils.stringFor(document, annotation);

            if (sentence != null && !sentence.isBlank()) {
                sentences.add(sentence.trim());
            }
        }

        return sentences;
    }

    private Map<String, String> extractPosTags(Document document) {
        AnnotationSet annotations = document.getAnnotations().get("Token");
        List<Annotation> sortedAnnotations = sortAnnotations(annotations);

        Map<String, String> posTags = new LinkedHashMap<>();

        for (Annotation annotation : sortedAnnotations) {
            String token = gate.Utils.stringFor(document, annotation);
            FeatureMap features = annotation.getFeatures();
            Object category = features.get("category");

            if (token != null && !token.isBlank() && category != null) {
                posTags.put(token, category.toString());
            }
        }

        return posTags;
    }

    private Map<String, Integer> countAnnotations(Document document) {
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String annotationType : document.getAnnotations().getAllTypes()) {
            counts.put(annotationType, document.getAnnotations().get(annotationType).size());
        }

        return counts;
    }

    private List<Annotation> sortAnnotations(AnnotationSet annotations) {
        List<Annotation> sortedAnnotations = new ArrayList<>(annotations);

        sortedAnnotations.sort(Comparator.comparingLong(
                annotation -> annotation.getStartNode().getOffset()
        ));

        return sortedAnnotations;
    }

    private GateProcessingResult emptyResult(String rawText) {
        return new GateProcessingResult(
                rawText,
                List.of(),
                List.of(),
                Map.of(),
                Map.of()
        );
    }
}