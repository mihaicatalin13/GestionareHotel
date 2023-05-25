package model;

import database.DatabaseService;

import java.sql.*;

public class Employee {
    private int id;
    private String name;
    private String position;
    private double salary;
    private int yearsOfService;
    private static Connection connection = DatabaseService.getInstance().getConnection();

    public Employee(int id, String name, String position, double salary, int yearsOfService) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.salary = salary;
        this.yearsOfService = yearsOfService;
    }

    public Employee(int employeeId, String name, String position, double salary) {
        this.id = employeeId;
        this.name = name;
        this.position = position;
        this.salary = salary;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public int getYearsOfService() {
        return yearsOfService;
    }

    public void setYearsOfService(int yearsOfService) {
        this.yearsOfService = yearsOfService;
    }

    public static void createEmployee(String name, String position, double salary) throws SQLException {
        String query = "INSERT INTO employees (name, position, salary) VALUES (?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, name);
        statement.setString(2, position);
        statement.setDouble(3, salary);
        int rowsAffected = statement.executeUpdate();

        if (rowsAffected > 0) {
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int employeeId = generatedKeys.getInt(1);
                System.out.println("Employee successfully created. Employee ID: " + employeeId);
            }
        } else {
            System.out.println("Failed to create the employee.");
        }
    }

    // Read an employee by ID
    public static Employee readEmployee(int employeeId) throws SQLException {
        String query = "SELECT * FROM employees WHERE employee_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, employeeId);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            String name = resultSet.getString("name");
            String position = resultSet.getString("position");
            double salary = resultSet.getDouble("salary");

            return new Employee(employeeId, name, position, salary);
        } else {
            System.out.println("Employee not found.");
        }
        return null;
    }

    // Update an employee
    public void updateEmployee() throws SQLException {
        String query = "UPDATE employees SET name = ?, position = ?, salary = ? WHERE employee_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        statement.setString(2, position);
        statement.setDouble(3, salary);
        statement.setInt(4, id);
        int rowsAffected = statement.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Employee successfully updated.");
        } else {
            System.out.println("Failed to update the employee.");
        }
    }

    // Delete an employee
    public static void deleteEmployee(int employeeId) throws SQLException {
        String query = "DELETE FROM employees WHERE employee_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, employeeId);
        int rowsAffected = statement.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Employee successfully deleted.");
        } else {
            System.out.println("Failed to delete the employee.");
        }
    }
}
