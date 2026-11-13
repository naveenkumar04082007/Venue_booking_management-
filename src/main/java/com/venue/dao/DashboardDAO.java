package com.venue.dao;

import com.venue.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class DashboardDAO {
    public ReportSummary getReportSummary() throws SQLException {
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN upper(status) IN ('CONFIRMED', 'COMPLETED') "
                + "THEN COALESCE(total_amount, 0) ELSE 0 END), 0) AS total_revenue, "
                + "COUNT(*) AS total_bookings, "
                + "COALESCE(SUM(CASE WHEN upper(status) = 'PENDING' THEN 1 ELSE 0 END), 0) "
                + "AS pending_approvals FROM bookings";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            if (!results.next()) {
                throw new SQLException("Report summary query returned no results.");
            }
            return new ReportSummary(
                    results.getDouble("total_revenue"),
                    results.getInt("total_bookings"),
                    results.getInt("pending_approvals"));
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Unable to load report summary.", e);
        }
    }

    public DashboardStats getDashboardStats() throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM venues) AS total_venues, "
                + "(SELECT COUNT(*) FROM customers) AS total_customers, "
                + "(SELECT COUNT(*) FROM bookings "
                + "WHERE event_date = date('now', 'localtime')) AS todays_bookings";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new SQLException("Dashboard summary query returned no results.");
            }
            return new DashboardStats(
                    resultSet.getInt("total_venues"),
                    resultSet.getInt("total_customers"),
                    resultSet.getInt("todays_bookings"));
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Unable to load dashboard summary.", e);
        }
    }

    public record DashboardStats(int totalVenues, int totalCustomers, int todaysBookings) {
    }

    public record ReportSummary(double totalRevenue, int totalBookings, int pendingApprovals) {
    }
}
