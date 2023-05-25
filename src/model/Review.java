package model;

public class Review {
    private static int cntId = 0;
    private int id;
    private Client client;
    private String text;
    private int rating;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Review(Client client, String text, int rating) {
        this.id = ++cntId;
        this.client = client;
        this.text = text;
        this.rating = rating;
    }
}