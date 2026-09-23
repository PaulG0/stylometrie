package fr.unicaen.service;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;

import gate.Corpus;
import gate.DataStore;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.util.GateException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;

public class DataStoreService {

    private static final Path DATA_DIR = Paths.get("data");
    private final AnalysisDao analysisDao = new AnalysisDao();

    public DataStoreService() {
        try {
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du dossier data/ : " + e.getMessage());
        }
    }

    public int importAndCopyDataStore(File sourceFolder) throws IOException {
        if (sourceFolder == null || !sourceFolder.isDirectory()) {
            throw new IllegalArgumentException("Le chemin spécifié n'est pas un dossier valide.");
        }

        int copiedFilesCount = 0;
        File[] authorFolders = sourceFolder.listFiles(File::isDirectory);
        if (authorFolders != null) {
            for (File authorFolder : authorFolders) {
                Path targetAuthorDir = DATA_DIR.resolve(authorFolder.getName());
                if (!Files.exists(targetAuthorDir)) {
                    Files.createDirectories(targetAuthorDir);
                }

                File[] textFiles = authorFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
                if (textFiles != null) {
                    for (File textFile : textFiles) {
                        Path targetFile = targetAuthorDir.resolve(textFile.getName());
                        Files.copy(textFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
                        copiedFilesCount++;
                    }
                }
            }
        }

        syncDataStoreWithDatabase();
        return copiedFilesCount;
    }

    public SyncResult syncDataStoreWithDatabase() {
        int newAuthors = 0;
        int newTexts = 0;

        File dataFolder = DATA_DIR.toFile();
        File[] authorFolders = dataFolder.listFiles(File::isDirectory);

        if (authorFolders == null) {
            return new SyncResult(0, 0);
        }

        for (File authorFolder : authorFolders) {
            String authorName = authorFolder.getName();

            Author author = analysisDao.getAuthorByName(authorName);
            if (author == null) {
                int authorId = analysisDao.insertAuthor(new Author(0, authorName, "", "", ""));
                author = new Author(authorId, authorName, "", "", "");
                newAuthors++;
            }

            File[] textFiles = authorFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (textFiles != null) {
                List<Text> existingTexts = analysisDao.getTextsByAuthorId(author.getId());

                for (File textFile : textFiles) {
                    String title = textFile.getName().replace(".txt", "");
                    boolean exists = existingTexts.stream().anyMatch(t -> t.getTitle().equalsIgnoreCase(title));

                    if (!exists) {
                        String path = textFile.toPath().normalize().toString();
                        analysisDao.insertText(new Text(0, author.getId(), title, path));
                        newTexts++;
                    }
                }
            }
        }

        return new SyncResult(newAuthors, newTexts);
    }

    public SyncResult syncGateDataStoreWithDatabase(File gateDataStoreFolder) throws GateException, MalformedURLException {
        if (gateDataStoreFolder == null || !gateDataStoreFolder.isDirectory()) {
            throw new IllegalArgumentException("Le dossier GATE spécifié n'est pas un répertoire valide.");
        }

        Gate.init();

        int newAuthors = 0;
        int newTexts = 0;

        String dsUrl = gateDataStoreFolder.toURI().toURL().toExternalForm();
        DataStore ds = Factory.openDataStore("gate.corpora.SerialDataStoreImpl", dsUrl);

        try {
            List<String> corpusIds = ds.getLrIds("gate.corpora.SerialCorpusImpl");

            for (String corpusId : corpusIds) {
                FeatureMap corpusParams = Factory.newFeatureMap();
                corpusParams.put(DataStore.DATASTORE_FEATURE_NAME, ds);
                corpusParams.put(DataStore.LR_ID_FEATURE_NAME, corpusId);

                Corpus corpus = (Corpus) Factory.createResource("gate.corpora.SerialCorpusImpl", corpusParams);
                String authorName = corpus.getName();

                Author author = analysisDao.getAuthorByName(authorName);
                if (author == null) {
                    int authorId = analysisDao.insertAuthor(new Author(0, authorName, "", "", ""));
                    author = new Author(authorId, authorName, "", "", "");
                    newAuthors++;
                }

                List<Text> existingTexts = analysisDao.getTextsByAuthorId(author.getId());

                for (int i = 0; i < corpus.size(); i++) {
                    Document doc = corpus.get(i);
                    String textTitle = doc.getName();

                    boolean exists = existingTexts.stream().anyMatch(t -> t.getTitle().equalsIgnoreCase(textTitle));

                    if (!exists) {
                        // Correctio: getDataStore() adhibetur loco getLDataStore()
                        DataStore docDs = doc.getDataStore();
                        Object lrId = docDs != null ? docDs.getLrName(doc) : null;

                        String persistentRef = lrId != null
                                ? lrId.toString()
                                : gateDataStoreFolder.getAbsolutePath() + "#" + textTitle;

                        analysisDao.insertText(new Text(0, author.getId(), textTitle, persistentRef));
                        newTexts++;
                    }

                    Factory.deleteResource(doc);
                }

                Factory.deleteResource(corpus);
            }
        } finally {
            ds.close();
        }

        return new SyncResult(newAuthors, newTexts);
    }

    public record SyncResult(int newAuthorsCount, int newTextsCount) {}
}