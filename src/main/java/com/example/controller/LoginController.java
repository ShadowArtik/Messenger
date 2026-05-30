package com.example.controller;

import com.example.model.Session;
import com.example.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {

    @FXML private VBox loginBox;
    @FXML private VBox registerBox;

    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;

    @FXML private TextField registerUsernameField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField registerPasswordField;

    private final UserService userService = new UserService();

    @FXML
    private void showLoginForm() {
        loginBox.setVisible(true);
        loginBox.setManaged(true);

        registerBox.setVisible(false);
        registerBox.setManaged(false);
    }

    @FXML
    private void showRegisterForm() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);

        registerBox.setVisible(true);
        registerBox.setManaged(true);
    }

    @FXML
    private void onLoginClick() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText().trim();

        if (userService.login(username, password)) {
            Session.setCurrentUser(userService.getCurrentUser());
            openMessenger();
        } else {
            showError("Login failed", "Wrong username or password.");
        }
    }

    @FXML
    private void onRegisterClick() {
        String username = registerUsernameField.getText().trim();
        String displayName = displayNameField.getText().trim();
        String password = registerPasswordField.getText().trim();

        UserService.RegisterResult result = userService.register(
                username,
                displayName,
                password
        );

        switch (result) {
            case SUCCESS -> {
                showSuccess(
                        "Success",
                        "Account created successfully."
                );

                registerUsernameField.clear();
                displayNameField.clear();
                registerPasswordField.clear();

                showLoginForm();
            }

            case INVALID_USERNAME -> showError(
                    "Invalid username",
                    "Username must be 3-20 characters and contain only letters, numbers or _."
            );

            case INVALID_DISPLAY_NAME -> showError(
                    "Invalid display name",
                    "Display name must be 2-30 characters."
            );

            case INVALID_PASSWORD -> showError(
                    "Invalid password",
                    "Password must be at least 6 characters."
            );

            case USER_ALREADY_EXISTS -> showError(
                    "User already exists",
                    "This username is already taken."
            );
        }
    }

    private void openMessenger() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/chat-view.fxml")
            );

            Scene scene = new Scene(loader.load(), 800, 500);
            scene.getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm()
            );

            Stage stage = (Stage) loginUsernameField.getScene().getWindow();
            stage.setTitle(
                    "Messenger - " +
                            Session.getCurrentUser().getUsername()
            );
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Cannot open messenger.");
        }
    }

    private void showError(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);

        alert.getDialogPane()
                .getStylesheets()
                .add(
                        getClass()
                                .getResource("/style.css")
                                .toExternalForm()
                );

        alert.showAndWait();
    }

    private void showSuccess(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);

        alert.getDialogPane()
                .getStylesheets()
                .add(
                        getClass()
                                .getResource("/style.css")
                                .toExternalForm()
                );

        alert.showAndWait();
    }
}