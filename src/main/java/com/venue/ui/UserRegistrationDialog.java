package com.venue.ui;

import com.venue.dao.UserDAO;
import java.awt.*;
import java.awt.event.*;

public class UserRegistrationDialog extends Dialog {
    private static final Color SLATE_BLUE = new Color(44, 62, 80);
    private static final Color MUTED_COLOR = new Color(112, 128, 144);

    private final TextField usernameField;
    private final TextField passwordField;
    private final Choice roleChoice;
    private final UserDAO userDAO;

    public UserRegistrationDialog(Frame owner) {
        super(owner, "User Registration", true);
        userDAO = new UserDAO();

        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setResizable(false);
        setSize(440, 390);
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dispose();
            }
        });

        Panel form = new Panel(new GridBagLayout());
        form.setBackground(Color.WHITE);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 8, 0);

        Label title = new Label("Create a user account");
        title.setForeground(SLATE_BLUE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        form.add(title, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 24, 0);
        Label subtitle = new Label("Add an administrator or staff member");
        subtitle.setForeground(MUTED_COLOR);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        form.add(subtitle, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 7, 12);
        form.add(createFieldLabel("Username"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 7, 0);
        form.add(createFieldLabel("Password"), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 18, 12);
        usernameField = createTextField();
        form.add(usernameField, constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 18, 0);
        passwordField = createTextField();
        passwordField.setEchoChar('*');
        form.add(passwordField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 0, 7, 0);
        form.add(createFieldLabel("Role"), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 22, 0);
        roleChoice = new Choice();
        roleChoice.add("ADMIN");
        roleChoice.add("STAFF");
        roleChoice.select("STAFF");
        roleChoice.setFont(new Font("SansSerif", Font.PLAIN, 14));
        form.add(roleChoice, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(4, 0, 0, 0);
        Button createButton = new Button("Save");
        createButton.setBackground(SLATE_BLUE);
        createButton.setForeground(Color.WHITE);
        createButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        createButton.addActionListener(event -> createUser());
        form.add(createButton, constraints);

        add(form);
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setForeground(SLATE_BLUE);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        return label;
    }

    private TextField createTextField() {
        TextField field = new TextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(180, 34));
        return field;
    }

    private void createUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and password are required.", "Input Required");
            return;
        }

        if (userDAO.createUser(username, password, roleChoice.getSelectedItem())) {
            showMessage("User created successfully.", "Success");
            usernameField.setText("");
            passwordField.setText("");
            roleChoice.select("STAFF");
        } else {
            showMessage("Could not create user. The username may already be in use.",
                    "Creation Failed");
        }
    }

    private void showMessage(String message, String title) {
        Dialog messageDialog = new Dialog(this, title, true);
        messageDialog.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 14));
        messageDialog.setBackground(Color.WHITE);
        messageDialog.add(new Label(message));

        Button closeButton = new Button("OK");
        closeButton.addActionListener(event -> messageDialog.dispose());
        messageDialog.add(closeButton);

        messageDialog.setSize(360, 120);
        messageDialog.setLocationRelativeTo(this);
        messageDialog.setVisible(true);
    }
}
