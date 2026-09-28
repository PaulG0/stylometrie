package fr.unicaen.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle encapsulant les métadonnées enrichies récupérées en ligne depuis Wikidata et Wikipédia.
 */
public class AuthorEnrichedData {

    private String wikidataId;
    private String wikidataUri;
    private String wikipediaUrl;
    private String imageUrl;
    private String label;
    private String description;
    private String biographyExtract;
    private String birthDate;
    private String birthPlace;
    private String deathDate;
    private String deathPlace;
    private String citizenship;

    private List<String> occupations = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<String> movements = new ArrayList<>();
    private List<String> awards = new ArrayList<>();
    private List<String> notableWorks = new ArrayList<>();

    public AuthorEnrichedData() {}

    public String getWikidataId() { return wikidataId; }
    public void setWikidataId(String wikidataId) { this.wikidataId = wikidataId; }

    public String getWikidataUri() { return wikidataUri; }
    public void setWikidataUri(String wikidataUri) { this.wikidataUri = wikidataUri; }

    public String getWikipediaUrl() { return wikipediaUrl; }
    public void setWikipediaUrl(String wikipediaUrl) { this.wikipediaUrl = wikipediaUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBiographyExtract() { return biographyExtract; }
    public void setBiographyExtract(String biographyExtract) { this.biographyExtract = biographyExtract; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }

    public String getDeathDate() { return deathDate; }
    public void setDeathDate(String deathDate) { this.deathDate = deathDate; }

    public String getDeathPlace() { return deathPlace; }
    public void setDeathPlace(String deathPlace) { this.deathPlace = deathPlace; }

    public String getCitizenship() { return citizenship; }
    public void setCitizenship(String citizenship) { this.citizenship = citizenship; }

    public List<String> getOccupations() { return occupations; }
    public void setOccupations(List<String> occupations) { this.occupations = occupations; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getMovements() { return movements; }
    public void setMovements(List<String> movements) { this.movements = movements; }

    public List<String> getAwards() { return awards; }
    public void setAwards(List<String> awards) { this.awards = awards; }

    public List<String> getNotableWorks() { return notableWorks; }
    public void setNotableWorks(List<String> notableWorks) { this.notableWorks = notableWorks; }
}
