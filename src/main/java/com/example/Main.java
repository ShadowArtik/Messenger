package com.example;

import com.example.database.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/chat-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 800, 500);

        scene.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        stage.setTitle("Messenger");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Database connected successfully");

            DatabaseConnection.initDatabase();

        } catch (Exception e) {
            System.out.println("Database connection failed");
            e.printStackTrace();
        }

        launch();
    }
}