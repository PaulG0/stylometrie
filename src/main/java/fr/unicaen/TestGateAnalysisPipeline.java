package fr.unicaen;

import fr.unicaen.model.algo.tool.gate.pipeline.GateAnalysisPipeline;
import fr.unicaen.model.algo.tool.gate.pipeline.GateProcessingResult;

public class TestGateAnalysisPipeline {

    public static void main(String[] args) {
        GateAnalysisPipeline pipeline = new GateAnalysisPipeline();

        GateProcessingResult result = pipeline.analyzeWithFullPipeline(
                "Victor Hugo écrit une phrase magnifique. Le style est puissant."
        );

        System.out.println(result.getTokens());
        System.out.println(result.getSentences());
        System.out.println(result.getPosTags());
    }
}