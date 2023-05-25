package model;

import database.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Client extends Person {
    private int scorFidelitate;
    private static Connection connection = DatabaseService.getInstance().getConnection();

    public int getScorFidelitate() {
        return scorFidelitate;
    }

    public void setScorFidelitate(int scorFidelitate) {
        this.scorFidelitate = scorFidelitate;
    }

    public void save() throws SQLException {
        String query = "INSERT INTO clients (name, email) VALUES (?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        statement.setString(2, email);
        statement.executeUpdate();
        System.out.println("Client saved successfully.");
    }

    public void update() throws SQLException {
        String query = "UPDATE clients SET name = ?, email = ? WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        statement.setString(2, email);
        statement.setInt(3, id);
        statement.executeUpdate();
        System.out.println("Client updated successfully.");
    }

    public void delete() throws SQLException {
        String query = "DELETE FROM clients WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        statement.executeUpdate();
        System.out.println("Client deleted successfully.");
    }

    public static Client getById(int id) throws SQLException {
        Client client = null;
            String query = "SELECT * FROM clients WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                client = new Client();
                client.setId(resultSet.getInt("id"));
                client.setName(resultSet.getString("name"));
                client.setEmail(resultSet.getString("email"));
            }

        return client;
    }
}