package fr.unicaen.controller.list;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class DataStoreController {

    @FXML private TableView<TextRow> textsTable;
    @FXML private TableColumn<TextRow, String> idColumn;
    @FXML private TableColumn<TextRow, String> authorColumn;
    @FXML private TableColumn<TextRow, String> titleColumn;
    @FXML private TableColumn<TextRow, String> pathColumn;

    private final AnalysisDao analysisDao = new AnalysisDao();

    @FXML
    public void initialize() {
        // Initialisation sécurisée uniquement dans le contrôleur de la vue associée
        if (idColumn != null) {
            idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        }
        if (authorColumn != null) {
            authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthorName()));
        }
        if (titleColumn != null) {
            titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        }
        if (pathColumn != null) {
            pathColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFilePath()));
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
                        tableData.add(new TextRow(text.getId(), author.getName(), text.getTitle(), text.getFilePath()));
                    }
                }
            }
        }
        if (textsTable != null) {
            textsTable.setItems(tableData);
        }
    }

    public static class TextRow {
        private final int id;
        private final String authorName;
        private final String title;
        private final String filePath;

        public TextRow(int id, String authorName, String title, String filePath) {
            this.id = id;
            this.authorName = authorName;
            this.title = title;
            this.filePath = filePath;
        }

        public int getId() { return id; }
        public String getAuthorName() { return authorName; }
        public String getTitle() { return title; }
        public String getFilePath() { return filePath; }
    }
}