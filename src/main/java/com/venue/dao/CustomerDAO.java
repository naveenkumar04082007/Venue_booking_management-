package com.venue.dao;

import com.venue.database.DatabaseConnection;
import com.venue.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    public List<CustomerSummary> getAllCustomers() throws SQLException {
        String sql = "SELECT name, phone, COALESCE(email, '') AS email "
                + "FROM customers ORDER BY name";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<CustomerSummary> customers = new ArrayList<>();
            while (results.next()) {
                customers.add(new CustomerSummary(
                        results.getString("name"),
                        results.getString("phone"),
                        results.getString("email")));
            }
            return customers;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Unable to load customers.", e);
        }
    }

    public boolean createCustomerAccount(String name, String phone, String username,
                                         String plainPassword) {
        String userSql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        String customerSql = "INSERT INTO customers (name, phone, user_id) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement userStatement = connection.prepareStatement(
                             userSql, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement customerStatement = connection.prepareStatement(customerSql)) {
                    userStatement.setString(1, username);
                    userStatement.setString(2, PasswordUtil.hashPassword(plainPassword));
                    userStatement.setString(3, "CUSTOMER");
                    userStatement.executeUpdate();
                    int userId;
                    try (ResultSet keys = userStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("User creation did not return an ID.");
                        }
                        userId = keys.getInt(1);
                    }

                    customerStatement.setString(1, name);
                    customerStatement.setString(2, phone);
                    customerStatement.setInt(3, userId);
                    customerStatement.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public record CustomerSummary(String name, String phone, String email) {
    }
}
