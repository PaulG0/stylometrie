package fr.unicaen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.unicaen.model.AuthorEnrichedData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service pour interroger l'API Wikidata et l'API Wikipédia REST
 * afin d'enrichir la fiche d'un auteur avec ses données personnelles,
 * biographie, œuvres notables, récompenses et portraits.
 */
public class AuthorMetadataService {

    private static final String USER_AGENT = "StylometrieUNICAEN/1.0 (https://unicaen.fr; contact-projet-l3@unicaen.fr)";
    private static final ConcurrentHashMap<String, AuthorEnrichedData> cache = new ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AuthorMetadataService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Récupération asynchrone des métadonnées enrichies d'un auteur.
     */
    public CompletableFuture<AuthorEnrichedData> fetchEnrichedDataAsync(String wikidataUri, String authorName) {
        return CompletableFuture.supplyAsync(() -> fetchEnrichedData(wikidataUri, authorName));
    }

    /**
     * Récupération synchrone des métadonnées enrichies.
     */
    public AuthorEnrichedData fetchEnrichedData(String wikidataUri, String authorName) {
        String qid = extractQid(wikidataUri);
        if (qid == null || qid.isBlank()) {
            AuthorEnrichedData fallback = new AuthorEnrichedData();
            fallback.setLabel(authorName);
            fallback.setDescription("Aucun identifiant Wikidata associé.");
            return fallback;
        }

        if (cache.containsKey(qid)) {
            return cache.get(qid);
        }

        AuthorEnrichedData data = new AuthorEnrichedData();
        data.setWikidataId(qid);
        data.setWikidataUri(wikidataUri != null && !wikidataUri.isBlank() ? wikidataUri : "https://www.wikidata.org/wiki/" + qid);
        data.setLabel(authorName);

        try {
            // 1. Interrogation de l'API Wikidata
            String wikidataApiUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + qid
                    + "&languages=fr%7Cen&props=labels%7Cdescriptions%7Cclaims%7Csitelinks&format=json";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(wikidataApiUrl))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode entities = root.path("entities");
                JsonNode entity = entities.path(qid);

                if (!entity.isMissingNode()) {
                    parseWikidataEntity(entity, data);
                }
            }

            // 2. Interrogation de l'API Wikipédia REST pour la biographie et photo HD si disponible
            if (data.getWikipediaUrl() != null && !data.getWikipediaUrl().isBlank()) {
                enrichFromWikipedia(data);
            }

            cache.put(qid, data);

        } catch (Exception e) {
            System.err.println("Avertissement lors de la récupération Wikidata pour " + authorName + " (" + qid + "): " + e.getMessage());
            if (data.getDescription() == null || data.getDescription().isBlank()) {
                data.setDescription("Données indisponibles (hors ligne ou délai dépassé).");
            }
        }

        return data;
    }

    private void parseWikidataEntity(JsonNode entity, AuthorEnrichedData data) {
        // Label
        JsonNode labels = entity.path("labels");
        if (labels.has("fr")) {
            data.setLabel(labels.path("fr").path("value").asText());
        } else if (labels.has("en")) {
            data.setLabel(labels.path("en").path("value").asText());
        }

        // Description courte
        JsonNode descs = entity.path("descriptions");
        if (descs.has("fr")) {
            data.setDescription(descs.path("fr").path("value").asText());
        } else if (descs.has("en")) {
            data.setDescription(descs.path("en").path("value").asText());
        }

        // Sitelink Wikipédia
        JsonNode sitelinks = entity.path("sitelinks");
        if (sitelinks.has("frwiki")) {
            String title = sitelinks.path("frwiki").path("title").asText();
            data.setWikipediaUrl("https://fr.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8));
        }

        JsonNode claims = entity.path("claims");

        // P18 : Image Commons
        if (claims.has("P18")) {
            JsonNode p18 = claims.path("P18");
            if (p18.isArray() && p18.size() > 0) {
                String imageName = p18.get(0).path("mainsnak").path("datavalue").path("value").asText();
                if (imageName != null && !imageName.isBlank()) {
                    String cleanName = imageName.replace(" ", "_");
                    data.setImageUrl("https://commons.wikimedia.org/wiki/Special:FilePath/" + URLEncoder.encode(cleanName, StandardCharsets.UTF_8) + "?width=400");
                }
            }
        }

        // P569 : Date de naissance
        if (claims.has("P569")) {
            data.setBirthDate(extractFormattedDate(claims.path("P569")));
        }

        // P570 : Date de décès
        if (claims.has("P570")) {
            data.setDeathDate(extractFormattedDate(claims.path("P570")));
        }

        // Collecte des identifiants QID à résoudre pour les labels français
        Set<String> qidsToResolve = new LinkedHashSet<>();

        String birthPlaceQid = extractFirstEntityId(claims.path("P19"));
        if (birthPlaceQid != null) qidsToResolve.add(birthPlaceQid);

        String deathPlaceQid = extractFirstEntityId(claims.path("P20"));
        if (deathPlaceQid != null) qidsToResolve.add(deathPlaceQid);

        String citizenshipQid = extractFirstEntityId(claims.path("P27"));
        if (citizenshipQid != null) qidsToResolve.add(citizenshipQid);

        List<String> occupationQids = extractEntityIds(claims.path("P106"), 5);
        qidsToResolve.addAll(occupationQids);

        List<String> genreQids = extractEntityIds(claims.path("P136"), 5);
        qidsToResolve.addAll(genreQids);

        List<String> movementQids = extractEntityIds(claims.path("P135"), 4);
        qidsToResolve.addAll(movementQids);

        List<String> awardQids = extractEntityIds(claims.path("P166"), 5);
        qidsToResolve.addAll(awardQids);

        List<String> workQids = extractEntityIds(claims.path("P800"), 8);
        qidsToResolve.addAll(workQids);

        // Résolution groupée des labels pour tous les QIDs identifiés
        Map<String, String> resolvedLabels = batchResolveLabels(qidsToResolve);

        if (birthPlaceQid != null && resolvedLabels.containsKey(birthPlaceQid)) {
            data.setBirthPlace(resolvedLabels.get(birthPlaceQid));
        }
        if (deathPlaceQid != null && resolvedLabels.containsKey(deathPlaceQid)) {
            data.setDeathPlace(resolvedLabels.get(deathPlaceQid));
        }
        if (citizenshipQid != null && resolvedLabels.containsKey(citizenshipQid)) {
            data.setCitizenship(resolvedLabels.get(citizenshipQid));
        }

        for (String id : occupationQids) {
            if (resolvedLabels.containsKey(id)) data.getOccupations().add(resolvedLabels.get(id));
        }
        for (String id : genreQids) {
            if (resolvedLabels.containsKey(id)) data.getGenres().add(resolvedLabels.get(id));
        }
        for (String id : movementQids) {
            if (resolvedLabels.containsKey(id)) data.getMovements().add(resolvedLabels.get(id));
        }
        for (String id : awardQids) {
            if (resolvedLabels.containsKey(id)) data.getAwards().add(resolvedLabels.get(id));
        }
        for (String id : workQids) {
            if (resolvedLabels.containsKey(id)) data.getNotableWorks().add(resolvedLabels.get(id));
        }
    }

    private void enrichFromWikipedia(AuthorEnrichedData data) {
        try {
            String wpUrl = data.getWikipediaUrl();
            String pageTitle = wpUrl.substring(wpUrl.lastIndexOf('/') + 1);

            String summaryApiUrl = "https://fr.wikipedia.org/api/rest_v1/page/summary/" + pageTitle;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(summaryApiUrl))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode summary = objectMapper.readTree(resp.body());

                if (summary.has("extract") && !summary.path("extract").asText().isBlank()) {
                    data.setBiographyExtract(summary.path("extract").asText());
                }

                if ((data.getDescription() == null || data.getDescription().isBlank()) && summary.has("description")) {
                    data.setDescription(summary.path("description").asText());
                }

                // Si pas d'image Wikidata, on prend celle de Wikipédia
                if (data.getImageUrl() == null && summary.has("thumbnail")) {
                    data.setImageUrl(summary.path("thumbnail").path("source").asText());
                }
            }
        } catch (Exception ignored) {
            // Enrichissement Wikipédia optionnel
        }
    }

    /**
     * Résout les libellés en français pour un ensemble de QIDs Wikidata en un seul appel HTTP.
     */
    private Map<String, String> batchResolveLabels(Set<String> qids) {
        Map<String, String> result = new HashMap<>();
        if (qids == null || qids.isEmpty()) return result;

        List<String> idList = new ArrayList<>(qids);
        int batchSize = 40;

        for (int i = 0; i < idList.size(); i += batchSize) {
            List<String> subList = idList.subList(i, Math.min(i + batchSize, idList.size()));
            String idsParam = String.join("%7C", subList);

            try {
                String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + idsParam
                        + "&props=labels&languages=fr%7Cen&format=json";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(resp.body());
                    JsonNode entities = root.path("entities");

                    for (String qid : subList) {
                        JsonNode ent = entities.path(qid);
                        if (!ent.isMissingNode()) {
                            JsonNode labels = ent.path("labels");
                            if (labels.has("fr")) {
                                result.put(qid, labels.path("fr").path("value").asText());
                            } else if (labels.has("en")) {
                                result.put(qid, labels.path("en").path("value").asText());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur résolution batch labels Wikidata : " + e.getMessage());
            }
        }

        return result;
    }

    private String extractFirstEntityId(JsonNode claimProperty) {
        if (claimProperty != null && claimProperty.isArray() && claimProperty.size() > 0) {
            JsonNode datavalue = claimProperty.get(0).path("mainsnak").path("datavalue");
            if (datavalue.has("value") && datavalue.path("value").has("id")) {
                return datavalue.path("value").path("id").asText();
            }
        }
        return null;
    }

    private List<String> extractEntityIds(JsonNode claimProperty, int maxCount) {
        List<String> list = new ArrayList<>();
        if (claimProperty != null && claimProperty.isArray()) {
            for (int i = 0; i < claimProperty.size() && list.size() < maxCount; i++) {
                JsonNode datavalue = claimProperty.get(i).path("mainsnak").path("datavalue");
                if (datavalue.has("value") && datavalue.path("value").has("id")) {
                    String id = datavalue.path("value").path("id").asText();
                    if (id != null && !id.isBlank() && !list.contains(id)) {
                        list.add(id);
                    }
                }
            }
        }
        return list;
    }

    private String extractFormattedDate(JsonNode claimProperty) {
        if (claimProperty != null && claimProperty.isArray() && claimProperty.size() > 0) {
            JsonNode timeNode = claimProperty.get(0).path("mainsnak").path("datavalue").path("value").path("time");
            if (!timeNode.isMissingNode()) {
                String timeStr = timeNode.asText(); // ex: "+1913-11-07T00:00:00Z" ou "+0925-00-00T00:00:00Z"
                return formatDateString(timeStr);
            }
        }
        return null;
    }

    private String formatDateString(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            // Nettoyage du préfixe '+'
            String raw = timeStr.startsWith("+") ? timeStr.substring(1) : timeStr;
            int tIndex = raw.indexOf('T');
            String datePart = tIndex > 0 ? raw.substring(0, tIndex) : raw;
            String[] parts = datePart.split("-");

            if (parts.length >= 3) {
                String year = parts[0];
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);

                if (month > 0 && day > 0) {
                    try {
                        LocalDate d = LocalDate.of(Integer.parseInt(year), month, day);
                        return d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
                    } catch (Exception e) {
                        return day + "/" + month + "/" + year;
                    }
                } else if (month > 0) {
                    return month + "/" + year;
                } else {
                    return year;
                }
            } else if (parts.length == 1) {
                return parts[0];
            }
        } catch (Exception ignored) {}
        return timeStr;
    }

    public static String extractQid(String wikidataUri) {
        if (wikidataUri == null || wikidataUri.isBlank()) return null;
        String trimmed = wikidataUri.trim();
        int lastSlash = trimmed.lastIndexOf('/');
        String candidate = (lastSlash >= 0) ? trimmed.substring(lastSlash + 1) : trimmed;
        if (candidate.matches("Q\\d+")) {
            return candidate;
        }
        return null;
    }
}
