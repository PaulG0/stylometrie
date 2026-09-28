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
import fr.unicaen.controller.dialog.AuthorCardController;
import fr.unicaen.model.Text;
import fr.unicaen.service.AuthorImageService;

public class AuthorsListController {

    @FXML private Label totalCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<Integer> pageSizeComboBox;
    @FXML private ToggleButton tableModeBtn;
    @FXML private ToggleButton galleryModeBtn;
    @FXML private Button btnViewCard;

    @FXML private VBox tableViewBox;
    @FXML private ScrollPane galleryScrollView;
    @FXML private FlowPane galleryFlowPane;

    @FXML private TableView<Author> authorsTable;
    @FXML private TableColumn<Author, Integer> colId;
    @FXML private TableColumn<Author, Void> colAction;
    @FXML private TableColumn<Author, String> colName;
    @FXML private TableColumn<Author, String> colBirthDate;
    @FXML private TableColumn<Author, String> colMovement;
    @FXML private TableColumn<Author, Integer> colWorksCount;
    @FXML private TableColumn<Author, String> colUri;

    @FXML private Pagination pagination;

    private final ObservableList<Author> masterData = FXCollections.observableArrayList();
    private FilteredList<Author> filteredData;
    private final AnalysisDao analysisDao = new AnalysisDao();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBirthDate.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        colMovement.setCellValueFactory(new PropertyValueFactory<>("movement"));
        colWorksCount.setCellValueFactory(new PropertyValueFactory<>("worksCount"));
        colUri.setCellValueFactory(new PropertyValueFactory<>("wikidataUri"));

        // Colonne d'interaction avec l'auteur (icône œil)
        colAction.setCellFactory(col -> new TableCell<Author, Void>() {
            private final Button btn = new Button("👁️");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f1c40f; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2px 7px; -fx-border-color: #d4af37; -fx-border-radius: 4px;");
                btn.setTooltip(new Tooltip("Consulter la fiche complète de cet auteur"));
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #0f1c2e; -fx-text-fill: #f1c40f; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2px 7px; -fx-border-color: #f1c40f; -fx-border-radius: 4px; -fx-effect: dropshadow(three-pass-box, rgba(212,175,55,0.4), 4, 0, 0, 1);"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f1c40f; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2px 7px; -fx-border-color: #d4af37; -fx-border-radius: 4px;"));
                btn.setOnAction(e -> {
                    Author author = getTableView().getItems().get(getIndex());
                    if (author != null) openAuthorCard(author);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Interaction tableau : double-clic et menu contextuel pour ouvrir la fiche auteur
        authorsTable.setRowFactory(tv -> {
            TableRow<Author> row = new TableRow<>();

            // Menu contextuel au clic droit
            ContextMenu contextMenu = new ContextMenu();
            MenuItem viewCardItem = new MenuItem("👁️ Consulter la fiche complète de l'auteur");
            viewCardItem.setOnAction(event -> {
                Author author = row.getItem();
                if (author != null) openAuthorCard(author);
            });
            contextMenu.getItems().add(viewCardItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            // Double clic
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Author selectedAuthor = row.getItem();
                    openAuthorCard(selectedAuthor);
                }
            });
            return row;
        });

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

            card.setCursor(javafx.scene.Cursor.HAND);
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #ffffff; -fx-padding: 12px; -fx-background-radius: 10px; -fx-border-color: #d4af37; -fx-border-width: 1.5px; -fx-border-radius: 10px; -fx-pref-width: 170px; -fx-effect: dropshadow(three-pass-box, rgba(212,175,55,0.25), 8, 0, 0, 3);"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #ffffff; -fx-padding: 12px; -fx-background-radius: 10px; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-pref-width: 170px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);"));
            card.setOnMouseClicked(e -> openAuthorCard(author));

            Button btnCard = new Button("👁️ Consulter fiche");
            btnCard.setStyle("-fx-background-color: rgba(15,28,46,0.08); -fx-text-fill: #0f1c2e; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-font-weight: bold; -fx-cursor: hand;");
            btnCard.setOnAction(e -> {
                e.consume();
                openAuthorCard(author);
            });

            card.getChildren().addAll(imageContainer, nameLabel, subLabel, btnCard);
            galleryFlowPane.getChildren().add(card);

            // Chargement haute performance et sans blocage 403 via AuthorImageService
            AuthorImageService.fetchAuthorPhotoAsync(author.getWikidataUri(), author.getName(), img -> {
                spinner.setVisible(false);
                if (img != null) {
                    imgView.setImage(img);
                }
            });
        }
    }

    @FXML
    protected void onRefreshClick() {
        loadAuthorsFromDb();
    }

    @FXML
    protected void onViewAuthorCardClick() {
        Author selected = authorsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openAuthorCard(selected);
        } else if (!authorsTable.getItems().isEmpty()) {
            openAuthorCard(authorsTable.getItems().get(0));
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un auteur dans le tableau ou cliquer sur une carte.");
            alert.showAndWait();
        }
    }

    private void openAuthorCard(Author author) {
        if (author == null) return;
        AuthorCardController.open(author, authorsTable.getScene().getWindow(), this::loadAuthorsFromDb);
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
}