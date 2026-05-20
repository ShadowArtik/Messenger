package com.example.controller;

import com.example.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.example.model.Session;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final UserService userService = new UserService();

    @FXML
    private void onLoginClick() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (userService.login(username, password)) {

            Session.setCurrentUser(
                    userService.getCurrentUser()
            );

            openMessenger();
        } else {
            showError("Login failed", "Wrong username or password.");
        }
    }

    @FXML
    private void onRegisterClick() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (userService.register(username, password)) {

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Account created successfully.");
            alert.showAndWait();

            usernameField.clear();
            passwordField.clear();

        } else {
            showError(
                    "Registration failed",
                    "User already exists or fields are empty."
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

            Stage stage = (Stage) usernameField.getScene().getWindow();
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
        alert.showAndWait();
    }
}