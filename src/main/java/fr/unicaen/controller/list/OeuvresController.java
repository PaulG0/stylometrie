package fr.unicaen.controller.list;

import fr.unicaen.controller.dialog.AuthorCardController;
import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import fr.unicaen.service.ImportService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class OeuvresController {

    @FXML private TableView<Text> oeuvresTable;
    @FXML private TableColumn<Text, Integer> colId;
    @FXML private TableColumn<Text, Hyperlink> colAuthor; // Colonne sous forme de lien Hyperlink
    @FXML private TableColumn<Text, String> colTitle;
    @FXML private TableColumn<Text, String> colPath;

    @FXML private ComboBox<Integer> comboLimit;
    @FXML private Button btnPrevious;
    @FXML private Button btnNext;
    @FXML private Label lblPageInfo;

    private final AnalysisDao analysisDao = new AnalysisDao();
    private final ImportService importService = new ImportService();

    private int currentPage = 0;
    private int currentLimit = 20;

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colPath.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFilePath()));

        // Rendu spécifique pour rendre le nom de l'Auteur cliquable
        colAuthor.setCellValueFactory(cell -> {
            Text text = cell.getValue();
            String name = (text.getAuthorName() != null && !text.getAuthorName().isEmpty())
                    ? text.getAuthorName()
                    : "Auteur Inconnu";

            Hyperlink link = new Hyperlink(name);
            link.setOnAction(e -> openAuthorCard(text.getAuthorId(), name));
            return new SimpleObjectProperty<>(link);
        });

        // Écouteur pour la lecture de document sur double-clic dans le tableau
        oeuvresTable.setRowFactory(tv -> {
            TableRow<Text> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Text selectedText = row.getItem();
                    openDocumentReader(selectedText);
                }
            });
            return row;
        });

        comboLimit.setItems(FXCollections.observableArrayList(20, 50, 100));
        comboLimit.setValue(20);
        comboLimit.setOnAction(e -> {
            currentLimit = comboLimit.getValue();
            currentPage = 0;
            loadData();
        });

        loadData();
    }

    public void loadData() {
        int offset = currentPage * currentLimit;
        List<Text> texts = analysisDao.getPaginatedTexts(currentLimit, offset, "title", "ASC");

        ObservableList<Text> observableList = FXCollections.observableArrayList(texts);
        oeuvresTable.setItems(observableList);

        int totalCount = analysisDao.getTotalTextsCount();
        int maxPage = (int) Math.ceil((double) totalCount / currentLimit);
        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + Math.max(1, maxPage));

        btnPrevious.setDisable(currentPage == 0);
        btnNext.setDisable((offset + currentLimit) >= totalCount);
    }

    /**
     * Ouvre la fiche détaillée et enrichie de l'auteur.
     */
    private void openAuthorCard(int authorId, String authorName) {
        Author author = analysisDao.getAuthorById(authorId);
        if (author == null) {
            author = analysisDao.getAuthorByName(authorName);
        }

        if (author != null) {
            AuthorCardController.open(author, oeuvresTable.getScene().getWindow(), this::loadData);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Impossible de trouver l'auteur : " + authorName);
            alert.showAndWait();
        }
    }

    /**
     * Ouvre un éditeur/lecteur texte permettant de consulter le contenu du document sélectionné.
     */
    private void openDocumentReader(Text text) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lecteur de Document — " + text.getTitle());
        dialog.setHeaderText("Fichier : " + text.getFilePath());

        ButtonType closeButtonType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(700, 500);

        try {
            File f = new File(text.getFilePath());
            if (f.exists()) {
                String content = Files.readString(Paths.get(text.getFilePath()));
                textArea.setText(content);
            } else {
                textArea.setText("[ERREUR] Le fichier n'existe pas sur le disque local : " + text.getFilePath());
            }
        } catch (IOException e) {
            textArea.setText("[ERREUR] Impossible de lire le fichier : " + e.getMessage());
        }

        VBox contentBox = new VBox(textArea);
        dialog.getDialogPane().setContent(contentBox);
        dialog.showAndWait();
    }

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        loadData();
    }

    @FXML
    private void handleImportStory() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier à importer");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les documents supportés", "*.txt", "*.csv", "*.xml", "*.pdf", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("Fichiers Texte / Corpus", "*.txt", "*.csv", "*.xml")
        );

        File selectedFile = fileChooser.showOpenDialog(oeuvresTable.getScene().getWindow());
        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            String defaultName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

            boolean success = importService.importStory(selectedFile, defaultName, defaultName);
            if (success) {
                loadData();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Échec de l'importation du fichier.");
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void handleImportDataStore() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Sélectionner le dossier racine du DataStore GATE");

        File selectedFolder = directoryChooser.showDialog(oeuvresTable.getScene().getWindow());
        if (selectedFolder != null) {
            int imported = importService.importGateDataStore(selectedFolder);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, imported + " document(s) du DataStore GATE importé(s) avec succès !");
            alert.showAndWait();
            loadData();
        }
    }
}