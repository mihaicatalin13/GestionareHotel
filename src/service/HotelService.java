package service;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

import database.DatabaseService;
import model.Client;
import model.Reservation;

public class HotelService {
    private static Scanner scanner;
    private static Connection connection;

    public HotelService() {
        scanner = new Scanner(System.in);
        this.connection = DatabaseService.getInstance().getConnection();
    }

    public static void makeReservation() throws SQLException {
        System.out.println("Enter the room ID:");
        int roomId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        System.out.println("Enter the client ID:");
        int clientId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        System.out.println("Enter the start date (YYYY-MM-DD):");
        String startDate = scanner.nextLine();

        System.out.println("Enter the end date (YYYY-MM-DD):");
        String endDate = scanner.nextLine();

        // Check if the room is available for the specified time range
        if (!isRoomAvailable(roomId, startDate, endDate, connection)) {
            System.out.println("The room is not available for the specified time range.");
            return;
        }

        // Check if the client has any overlapping reservations
        if (hasOverlappingReservation(clientId, startDate, endDate, connection)) {
            System.out.println("The client already has a reservation overlapping with the specified time range.");
            return;
        }

        // Insert the new reservation into the reservations table
        String insertReservationQuery = "INSERT INTO reservations (client_id, room_id, start_date, end_date, status, created_at) " +
                "VALUES (?, ?, ?, ?, 'Confirmed', NOW())";

        String insertBillQuery = "INSERT INTO bills (reservation_id, amount, created_at) " +
                "VALUES (LAST_INSERT_ID(), ?, NOW())";

        PreparedStatement reservationStatement = connection.prepareStatement(insertReservationQuery);
        reservationStatement.setInt(1, clientId);
        reservationStatement.setInt(2, roomId);
        reservationStatement.setString(3, startDate);
        reservationStatement.setString(4, endDate);
        int rowsAffected = reservationStatement.executeUpdate();

        if (rowsAffected > 0) {
            // Insert the bill for the reservation
            PreparedStatement billStatement = connection.prepareStatement(insertBillQuery, Statement.RETURN_GENERATED_KEYS);
            billStatement.setDouble(1, calculateBillAmount(roomId, startDate, endDate));
            int billRowsAffected = billStatement.executeUpdate();

            if (billRowsAffected > 0) {
                ResultSet generatedKeys = billStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int billId = generatedKeys.getInt(1);
                    System.out.println("Reservation successfully made. Bill ID: " + billId);
                } else {
                    System.out.println("Failed to retrieve the bill ID.");
                }
            } else {
                System.out.println("Failed to create the bill for the reservation.");
            }
        } else {
            System.out.println("Failed to make the reservation.");
        }
    }

    private static double calculateBillAmount(int roomId, String startDate, String endDate) throws SQLException {
        double roomRate = getRoomRate(roomId);
        int numberOfDays = calculateNumberOfDays(startDate, endDate);
        return roomRate * numberOfDays;
    }

    private static double getRoomRate(int roomId) throws SQLException {
        double roomRate = 0.0;
        String query = "SELECT price_per_night FROM rooms WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            roomRate = resultSet.getDouble("price_per_night");
        }
        return roomRate;
    }

    private static int calculateNumberOfDays(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return (int) ChronoUnit.DAYS.between(start, end);
    }


    private static boolean isRoomAvailable(int roomId, String startDate, String endDate, Connection connection)
            throws SQLException {
        String query = "SELECT id FROM reservations " +
                "WHERE room_id = ? AND start_date <= ? AND end_date >= ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        statement.setString(2, endDate);
        statement.setString(3, startDate);
        ResultSet resultSet = statement.executeQuery();

        return !resultSet.next();
    }

    private static boolean hasOverlappingReservation(int clientId, String startDate, String endDate, Connection connection)
            throws SQLException {
        String query = "SELECT id FROM reservations " +
                "WHERE client_id = ? AND start_date <= ? AND end_date >= ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, clientId);
        statement.setString(2, endDate);
        statement.setString(3, startDate);
        ResultSet resultSet = statement.executeQuery();

        return resultSet.next();
    }

    public void cancelReservation() throws SQLException {
        System.out.println("Enter the reservation ID:");
        int reservationId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        // Delete the corresponding bill for the reservation
        String deleteBillQuery = "DELETE FROM bills WHERE reservation_id = ?";

        PreparedStatement billStatement = connection.prepareStatement(deleteBillQuery);
        billStatement.setInt(1, reservationId);
        int billRowsAffected = billStatement.executeUpdate();

        if (billRowsAffected > 0) {
            // Cancel the reservation
            String cancelReservationQuery = "UPDATE reservations SET status = 'Cancelled' WHERE id = ?";
            PreparedStatement reservationStatement = connection.prepareStatement(cancelReservationQuery);
            reservationStatement.setInt(1, reservationId);
            int reservationRowsAffected = reservationStatement.executeUpdate();

            if (reservationRowsAffected > 0) {
                System.out.println("Reservation cancelled successfully.");
            } else {
                System.out.println("Failed to cancel the reservation.");
            }
        } else {
            System.out.println("Failed to delete the bill for the reservation.");
        }
    }

    public void extendReservation() throws SQLException {
        System.out.println("Enter the reservation ID:");
        int reservationId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        System.out.println("Enter the new end date (YYYY-MM-DD):");
        String newEndDate = scanner.nextLine();

        // Check if the reservation exists and is not cancelled
        if (!isReservationValid(reservationId)) {
            System.out.println("The reservation does not exist or has been cancelled.");
            return;
        }

        // Calculate the new bill amount
        double newBillAmount = calculateBillAmountForExtension(reservationId, newEndDate);

        // Update the reservation end date
        String updateReservationQuery = "UPDATE reservations SET end_date = ? WHERE id = ?";
        PreparedStatement reservationStatement = connection.prepareStatement(updateReservationQuery);
        reservationStatement.setString(1, newEndDate);
        reservationStatement.setInt(2, reservationId);
        int reservationRowsAffected = reservationStatement.executeUpdate();

        if (reservationRowsAffected > 0) {
            // Update the bill with the new amount
            String updateBillQuery = "UPDATE bills SET amount = ? WHERE reservation_id = ?";
            PreparedStatement billStatement = connection.prepareStatement(updateBillQuery);
            billStatement.setDouble(1, newBillAmount);
            billStatement.setInt(2, reservationId);
            int billRowsAffected = billStatement.executeUpdate();

            if (billRowsAffected > 0) {
                System.out.println("Reservation extended successfully. New bill amount: " + newBillAmount);
            } else {
                System.out.println("Failed to update the bill for the reservation.");
            }
        } else {
            System.out.println("Failed to extend the reservation.");
        }
    }

    private static boolean isReservationValid(int reservationId) throws SQLException {
        String selectReservationQuery = "SELECT id FROM reservations WHERE id = ? AND status != 'Cancelled'";
        PreparedStatement selectReservationStatement = connection.prepareStatement(selectReservationQuery);
        selectReservationStatement.setInt(1, reservationId);
        ResultSet reservationResultSet = selectReservationStatement.executeQuery();

        return reservationResultSet.next();
    }

    private static double calculateBillAmountForExtension(int reservationId, String newEndDate) throws SQLException {
        // Retrieve the existing bill amount and start date for the reservation
        String selectBillQuery = "SELECT amount, start_date FROM bills JOIN reservations ON bills.reservation_id = reservations.id WHERE reservations.id = ?";
        PreparedStatement selectBillStatement = connection.prepareStatement(selectBillQuery);
        selectBillStatement.setInt(1, reservationId);
        ResultSet billResultSet = selectBillStatement.executeQuery();

        if (billResultSet.next()) {
            double existingAmount = billResultSet.getDouble("amount");
            String startDate = billResultSet.getString("start_date");
            System.out.println("start_date: " + startDate);
            System.out.println("end_date: " + newEndDate);


            // Calculate the number of days to extend the reservation
            int numberOfDays = calculateNumberOfDays(startDate, newEndDate);
            System.out.println("no_days " + numberOfDays);
            // Calculate the new bill amount based on the existing amount and number of days
            double newAmount = existingAmount * numberOfDays;

            return newAmount;
        } else {
            System.out.println("No bill found for the reservation ID: " + reservationId);
        }

        return 0; // Return a default value in case of error
    }

    public void displayAvailableRooms() throws SQLException {
        System.out.println("Enter the start date (YYYY-MM-DD):");
        String startDate = scanner.nextLine();

        System.out.println("Enter the end date (YYYY-MM-DD):");
        String endDate = scanner.nextLine();

        String query = "SELECT * FROM rooms WHERE id NOT IN (SELECT room_id FROM reservations WHERE start_date <= ? AND end_date >= ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, endDate);
        statement.setString(2, startDate);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Available Rooms:");
        while (resultSet.next()) {
            System.out.println("Room ID: " + resultSet.getInt("id"));
            System.out.println("Room Number: " + resultSet.getString("number"));
            System.out.println("Room Type: " + resultSet.getString("type"));
            System.out.println("------------------------");
        }
    }

    public void addClient() throws SQLException {
        System.out.println("Enter the client name:");
        String name = scanner.nextLine();

        System.out.println("Enter the client email:");
        String email = scanner.nextLine();

        System.out.println("Enter the client phone:");
        String phone = scanner.nextLine();

        // Insert the new client into the clients table
        String query = "INSERT INTO clients (name, email, phone) VALUES (?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, name);
        statement.setString(2, email);
        statement.setString(3, phone);
        int rowsAffected = statement.executeUpdate();

        if (rowsAffected > 0) {
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int clientId = generatedKeys.getInt(1);
                System.out.println("Client successfully added. Client ID: " + clientId);
            }
        } else {
            System.out.println("Failed to add the client.");
        }
    }

    public void checkRoomAvailability() throws SQLException {
        System.out.println("Enter the room ID:");
        int roomId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        System.out.println("Enter the start date (YYYY-MM-DD):");
        String startDate = scanner.nextLine();

        System.out.println("Enter the end date (YYYY-MM-DD):");
        String endDate = scanner.nextLine();

        String query = "SELECT id FROM reservations " +
                "WHERE room_id = ? AND start_date <= ? AND end_date >= ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        statement.setString(2, endDate);
        statement.setString(3, startDate);
        ResultSet resultSet = statement.executeQuery();

        if (!resultSet.next())
            System.out.println("Room is available!");
        else
            System.out.println("Room is not available!");
    }

    public void displayGuestsCheckedIn() throws SQLException {
        String query = "SELECT * FROM reservations INNER JOIN clients ON reservations.client_id = clients.id WHERE start_date <= CURRENT_DATE() AND end_date >= CURRENT_DATE()";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        System.out.println("Guests Checked In:");
        while (resultSet.next()) {
            System.out.println("Reservation ID: " + resultSet.getInt("reservations.id"));
            System.out.println("Client Name: " + resultSet.getString("clients.name"));
            System.out.println("Room ID: " + resultSet.getInt("reservations.room_id"));
            System.out.println("Start Date: " + resultSet.getString("reservations.start_date"));
            System.out.println("End Date: " + resultSet.getString("reservations.end_date"));
            System.out.println("------------------------");
        }
    }

    public void displayNegativeReviews() throws SQLException {
        String query = "SELECT * FROM reviews WHERE rating < 3";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        System.out.println("Negative Reviews:");
        while (resultSet.next()) {
            System.out.println("Review ID: " + resultSet.getInt("id"));
            System.out.println("Reservation ID: " + resultSet.getInt("reservation_id"));
            System.out.println("Rating: " + resultSet.getInt("rating"));
            System.out.println("Comment: " + resultSet.getString("comment"));
            System.out.println("------------------------");
        }
    }

    public void checkTotalRevenue() throws SQLException {
        System.out.println("Enter the start date (YYYY-MM-DD):");
        String startDate = scanner.nextLine();

        System.out.println("Enter the end date (YYYY-MM-DD):");
        String endDate = scanner.nextLine();

        String query = "SELECT SUM(b.amount) AS total_revenue " +
                "FROM bills b " +
                "LEFT JOIN reservations r ON b.reservation_id = r.id " +
                "WHERE r.start_date >= ? AND r.end_date <= ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, startDate);
        statement.setString(2, endDate);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            double totalRevenue = resultSet.getDouble("total_revenue");
            System.out.println("Total Revenue for the specified time range: $" + totalRevenue);
        } else {
            System.out.println("No revenue data available for the specified time range.");
        }
    }

    public static void checkEmployeeSalaryIncreaseEligibility() throws SQLException {
        System.out.println("Enter the employee ID:");
        int employeeId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        String query = "SELECT * FROM employees WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, employeeId);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Employee Salary Increase Eligibility:");

        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            String position = resultSet.getString("position");
            double salary = resultSet.getDouble("salary");
            LocalDate hireDate = resultSet.getDate("hire_date").toLocalDate();
            LocalDate currentDate = LocalDate.now();
            int yearsOfService = currentDate.getYear() - hireDate.getYear();

            // Check eligibility for salary increase
            boolean isEligible = yearsOfService >= 2 && salary < 5000.0;

            System.out.println("Employee ID: " + id);
            System.out.println("Name: " + name);
            System.out.println("Position: " + position);
            System.out.println("Salary: $" + salary);
            System.out.println("Years of Service: " + yearsOfService);
            System.out.println("Eligible for Salary Increase: " + isEligible);
            System.out.println();
        }
    }
}