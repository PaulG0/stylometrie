package fr.unicaen.controller;

import fr.unicaen.service.CsvImportService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import java.io.IOException;
import java.io.File;
import java.util.function.Consumer;

public class HeaderController {

    @FXML private TabPane mainTabPane;
    @FXML private TabPane librarySubTabPane;

    @FXML private Tab libraryTab;
    @FXML private Tab authorsSubTab;
    @FXML private Tab worksSubTab;
    @FXML private Tab searchSubTab;
    @FXML private Tab importSubTab;

    private Consumer<String> onSubTabChangedListener;

    @FXML
    public void initialize() {
        // Masquer la barre de sous-onglets si la Bibliothèque n'est pas sélectionnée
        librarySubTabPane.setManaged(false);
        librarySubTabPane.setVisible(false);

        // Écouteur sur les onglets principaux
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            boolean isLibrary = (newTab == libraryTab);
            librarySubTabPane.setManaged(isLibrary);
            librarySubTabPane.setVisible(isLibrary);

            if (isLibrary && onSubTabChangedListener != null) {
                notifySelectedSubTab(librarySubTabPane.getSelectionModel().getSelectedItem());
            }
        });

        // Écouteur sur les sous-onglets
        librarySubTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (onSubTabChangedListener != null && newTab != null) {
                notifySelectedSubTab(newTab);
            }
        });
    }

    /**
     * Méthode unique de notification pour charger le FXML associé au sous-onglet
     */
    private void notifySelectedSubTab(Tab selectedTab) {
        if (onSubTabChangedListener == null) return;

        if (selectedTab == authorsSubTab) {
            onSubTabChangedListener.accept("/fr/unicaen/list/authors-list-view.fxml");
        } else if (selectedTab == worksSubTab) {
            // Chargement de la vue des oeuvres / dataStore
            onSubTabChangedListener.accept("/fr/unicaen/list/DataStoreView.fxml");
        } else if (selectedTab == importSubTab) {
            onSubTabChangedListener.accept("/fr/unicaen/config/datastore-manage-view.fxml");
        }
    }

    public void setOnSubTabChangedListener(Consumer<String> listener) {
        this.onSubTabChangedListener = listener;
    }

    @FXML
    protected void onMinimizeClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    protected void onMaximizeClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    protected void onCloseClick() {
        System.exit(0);
    }

    @FXML
    protected void onImportCsvClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer le référentiel des auteurs (autors_fr.csv)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            CsvImportService importService = new CsvImportService();
            int importedCount = importService.importAuthorsFromCsv(file);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Importation réussie");
            alert.setHeaderText(null);
            alert.setContentText(importedCount + " auteurs ont été importés / mis à jour dans SQLite !");
            alert.showAndWait();
        }
    }

    @FXML
    public void onOeuvresAndTextesClicked() {
        if (mainTabPane.getSelectionModel().getSelectedItem() != libraryTab) {
            mainTabPane.getSelectionModel().select(libraryTab);
        }
        librarySubTabPane.getSelectionModel().select(worksSubTab);
    }
}