package fr.unicaen.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private HeaderController headerController; // Injection du contrôleur de header.fxml
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        if (headerController != null) {
            // Configuration de l'écouteur de changement de sous-onglet
            headerController.setOnSubTabChangedListener(this::loadView);
        }

        // Vue chargée par défaut au lancement
        loadView("/fr/unicaen/list/authors-list-view.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}