package model;

import database.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Room {
    private int id;
    private String number;
    private String type;
    private static Connection connection = DatabaseService.getInstance().getConnection();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void save() throws SQLException {
        String query = "INSERT INTO rooms (number, type) VALUES (?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, number);
        statement.setString(2, type);
        statement.executeUpdate();
        System.out.println("Room saved successfully.");
    }

    public void update() throws SQLException {
        String query = "UPDATE rooms SET number = ?, type = ? WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, number);
        statement.setString(2, type);
        statement.setInt(3, id);
        statement.executeUpdate();
        System.out.println("Room updated successfully.");
    }

    public void delete() throws SQLException {
        String query = "DELETE FROM rooms WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        statement.executeUpdate();
        System.out.println("Room deleted successfully.");
    }

    public static Room getById(int id) throws SQLException {
        Room room = null;
        String query = "SELECT * FROM rooms WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            room = new Room();
            room.setId(resultSet.getInt("id"));
            room.setNumber(resultSet.getString("number"));
            room.setType(resultSet.getString("type"));
        }

        return room;
    }
}