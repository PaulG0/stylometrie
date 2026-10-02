package fr.unicaen.model.algo.tool.gate.composant;

/**
 * Types de composants GATE disponibles pour construire les pipelines TAL.
 *
 * <p>Chaque constante correspond à une ProcessingResource GATE.</p>
 */
public enum GateComponentType {

    TOKENISER("gate.creole.tokeniser.DefaultTokeniser"),
    SENTENCE_SPLITTER("gate.creole.splitter.SentenceSplitter"),
    GAZETTEER("gate.creole.gazetteer.DefaultGazetteer"),
    POS_TAGGER("gate.creole.POSTagger");

    private final String gateResourceClassName;

    GateComponentType(String gateResourceClassName) {
        this.gateResourceClassName = gateResourceClassName;
    }

    public String getGateResourceClassName() {
        return gateResourceClassName;
    }
}
