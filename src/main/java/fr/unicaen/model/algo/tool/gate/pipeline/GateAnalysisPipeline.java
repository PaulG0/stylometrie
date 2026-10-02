package fr.unicaen.model.algo.tool.gate.pipeline;

import fr.unicaen.model.algo.tool.gate.composant.GateComponentType;
import fr.unicaen.service.GatePipelineService;
import gate.creole.SerialAnalyserController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Façade simple pour lancer des analyses GATE.
 *
 * <p>Cette classe cache la création du pipeline et l'exécution GATE.</p>
 */
public class GateAnalysisPipeline {

    private final GatePipelineExecutor executor;

    public GateAnalysisPipeline() {
        this.executor = new GatePipelineExecutor();
    }

    public GateAnalysisPipeline(GatePipelineService gateService) {
        this.executor = new GatePipelineExecutor(gateService);
    }

    public GateProcessingResult analyzeWithFullPipeline(String rawText) {
        SerialAnalyserController pipeline = new GatePipelineBuilder()
                .tokeniser()
                .gazetteer()
                .sentenceSplitter()
                .posTagger()
                .build();

        return executor.execute(rawText, pipeline);
    }

    public GateProcessingResult analyzeWithTokeniser(String rawText) {
        SerialAnalyserController pipeline = new GatePipelineBuilder()
                .tokeniser()
                .build();

        return executor.execute(rawText, pipeline);
    }

    public GateProcessingResult analyzeWithSentenceSplitter(String rawText) {
        SerialAnalyserController pipeline = new GatePipelineBuilder()
                .tokeniser()
                .sentenceSplitter()
                .build();

        return executor.execute(rawText, pipeline);
    }

    public GateProcessingResult analyzeWithCustomPipeline(String rawText, GateComponentType... componentTypes) {
        GatePipelineBuilder builder = new GatePipelineBuilder();

        if (componentTypes != null) {
            for (GateComponentType componentType : componentTypes) {
                builder.add(componentType);
            }
        }

        return executor.execute(rawText, builder.build());
    }

    public GateProcessingResult analyzeFileWithFullPipeline(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.canRead()) {
            throw new IOException("Fichier inaccessible : " + filePath);
        }

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return analyzeWithFullPipeline(content);
    }
}