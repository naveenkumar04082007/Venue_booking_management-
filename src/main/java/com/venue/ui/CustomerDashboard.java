package com.venue.ui;

import com.venue.dao.CustomerBookingDAO;
import com.venue.dao.CustomerBookingDAO.Booking;
import com.venue.dao.VenueDAO;
import com.venue.dao.VenueDAO.Venue;
import com.venue.model.User;
import java.awt.*;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CustomerDashboard extends Frame {
    private static final Color SIDEBAR_COLOR = new Color(44, 62, 80);
    private static final Color CONTENT_COLOR = new Color(245, 247, 250);
    private static final Color MUTED_COLOR = new Color(112, 128, 144);
    private static final Color GREEN_COLOR = new Color(39, 120, 75);
    private static final String DASHBOARD_CARD = "dashboard";
    private static final String BOOK_HALL_CARD = "book-hall";
    private static final String BOOKINGS_CARD = "bookings";
    private static final String REPORTS_CARD = "reports";

    private final User currentUser;
    private final VenueDAO venueDAO = new VenueDAO();
    private final CustomerBookingDAO bookingDAO = new CustomerBookingDAO();
    private final Panel contentPanel = new Panel(new CardLayout());
    private final Panel bookingsList = new Panel(new GridLayout(0, 1, 0, 8));
    private final Panel dashboardStats = new Panel(new GridLayout(1, 2, 16, 16));
    private final Label dashboardError = createMessageLabel();
    private final Label bookingsError = createMessageLabel();
    private final Label reportsError = createMessageLabel();
    private final Label dashboardPromoLabel = new Label(
            "Smart Slots Available Today - Book now for 20% off!", Label.LEFT);
    private final Choice venueChoice = new Choice();
    private final TextField bookingDateField = new TextField(LocalDate.now().plusDays(1).toString());
    private final TextField startTimeField = new TextField("09:00");
    private final TextField endTimeField = new TextField("12:00");
    private final Label smartOfferLabel = new Label("", Label.CENTER);
    private final Label priceEstimateLabel = new Label(
            "Choose a venue and calculate your booking price.", Label.LEFT);
    private final Button confirmBookingButton = new Button("Confirm Booking");
    private final TextArea invoiceArea = new TextArea("", 20, 70,
            TextArea.SCROLLBARS_VERTICAL_ONLY);
    private List<Venue> availableVenues = List.of();
    private boolean priceCalculated;
    private double calculatedFinalPrice;

    public CustomerDashboard(User user) {
        currentUser = user;
        setTitle("Venue Booking - Customer Dashboard");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout());
        setBackground(CONTENT_COLOR);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });

        add(createSidebar(), BorderLayout.WEST);
        contentPanel.add(createDashboardPage(), DASHBOARD_CARD);
        contentPanel.add(createBookHallPage(), BOOK_HALL_CARD);
        contentPanel.add(createBookingsPage(), BOOKINGS_CARD);
        contentPanel.add(createReportsPage(), REPORTS_CARD);
        add(contentPanel, BorderLayout.CENTER);

        refreshDashboard();
        refreshBookings();
        refreshReports();
        loadVenues();
    }

    private Panel createSidebar() {
        Panel sidebar = new Panel(new BorderLayout(0, 18));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setPreferredSize(new Dimension(230, 0));

        Label brand = new Label("VENUE BOOKING", Label.CENTER);
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setPreferredSize(new Dimension(230, 76));
        sidebar.add(brand, BorderLayout.NORTH);

        Panel navigation = new Panel(new GridLayout(0, 1, 0, 8));
        navigation.setBackground(SIDEBAR_COLOR);
        addNavigationButton(navigation, "Dashboard", DASHBOARD_CARD);
        addNavigationButton(navigation, "Book a Hall", BOOK_HALL_CARD);
        addNavigationButton(navigation, "My Bookings", BOOKINGS_CARD);
        addNavigationButton(navigation, "My Reports", REPORTS_CARD);
        sidebar.add(navigation, BorderLayout.CENTER);

        Panel footer = new Panel(new GridLayout(1, 1));
        footer.setBackground(SIDEBAR_COLOR);
        Button logout = createNavigationButton("Logout");
        logout.addActionListener(event -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        footer.add(logout);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addNavigationButton(Panel navigation, String title, String card) {
        Button button = createNavigationButton(title);
        button.addActionListener(event -> {
            ((CardLayout) contentPanel.getLayout()).show(contentPanel, card);
            if (BOOKINGS_CARD.equals(card)) {
                refreshBookings();
            } else if (REPORTS_CARD.equals(card)) {
                refreshReports();
            } else if (DASHBOARD_CARD.equals(card)) {
                refreshDashboard();
            } else if (BOOK_HALL_CARD.equals(card)) {
                loadVenues();
            }
        });
        navigation.add(button);
    }

    private Button createNavigationButton(String title) {
        Button button = new Button(title);
        button.setForeground(Color.WHITE);
        button.setBackground(SIDEBAR_COLOR);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        return button;
    }

    private Panel createDashboardPage() {
        Panel page = createPage("Dashboard", "Welcome back, " + currentUser.getUsername() + "!");
        dashboardStats.setBackground(CONTENT_COLOR);
        Panel content = new Panel(new BorderLayout(0, 18));
        content.setBackground(CONTENT_COLOR);
        content.add(dashboardStats, BorderLayout.NORTH);

        Panel promotion = new Panel(new BorderLayout());
        promotion.setBackground(new Color(232, 247, 238));
        dashboardPromoLabel.setForeground(GREEN_COLOR);
        dashboardPromoLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        promotion.add(dashboardPromoLabel, BorderLayout.CENTER);
        content.add(promotion, BorderLayout.CENTER);
        content.add(dashboardError, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createBookHallPage() {
        Panel page = createPage("Book a Hall", "Choose an available venue and submit a booking request.");
        Panel content = new Panel(new BorderLayout(0, 16));
        content.setBackground(CONTENT_COLOR);
        Label intro = new Label("Select a venue and provide your event schedule.");
        intro.setForeground(MUTED_COLOR);
        content.add(intro, BorderLayout.NORTH);

        Panel form = new Panel(new GridLayout(0, 2, 12, 12));
        form.setBackground(Color.WHITE);
        addBookingFormField(form, "Venue", venueChoice);
        addBookingFormField(form, "Event Date (YYYY-MM-DD)", bookingDateField);
        addBookingFormField(form, "Start Time (HH:MM)", startTimeField);
        addBookingFormField(form, "End Time (HH:MM)", endTimeField);
        smartOfferLabel.setForeground(GREEN_COLOR);
        smartOfferLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        smartOfferLabel.setVisible(false);
        priceEstimateLabel.setForeground(SIDEBAR_COLOR);
        priceEstimateLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        Panel estimate = new Panel(new GridLayout(0, 1, 0, 8));
        estimate.setBackground(CONTENT_COLOR);
        estimate.add(smartOfferLabel);
        estimate.add(priceEstimateLabel);

        Panel controls = new Panel(new GridLayout(1, 2, 12, 0));
        controls.setBackground(CONTENT_COLOR);
        Button calculateButton = new Button("Calculate Price");
        calculateButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        calculateButton.addActionListener(event -> calculateBookingPrice());
        confirmBookingButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmBookingButton.setEnabled(false);
        confirmBookingButton.addActionListener(event -> submitBooking());
        controls.add(calculateButton);
        controls.add(confirmBookingButton);

        Panel formContent = new Panel(new BorderLayout(0, 16));
        formContent.setBackground(CONTENT_COLOR);
        formContent.add(form, BorderLayout.NORTH);
        formContent.add(estimate, BorderLayout.CENTER);
        formContent.add(controls, BorderLayout.SOUTH);
        Panel paddedForm = withPadding(formContent);
        content.add(paddedForm, BorderLayout.CENTER);

        TextListener priceInputListener = event -> invalidatePriceCalculation();
        bookingDateField.addTextListener(priceInputListener);
        startTimeField.addTextListener(priceInputListener);
        endTimeField.addTextListener(priceInputListener);
        venueChoice.addItemListener(event -> invalidatePriceCalculation());
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private void addBookingFormField(Panel form, String title, Component input) {
        Label label = new Label(title);
        label.setForeground(SIDEBAR_COLOR);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        input.setFont(new Font("SansSerif", Font.PLAIN, 14));
        form.add(label);
        form.add(input);
    }

    private Panel createBookingsPage() {
        Panel page = createPage("My Bookings", "Your booking history and current reservation status.");
        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(CONTENT_COLOR);
        Panel columns = createBookingColumns(false);
        content.add(columns, BorderLayout.NORTH);
        ScrollPane scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        scrollPane.add(bookingsList);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(bookingsError, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createReportsPage() {
        Panel page = createPage("My Reports",
                "Confirmed bookings with original and discounted prices.");
        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(CONTENT_COLOR);
        invoiceArea.setEditable(false);
        invoiceArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        invoiceArea.setText("Confirmed booking invoices will appear here.");
        Panel reportActions = new Panel(new BorderLayout(0, 6));
        reportActions.setBackground(CONTENT_COLOR);
        reportActions.add(reportsError, BorderLayout.NORTH);
        Button downloadPrintButton = new Button("Download/Print Text");
        downloadPrintButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        downloadPrintButton.addActionListener(event -> showInvoiceActions());
        Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(CONTENT_COLOR);
        actions.add(downloadPrintButton);
        reportActions.add(actions, BorderLayout.SOUTH);
        content.add(invoiceArea, BorderLayout.CENTER);
        content.add(reportActions, BorderLayout.SOUTH);
        page.add(withPadding(content), BorderLayout.CENTER);
        return page;
    }

    private Panel createPage(String title, String subtitle) {
        Panel page = new Panel(new BorderLayout());
        page.setBackground(CONTENT_COLOR);
        Panel headerPadding = new Panel(new BorderLayout());
        headerPadding.setBackground(CONTENT_COLOR);
        headerPadding.add(createHeader(title, subtitle), BorderLayout.CENTER);
        headerPadding.add(createSpacer(22, 1), BorderLayout.NORTH);
        headerPadding.add(createSpacer(22, 1), BorderLayout.WEST);
        headerPadding.add(createSpacer(22, 1), BorderLayout.EAST);
        page.add(headerPadding, BorderLayout.NORTH);
        return page;
    }

    private Panel withPadding(Component content) {
        Panel padded = new Panel(new BorderLayout());
        padded.setBackground(CONTENT_COLOR);
        padded.add(content, BorderLayout.CENTER);
        padded.add(createSpacer(22, 1), BorderLayout.NORTH);
        padded.add(createSpacer(22, 1), BorderLayout.SOUTH);
        padded.add(createSpacer(22, 1), BorderLayout.WEST);
        padded.add(createSpacer(22, 1), BorderLayout.EAST);
        return padded;
    }

    private Panel createHeader(String title, String subtitle) {
        Panel header = new Panel(new GridLayout(0, 1, 0, 7));
        header.setBackground(CONTENT_COLOR);
        Label heading = new Label(title);
        heading.setForeground(SIDEBAR_COLOR);
        heading.setFont(new Font("SansSerif", Font.BOLD, 28));
        Label description = new Label(subtitle);
        description.setForeground(MUTED_COLOR);
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));
        header.add(heading);
        header.add(description);
        return header;
    }

    private Panel createSpacer(int width, int height) {
        Panel spacer = new Panel();
        spacer.setBackground(CONTENT_COLOR);
        spacer.setPreferredSize(new Dimension(width, height));
        return spacer;
    }

    private Panel createBookingColumns(boolean includeInvoice) {
        Panel row = new Panel(new GridLayout(1, 6, 8, 0));
        row.setBackground(new Color(225, 231, 238));
        addTableHeading(row, "Venue");
        addTableHeading(row, "Date");
        addTableHeading(row, "Time");
        addTableHeading(row, "Status");
        addTableHeading(row, "Amount");
        addTableHeading(row, includeInvoice ? "Invoice" : "Action");
        return row;
    }

    private void addTableHeading(Panel row, String text) {
        Label label = new Label(text);
        label.setForeground(SIDEBAR_COLOR);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(label);
    }

    private void refreshDashboard() {
        dashboardStats.removeAll();
        dashboardError.setText("");
        try {
            CustomerBookingDAO.DashboardStats stats =
                    bookingDAO.getDashboardStats(currentUser.getId());
            dashboardStats.add(createSummaryCard("Total Bookings Made", stats.totalBookings()));
            dashboardStats.add(createSummaryCard("Upcoming Events", stats.upcomingEvents()));
        } catch (SQLException e) {
            e.printStackTrace();
            dashboardStats.add(createSummaryCard("Total Bookings Made", "Unavailable"));
            dashboardStats.add(createSummaryCard("Upcoming Events", "Unavailable"));
            dashboardError.setText("Your dashboard statistics could not be loaded.");
        }
        dashboardStats.validate();
        dashboardStats.repaint();
    }

    private Panel createSummaryCard(String title, int value) {
        return createSummaryCard(title, Integer.toString(value));
    }

    private Panel createSummaryCard(String title, String value) {
        Panel card = new Panel(new GridLayout(2, 1, 0, 8));
        card.setBackground(Color.WHITE);
        Label valueLabel = new Label(value);
        valueLabel.setForeground(SIDEBAR_COLOR);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        Label titleLabel = new Label(title);
        titleLabel.setForeground(MUTED_COLOR);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(valueLabel);
        card.add(titleLabel);
        return card;
    }

    private void loadVenues() {
        int previousSelection = venueChoice.getSelectedIndex();
        venueChoice.removeAll();
        try {
            availableVenues = venueDAO.getAvailableVenues();
            if (availableVenues.isEmpty()) {
                venueChoice.add("No available venues");
                venueChoice.setEnabled(false);
                priceEstimateLabel.setText("There are no available venues right now.");
                confirmBookingButton.setEnabled(false);
                return;
            }
            venueChoice.setEnabled(true);
            for (Venue venue : availableVenues) {
                venueChoice.add(venue.name() + " - " + venue.type());
            }
            if (previousSelection >= 0 && previousSelection < availableVenues.size()) {
                venueChoice.select(previousSelection);
            }
            invalidatePriceCalculation();
        } catch (SQLException e) {
            e.printStackTrace();
            availableVenues = List.of();
            venueChoice.add("Unable to load venues");
            venueChoice.setEnabled(false);
            priceEstimateLabel.setText("Available venues could not be loaded.");
            showMessage("Available venues could not be loaded: " + e.getMessage(),
                    "Venue Loading Failed");
        }
    }

    private void calculateBookingPrice() {
        int selectedIndex = venueChoice.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= availableVenues.size()) {
            showMessage("Select an available venue first.", "Venue Required");
            return;
        }
        Venue venue = availableVenues.get(selectedIndex);
        try {
            LocalDate eventDate = LocalDate.parse(bookingDateField.getText().trim());
            LocalTime start = parseTimeField(startTimeField);
            LocalTime end = parseTimeField(endTimeField);
            if (eventDate.isBefore(LocalDate.now())
                    || (eventDate.equals(LocalDate.now()) && start.isBefore(LocalTime.now()))
                    || !end.isAfter(start)) {
                throw new IllegalArgumentException(
                        "Choose a future date and an end time after the start time.");
            }
            VenueDAO.BookingPrice price = venueDAO.calculateBookingPrice(venue.id(), start, end);
            calculatedFinalPrice = price.finalPrice();
            priceEstimateLabel.setText(String.format(
                    "Original Price: INR %.2f     Final Price: INR %.2f",
                    price.originalPrice(), calculatedFinalPrice));
            smartOfferLabel.setText(price.smartSlot()
                    ? "🎉 SMART SLOT ACTIVATED: 20% DISCOUNT APPLIED!"
                    : "No smart-slot discount for this duration.");
            smartOfferLabel.setForeground(price.smartSlot() ? GREEN_COLOR : MUTED_COLOR);
            smartOfferLabel.setVisible(true);
            priceCalculated = true;
            confirmBookingButton.setEnabled(true);
        } catch (DateTimeParseException e) {
            showMessage("Use YYYY-MM-DD for the date and HH:MM for times.",
                    "Invalid Date or Time");
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), "Invalid Booking");
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("The booking price could not be calculated: " + e.getMessage(),
                    "Price Calculation Failed");
        }
    }

    private void invalidatePriceCalculation() {
        priceCalculated = false;
        calculatedFinalPrice = 0;
        confirmBookingButton.setEnabled(false);
        smartOfferLabel.setVisible(false);
        priceEstimateLabel.setText("Booking details changed. Calculate the price again.");
    }

    private void refreshBookings() {
        bookingsList.removeAll();
        bookingsError.setText("");
        try {
            List<Booking> bookings = bookingDAO.getCustomerBookings(currentUser.getId());
            if (bookings.isEmpty()) {
                bookingsList.add(new Label("You have not made any bookings yet."));
            } else {
                for (Booking booking : bookings) {
                    bookingsList.add(createBookingRow(booking));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bookingsError.setText("Your bookings could not be loaded.");
        }
        bookingsList.validate();
        bookingsList.repaint();
    }

    private void refreshReports() {
        reportsError.setText("");
        try {
            List<Booking> bookings = bookingDAO.getConfirmedBookings(currentUser.getId());
            if (bookings.isEmpty()) {
                invoiceArea.setText("No confirmed bookings are available for an invoice.");
                return;
            }
            StringBuilder invoice = new StringBuilder();
            invoice.append("                 VENUE BOOKING REPORT\n")
                    .append("============================================================\n")
                    .append("Customer: ").append(currentUser.getUsername()).append("\n\n");
            for (Booking booking : bookings) {
                invoice.append("Booking #").append(booking.id()).append('\n')
                        .append("Venue: ").append(booking.venueName()).append('\n')
                        .append("Date: ").append(booking.eventDate()).append('\n')
                        .append("Time: ").append(formatTime(booking.startTime()))
                        .append(" - ").append(formatTime(booking.endTime())).append('\n')
                        .append(String.format("Original Price: INR %.2f%n",
                                booking.originalAmount()))
                        .append(String.format("Final Price:    INR %.2f%n",
                                booking.totalAmount()))
                        .append("------------------------------------------------------------\n");
            }
            invoiceArea.setText(invoice.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            reportsError.setText("Your confirmed booking report could not be loaded: "
                    + e.getMessage());
            invoiceArea.setText("The confirmed booking report could not be loaded.");
        }
    }

    private Panel createBookingRow(Booking booking) {
        Panel row = new Panel(new GridLayout(1, 6, 8, 0));
        row.setBackground(Color.WHITE);
        row.add(new Label(booking.venueName()));
        row.add(new Label(booking.eventDate()));
        row.add(new Label(formatTime(booking.startTime()) + " - " + formatTime(booking.endTime())));
        row.add(new Label(booking.status()));
        row.add(new Label(String.format("INR %.2f", booking.totalAmount())));

        if ("PENDING".equalsIgnoreCase(booking.status())) {
            Button cancelButton = new Button("Cancel Booking");
            cancelButton.addActionListener(event -> cancelBooking(booking));
            row.add(cancelButton);
        } else {
            row.add(new Label(""));
        }
        return row;
    }

    private String formatTime(String value) {
        return value != null && value.length() >= 5 ? value.substring(0, 5) : value;
    }

    private LocalTime parseTimeField(TextField field) {
        String value = field.getText().trim();
        if (!value.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Enter times using HH:MM.");
        }
        return LocalTime.parse(value);
    }

    private void cancelBooking(Booking booking) {
        try {
            if (bookingDAO.cancelPendingBooking(currentUser.getId(), booking.id())) {
                refreshBookings();
                refreshDashboard();
            } else {
                showMessage("This booking can no longer be cancelled.", "Cancellation Unavailable");
                refreshBookings();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("The booking could not be cancelled: " + e.getMessage(), "Cancellation Failed");
        }
    }

    private void showInvoiceActions() {
        Dialog dialog = new Dialog(this, "Download or Print Report", true);
        dialog.setLayout(new GridLayout(0, 1, 8, 8));
        dialog.setBackground(Color.WHITE);

        Button downloadButton = new Button("Download Text File");
        downloadButton.addActionListener(event -> {
            dialog.dispose();
            FileDialog fileDialog = new FileDialog(this, "Save Booking Report", FileDialog.SAVE);
            fileDialog.setFile("venue-booking-report.txt");
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }
            Path output = Path.of(fileDialog.getDirectory(), fileDialog.getFile());
            try {
                Files.writeString(output, invoiceArea.getText(), StandardCharsets.UTF_8);
                showMessage("Report saved to " + output + ".", "Report Downloaded");
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("The report could not be saved: " + e.getMessage(),
                        "Download Failed");
            }
        });

        Button printButton = new Button("Print");
        printButton.addActionListener(event -> {
            dialog.dispose();
            printInvoiceReport();
        });

        Button closeButton = new Button("Close");
        closeButton.addActionListener(event -> dialog.dispose());
        dialog.add(downloadButton);
        dialog.add(printButton);
        dialog.add(closeButton);
        dialog.setSize(300, 160);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void printInvoiceReport() {
        PrintJob printJob = getToolkit().getPrintJob(this, "Venue Booking Report", null);
        if (printJob == null) {
            return;
        }
        Dimension pageSize = printJob.getPageDimension();
        int lineHeight = 16;
        int y = 40;
        Graphics graphics = printJob.getGraphics();
        graphics.setFont(new Font("Monospaced", Font.PLAIN, 11));
        for (String line : invoiceArea.getText().split("\\R", -1)) {
            if (y + lineHeight > pageSize.height - 30) {
                graphics.dispose();
                graphics = printJob.getGraphics();
                y = 40;
            }
            graphics.drawString(line, 40, y);
            y += lineHeight;
        }
        graphics.dispose();
        printJob.end();
    }

    private void showMessage(String message, String title) {
        Dialog dialog = new Dialog(this, title, true);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 14));
        dialog.add(new Label(message));
        Button close = new Button("OK");
        close.addActionListener(event -> dialog.dispose());
        dialog.add(close);
        dialog.setSize(420, 130);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private Label createMessageLabel() {
        Label label = new Label("");
        label.setForeground(new Color(192, 57, 43));
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return label;
    }

    private void submitBooking() {
        if (!priceCalculated) {
            showMessage("Calculate the price before confirming your booking.",
                    "Price Calculation Required");
            return;
        }
        int selectedIndex = venueChoice.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= availableVenues.size()) {
            showMessage("Select an available venue first.", "Venue Required");
            return;
        }
        Venue venue = availableVenues.get(selectedIndex);
        try {
            LocalDate eventDate = LocalDate.parse(bookingDateField.getText().trim());
            LocalTime start = parseTimeField(startTimeField);
            LocalTime end = parseTimeField(endTimeField);
            double savedPrice = venueDAO.createBooking(
                    currentUser, venue.id(), eventDate, start, end, calculatedFinalPrice);
            showMessage(String.format("Booking request submitted for %s. Final price: INR %.2f",
                    venue.name(), savedPrice), "Booking Submitted");
            invalidatePriceCalculation();
            refreshBookings();
            refreshDashboard();
            refreshReports();
        } catch (DateTimeParseException e) {
            invalidatePriceCalculation();
            showMessage("Use YYYY-MM-DD for the date and HH:MM for times.",
                    "Invalid Date or Time");
        } catch (IllegalArgumentException e) {
            invalidatePriceCalculation();
            showMessage(e.getMessage(), "Invalid Booking");
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("The booking could not be saved: " + e.getMessage(), "Booking Failed");
        }
    }
}
