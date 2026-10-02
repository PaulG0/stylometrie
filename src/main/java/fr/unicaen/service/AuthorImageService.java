package fr.unicaen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service haute performance pour le téléchargement et la mise en cache
 * des photos des auteurs depuis Wikimedia Commons et Wikipédia.
 */
public class AuthorImageService {

    private static final String USER_AGENT = "StylometrieUNICAEN/1.0 (https://unicaen.fr; contact-projet-l3@unicaen.fr)";
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> photoUrlCache = new ConcurrentHashMap<>();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void fetchAuthorPhotoAsync(String wikidataUri, String authorName, Consumer<Image> callback) {
        String qid = AuthorMetadataService.extractQid(wikidataUri);

        if (qid == null) {
            Image defaultImg = getOrCreateDefaultAvatar(authorName);
            Platform.runLater(() -> callback.accept(defaultImg));
            return;
        }

        if (imageCache.containsKey(qid)) {
            Image cached = imageCache.get(qid);
            Platform.runLater(() -> callback.accept(cached));
            return;
        }

        new Thread(() -> {
            try {
                String directImageUrl = photoUrlCache.get(qid);

                if (directImageUrl == null) {
                    String apiUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + qid
                            + "&props=claims&format=json";

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header("User-Agent", USER_AGENT)
                            .timeout(Duration.ofSeconds(6))
                            .GET()
                            .build();

                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                    if (resp.statusCode() == 200) {
                        JsonNode root = objectMapper.readTree(resp.body());
                        JsonNode p18 = root.path("entities").path(qid).path("claims").path("P18");

                        if (p18.isArray() && p18.size() > 0) {
                            String fileName = p18.get(0)
                                    .path("mainsnak")
                                    .path("datavalue")
                                    .path("value")
                                    .asText();

                            if (fileName != null && !fileName.isBlank()) {
                                String cleanName = fileName.replace(" ", "_");
                                directImageUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/"
                                        + URLEncoder.encode(cleanName, StandardCharsets.UTF_8)
                                        + "?width=350";
                                photoUrlCache.put(qid, directImageUrl);
                            }
                        }
                    }
                }

                if (directImageUrl != null) {
                    downloadAndCacheImage(qid, directImageUrl, authorName, callback);
                } else {
                    fallbackDefault(authorName, callback);
                }

            } catch (Exception e) {
                fallbackDefault(authorName, callback);
            }
        }).start();
    }

    public static void downloadAndCacheImage(String cacheKey, String imageUrl, String authorName, Consumer<Image> callback) {
        if (imageUrl == null || imageUrl.isBlank()) {
            fallbackDefault(authorName, callback);
            return;
        }

        if (imageCache.containsKey(cacheKey)) {
            Image cached = imageCache.get(cacheKey);
            Platform.runLater(() -> callback.accept(cached));
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest imgReq = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<byte[]> imgResp = httpClient.send(imgReq, HttpResponse.BodyHandlers.ofByteArray());

                if (imgResp.statusCode() == 200 && imgResp.body() != null && imgResp.body().length > 0) {
                    byte[] bytes = imgResp.body();

                    Platform.runLater(() -> {
                        try {
                            Image fxImage = new Image(new ByteArrayInputStream(bytes));

                            if (!fxImage.isError() && fxImage.getWidth() > 0) {
                                imageCache.put(cacheKey, fxImage);
                                callback.accept(fxImage);
                            } else {
                                fallbackDefault(authorName, callback);
                            }
                        } catch (Exception ex) {
                            fallbackDefault(authorName, callback);
                        }
                    });
                } else {
                    fallbackDefault(authorName, callback);
                }
            } catch (Exception e) {
                fallbackDefault(authorName, callback);
            }
        }).start();
    }

    private static void fallbackDefault(String authorName, Consumer<Image> callback) {
        Image fallback = getOrCreateDefaultAvatar(authorName);
        Platform.runLater(() -> callback.accept(fallback));
    }

    public static Image getOrCreateDefaultAvatar(String authorName) {
        String key = "default_" + (authorName != null ? authorName : "unknown");

        if (imageCache.containsKey(key)) {
            return imageCache.get(key);
        }

        try {
            Canvas canvas = new Canvas(140, 180);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.web("#0f1c2e"));
            gc.fillRect(0, 0, 140, 180);

            gc.setFill(Color.web("#18283d"));
            gc.fillRoundRect(8, 8, 124, 164, 12, 12);

            gc.setStroke(Color.web("#d4af37"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(8, 8, 124, 164, 12, 12);

            gc.setFill(Color.web("#f1c40f"));
            gc.setFont(Font.font("Georgia", FontWeight.BOLD, 42));
            gc.fillText("⚜", 48, 85);

            String initials = getInitials(authorName);

            gc.setFill(Color.web("#cbd5e1"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            gc.fillText(initials, 70 - (initials.length() * 4.5), 125);

            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText("Écrivain", 50, 145);

            WritableImage snap = new WritableImage(140, 180);
            canvas.snapshot(null, snap);
            imageCache.put(key, snap);

            return snap;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "A";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length >= 2) {
            return parts[0].substring(0, 1).toUpperCase()
                    + "."
                    + parts[parts.length - 1].substring(0, 1).toUpperCase()
                    + ".";
        }

        if (!parts[0].isBlank()) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return "A";
    }
}