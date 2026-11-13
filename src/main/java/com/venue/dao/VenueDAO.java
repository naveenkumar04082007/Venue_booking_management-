package com.venue.dao;

import com.venue.database.DatabaseConnection;
import com.venue.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class VenueDAO {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATABASE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public List<Venue> getAvailableVenues() throws SQLException {
        return getVenues("WHERE upper(status) = 'AVAILABLE'");
    }

    public List<Venue> getAllVenues() throws SQLException {
        return getVenues("");
    }

    private List<Venue> getVenues(String filter) throws SQLException {
        String sql = "SELECT id, name, type, address, capacity, hourly_price, daily_price, status "
                + "FROM venues " + filter + " ORDER BY name";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Venue> venues = new ArrayList<>();
            while (results.next()) {
                double hourlyPrice = results.getDouble("hourly_price");
                boolean hourlyPriceMissing = results.wasNull();
                double dailyPrice = results.getDouble("daily_price");
                boolean dailyPriceMissing = results.wasNull();
                venues.add(new Venue(
                        results.getInt("id"),
                        results.getString("name"),
                        results.getString("type"),
                        results.getString("address"),
                        results.getInt("capacity"),
                        hourlyPrice,
                        hourlyPriceMissing,
                        dailyPrice,
                        dailyPriceMissing,
                        results.getString("status")));
            }
            return venues;
        }
    }

    public void addVenue(String name, String type, String address, int capacity,
                         double hourlyPrice, double dailyPrice) throws SQLException {
        String sql = "INSERT INTO venues "
                + "(name, type, address, capacity, hourly_price, daily_price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE')";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setVenueFields(statement, name, type, address, capacity, hourlyPrice, dailyPrice);
            statement.executeUpdate();
        }
    }

    public boolean updateVenue(int venueId, String name, String type, String address,
                               int capacity, double hourlyPrice, double dailyPrice)
            throws SQLException {
        String sql = "UPDATE venues SET name = ?, type = ?, address = ?, capacity = ?, "
                + "hourly_price = ?, daily_price = ? WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setVenueFields(statement, name, type, address, capacity, hourlyPrice, dailyPrice);
            statement.setInt(7, venueId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateVenueStatus(int venueId, String status) throws SQLException {
        if (!"AVAILABLE".equals(status) && !"UNDER MAINTENANCE".equals(status)) {
            throw new IllegalArgumentException("Unsupported venue status.");
        }
        String sql = "UPDATE venues SET status = ? WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, venueId);
            return statement.executeUpdate() == 1;
        }
    }

    private void setVenueFields(PreparedStatement statement, String name, String type,
                                String address, int capacity, double hourlyPrice,
                                double dailyPrice) throws SQLException {
        statement.setString(1, name);
        statement.setString(2, type);
        statement.setString(3, address);
        statement.setInt(4, capacity);
        statement.setDouble(5, hourlyPrice);
        statement.setDouble(6, dailyPrice);
    }

    public List<SmartSlot> getSmartSlots(int venueId) throws SQLException {
        String sql = "SELECT event_date, start_time, end_time FROM bookings "
                + "WHERE venue_id = ? AND upper(status) <> 'CANCELLED' "
                + "ORDER BY event_date, time(start_time)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, venueId);
            try (ResultSet results = statement.executeQuery()) {
                List<SmartSlot> slots = new ArrayList<>();
                LocalDate currentDate = null;
                LocalTime previousEnd = null;
                while (results.next()) {
                    LocalDate bookingDate = LocalDate.parse(results.getString("event_date"));
                    LocalTime bookingStart = parseTime(results.getString("start_time"));
                    LocalTime bookingEnd = parseTime(results.getString("end_time"));

                    if (currentDate != null && currentDate.equals(bookingDate)
                            && previousEnd != null && bookingStart.isAfter(previousEnd)) {
                        Duration gap = Duration.between(previousEnd, bookingStart);
                        if (gap.compareTo(Duration.ofHours(3)) >= 0
                                && gap.compareTo(Duration.ofHours(5)) <= 0) {
                            slots.add(new SmartSlot(bookingDate, previousEnd, bookingStart));
                        }
                    }

                    if (!bookingDate.equals(currentDate)) {
                        currentDate = bookingDate;
                        previousEnd = bookingEnd;
                    } else if (previousEnd == null || bookingEnd.isAfter(previousEnd)) {
                        previousEnd = bookingEnd;
                    }
                }
                return slots;
            } catch (DateTimeParseException e) {
                throw new SQLException("A booking has an invalid date or time.", e);
            }
        }
    }

    public void createBooking(User user, int venueId, LocalDate eventDate,
                              LocalTime startTime, LocalTime endTime) throws SQLException {
        createBooking(user, venueId, eventDate, startTime, endTime, Double.NaN);
    }

    public double createBooking(User user, int venueId, LocalDate eventDate,
                                LocalTime startTime, LocalTime endTime,
                                double expectedTotal) throws SQLException {
        if (eventDate.isBefore(LocalDate.now())
                || (eventDate.equals(LocalDate.now()) && startTime.isBefore(LocalTime.now()))
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Choose a future date and an end time after the start time.");
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int customerId = getCustomerId(connection, user.getId());
                double hourlyPrice = getHourlyPrice(connection, venueId);
                if (bookingOverlaps(connection, venueId, eventDate, startTime, endTime)) {
                    throw new IllegalArgumentException("That time overlaps another booking.");
                }

                BookingPrice price = calculatePrice(hourlyPrice, startTime, endTime);
                double total = price.finalPrice();
                if (!Double.isNaN(expectedTotal) && Double.compare(total, expectedTotal) != 0) {
                    throw new IllegalArgumentException(
                            "Venue pricing changed. Calculate the price again before booking.");
                }

                String sql = "INSERT INTO bookings "
                        + "(customer_id, venue_id, event_date, start_time, end_time, status, total_amount) "
                        + "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, customerId);
                    statement.setInt(2, venueId);
                    statement.setString(3, eventDate.toString());
                    statement.setString(4, startTime.format(DATABASE_TIME));
                    statement.setString(5, endTime.format(DATABASE_TIME));
                    statement.setDouble(6, total);
                    statement.executeUpdate();
                }
                connection.commit();
                return total;
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }
                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }
                throw new SQLException("Unable to create the booking.", e);
            }
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw (SQLException) e;
            }
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new SQLException("Unable to create the booking.", e);
        }
    }

    public BookingPrice calculateBookingPrice(int venueId, LocalTime startTime,
                                              LocalTime endTime) throws SQLException {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        try (Connection connection = openConnection()) {
            return calculatePrice(getHourlyPrice(connection, venueId), startTime, endTime);
        }
    }

    private BookingPrice calculatePrice(double hourlyPrice, LocalTime startTime,
                                        LocalTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        double originalPrice = roundCents(hourlyPrice * duration.toMinutes() / 60.0);
        boolean smartSlot = duration.compareTo(Duration.ofHours(3)) >= 0
                && duration.compareTo(Duration.ofHours(5)) <= 0;
        double finalPrice = smartSlot ? roundCents(originalPrice * 0.8) : originalPrice;
        return new BookingPrice(originalPrice, finalPrice, smartSlot);
    }

    private Connection openConnection() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (Exception e) {
            throw new SQLException("Unable to connect to the venue database.", e);
        }
    }

    private int getCustomerId(Connection connection, int userId) throws SQLException {
        String sql = "SELECT id FROM customers WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("This login has no linked customer profile.");
                }
                return results.getInt("id");
            }
        }
    }

    private double getHourlyPrice(Connection connection, int venueId) throws SQLException {
        String sql = "SELECT hourly_price FROM venues "
                + "WHERE id = ? AND upper(status) = 'AVAILABLE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, venueId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("This venue is no longer available.");
                }
                double price = results.getDouble("hourly_price");
                if (results.wasNull()) {
                    throw new SQLException("This venue does not have an hourly price configured.");
                }
                return price;
            }
        }
    }

    private double roundCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private boolean bookingOverlaps(Connection connection, int venueId, LocalDate eventDate,
                                    LocalTime startTime, LocalTime endTime) throws SQLException {
        String sql = "SELECT 1 FROM bookings WHERE venue_id = ? AND event_date = ? "
                + "AND upper(status) <> 'CANCELLED' "
                + "AND time(start_time) < time(?) AND time(end_time) > time(?) LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, venueId);
            statement.setString(2, eventDate.toString());
            statement.setString(3, endTime.toString());
            statement.setString(4, startTime.toString());
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    private LocalTime parseTime(String value) {
        String timeValue = value.trim();
        if (timeValue.length() > 8) {
            timeValue = timeValue.substring(0, 8);
        }
        return LocalTime.parse(timeValue.length() == 5 ? timeValue + ":00" : timeValue);
    }

    public record Venue(int id, String name, String type, String address, int capacity,
                        double hourlyPrice, boolean hourlyPriceMissing, double dailyPrice,
                        boolean dailyPriceMissing, String status) {
    }

    public record BookingPrice(double originalPrice, double finalPrice, boolean smartSlot) {
    }

    public record SmartSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
        public String displayText() {
            return date + " " + startTime.format(DISPLAY_TIME) + "-"
                    + endTime.format(DISPLAY_TIME);
        }
    }
}
