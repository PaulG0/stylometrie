package fr.unicaen.model.algo.tool.gate.pipeline;

/**
 * Exception spécifique aux erreurs d'exécution des pipelines GATE.
 */
public class GatePipelineException extends RuntimeException {

    public GatePipelineException(String message) {
        super(message);
    }

    public GatePipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}