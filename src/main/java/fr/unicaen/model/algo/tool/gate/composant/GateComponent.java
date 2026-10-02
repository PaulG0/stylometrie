package fr.unicaen.model.algo.tool.gate.composant;

import gate.Factory;
import gate.ProcessingResource;
import gate.util.GateException;

/**
 * Représente un composant GATE utilisable dans un pipeline.
 *
 * <p>Cette classe encapsule le nom Java complet de la ressource GATE
 * et permet de créer la ProcessingResource correspondante.</p>
 */
public class GateComponent {

    private final GateComponentType type;

    public GateComponent(GateComponentType type) {
        if (type == null) {
            throw new IllegalArgumentException("Le type de composant GATE ne peut pas être null.");
        }

        this.type = type;
    }

    public GateComponentType getType() {
        return type;
    }

    public String getGateResourceClassName() {
        return type.getGateResourceClassName();
    }

    public ProcessingResource createProcessingResource() throws GateException {
        return (ProcessingResource) Factory.createResource(getGateResourceClassName());
    }

    @Override
    public String toString() {
        return "GateComponent{" +
                "type=" + type +
                ", resource='" + getGateResourceClassName() + '\'' +
                '}';
    }
}
