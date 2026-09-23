package fr.unicaen.model;

public class Author {
    private int id;
    private String name;
    private String wikidataUri;
    private String birthDate;
    private String movement;

    public Author(int id, String name, String wikidataUri, String birthDate, String movement) {
        this.id = id;
        this.name = name;
        this.wikidataUri = wikidataUri;
        this.birthDate = birthDate;
        this.movement = movement;
    }

    public Author(int id, String name) {
        this(id, name, null, null, null);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getWikidataUri() { return wikidataUri; }
    public String getBirthDate() { return birthDate; }
    public String getMovement() { return movement; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setWikidataUri(String wikidataUri) { this.wikidataUri = wikidataUri; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public void setMovement(String movement) { this.movement = movement; }

    @Override
    public String toString() {
        if (movement != null && !movement.isBlank()) {
            return name + " (" + movement + ")";
        }
        return name;
    }
}