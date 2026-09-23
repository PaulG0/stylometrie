package fr.unicaen.service;

import fr.unicaen.database.AnalysisDao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.SQLException;
import fr.unicaen.model.Author;
import fr.unicaen.model.Text;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CsvImportService {

    private final AnalysisDao analysisDao = new AnalysisDao();

    public int importAuthorsFromCsv(File csvFile) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine(); // Passer l'en-tête (auteur,auteurLabel,dateNaissance,mouvementLabel)

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Gestion basique du CSV par découpage
                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (tokens.length >= 2) {
                    String uri = cleanToken(tokens[0]);
                    String name = cleanToken(tokens[1]);
                    String birthDate = tokens.length > 2 ? cleanToken(tokens[2]) : null;
                    String movement = tokens.length > 3 ? cleanToken(tokens[3]) : null;

                    // Formater la date (ex: 1888-03-13T00:00:00Z -> 1888)
                    if (birthDate != null && birthDate.contains("-")) {
                        birthDate = birthDate.split("-")[0];
                    }

                    analysisDao.insertOrUpdateAuthor(name, uri, birthDate, movement);
                    count++;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur d'importation CSV : " + e.getMessage());
        }
        return count;
    }

    private String cleanToken(String token) {
        return token.replaceAll("^\"|\"$", "").trim();
    }
}