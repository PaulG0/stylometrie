package fr.unicaen.model;

public class Text {
    private int id;
    private int authorId;
    private String title;
    private String authorName; // Champ pratique pour l'affichage direct dans le TableView

    public Text(int id, int authorId, String title, String authorName) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.authorName = authorName;
    }

    public int getId() { return id; }
    public int getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getAuthorName() { return authorName; }

    public void setId(int id) { this.id = id; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
}