package com.venue.ui;

import com.venue.dao.CustomerDAO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CustomerSignUpDialog extends Dialog {
    private static final Color SLATE_BLUE = new Color(44, 62, 80);

    private final TextField nameField;
    private final TextField phoneField;
    private final TextField usernameField;
    private final TextField passwordField;
    private final CustomerDAO customerDAO;

    public CustomerSignUpDialog(Frame owner) {
        super(owner, "Create New Account", true);
        customerDAO = new CustomerDAO();

        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setResizable(false);
        setSize(420, 400);
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
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 18, 0);

        Label title = new Label("Create your customer account");
        title.setForeground(SLATE_BLUE);
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        form.add(title, constraints);

        nameField = addField(form, constraints, "Name", false);
        phoneField = addField(form, constraints, "Phone Number", false);
        usernameField = addField(form, constraints, "Username", false);
        passwordField = addField(form, constraints, "Password", true);

        constraints.gridy++;
        constraints.insets = new Insets(10, 0, 0, 0);
        Button signUpButton = new Button("Sign Up");
        signUpButton.setBackground(SLATE_BLUE);
        signUpButton.setForeground(Color.WHITE);
        signUpButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        signUpButton.addActionListener(event -> signUp());
        form.add(signUpButton, constraints);

        add(form);
    }

    private TextField addField(Panel form, GridBagConstraints constraints, String labelText,
                               boolean password) {
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 6, 0);
        Label label = new Label(labelText);
        label.setForeground(SLATE_BLUE);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        form.add(label, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 12, 0);
        TextField field = new TextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(320, 32));
        if (password) {
            field.setEchoChar('*');
        }
        form.add(field, constraints);
        return field;
    }

    private void signUp() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showMessage("Please complete all fields.", "Input Required");
            return;
        }

        if (customerDAO.createCustomerAccount(name, phone, username, password)) {
            showMessage("Your account has been created. You can now sign in.", "Success");
            dispose();
        } else {
            showMessage("Could not create account. The username may already be in use.",
                    "Sign-Up Failed");
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
