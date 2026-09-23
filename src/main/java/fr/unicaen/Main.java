package fr.unicaen;

import java.io.OutputStream;
import java.io.PrintStream;
import fr.unicaen.database.DatabaseManager;
public class Main {

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();
        // Redirection temporaire de System.err pour intercepter le WARNING JavaFX au startup
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // Ignore le flux pendant l'initialisation de JavaFX
            }
        }));

        // Restauration du flux System.err après 1 seconde (une fois JavaFX démarré)
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            System.setErr(originalErr);
        }).start();

        // Lancement standard de l'application JavaFX
        App.main(args);
    }
}