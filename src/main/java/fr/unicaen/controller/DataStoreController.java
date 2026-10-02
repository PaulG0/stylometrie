package fr.unicaen.controller.list;

import fr.unicaen.controller.dialog.AuthorCardController;
import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

public class DataStoreController {

    @FXML private TableView<TextRow> textsTable;
    @FXML private TableColumn<TextRow, String>  idColumn;
    @FXML private TableColumn<TextRow, Void>    actionColumn;
    @FXML private TableColumn<TextRow, String>  authorColumn;
    @FXML private TableColumn<TextRow, String>  titleColumn;
    @FXML private TableColumn<TextRow, String>  pathColumn;

    private final AnalysisDao analysisDao = new AnalysisDao();

    @FXML
    public void initialize() {
        if (idColumn != null) {
            idColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        }

        // ── Colonne bouton 👁️ → ouvre la fiche auteur ──────────────────────────
        if (actionColumn != null) {
            actionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button btnFiche = new Button("👁");

                {
                    btnFiche.setStyle(
                            "-fx-background-color: transparent;" +
                            "-fx-text-fill: #d4af37;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 2px 6px;"
                    );
                    btnFiche.setTooltip(new Tooltip("Voir la fiche de l'auteur"));
                    btnFiche.setOnAction(e -> {
                        TextRow row = getTableView().getItems().get(getIndex());
                        if (row == null) return;
                        Author author = analysisDao.getAuthorByName(row.getAuthorName());
                        if (author != null) {
                            AuthorCardController.open(
                                    author,
                                    getScene() != null ? getScene().getWindow() : null,
                                    null
                            );
                        } else {
                            new Alert(Alert.AlertType.WARNING,
                                    "Auteur introuvable en base : " + row.getAuthorName())
                                    .showAndWait();
                        }
                    });

                    HBox box = new HBox(btnFiche);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : ((HBox) getGraphic()));
                }
            });
        }

        if (authorColumn != null) {
            authorColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getAuthorName()));
        }
        if (titleColumn != null) {
            titleColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getTitle()));
        }
        if (pathColumn != null) {
            pathColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getFilePath()));
        }

        loadData();
    }

    private void loadData() {
        ObservableList<TextRow> tableData = FXCollections.observableArrayList();
        List<Author> authors = analysisDao.getAllAuthors();

        if (authors != null) {
            for (Author author : authors) {
                List<Text> texts = analysisDao.getTextsByAuthorId(author.getId());
                if (texts != null) {
                    for (Text text : texts) {
                        tableData.add(new TextRow(
                                text.getId(),
                                author.getName(),
                                text.getTitle(),
                                text.getFilePath()
                        ));
                    }
                }
            }
        }

        if (textsTable != null) {
            textsTable.setItems(tableData);
        }
    }

    // ── Recharge les données (appelable depuis l'extérieur) ────────────────────
    public void refresh() {
        loadData();
    }

    // ── Inner class TextRow ────────────────────────────────────────────────────
    public static class TextRow {
        private final int    id;
        private final String authorName;
        private final String title;
        private final String filePath;

        public TextRow(int id, String authorName, String title, String filePath) {
            this.id         = id;
            this.authorName = authorName;
            this.title      = title;
            this.filePath   = filePath;
        }

        public int    getId()         { return id; }
        public String getAuthorName() { return authorName; }
        public String getTitle()      { return title; }
        public String getFilePath()   { return filePath; }
    }
}