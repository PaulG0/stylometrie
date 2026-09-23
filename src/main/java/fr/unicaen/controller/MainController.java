package fr.unicaen.controller;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.database.DatabaseManager;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import fr.unicaen.service.GatePipelineService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.Connection;
import java.util.List;

public class MainController {

    @FXML private TextArea logArea;
    @FXML private TextField authorNameField;
    @FXML private ComboBox<Author> authorComboBox;
    @FXML private TableView<Text> textsTable;
    @FXML private TableColumn<Text, String> titleColumn;
    @FXML private TableColumn<Text, String> authorColumn;

    private final GatePipelineService gateService = new GatePipelineService();
    private final AnalysisDao analysisDao = new AnalysisDao();

    @FXML
    public void initialize() {
        if (titleColumn != null && authorColumn != null) {
            titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
            authorColumn.setCellValueFactory(new PropertyValueFactory<>("authorName"));
            loadCorpusData();
        }
    }

    private void loadCorpusData() {
        try {
            List<Author> authors = analysisDao.getAllAuthors();
            if (authorComboBox != null) {
                authorComboBox.setItems(FXCollections.observableArrayList(authors));
            }

            List<Text> texts = analysisDao.getAllTexts();
            if (textsTable != null) {
                textsTable.setItems(FXCollections.observableArrayList(texts));
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement du corpus : " + e.getMessage());
        }
    }

    @FXML
    protected void onAddAuthorClick() {
        if (authorNameField == null) return;
        String name = authorNameField.getText().trim();
        if (!name.isEmpty()) {
            try {
                analysisDao.addAuthor(name);
                authorNameField.clear();
                loadCorpusData();
            } catch (Exception e) {
                showAlert("Erreur", "L'auteur existe déjà ou la base est inaccessible.");
            }
        }
    }

    @FXML
    protected void onImportTextClick() {
        Author selectedAuthor = authorComboBox != null ? authorComboBox.getValue() : null;
        if (selectedAuthor == null) {
            showAlert("Attention", "Veuillez sélectionner un auteur dans la liste.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier texte");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Texte (*.txt)", "*.txt")
        );

        File file = fileChooser.showOpenDialog(authorComboBox.getScene().getWindow());
        if (file != null) {
            try {
                analysisDao.addText(selectedAuthor.getId(), file.getName());
                loadCorpusData();
            } catch (Exception e) {
                showAlert("Erreur", "Impossible d'ajouter le texte : " + e.getMessage());
            }
        }
    }

    @FXML
    protected void onTestButtonClick() {
        if (logArea != null) {
            logArea.appendText("Début des tests...\n");
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn != null && !conn.isClosed() && logArea != null) {
                logArea.appendText("✅ SQLite connecté.\n");
            }
        } catch (Exception e) {
            if (logArea != null) {
                logArea.appendText("❌ Erreur SQLite : " + e.getMessage() + "\n");
            }
        }

        Task<Void> gateTask = new Task<>() {
            @Override
            protected Void call() {
                gateService.init();
                return null;
            }
        };

        gateTask.setOnSucceeded(event -> {
            if (logArea != null) {
                if (gateService.isInitialized()) {
                    logArea.appendText("✅ GATE initialisé.\n");
                } else {
                    logArea.appendText("❌ Échec GATE.\n");
                }
            }
        });

        new Thread(gateTask).start();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}