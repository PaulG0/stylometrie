package fr.unicaen.service;

import gate.Gate;
import gate.util.GateException;

public class GatePipelineService {

    private boolean isInitialized = false;

    public void init() {
        if (isInitialized) return;

        try {
            Gate.runInSandbox(true);
            Gate.init();
            isInitialized = true;
            System.out.println("[GATE] Initialisé.");
        } catch (GateException e) {
            System.err.println("[GATE] Erreur : " + e.getMessage());
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}