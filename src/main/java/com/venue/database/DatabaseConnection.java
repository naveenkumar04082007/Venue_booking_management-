package com.venue.database;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_DIR = "data";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/venue.db";

    public static Connection getConnection() throws Exception {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            InputStream is = DatabaseConnection.class.getResourceAsStream("/database/schema.sql");
            if (is != null) {
                String schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String[] queries = schema.split(";");
                for (String query : queries) {
                    if (!query.trim().isEmpty()) {
                        stmt.execute(query);
                    }
                }
                ensureCustomerUserLink(stmt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureCustomerUserLink(Statement statement) throws Exception {
        boolean hasUserId = false;
        try (var columns = statement.executeQuery("PRAGMA table_info(customers)")) {
            while (columns.next()) {
                if ("user_id".equalsIgnoreCase(columns.getString("name"))) {
                    hasUserId = true;
                    break;
                }
            }
        }
        if (!hasUserId) {
            statement.execute("ALTER TABLE customers ADD COLUMN user_id INTEGER REFERENCES users(id)");
        }
        statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_user_id "
                + "ON customers(user_id)");
    }
}
