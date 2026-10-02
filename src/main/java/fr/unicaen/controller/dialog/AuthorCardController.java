package fr.unicaen.controller.dialog;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import fr.unicaen.model.AuthorEnrichedData;
import fr.unicaen.model.Text;
import fr.unicaen.service.AuthorImageService;
import fr.unicaen.service.AuthorMetadataService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
public class AuthorCardController {

    @FXML private ImageView authorPhotoView;
    @FXML private ProgressIndicator imageSpinner;
    @FXML private Label authorNameLabel;
    @FXML private Label authorIdBadge;
    @FXML private Label authorShortDescLabel;
    @FXML private Label worksCountBadge;
    @FXML private Label movementBadge;
    @FXML private Label wikidataIdBadge;
    @FXML private Button btnOpenWikidata;
    @FXML private Button btnOpenWikipedia;

    @FXML private TabPane cardTabPane;
    @FXML private ProgressIndicator bioSpinner;
    @FXML private Label biographyLabel;

    @FXML private Label lblBirthDate;
    @FXML private Label lblBirthPlace;
    @FXML private Label lblDeathDate;
    @FXML private Label lblDeathPlace;
    @FXML private Label lblCitizenship;

    @FXML private Label lblMovementDetails;
    @FXML private Label lblOccupations;
    @FXML private Label lblGenres;
    @FXML private Label lblAwards;

    @FXML private FlowPane notableWorksFlowPane;
    @FXML private Label noNotableWorksLabel;

    @FXML private Label syncStatusLabel;
    @FXML private Button btnSyncDatabase;

    @FXML private TableView<Text> authorTextsTable;
    @FXML private TableColumn<Text, Integer> colTextId;
    @FXML private TableColumn<Text, String> colTextTitle;
    @FXML private TableColumn<Text, String> colTextPath;
    @FXML private TableColumn<Text, Button> colTextAction;
    @FXML private HBox noTextsMessageBox;
    @FXML private Label localTextsCountLabel;

    @FXML private Label footerInfoLabel;

    private final AnalysisDao analysisDao = new AnalysisDao();
    private final AuthorMetadataService metadataService = new AuthorMetadataService();

    private Author currentAuthor;
    private AuthorEnrichedData currentEnrichedData;
    private Stage stage;
    private Runnable onAuthorUpdated;

    /**
     * Méthode utilitaire statique pour ouvrir la fiche auteur de n'importe où.
     */
    public static void open(Author author, Window ownerWindow, Runnable onAuthorUpdated) {
        if (author == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(AuthorCardController.class.getResource("/fr/unicaen/dialog/author-card-view.fxml"));
            VBox root = loader.load();

            AuthorCardController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Fiche Auteur — " + author.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (ownerWindow != null) {
                dialogStage.initOwner(ownerWindow);
            }

            Scene scene = new Scene(root, 880, 680);
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(750);
            dialogStage.setMinHeight(550);

            controller.setAuthor(author, dialogStage, onAuthorUpdated);

            dialogStage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture fiche auteur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setAuthor(Author author, Stage stage, Runnable onAuthorUpdated) {
        this.currentAuthor = author;
        this.stage = stage;
        this.onAuthorUpdated = onAuthorUpdated;

        // 1. Initialisation avec les données locales
        authorNameLabel.setText(author.getName());
        authorIdBadge.setText("ID #" + author.getId());

        String initialMovement = (author.getMovement() != null && !author.getMovement().isBlank())
                ? author.getMovement()
                : "Non renseigné";
        movementBadge.setText("🎭 " + initialMovement);
        lblMovementDetails.setText(initialMovement);

        String initialBirth = (author.getBirthDate() != null && !author.getBirthDate().isBlank())
                ? author.getBirthDate()
                : "Non renseignée";
        lblBirthDate.setText(initialBirth);

        String qid = AuthorMetadataService.extractQid(author.getWikidataUri());
        if (qid != null) {
            wikidataIdBadge.setText(qid);
            btnOpenWikidata.setDisable(false);
        } else {
            wikidataIdBadge.setText("Aucun QID");
            btnOpenWikidata.setDisable(true);
        }

        btnOpenWikipedia.setDisable(true);

        // 2. Chargement des textes locaux en BDD SQLite
        loadLocalTexts();

        // 3. Récupération asynchrone des métadonnées enrichies Wikidata / Wikipédia
        fetchEnrichedMetadata(qid);
    }

    private void loadLocalTexts() {
        colTextId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colTextTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colTextPath.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFilePath()));

        colTextAction.setCellValueFactory(c -> {
            Text text = c.getValue();

            Button btnRead = new Button("Lire");
            btnRead.setDisable(false);
            btnRead.setStyle("-fx-background-color: #0f1c2e; -fx-text-fill: #f1c40f; -fx-padding: 4px 10px; -fx-background-radius: 4px; -fx-font-size: 11px; -fx-cursor: hand;");
            btnRead.setOnAction(event -> openDocumentReader(text));

            return new SimpleObjectProperty<>(btnRead);
        });

        List<Text> texts = analysisDao.getTextsByAuthor(currentAuthor.getId());
        authorTextsTable.setItems(FXCollections.observableArrayList(texts));

        int count = texts.size();
        currentAuthor.setWorksCount(count);
        worksCountBadge.setText(count + " œuvre(s) locale(s)");
        localTextsCountLabel.setText(count + " document(s) enregistré(s)");

        noTextsMessageBox.setVisible(count == 0);
        noTextsMessageBox.setManaged(count == 0);
    }

    private void fetchEnrichedMetadata(String qid) {
        imageSpinner.setVisible(true);
        bioSpinner.setVisible(true);

        metadataService.fetchEnrichedDataAsync(currentAuthor.getWikidataUri(), currentAuthor.getName())
                .thenAccept(data -> Platform.runLater(() -> applyEnrichedData(data)));
    }

    private void applyEnrichedData(AuthorEnrichedData data) {
        this.currentEnrichedData = data;
        btnSyncDatabase.setDisable(false);
        imageSpinner.setVisible(false);
        bioSpinner.setVisible(false);

        // Description courte
        if (data.getDescription() != null && !data.getDescription().isBlank()) {
            authorShortDescLabel.setText(data.getDescription());
        } else {
            authorShortDescLabel.setText("Auteur répertorié dans la base littéraire.");
        }

        // Image / Portrait haute performance et sans blocage 403 HTTP
        if (data.getImageUrl() != null && !data.getImageUrl().isBlank()) {
            AuthorImageService.downloadAndCacheImage(
                    data.getWikidataId() != null ? data.getWikidataId() : currentAuthor.getName(),
                    data.getImageUrl(),
                    currentAuthor.getName(),
                    img -> {
                        if (img != null) {
                            authorPhotoView.setImage(img);
                        } else {
                            setDefaultImage();
                        }
                    }
            );
        } else {
            AuthorImageService.fetchAuthorPhotoAsync(currentAuthor.getWikidataUri(), currentAuthor.getName(), img -> {
                if (img != null) {
                    authorPhotoView.setImage(img);
                } else {
                    setDefaultImage();
                }
            });
        }

        // Liens externes
        if (data.getWikidataUri() != null && !data.getWikidataUri().isBlank()) {
            btnOpenWikidata.setDisable(false);
        }
        if (data.getWikipediaUrl() != null && !data.getWikipediaUrl().isBlank()) {
            btnOpenWikipedia.setDisable(false);
        }

        // Biographie Wikipédia
        if (data.getBiographyExtract() != null && !data.getBiographyExtract().isBlank()) {
            biographyLabel.setText(data.getBiographyExtract());
        } else if (data.getDescription() != null && !data.getDescription().isBlank()) {
            biographyLabel.setText(data.getDescription());
        } else {
            biographyLabel.setText("Aucune biographie Wikipédia disponible pour cet auteur.");
        }

        // État Civil & Lieux
        if (data.getBirthDate() != null) lblBirthDate.setText(data.getBirthDate());
        if (data.getBirthPlace() != null) lblBirthPlace.setText(data.getBirthPlace());
        if (data.getDeathDate() != null) lblDeathDate.setText(data.getDeathDate());
        if (data.getDeathPlace() != null) lblDeathPlace.setText(data.getDeathPlace());
        if (data.getCitizenship() != null) lblCitizenship.setText(data.getCitizenship());

        // Profil littéraire
        if (data.getMovements() != null && !data.getMovements().isEmpty()) {
            lblMovementDetails.setText(String.join(", ", data.getMovements()));
            movementBadge.setText("🎭 " + data.getMovements().get(0));
        }

        if (data.getOccupations() != null && !data.getOccupations().isEmpty()) {
            lblOccupations.setText(String.join(", ", data.getOccupations()));
        }

        if (data.getGenres() != null && !data.getGenres().isEmpty()) {
            lblGenres.setText(String.join(", ", data.getGenres()));
        }

        if (data.getAwards() != null && !data.getAwards().isEmpty()) {
            lblAwards.setText(String.join(", ", data.getAwards()));
        }

        // Œuvres notables Wikidata
        notableWorksFlowPane.getChildren().clear();
        if (data.getNotableWorks() != null && !data.getNotableWorks().isEmpty()) {
            noNotableWorksLabel.setVisible(false);
            noNotableWorksLabel.setManaged(false);

            for (String work : data.getNotableWorks()) {
                Label badge = new Label("📖 " + work);
                badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 4px 10px; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-font-size: 11px;");
                notableWorksFlowPane.getChildren().add(badge);
            }
        } else {
            noNotableWorksLabel.setVisible(true);
            noNotableWorksLabel.setManaged(true);
        }

        footerInfoLabel.setText("Données Wikidata & Wikipédia synchronisées avec succès (" + (data.getWikidataId() != null ? data.getWikidataId() : "N/A") + ")");
    }

    private void setDefaultImage() {
        Image avatar = AuthorImageService.getOrCreateDefaultAvatar(currentAuthor != null ? currentAuthor.getName() : "Auteur");
        authorPhotoView.setImage(avatar);
    }

    @FXML
    private void handleSyncDatabase() {
        if (currentEnrichedData == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Veuillez patienter pendant la fin de la récupération des données Wikidata.");
            alert.showAndWait();
            return;
        }

        String newBirth = currentEnrichedData.getBirthDate() != null ? currentEnrichedData.getBirthDate() : currentAuthor.getBirthDate();
        String newMovement = (currentEnrichedData.getMovements() != null && !currentEnrichedData.getMovements().isEmpty())
                ? String.join(", ", currentEnrichedData.getMovements())
                : currentAuthor.getMovement();

        boolean updated = analysisDao.updateAuthorMetadata(currentAuthor.getId(), newBirth, newMovement);
        if (updated) {
            currentAuthor.setBirthDate(newBirth);
            currentAuthor.setMovement(newMovement);
            syncStatusLabel.setText("Base locale SQLite mise à jour avec succès !");
            syncStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 12px;");

            if (onAuthorUpdated != null) {
                onAuthorUpdated.run();
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Les métadonnées de l'auteur ont été enregistrées en base SQLite avec succès !");
            alert.setTitle("Synchronisation réussie");
            alert.setHeaderText("Auteur mis à jour : " + currentAuthor.getName());
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible de mettre à jour la base de données locale.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleOpenWikidata() {
        if (currentEnrichedData != null
                && currentEnrichedData.getWikidataUri() != null
                && !currentEnrichedData.getWikidataUri().isBlank()) {
            openWebPage(currentEnrichedData.getWikidataUri());
            return;
        }

        if (currentAuthor != null
                && currentAuthor.getWikidataUri() != null
                && !currentAuthor.getWikidataUri().isBlank()) {
            openWebPage(currentAuthor.getWikidataUri());
        }
    }

    @FXML
    private void handleOpenWikipedia() {
        if (currentEnrichedData != null
                && currentEnrichedData.getWikipediaUrl() != null
                && !currentEnrichedData.getWikipediaUrl().isBlank()) {
            openWebPage(currentEnrichedData.getWikipediaUrl());
        }
    }

    private void openWebPage(String url) {
        if (url == null || url.isBlank()) return;
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    // Fallback Windows ProcessBuilder
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                }
            } catch (Exception e) {
                System.err.println("Impossible d'ouvrir le navigateur : " + e.getMessage());
            }
        }).start();
    }

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
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
