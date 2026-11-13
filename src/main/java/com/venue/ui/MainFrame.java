package com.venue.ui;

import com.venue.dao.DashboardDAO;
import com.venue.dao.BookingDAO;
import com.venue.dao.BookingDAO.AdminBooking;
import com.venue.dao.CustomerDAO;
import com.venue.dao.CustomerDAO.CustomerSummary;
import com.venue.dao.VenueDAO;
import com.venue.dao.VenueDAO.Venue;
import com.venue.model.User;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

public class MainFrame extends Frame {
    private static final Color SIDEBAR_COLOR = new Color(44, 62, 80);
    private static final Color CONTENT_COLOR = new Color(245, 247, 250);
    private static final Color ACCENT_COLOR = new Color(52, 152, 219);
    private static final Color MUTED_COLOR = new Color(112, 128, 144);
    private static final String DASHBOARD_CARD = "dashboard";
    private static final String VENUES_CARD = "venues";
    private static final String CUSTOMERS_CARD = "customers";
    private static final String BOOKINGS_CARD = "bookings";
    private static final String REPORTS_CARD = "reports";

    private final User currentUser;
    private final VenueDAO venueDAO = new VenueDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final java.awt.List venueList = new java.awt.List();
    private final java.awt.List customerList = new java.awt.List();
    private final java.awt.List bookingList = new java.awt.List();
    private final TextArea reportsArea = new TextArea("", 10, 50, TextArea.SCROLLBARS_NONE);
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final Label venueError = createErrorLabel();
    private final Label customerError = createErrorLabel();
    private final Label bookingError = createErrorLabel();
    private java.util.List<Venue> currentVenues = java.util.List.of();
    private java.util.List<CustomerSummary> currentCustomers = java.util.List.of();
    private java.util.List<AdminBooking> currentBookings = java.util.List.of();

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("Venue Booking Dashboard - Logged in as: " + currentUser.getUsername());
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        Panel sidebar = new Panel(new BorderLayout(0, 24));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setPreferredSize(new Dimension(230, 0));

        Label brand = new Label("VENUE MANAGER", Label.CENTER);
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setPreferredSize(new Dimension(230, 76));
        sidebar.add(brand, BorderLayout.NORTH);

        Panel navigation = new Panel(new GridLayout(0, 1, 0, 8));
        navigation.setBackground(SIDEBAR_COLOR);
        Button dashboardButton = createNavigationButton("Dashboard", true);
        Button venuesButton = createNavigationButton("Manage Venues", false);
        Button customersButton = createNavigationButton("Manage Customers", false);
        Button bookingsButton = createNavigationButton("Manage Bookings", false);
        Button reportsButton = createNavigationButton("Reports", false);
        navigation.add(dashboardButton);
        navigation.add(venuesButton);
        navigation.add(customersButton);
        navigation.add(bookingsButton);
        navigation.add(reportsButton);
        Button btnLogout = createNavigationButton("Logout", false);
        navigation.add(btnLogout);
        sidebar.add(navigation, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);

        CardLayout cardLayout = new CardLayout();
        Panel contentPanel = new Panel(cardLayout);
        contentPanel.setBackground(CONTENT_COLOR);

        Panel dashboardPage = new Panel(new BorderLayout(0, 28));
        dashboardPage.setBackground(CONTENT_COLOR);
        dashboardPage.add(createDashboardHeader(), BorderLayout.NORTH);

        Label loadError = new Label("", Label.LEFT);
        loadError.setForeground(new Color(192, 57, 43));
        loadError.setFont(new Font("SansSerif", Font.PLAIN, 13));

        Panel dashboardPanel = new Panel(new GridLayout(1, 3, 18, 18));
        dashboardPanel.setBackground(CONTENT_COLOR);
        try {
            DashboardDAO.DashboardStats stats = new DashboardDAO().getDashboardStats();
            dashboardPanel.add(createSummaryCard("Total Venues", stats.totalVenues()));
            dashboardPanel.add(createSummaryCard("Total Customers", stats.totalCustomers()));
            dashboardPanel.add(createSummaryCard("Today's Bookings", stats.todaysBookings()));
        } catch (SQLException e) {
            e.printStackTrace();
            dashboardPanel.add(createSummaryCard("Total Venues", "Unavailable"));
            dashboardPanel.add(createSummaryCard("Total Customers", "Unavailable"));
            dashboardPanel.add(createSummaryCard("Today's Bookings", "Unavailable"));
            loadError.setText("Dashboard summary could not be loaded.");
        }

        Panel dashboardContent = new Panel(new BorderLayout(0, 18));
        dashboardContent.setBackground(CONTENT_COLOR);
        dashboardContent.add(dashboardPanel, BorderLayout.NORTH);
        dashboardContent.add(loadError, BorderLayout.SOUTH);
        dashboardPage.add(dashboardContent, BorderLayout.CENTER);

        contentPanel.add(dashboardPage, DASHBOARD_CARD);
        contentPanel.add(createVenuesPanel(), VENUES_CARD);
        contentPanel.add(createCustomersPanel(), CUSTOMERS_CARD);
        contentPanel.add(createBookingsPanel(), BOOKINGS_CARD);
        contentPanel.add(createReportsPanel(), REPORTS_CARD);
        add(contentPanel, BorderLayout.CENTER);

        dashboardButton.addActionListener(e -> cardLayout.show(contentPanel, DASHBOARD_CARD));
        venuesButton.addActionListener(e -> {
            cardLayout.show(contentPanel, VENUES_CARD);
            refreshVenues();
        });
        customersButton.addActionListener(e -> {
            cardLayout.show(contentPanel, CUSTOMERS_CARD);
            refreshCustomers();
        });
        bookingsButton.addActionListener(e -> {
            cardLayout.show(contentPanel, BOOKINGS_CARD);
            refreshBookings();
        });
        reportsButton.addActionListener(e -> {
            cardLayout.show(contentPanel, REPORTS_CARD);
            refreshReports();
        });
        btnLogout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });
    }

    private Panel createVenuesPanel() {
        Panel page = createAdminPage("Manage Venues",
                "Create, update, and maintain your venue inventory.");
        venueList.setFont(new Font("SansSerif", Font.PLAIN, 14));

        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(CONTENT_COLOR);
        content.add(venueList, BorderLayout.CENTER);
        Panel actions = new Panel(new GridLayout(1, 4, 10, 0));
        actions.setBackground(CONTENT_COLOR);
        Button addButton = createActionButton("Add");
        Button editButton = createActionButton("Edit");
        Button statusButton = createActionButton("Change Status");
        Button refreshButton = createActionButton("Refresh");
        addButton.addActionListener(event -> new VenueDialog(null).setVisible(true));
        editButton.addActionListener(event -> editSelectedVenue());
        statusButton.addActionListener(event -> changeSelectedVenueStatus());
        refreshButton.addActionListener(event -> refreshVenues());
        actions.add(addButton);
        actions.add(editButton);
        actions.add(statusButton);
        actions.add(refreshButton);
        Panel footer = new Panel(new BorderLayout(0, 8));
        footer.setBackground(CONTENT_COLOR);
        footer.add(venueError, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createCustomersPanel() {
        Panel page = createAdminPage("Manage Customers",
                "Customer contact directory.");
        customerList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        Panel content = new Panel(new BorderLayout(0, 10));
        content.setBackground(CONTENT_COLOR);
        content.add(customerList, BorderLayout.CENTER);
        Panel footer = new Panel(new BorderLayout(0, 8));
        footer.setBackground(CONTENT_COLOR);
        footer.add(customerError, BorderLayout.NORTH);
        Button refreshButton = createActionButton("Refresh");
        refreshButton.addActionListener(event -> refreshCustomers());
        Panel actions = new Panel(new GridLayout(1, 1));
        actions.setBackground(CONTENT_COLOR);
        actions.add(refreshButton);
        footer.add(actions, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createBookingsPanel() {
        Panel page = createAdminPage("Manage Bookings",
                "Review customer reservations and resolve pending requests.");
        bookingList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        Panel content = new Panel(new BorderLayout(0, 10));
        content.setBackground(CONTENT_COLOR);
        content.add(bookingList, BorderLayout.CENTER);
        Panel footer = new Panel(new BorderLayout(0, 8));
        footer.setBackground(CONTENT_COLOR);
        footer.add(bookingError, BorderLayout.NORTH);
        Panel actions = new Panel(new GridLayout(1, 3, 10, 0));
        actions.setBackground(CONTENT_COLOR);
        Button approveButton = createActionButton("Approve");
        Button rejectButton = createActionButton("Reject");
        Button refreshButton = createActionButton("Refresh");
        approveButton.addActionListener(event -> updateSelectedBooking("CONFIRMED"));
        rejectButton.addActionListener(event -> updateSelectedBooking("CANCELLED"));
        refreshButton.addActionListener(event -> refreshBookings());
        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(refreshButton);
        footer.add(actions, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createReportsPanel() {
        Panel page = createAdminPage("Reports", "Live booking and revenue summary.");
        reportsArea.setEditable(false);
        reportsArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        Panel content = new Panel(new BorderLayout(0, 10));
        content.setBackground(CONTENT_COLOR);
        content.add(reportsArea, BorderLayout.CENTER);
        Button refreshButton = createActionButton("Refresh Reports");
        refreshButton.addActionListener(event -> refreshReports());
        Panel actions = new Panel(new GridLayout(1, 1));
        actions.setBackground(CONTENT_COLOR);
        actions.add(refreshButton);
        content.add(actions, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createAdminPage(String title, String subtitle) {
        Panel page = new Panel(new BorderLayout(0, 16));
        page.setBackground(CONTENT_COLOR);
        Panel header = new Panel(new GridLayout(0, 1, 0, 6));
        header.setBackground(CONTENT_COLOR);
        Label heading = new Label(title);
        heading.setForeground(SIDEBAR_COLOR);
        heading.setFont(new Font("SansSerif", Font.BOLD, 28));
        Label description = new Label(subtitle);
        description.setForeground(MUTED_COLOR);
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));
        header.add(heading);
        header.add(description);
        page.add(header, BorderLayout.NORTH);
        page.add(createSpacer(20, 1), BorderLayout.WEST);
        page.add(createSpacer(20, 1), BorderLayout.EAST);
        return page;
    }

    private Panel withPadding(Component content) {
        Panel padded = new Panel(new BorderLayout());
        padded.setBackground(CONTENT_COLOR);
        padded.add(content, BorderLayout.CENTER);
        padded.add(createSpacer(20, 1), BorderLayout.NORTH);
        padded.add(createSpacer(20, 1), BorderLayout.SOUTH);
        padded.add(createSpacer(20, 1), BorderLayout.WEST);
        padded.add(createSpacer(20, 1), BorderLayout.EAST);
        return padded;
    }

    private Panel createSpacer(int width, int height) {
        Panel spacer = new Panel();
        spacer.setBackground(CONTENT_COLOR);
        spacer.setPreferredSize(new Dimension(width, height));
        return spacer;
    }

    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(ACCENT_COLOR);
        button.setForeground(Color.WHITE);
        return button;
    }

    private Label createErrorLabel() {
        Label label = new Label("");
        label.setForeground(new Color(192, 57, 43));
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return label;
    }

    private void refreshVenues() {
        venueError.setText("");
        venueList.removeAll();
        try {
            currentVenues = venueDAO.getAllVenues();
            if (currentVenues.isEmpty()) {
                venueList.add("No venues have been added.");
            }
            for (Venue venue : currentVenues) {
                venueList.add(String.format(
                        "%d - %s - %s - INR %.2f/hour - %s",
                        venue.id(), venue.name(), venue.type(), venue.hourlyPrice(), venue.status()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            venueError.setText("Venues could not be loaded: " + e.getMessage());
        }
    }

    private void refreshCustomers() {
        customerError.setText("");
        customerList.removeAll();
        try {
            currentCustomers = customerDAO.getAllCustomers();
            if (currentCustomers.isEmpty()) {
                customerList.add("No customers have been registered.");
            }
            for (CustomerSummary customer : currentCustomers) {
                customerList.add(String.format("%s - %s - %s",
                        customer.name(), customer.phone(), customer.email()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            currentCustomers = java.util.List.of();
            customerError.setText("Customers could not be loaded: " + e.getMessage());
        }
    }

    private void refreshReports() {
        reportsArea.setText("Loading report summary...");
        try {
            DashboardDAO.ReportSummary summary = new DashboardDAO().getReportSummary();
            reportsArea.setText(String.format(
                    "VENUE BOOKING SUMMARY%n"
                            + "==============================%n%n"
                            + "Total Revenue:       INR %.2f%n%n"
                            + "Total Bookings:      %d%n%n"
                            + "Pending Approvals:   %d%n",
                    summary.totalRevenue(), summary.totalBookings(), summary.pendingApprovals()));
        } catch (SQLException e) {
            e.printStackTrace();
            reportsArea.setText("Report summary could not be loaded: " + e.getMessage());
        }
    }

    private Venue getSelectedVenue() {
        int selectedIndex = venueList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= currentVenues.size()) {
            showMessage("Select a venue first.", "Venue Required");
            return null;
        }
        return currentVenues.get(selectedIndex);
    }

    private void editSelectedVenue() {
        Venue venue = getSelectedVenue();
        if (venue != null) {
            new VenueDialog(venue).setVisible(true);
        }
    }

    private void changeSelectedVenueStatus() {
        Venue venue = getSelectedVenue();
        if (venue == null) {
            return;
        }
        Dialog dialog = new Dialog(this, "Change Venue Status", true);
        dialog.setLayout(new GridLayout(0, 1, 8, 8));
        dialog.setBackground(Color.WHITE);
        dialog.add(new Label("Choose status for " + venue.name() + ":"));
        Choice statusChoice = new Choice();
        statusChoice.add("AVAILABLE");
        statusChoice.add("UNDER MAINTENANCE");
        statusChoice.select(venue.status());
        dialog.add(statusChoice);
        Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
        Button save = createActionButton("Save Status");
        Button cancel = new Button("Cancel");
        save.addActionListener(event -> {
            try {
                if (!venueDAO.updateVenueStatus(venue.id(), statusChoice.getSelectedItem())) {
                    throw new SQLException("The venue no longer exists.");
                }
                dialog.dispose();
                refreshVenues();
            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("Venue status could not be updated: " + e.getMessage(),
                        "Update Failed");
            }
        });
        cancel.addActionListener(event -> dialog.dispose());
        buttons.add(save);
        buttons.add(cancel);
        dialog.add(buttons);
        dialog.setSize(360, 170);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void refreshBookings() {
        bookingError.setText("");
        bookingList.removeAll();
        try {
            currentBookings = bookingDAO.getAllBookings();
            if (currentBookings.isEmpty()) {
                bookingList.add("No bookings have been submitted.");
            } else {
                for (AdminBooking booking : currentBookings) {
                    String status = booking.status();
                    String prefix = "PENDING".equalsIgnoreCase(status) ? "[PENDING] " : "";
                    bookingList.add(prefix + booking.customerName() + " - "
                            + booking.venueName() + " - " + booking.eventDate() + " - "
                            + formatTime(booking.startTime()) + " to "
                            + formatTime(booking.endTime()) + " - " + status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            currentBookings = java.util.List.of();
            bookingError.setText("Bookings could not be loaded: " + e.getMessage());
        }
    }

    private void updateSelectedBooking(String status) {
        int selectedIndex = bookingList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= currentBookings.size()) {
            showMessage("Select a booking first.", "Booking Required");
            return;
        }
        AdminBooking booking = currentBookings.get(selectedIndex);
        if (!"PENDING".equalsIgnoreCase(booking.status())) {
            showMessage("Only pending bookings can be approved or rejected.",
                    "Action Unavailable");
            return;
        }
        updateBookingStatus(booking, status);
    }

    private String formatTime(String value) {
        return value != null && value.length() >= 5 ? value.substring(0, 5) : value;
    }

    private void updateBookingStatus(AdminBooking booking, String status) {
        try {
            if (!bookingDAO.updatePendingBookingStatus(booking.id(), status)) {
                showMessage("This booking is no longer pending.", "Booking Updated");
            }
            refreshBookings();
            refreshReports();
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Booking status could not be updated: " + e.getMessage(),
                    "Update Failed");
        }
    }

    private void showBookingDetails(AdminBooking booking) {
        Dialog dialog = new Dialog(this, "Booking Details", true);
        dialog.setLayout(new BorderLayout(12, 12));
        TextArea details = new TextArea(String.format(
                "Booking #%d%nCustomer: %s%nVenue: %s%nDate: %s%nTime: %s - %s"
                        + "%nStatus: %s%nTotal: INR %.2f%nAdvance: INR %.2f",
                booking.id(), booking.customerName(), booking.venueName(), booking.eventDate(),
                formatTime(booking.startTime()), formatTime(booking.endTime()), booking.status(),
                booking.totalAmount(), booking.advanceAmount()), 8, 48,
                TextArea.SCROLLBARS_NONE);
        details.setEditable(false);
        details.setFont(new Font("Monospaced", Font.PLAIN, 13));
        dialog.add(details, BorderLayout.CENTER);
        Button close = new Button("Close");
        close.addActionListener(event -> dialog.dispose());
        Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(500, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showMessage(String message, String title) {
        Dialog dialog = new Dialog(this, title, true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.add(new Label(message), BorderLayout.CENTER);
        Button close = new Button("OK");
        close.addActionListener(event -> dialog.dispose());
        Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(420, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private class VenueDialog extends Dialog {
        private final Venue venue;
        private final TextField nameField = new TextField();
        private final TextField typeField = new TextField();
        private final TextField addressField = new TextField();
        private final TextField capacityField = new TextField();
        private final TextField hourlyPriceField = new TextField();
        private final TextField dailyPriceField = new TextField();

        VenueDialog(Venue venue) {
            super(MainFrame.this, venue == null ? "Add New Venue" : "Edit Venue", true);
            this.venue = venue;
            setLayout(new BorderLayout(12, 12));
            setBackground(Color.WHITE);
            Panel form = new Panel(new GridLayout(0, 2, 8, 8));
            form.setBackground(Color.WHITE);
            addVenueField(form, "Venue Name", nameField);
            addVenueField(form, "Type", typeField);
            addVenueField(form, "Address", addressField);
            addVenueField(form, "Capacity", capacityField);
            addVenueField(form, "Hourly Price (INR)", hourlyPriceField);
            addVenueField(form, "Daily Price (INR)", dailyPriceField);
            if (venue != null) {
                nameField.setText(venue.name());
                typeField.setText(venue.type());
                addressField.setText(venue.address());
                capacityField.setText(Integer.toString(venue.capacity()));
                hourlyPriceField.setText(venue.hourlyPriceMissing()
                        ? "0" : Double.toString(venue.hourlyPrice()));
                dailyPriceField.setText(venue.dailyPriceMissing()
                        ? "0" : Double.toString(venue.dailyPrice()));
            } else {
                hourlyPriceField.setText("0");
                dailyPriceField.setText("0");
            }
            Panel paddedForm = new Panel(new BorderLayout());
            paddedForm.add(createSpacer(18, 1), BorderLayout.NORTH);
            paddedForm.add(createSpacer(18, 1), BorderLayout.WEST);
            paddedForm.add(createSpacer(18, 1), BorderLayout.EAST);
            paddedForm.add(form, BorderLayout.CENTER);
            add(paddedForm, BorderLayout.CENTER);

            Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT));
            Button save = createActionButton(venue == null ? "Add Venue" : "Save Changes");
            Button cancel = new Button("Cancel");
            save.addActionListener(event -> saveVenue());
            cancel.addActionListener(event -> dispose());
            actions.add(save);
            actions.add(cancel);
            add(actions, BorderLayout.SOUTH);
            setSize(520, 380);
            setLocationRelativeTo(MainFrame.this);
        }

        private void addVenueField(Panel form, String labelText, TextField field) {
            Label label = new Label(labelText);
            label.setForeground(SIDEBAR_COLOR);
            label.setFont(new Font("SansSerif", Font.BOLD, 13));
            field.setFont(new Font("SansSerif", Font.PLAIN, 13));
            form.add(label);
            form.add(field);
        }

        private void saveVenue() {
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String address = addressField.getText().trim();
            if (name.isEmpty() || type.isEmpty() || address.isEmpty()) {
                showMessage("Name, type, and address are required.", "Input Required");
                return;
            }
            try {
                int capacity = Integer.parseInt(capacityField.getText().trim());
                double hourlyPrice = Double.parseDouble(hourlyPriceField.getText().trim());
                double dailyPrice = Double.parseDouble(dailyPriceField.getText().trim());
                if (capacity <= 0 || hourlyPrice < 0 || dailyPrice < 0) {
                    throw new IllegalArgumentException(
                            "Capacity must be positive and prices cannot be negative.");
                }
                if (venue == null) {
                    venueDAO.addVenue(name, type, address, capacity, hourlyPrice, dailyPrice);
                } else if (!venueDAO.updateVenue(venue.id(), name, type, address, capacity,
                        hourlyPrice, dailyPrice)) {
                    throw new SQLException("The venue no longer exists.");
                }
                dispose();
                refreshVenues();
            } catch (NumberFormatException e) {
                showMessage("Capacity and prices must be valid numbers.", "Invalid Values");
            } catch (IllegalArgumentException e) {
                showMessage(e.getMessage(), "Invalid Venue");
            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("Venue could not be saved: " + e.getMessage(), "Save Failed");
            }
        }
    }

    private Button createNavigationButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setForeground(Color.WHITE);
        button.setBackground(selected ? ACCENT_COLOR : SIDEBAR_COLOR);
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
        return button;
    }

    private Panel createDashboardHeader() {
        Panel header = new Panel(new BorderLayout(0, 8));
        header.setBackground(CONTENT_COLOR);

        Label title = new Label("Dashboard");
        title.setForeground(new Color(44, 62, 80));
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        header.add(title, BorderLayout.NORTH);

        Label welcome = new Label("Welcome back, " + currentUser.getUsername());
        welcome.setForeground(MUTED_COLOR);
        welcome.setFont(new Font("SansSerif", Font.PLAIN, 15));
        header.add(welcome, BorderLayout.CENTER);
        return header;
    }

    private Panel createSummaryCard(String title, int value) {
        return createSummaryCard(title, Integer.toString(value));
    }

    private Panel createSummaryCard(String title, String value) {
        Panel card = new Panel(new GridLayout(2, 1, 0, 12));
        card.setBackground(Color.WHITE);

        Label valueLabel = new Label(value);
        valueLabel.setForeground(new Color(44, 62, 80));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        card.add(valueLabel);

        Label titleLabel = new Label(title);
        titleLabel.setForeground(MUTED_COLOR);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(titleLabel);
        return card;
    }
}
