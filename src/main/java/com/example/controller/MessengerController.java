package com.example.controller;

import com.example.model.Message;
import com.example.model.MessengerModel;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class MessengerController {

    @FXML
    private ListView<String> contactsListView;

    @FXML
    private ListView<Message> messagesListView;

    @FXML
    private Label chatTitleLabel;

    @FXML
    private StackPane chatAvatar;

    @FXML
    private TextField messageTextField;

    @FXML
    private Button menuButton;

    private MessengerModel model;

    private String selectedContact;

    @FXML
    public void initialize() {
        model = new MessengerModel();

        contactsListView.setItems(model.getContacts());

        contactsListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);

                if (empty || name == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Circle avatarCircle = new Circle(18);
                Color avatarColor;

                switch (name) {
                    case "Anna":
                        avatarColor = Color.web("#FF6B81");
                        break;

                    case "Victor":
                        avatarColor = Color.web("#7D5FFF");
                        break;

                    case "Bot":
                        avatarColor = Color.web("#2ED573");
                        break;

                    default:
                        avatarColor = Color.web("#5B9DFF");
                }

                avatarCircle.setFill(avatarColor);

                Label letterLabel = new Label(name.substring(0, 1).toUpperCase());
                letterLabel.setStyle(
                        "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;"
                );

                StackPane avatar = new StackPane(avatarCircle, letterLabel);

                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: white;");

                HBox container = new HBox(10, avatar, nameLabel);
                container.setAlignment(Pos.CENTER_LEFT);

                setGraphic(container);
                setText(null);
            }
        });

        selectedContact = "Bot";
        updateChatHeader(selectedContact);
        contactsListView.getSelectionModel().select(selectedContact);
        messagesListView.setItems(model.getMessagesForContact(selectedContact));

        contactsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedContact = newValue;
                        updateChatHeader(selectedContact);
                        messagesListView.setItems(model.getMessagesForContact(selectedContact));
                    }
                }
        );

        messagesListView.setCellFactory(listView -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);

                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label messageLabel = new Label(
                        message.getText() + "\n" + message.getFormattedTime()
                );

                messageLabel.setWrapText(true);
                messageLabel.setMaxWidth(300);

                HBox messageBox = new HBox(messageLabel);

                if (message.getSender().equals("You")) {
                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    messageLabel.setStyle(
                            "-fx-background-color: #5B9DFF;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-padding: 10;" +
                                    "-fx-background-radius: 16;"
                    );
                } else {
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageLabel.setStyle(
                            "-fx-background-color: #3A3D41;" +
                                    "-fx-text-fill: #F1F1F1;" +
                                    "-fx-padding: 12 14 12 14;" +
                                    "-fx-background-radius: 18;"
                    );
                }

                setText(null);
                setGraphic(messageBox);
            }
        });

        messageTextField.setOnAction(event -> onSendButtonClick());
    }

    private void updateChatHeader(String name) {

        chatTitleLabel.setText(name);

        chatAvatar.getChildren().clear();

        Color avatarColor;

        switch (name) {
            case "Anna":
                avatarColor = Color.web("#FF6B81");
                break;

            case "Victor":
                avatarColor = Color.web("#7D5FFF");
                break;

            case "Bot":
                avatarColor = Color.web("#2ED573");
                break;

            default:
                avatarColor = Color.web("#5B9DFF");
        }

        Circle circle = new Circle(18);
        circle.setFill(avatarColor);

        Label letter = new Label(name.substring(0, 1).toUpperCase());

        letter.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        StackPane avatar = new StackPane(circle, letter);

        chatAvatar.getChildren().add(avatar);
    }

    @FXML
    private void onMenuButtonClick() {
        ContextMenu menu = new ContextMenu();

        MenuItem clearChatItem = new MenuItem("Clear chat");
        clearChatItem.setOnAction(event -> model.clearChat(selectedContact));

        menu.getItems().add(clearChatItem);

        menu.show(menuButton, javafx.geometry.Side.BOTTOM, 0, 5);
    }

    @FXML
    private void onClearChatClick() {
        model.clearChat(selectedContact);
    }

    @FXML
    private void onSendButtonClick() {
        String text = messageTextField.getText().trim();

        if (text.isEmpty()) {
            messageTextField.clear();
            return;
        }

        Message userMessage = new Message("You", text);
        model.addMessage(selectedContact, userMessage);

        if (selectedContact.equals("Bot")) {
            Message botMessage = model.generateBotResponse(text);
            model.addMessage(selectedContact, botMessage);
        }

        messageTextField.clear();

        messagesListView.scrollTo(model.getMessagesForContact(selectedContact).size() - 1);
    }
}
