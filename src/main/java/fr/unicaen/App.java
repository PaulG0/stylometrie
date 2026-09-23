package fr.unicaen;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));

        // Masquage de la barre système native Windows
        stage.initStyle(StageStyle.UNDECORATED);

        // Définition de l'icône personnalisée dans la barre des tâches
        stage.getIcons().add(createApplicationIcon());

        Scene scene = new Scene(fxmlLoader.load(), 980, 700);
        stage.setTitle("Stylométrie - UNICAEN L3");
        stage.setScene(scene);

        // Déplacement de la fenêtre à la souris
        scene.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        scene.setOnMouseDragged(event -> {
            if (event.getSceneY() < 60) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        stage.show();
    }

    /**
     * Génère dynamiquement l'icône Fleur de Lys Or pour la barre des tâches
     */
    private Image createApplicationIcon() {
        Canvas canvas = new Canvas(64, 64);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond bleu nuit royal arrondi
        gc.setFill(Color.web("#0a1320"));
        gc.fillRoundRect(0, 0, 64, 64, 16, 16);

        // Contour Or
        gc.setStroke(Color.web("#d4af37"));
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(1, 1, 62, 62, 16, 16);

        // Symbole Fleur de Lys Or
        gc.setFill(Color.web("#f1c40f"));
        gc.setFont(Font.font("Georgia", 40));
        gc.fillText("⚜", 14, 47);

        WritableImage image = new WritableImage(64, 64);
        canvas.snapshot(null, image);
        return image;
    }

    public static void main(String[] args) {
        launch(args);
    }
}