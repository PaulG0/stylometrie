package fr.unicaen.model.algo.tool.gate.composant;

/**
 * Exception spécifique aux erreurs de création ou d'utilisation
 * des composants GATE.
 */
public class GateComponentException extends RuntimeException {

    public GateComponentException(String message) {
        super(message);
    }

    public GateComponentException(String message, Throwable cause) {
        super(message, cause);
    }
}
