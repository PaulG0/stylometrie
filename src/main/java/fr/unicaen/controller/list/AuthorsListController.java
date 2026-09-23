package fr.unicaen.controller.list;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fr.unicaen.model.Text;
public class AuthorsListController {

    @FXML private Label totalCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<Integer> pageSizeComboBox;
    @FXML private ToggleButton tableModeBtn;
    @FXML private ToggleButton galleryModeBtn;

    @FXML private VBox tableViewBox;
    @FXML private ScrollPane galleryScrollView;
    @FXML private FlowPane galleryFlowPane;

    @FXML private TableView<Author> authorsTable;
    @FXML private TableColumn<Author, Integer> colId;
    @FXML private TableColumn<Author, String> colName;
    @FXML private TableColumn<Author, String> colBirthDate;
    @FXML private TableColumn<Author, String> colMovement;
    @FXML private TableColumn<Author, Integer> colWorksCount;
    @FXML private TableColumn<Author, String> colUri;

    @FXML private Pagination pagination;

    private final ObservableList<Author> masterData = FXCollections.observableArrayList();
    private FilteredList<Author> filteredData;
    private final AnalysisDao analysisDao = new AnalysisDao();

    // Cache local pour ne pas télécharger 10 fois la même image Wikidata
    private static final ConcurrentHashMap<String, String> imageCache = new ConcurrentHashMap<>();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBirthDate.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        colMovement.setCellValueFactory(new PropertyValueFactory<>("movement"));
        colUri.setCellValueFactory(new PropertyValueFactory<>("wikidataUri"));

        // Tailles de pagination
        pageSizeComboBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        pageSizeComboBox.setValue(20);
        pageSizeComboBox.setOnAction(e -> updatePagination());

        // Filtrage en temps réel
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(author -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filter = newVal.toLowerCase();
                return (author.getName() != null && author.getName().toLowerCase().contains(filter))
                        || (author.getMovement() != null && author.getMovement().toLowerCase().contains(filter));
            });
            updatePagination();
        });

        // Toggle entre Tableau et Galerie
        tableModeBtn.setOnAction(e -> toggleView(true));
        galleryModeBtn.setOnAction(e -> toggleView(false));

        pagination.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> renderCurrentPage());

        loadAuthorsFromDb();
    }

    private void toggleView(boolean isTable) {
        tableViewBox.setVisible(isTable);
        galleryScrollView.setVisible(!isTable);
        renderCurrentPage();
    }

    private void updatePagination() {
        int pageSize = pageSizeComboBox.getValue();
        int pageCount = (int) Math.ceil((double) filteredData.size() / pageSize);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        int pageSize = pageSizeComboBox.getValue();
        int pageIndex = pagination.getCurrentPageIndex();
        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredData.size());

        List<Author> pageItems = filteredData.subList(fromIndex, toIndex);

        if (tableModeBtn.isSelected()) {
            authorsTable.setItems(FXCollections.observableArrayList(pageItems));
        } else {
            renderGallery(pageItems);
        }
    }

    private void renderGallery(List<Author> authors) {
        galleryFlowPane.getChildren().clear();

        for (Author author : authors) {
            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: #ffffff; -fx-padding: 12px; -fx-background-radius: 10px; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; -fx-pref-width: 170px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

            ImageView imgView = new ImageView();
            imgView.setFitWidth(140);
            imgView.setFitHeight(180);
            imgView.setPreserveRatio(true);

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(30, 30);

            StackPane imageContainer = new StackPane(spinner, imgView);
            imageContainer.setPrefSize(140, 180);

            Label nameLabel = new Label(author.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f1c2e; -fx-alignment: center;");
            nameLabel.setWrapText(true);

            Label subLabel = new Label(author.getMovement() != null ? author.getMovement() : "Écrivain");
            subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            card.getChildren().addAll(imageContainer, nameLabel, subLabel);
            galleryFlowPane.getChildren().add(card);

            // Fetch de l'image sur Internet de manière asynchrone pour ne pas freezer l'IHM
            fetchAuthorImageAsync(author.getWikidataUri(), imgUrl -> {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    if (imgUrl != null) {
                        imgView.setImage(new Image(imgUrl, true));
                    } else {
                        // Image par défaut si pas d'image Wikidata trouvée
                        imgView.setImage(new Image(getClass().getResourceAsStream("/fr/unicaen/images/default-author.png")));
                    }
                });
            });
        }
    }

    private void fetchAuthorImageAsync(String wikidataUri, java.util.function.Consumer<String> callback) {
        if (wikidataUri == null || !wikidataUri.contains("Q")) {
            callback.accept(null);
            return;
        }

        String entityId = wikidataUri.substring(wikidataUri.lastIndexOf('/') + 1);

        if (imageCache.containsKey(entityId)) {
            callback.accept(imageCache.get(entityId));
            return;
        }

        new Thread(() -> {
            try {
                String apiUrl = "https://www.wikidata.org/wiki/Special:EntityData/" + entityId + ".json";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Regex rapide pour récupérer la propriété P18 (Image Wikimedia)
                Pattern pattern = Pattern.compile("\"P18\":\\[\\{\"mainsnak\":\\{.*?\"value\":\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response.body());

                if (matcher.find()) {
                    String imageName = matcher.group(1).replace(" ", "_");
                    // URL directe vers les vignettes Wikimedia Commons
                    String imageUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/" + imageName + "?width=300";
                    imageCache.put(entityId, imageUrl);
                    callback.accept(imageUrl);
                } else {
                    callback.accept(null);
                }
            } catch (Exception e) {
                callback.accept(null);
            }
        }).start();
    }

    @FXML
    protected void onRefreshClick() {
        loadAuthorsFromDb();
    }

    private void loadAuthorsFromDb() {
        try {
            List<Author> authors = analysisDao.getAllAuthors();
            masterData.setAll(authors);
            totalCountLabel.setText(authors.size() + " auteur(s) en base");
            updatePagination();
        } catch (Exception e) {
            System.err.println("Erreur de chargement des auteurs : " + e.getMessage());
        }
    }


    private void showTextsForAuthor(Author author) {
        List<Text> texts = analysisDao.getTextsByAuthor(author.getId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Œuvres de " + author.getName());
        dialog.setHeaderText("Liste des documents enregistrés pour " + author.getName());

        ListView<String> listView = new ListView<>();
        for (Text text : texts) {
            listView.getItems().add(text.getTitle() + " (" + text.getFilePath() + ")");
        }

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}