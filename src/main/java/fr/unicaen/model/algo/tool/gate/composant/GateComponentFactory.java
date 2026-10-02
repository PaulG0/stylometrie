package fr.unicaen.model.algo.tool.gate.composant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fabrique de composants GATE.
 *
 * <p>Elle permet de créer rapidement les composants nécessaires à un pipeline :
 * tokeniseur seul, pipeline avec phrases, pipeline complet de stylométrie, etc.</p>
 */
public class GateComponentFactory {

    public GateComponent create(GateComponentType type) {
        return new GateComponent(type);
    }

    public List<GateComponent> createAll(GateComponentType... types) {
        if (types == null || types.length == 0) {
            return List.of();
        }

        return Arrays.stream(types)
                .map(this::create)
                .toList();
    }

    public List<GateComponent> createTokeniserComponents() {
        return createAll(GateComponentType.TOKENISER);
    }

    public List<GateComponent> createSentenceComponents() {
        return createAll(
                GateComponentType.TOKENISER,
                GateComponentType.SENTENCE_SPLITTER
        );
    }

    public List<GateComponent> createFullStylometryComponents() {
        return createAll(
                GateComponentType.TOKENISER,
                GateComponentType.GAZETTEER,
                GateComponentType.SENTENCE_SPLITTER,
                GateComponentType.POS_TAGGER
        );
    }

    public List<GateComponent> copyOf(List<GateComponent> components) {
        if (components == null || components.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(components);
    }
}
