package fr.unicaen;

import fr.unicaen.model.algo.tool.gate.composant.GateComponentType;
import fr.unicaen.model.algo.tool.gate.pipeline.GateAnalysisPipeline;
import fr.unicaen.model.algo.tool.gate.pipeline.GateProcessingResult;

public class TestGateCustomPipeline {

    public static void main(String[] args) {
        GateAnalysisPipeline pipeline = new GateAnalysisPipeline();

        GateProcessingResult result = pipeline.analyzeWithCustomPipeline(
                "Bonjour. Ceci est une deuxième phrase.",
                GateComponentType.TOKENISER,
                GateComponentType.SENTENCE_SPLITTER
        );

        System.out.println(result.getTokens());
        System.out.println(result.getSentences());
    }
}
