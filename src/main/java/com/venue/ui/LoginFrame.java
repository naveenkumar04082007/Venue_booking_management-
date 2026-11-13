package com.venue.ui;

import com.venue.model.User;
import com.venue.service.AuthService;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends Frame {
    private static final Color SLATE_BLUE = new Color(44, 62, 80);
    private static final Color MUTED_COLOR = new Color(112, 128, 144);

    private final TextField txtUsername;
    private final TextField txtPassword;
    private final AuthService authService;

    public LoginFrame() {
        authService = new AuthService();
        setTitle("Venue Booking Management System - Login");
        setSize(520, 500);
        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        Panel loginPanel = new Panel(new GridBagLayout());
        loginPanel.setBackground(Color.WHITE);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 0, 8, 0);

        Label title = new Label("Welcome back", Label.CENTER);
        title.setForeground(SLATE_BLUE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        loginPanel.add(title, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 30, 0);
        Label subtitle = new Label("Sign in to manage your venue bookings", Label.CENTER);
        subtitle.setForeground(MUTED_COLOR);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        loginPanel.add(subtitle, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 8, 0);
        Label usernameLabel = new Label("Username");
        styleFieldLabel(usernameLabel);
        loginPanel.add(usernameLabel, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 20, 0);
        txtUsername = new TextField();
        styleTextField(txtUsername);
        loginPanel.add(txtUsername, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 8, 0);
        Label passwordLabel = new Label("Password");
        styleFieldLabel(passwordLabel);
        loginPanel.add(passwordLabel, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 26, 0);
        txtPassword = new TextField();
        txtPassword.setEchoChar('*');
        styleTextField(txtPassword);
        loginPanel.add(txtPassword, constraints);

        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        Button btnLogin = new Button("Sign in");
        btnLogin.setBackground(SLATE_BLUE);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnLogin.setPreferredSize(new Dimension(320, 44));
        btnLogin.addActionListener(e -> attemptLogin());
        loginPanel.add(btnLogin, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(12, 0, 0, 0);
        Button btnCreateAccount = new Button("Create New Account");
        btnCreateAccount.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnCreateAccount.addActionListener(e -> new CustomerSignUpDialog(this).setVisible(true));
        loginPanel.add(btnCreateAccount, constraints);

        add(loginPanel);
    }

    private void styleFieldLabel(Label label) {
        label.setForeground(SLATE_BLUE);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
    }

    private void styleTextField(TextField textField) {
        textField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        textField.setPreferredSize(new Dimension(320, 36));
    }

    private void attemptLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        User user = authService.login(username, password);
        if (user != null) {
            if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
                new CustomerDashboard(user).setVisible(true);
            } else {
                new MainFrame(user).setVisible(true);
            }
            this.dispose();
        } else {
            Dialog d = new Dialog(this, "Error", true);
            d.setLayout(new FlowLayout());
            d.add(new Label("Invalid credentials. Try admin / admin."));
            Button b = new Button("OK");
            b.addActionListener(ev -> d.dispose());
            d.add(b);
            d.setSize(300, 100);
            d.setLocationRelativeTo(this);
            d.setVisible(true);
        }
    }
}
