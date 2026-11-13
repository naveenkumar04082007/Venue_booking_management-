package com.venue.dao;

import com.venue.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {
    public List<AdminBooking> getAllBookings() throws SQLException {
        String sql = "SELECT b.id, c.name AS customer_name, v.name AS venue_name, "
                + "b.event_date, b.start_time, b.end_time, b.status, b.total_amount, "
                + "b.advance_amount FROM bookings b "
                + "LEFT JOIN customers c ON c.id = b.customer_id "
                + "LEFT JOIN venues v ON v.id = b.venue_id "
                + "ORDER BY CASE WHEN upper(b.status) = 'PENDING' THEN 0 ELSE 1 END, "
                + "date(b.event_date), time(b.start_time)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<AdminBooking> bookings = new ArrayList<>();
            while (results.next()) {
                bookings.add(new AdminBooking(
                        results.getInt("id"),
                        results.getString("customer_name") == null
                                ? "Unknown customer" : results.getString("customer_name"),
                        results.getString("venue_name") == null
                                ? "Unknown venue" : results.getString("venue_name"),
                        results.getString("event_date"),
                        results.getString("start_time"),
                        results.getString("end_time"),
                        results.getString("status"),
                        results.getDouble("total_amount"),
                        results.getDouble("advance_amount")));
            }
            return bookings;
        }
    }

    public boolean updatePendingBookingStatus(int bookingId, String status) throws SQLException {
        if (!"CONFIRMED".equals(status) && !"CANCELLED".equals(status)) {
            throw new IllegalArgumentException("Unsupported booking status.");
        }
        String sql = "UPDATE bookings SET status = ? WHERE id = ? AND upper(status) = 'PENDING'";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, bookingId);
            return statement.executeUpdate() == 1;
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (Exception e) {
            throw new SQLException("Unable to connect to the venue database.", e);
        }
    }

    public record AdminBooking(int id, String customerName, String venueName, String eventDate,
                               String startTime, String endTime, String status,
                               double totalAmount, double advanceAmount) {
    }
}
