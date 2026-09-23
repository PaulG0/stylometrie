package fr.unicaen.model;

public class Text {
    private int id;
    private int authorId;
    private String title;
    private String filePath;
    private String authorName; // Champ supplémentaire pour le nom de l'auteur

    public Text(int id, int authorId, String title, String filePath) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.filePath = filePath;
    }

    // Getters et Setters existants
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    // Nouveaux Getter/Setter pour le nom de l'auteur
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
}