package fr.unicaen.service;

import fr.unicaen.database.AnalysisDao;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import gate.Corpus;
import gate.DataStore;
import gate.Factory;
import gate.Gate;
import gate.persist.PersistenceException;
import gate.util.GateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImportService {

    private final AnalysisDao analysisDao = new AnalysisDao();
    private static final String DATA_DIR = "data";

    /**
     * Importe un fichier individuel (.txt, .pdf, .doc, etc.) dans le dossier data/ et l'enregistre en BDD.
     */
    public boolean importStory(File sourceFile, String authorName, String title) {
        try {
            Path dataFolderPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataFolderPath)) {
                Files.createDirectories(dataFolderPath);
            }

            Path targetPath = dataFolderPath.resolve(sourceFile.getName());
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            int authorId = getOrCreateAuthorId(authorName);

            Text newText = new Text(0, authorId, title, targetPath.toString());
            return analysisDao.insertText(newText) != -1;

        } catch (IOException e) {
            System.err.println("Erreur lors de l'importation du fichier : " + e.getMessage());
            return false;
        }
    }

    /**
     * Importe un DataStore GATE complet.
     * Associe les corpus (ex: "Corpus Alexandre Dumas") à l'auteur correspondant dans SQLite.
     */
    public int importGateDataStore(File gateDataStoreDir) {
        if (gateDataStoreDir == null || !gateDataStoreDir.isDirectory()) return 0;

        File marker = new File(gateDataStoreDir, "__GATE_SerialDataStore__");
        if (!marker.exists()) {
            System.err.println("Le dossier sélectionné n'est pas un SerialDataStore GATE valide.");
            return 0;
        }

        int importedCount = 0;
        DataStore ds = null;

        try {
            if (!Gate.isInitialised()) {
                Gate.init();
            }

            ds = Factory.openDataStore("gate.persist.SerialDataStore", gateDataStoreDir.toURI().toURL().toString());

            Path dataFolderPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataFolderPath)) {
                Files.createDirectories(dataFolderPath);
            }

            Set<String> processedDocNames = new HashSet<>();

            // 1. Traitement des Corpus enregistrés dans le DataStore
            @SuppressWarnings("unchecked")
            List<String> corpusIds = (List<String>) ds.getLrIds("gate.corpora.SerialCorpusImpl");

            for (String corpusId : corpusIds) {
                Corpus corpus = (Corpus) ds.getLr("gate.corpora.SerialCorpusImpl", corpusId);

                if (corpus != null) {
                    String authorName = corpus.getName();
                    int authorId = getOrCreateAuthorId(authorName);

                    for (int i = 0; i < corpus.size(); i++) {
                        gate.Document doc = corpus.get(i);
                        if (doc != null) {
                            String docName = doc.getName();
                            String content = doc.getContent().toString();

                            Path localFile = dataFolderPath.resolve(docName + ".txt");
                            try (FileWriter writer = new FileWriter(localFile.toFile())) {
                                writer.write(content);
                            }

                            Text text = new Text(0, authorId, docName, localFile.toString());
                            if (analysisDao.insertText(text) != -1) {
                                importedCount++;
                            }

                            // Mémorise le nom pour ne pas réimporter le doc s'il est aussi dans les docs isolés
                            processedDocNames.add(docName);

                            Factory.deleteResource(doc);
                        }
                    }
                    Factory.deleteResource(corpus);
                }
            }

            // 2. Traitement des documents isolés (non rattachés à un corpus)
            @SuppressWarnings("unchecked")
            List<String> docIds = (List<String>) ds.getLrIds("gate.corpora.DocumentImpl");

            for (String docId : docIds) {
                gate.Document doc = (gate.Document) ds.getLr("gate.corpora.DocumentImpl", docId);
                if (doc != null) {
                    String docName = doc.getName();

                    if (!processedDocNames.contains(docName)) {
                        String content = doc.getContent().toString();
                        String authorName = doc.getFeatures().containsKey("author")
                                ? doc.getFeatures().get("author").toString()
                                : docName;

                        Path localFile = dataFolderPath.resolve(docName + ".txt");
                        try (FileWriter writer = new FileWriter(localFile.toFile())) {
                            writer.write(content);
                        }

                        int authorId = getOrCreateAuthorId(authorName);

                        Text text = new Text(0, authorId, docName, localFile.toString());
                        if (analysisDao.insertText(text) != -1) {
                            importedCount++;
                        }
                    }

                    Factory.deleteResource(doc);
                }
            }

        } catch (GateException | IOException e) {
            System.err.println("Erreur lors de l'import du DataStore GATE : " + e.getMessage());
        } finally {
            if (ds != null) {
                try {
                    ds.close();
                } catch (PersistenceException e) {
                    System.err.println("Erreur à la fermeture du DataStore : " + e.getMessage());
                }
            }
        }

        return importedCount;
    }

    private int getOrCreateAuthorId(String authorName) {
        if (authorName == null || authorName.trim().isEmpty()) {
            return -1;
        }
        String cleanName = authorName.trim();

        if (cleanName.toLowerCase().startsWith("corpus ")) {
            cleanName = cleanName.substring(7).trim();
        }

        Author author = analysisDao.getAuthorByName(cleanName);
        if (author != null) {
            return author.getId();
        }

        Author newAuthor = new Author(0, cleanName, null, null, null);
        return analysisDao.insertAuthor(newAuthor);
    }
}