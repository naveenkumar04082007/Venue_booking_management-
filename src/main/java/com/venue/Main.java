package com.venue;

import com.venue.database.DatabaseConnection;
import com.venue.ui.LoginFrame;
import java.awt.EventQueue;

public class Main {
    public static void main(String[] args) {
        // Initialize SQLite DB
        DatabaseConnection.initializeDatabase();
        
        // Launch AWT UI on Event Dispatch Thread
        EventQueue.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
