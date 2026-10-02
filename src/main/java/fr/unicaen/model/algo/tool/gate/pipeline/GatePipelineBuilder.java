package fr.unicaen.model.algo.tool.gate.pipeline;

import fr.unicaen.model.algo.tool.gate.composant.GateComponent;
import fr.unicaen.model.algo.tool.gate.composant.GateComponentFactory;
import fr.unicaen.model.algo.tool.gate.composant.GateComponentType;
import gate.Factory;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder de pipelines GATE.
 */
public class GatePipelineBuilder {

    private final GateComponentFactory componentFactory = new GateComponentFactory();
    private final List<GateComponent> components = new ArrayList<>();

    public GatePipelineBuilder add(GateComponentType type) {
        if (type != null) {
            components.add(componentFactory.create(type));
        }
        return this;
    }

    public GatePipelineBuilder add(GateComponent component) {
        if (component != null) {
            components.add(component);
        }
        return this;
    }

    public GatePipelineBuilder addAll(List<GateComponent> components) {
        if (components != null) {
            components.forEach(this::add);
        }
        return this;
    }

    public GatePipelineBuilder tokeniser() {
        return add(GateComponentType.TOKENISER);
    }

    public GatePipelineBuilder sentenceSplitter() {
        return add(GateComponentType.SENTENCE_SPLITTER);
    }

    public GatePipelineBuilder gazetteer() {
        return add(GateComponentType.GAZETTEER);
    }

    public GatePipelineBuilder posTagger() {
        return add(GateComponentType.POS_TAGGER);
    }

    public SerialAnalyserController build() {
        try {
            SerialAnalyserController pipeline = (SerialAnalyserController) Factory.createResource(
                    "gate.creole.SerialAnalyserController"
            );

            for (GateComponent component : components) {
                ProcessingResource resource = component.createProcessingResource();
                pipeline.add(resource);
            }

            return pipeline;
        } catch (GateException e) {
            throw new GatePipelineException("Impossible de construire le pipeline GATE.", e);
        }
    }

    public List<GateComponent> getComponents() {
        return List.copyOf(components);
    }

    public void clear() {
        components.clear();
    }
}