package com.example.controller;

import com.example.model.Message;
import com.example.model.MessengerModel;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;

import java.util.Optional;

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

    @FXML
    private VBox dropdownMenu;

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

        if (!model.getContacts().isEmpty()) {
            selectedContact = model.getContacts().get(0);

            updateChatHeader(selectedContact);

            contactsListView.getSelectionModel().select(selectedContact);

            messagesListView.setItems(model.getMessagesForContact(selectedContact));
        }

        contactsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedContact = newValue;
                        updateChatHeader(selectedContact);
                        messagesListView.setItems(model.getMessagesForContact(selectedContact));
                        hideDropdownMenu();
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

                messageLabel.getStyleClass().add("message-bubble");

                if (message.getSender().equals("You")) {
                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    messageLabel.getStyleClass().add("message-bubble-outgoing");
                } else {
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageLabel.getStyleClass().add("message-bubble-incoming");
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
        boolean showMenu = !dropdownMenu.isVisible();
        dropdownMenu.setVisible(showMenu);
        dropdownMenu.setManaged(showMenu);
    }

    @FXML
    private void onClearChatClick() {
        model.clearChat(selectedContact);
        hideDropdownMenu();
    }

    @FXML
    private void onRenameChatClick() {
        hideDropdownMenu();

        TextInputDialog dialog = new TextInputDialog(selectedContact);
        dialog.setTitle("Rename chat");
        dialog.setHeaderText(null);
        dialog.setContentText("New chat name:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        String newName = result.get().trim();

        if (newName.isEmpty() || newName.equals(selectedContact)) {
            return;
        }

        if (model.hasContact(newName)) {
            showMessage("Chat already exists", "A chat with this name already exists.");
            return;
        }

        String oldName = selectedContact;
        model.renameChat(oldName, newName);
        selectedContact = newName;
        updateChatHeader(selectedContact);
        contactsListView.getSelectionModel().select(selectedContact);
        messagesListView.setItems(model.getMessagesForContact(selectedContact));
    }

    @FXML
    private void onDeleteChatClick() {
        hideDropdownMenu();

        if (model.getContacts().size() <= 1) {
            showMessage("Cannot delete chat", "You cannot delete the last chat.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete chat");
        alert.setHeaderText(null);
        alert.setContentText("Delete chat with " + selectedContact + "?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        int oldIndex = contactsListView.getSelectionModel().getSelectedIndex();
        oldIndex = Math.max(oldIndex, 0);
        model.deleteChat(selectedContact);

        int newIndex = Math.min(oldIndex, model.getContacts().size() - 1);
        selectedContact = model.getContacts().get(newIndex);
        contactsListView.getSelectionModel().select(selectedContact);
        updateChatHeader(selectedContact);
        messagesListView.setItems(model.getMessagesForContact(selectedContact));
    }

    @FXML
    private void onCreateChatClick() {
        hideDropdownMenu();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create chat");
        dialog.setHeaderText(null);
        dialog.setContentText("Chat name:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        String newName = result.get().trim();

        if (newName.isEmpty()) {
            return;
        }

        if (model.hasContact(newName)) {
            showMessage("Chat already exists", "A chat with this name already exists.");
            return;
        }

        model.addContact(newName);

        selectedContact = newName;
        contactsListView.getSelectionModel().select(selectedContact);
        updateChatHeader(selectedContact);
        messagesListView.setItems(model.getMessagesForContact(selectedContact));
    }

    private void showMessage(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void hideDropdownMenu() {
        dropdownMenu.setVisible(false);
        dropdownMenu.setManaged(false);
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
