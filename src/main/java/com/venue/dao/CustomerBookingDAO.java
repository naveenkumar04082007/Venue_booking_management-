package com.venue.dao;

import com.venue.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerBookingDAO {
    public DashboardStats getDashboardStats(int userId) throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM bookings b "
                + "JOIN customers c ON c.id = b.customer_id WHERE c.user_id = ?) AS total_bookings, "
                + "(SELECT COUNT(*) FROM bookings b "
                + "JOIN customers c ON c.id = b.customer_id "
                + "WHERE c.user_id = ? AND date(b.event_date) >= date('now', 'localtime') "
                + "AND upper(b.status) IN ('PENDING', 'CONFIRMED')) AS upcoming_events";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("Customer dashboard query returned no results.");
                }
                return new DashboardStats(
                        results.getInt("total_bookings"),
                        results.getInt("upcoming_events"));
            }
        }
    }

    public List<Booking> getCustomerBookings(int userId) throws SQLException {
        String sql = "SELECT b.id, v.name AS venue_name, b.event_date, b.start_time, "
                + "b.end_time, b.status, b.total_amount "
                + "FROM bookings b JOIN customers c ON c.id = b.customer_id "
                + "JOIN venues v ON v.id = b.venue_id WHERE c.user_id = ? "
                + "ORDER BY date(b.event_date) DESC, time(b.start_time) DESC";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (results.next()) {
                    bookings.add(readBooking(results));
                }
                return bookings;
            }
        }
    }

    public List<Booking> getCompletedBookings(int userId) throws SQLException {
        return getBookingsByStatus(userId, "COMPLETED", true);
    }

    public List<Booking> getConfirmedBookings(int userId) throws SQLException {
        return getBookingsByStatus(userId, "CONFIRMED", false);
    }

    private List<Booking> getBookingsByStatus(int userId, String status, boolean pastOnly)
            throws SQLException {
        String sql = "SELECT b.id, v.name AS venue_name, b.event_date, b.start_time, "
                + "b.end_time, b.status, b.total_amount, v.hourly_price "
                + "FROM bookings b JOIN customers c ON c.id = b.customer_id "
                + "JOIN venues v ON v.id = b.venue_id "
                + "WHERE c.user_id = ? AND upper(b.status) = ? "
                + (pastOnly ? "AND date(b.event_date) <= date('now', 'localtime') " : "")
                + "ORDER BY date(b.event_date) DESC, time(b.start_time) DESC";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, status);
            try (ResultSet results = statement.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (results.next()) {
                    Booking booking = readBooking(results);
                    double hourlyPrice = results.getDouble("hourly_price");
                    boolean priceMissing = results.wasNull();
                    double originalAmount = booking.totalAmount();
                    if (!priceMissing) {
                        long minutes = java.time.Duration.between(
                                parseTime(booking.startTime()), parseTime(booking.endTime())).toMinutes();
                        originalAmount = Math.round(hourlyPrice * minutes / 60.0 * 100.0)
                                / 100.0;
                    }
                    bookings.add(new Booking(
                            booking.id(), booking.venueName(), booking.eventDate(),
                            booking.startTime(), booking.endTime(), booking.status(),
                            originalAmount, booking.totalAmount()));
                }
                return bookings;
            }
        }
    }

    public double getTotalAmountSpent(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(b.total_amount), 0) AS total_spent "
                + "FROM bookings b JOIN customers c ON c.id = b.customer_id "
                + "WHERE c.user_id = ? AND upper(b.status) = 'COMPLETED' "
                + "AND date(b.event_date) <= date('now', 'localtime')";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("Customer spending query returned no results.");
                }
                return results.getDouble("total_spent");
            }
        }
    }

    public boolean cancelPendingBooking(int userId, int bookingId) throws SQLException {
        String sql = "UPDATE bookings SET status = 'CANCELLED' "
                + "WHERE id = ? AND upper(status) = 'PENDING' "
                + "AND customer_id = (SELECT id FROM customers WHERE user_id = ?)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    private Booking readBooking(ResultSet results) throws SQLException {
        return new Booking(
                results.getInt("id"),
                results.getString("venue_name"),
                results.getString("event_date"),
                results.getString("start_time"),
                results.getString("end_time"),
                results.getString("status"),
                results.getDouble("total_amount"),
                results.getDouble("total_amount"));
    }

    private java.time.LocalTime parseTime(String value) {
        String normalized = value.trim();
        return java.time.LocalTime.parse(normalized.length() >= 5
                ? normalized.substring(0, 5) : normalized);
    }

    private Connection openConnection() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (Exception e) {
            throw new SQLException("Unable to connect to the venue database.", e);
        }
    }

    public record DashboardStats(int totalBookings, int upcomingEvents) {
    }

    public record Booking(int id, String venueName, String eventDate, String startTime,
                          String endTime, String status, double originalAmount,
                          double totalAmount) {
    }
}
