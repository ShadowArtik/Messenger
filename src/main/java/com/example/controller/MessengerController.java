package com.example.controller;

import com.example.model.Message;
import com.example.model.MessengerModel;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.example.model.Session;

import java.util.Optional;

public class MessengerController {

    @FXML private ListView<String> contactsListView;
    @FXML private ListView<Message> messagesListView;
    @FXML private Label chatTitleLabel;
    @FXML private StackPane chatAvatar;
    @FXML private TextField messageTextField;
    @FXML private VBox dropdownMenu;

    private MessengerModel model;
    private String selectedContact;

    @FXML
    public void initialize() {
        model = new MessengerModel();

        contactsListView.setItems(model.getContacts());
        contactsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);

                if (empty || name == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                StackPane avatar = createAvatar(name, 18);

                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                HBox nameBox = new HBox(6, nameLabel);
                nameBox.setAlignment(Pos.CENTER_LEFT);

                if (model.isBotContact(name)) {
                    Label botBadge = createBotBadge();
                    nameBox.getChildren().add(botBadge);
                }

                HBox container = new HBox(10, avatar, nameBox);
                container.setAlignment(Pos.CENTER_LEFT);

                setText(null);
                setGraphic(container);
            }
        });

        messagesListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);

                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label textLabel = new Label(message.getText());
                textLabel.setWrapText(true);
                textLabel.setMaxWidth(300);
                textLabel.getStyleClass().add("message-text");

                Label timeLabel = new Label(message.getFormattedTime());
                timeLabel.getStyleClass().add("message-time");

                VBox bubble = new VBox(3, textLabel, timeLabel);
                bubble.getStyleClass().add("message-bubble");

                HBox messageBox = new HBox(bubble);

                if (message.getSender().equals(
                        Session.getCurrentUser().getUsername()
                )) {
                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    bubble.getStyleClass().add("message-bubble-outgoing");
                } else {
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    bubble.getStyleClass().add("message-bubble-incoming");
                }

                setText(null);
                setGraphic(messageBox);
            }
        });

        if (!model.getContacts().isEmpty()) {
            selectedContact = model.getContacts().get(0);
            contactsListView.getSelectionModel().select(selectedContact);
            openChat(selectedContact);
        }

        contactsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedContact = newValue;
                        openChat(selectedContact);
                        hideDropdownMenu();
                    }
                }
        );

        messageTextField.setOnAction(event -> onSendButtonClick());
    }

    private void openChat(String contactName) {
        updateChatHeader(contactName);
        messagesListView.setItems(model.getMessagesForContact(contactName));
    }

    private void updateChatHeader(String name) {
        chatTitleLabel.setText(name);
        chatTitleLabel.setGraphic(null);

        if (model.isBotContact(name)) {
            Label titleLabel = new Label(name);
            titleLabel.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-size: 24;" +
                            "-fx-font-weight: bold;"
            );

            HBox titleBox = new HBox(6, titleLabel, createBotBadge());
            titleBox.setAlignment(Pos.CENTER_LEFT);

            chatTitleLabel.setText("");
            chatTitleLabel.setGraphic(titleBox);
        }

        chatAvatar.getChildren().clear();
        chatAvatar.getChildren().add(createAvatar(name, 18));
    }

    private StackPane createAvatar(String name, double radius) {
        Circle circle = new Circle(radius);
        circle.setFill(Color.web(getAvatarColor(name)));

        Label letter = new Label(name.substring(0, 1).toUpperCase());
        letter.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        return new StackPane(circle, letter);
    }

    private String getAvatarColor(String name) {
        String[] colors = {
                "#5B9DFF",
                "#FF6B81",
                "#7D5FFF",
                "#2ED573",
                "#FFA502",
                "#FF4757",
                "#1DD1A1",
                "#A55EEA"
        };

        int index = Math.abs(name.hashCode()) % colors.length;
        return colors[index];
    }

    private Label createBotBadge() {
        Label botBadge = new Label("BOT");
        botBadge.getStyleClass().add("bot-badge");
        return botBadge;
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

        model.renameChat(selectedContact, newName);
        selectedContact = newName;

        contactsListView.getSelectionModel().select(selectedContact);
        openChat(selectedContact);
        contactsListView.refresh();
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
        openChat(selectedContact);
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
        openChat(selectedContact);
    }

    @FXML
    private void onSendButtonClick() {
        String text = messageTextField.getText().trim();

        if (text.isEmpty()) {
            messageTextField.clear();
            return;
        }

        Message userMessage = new Message(
                Session.getCurrentUser().getUsername(),
                text
        );
        model.addMessage(selectedContact, userMessage);

        if (model.isBotContact(selectedContact)) {
            Message botMessage = model.generateBotResponse(text);
            model.addMessage(selectedContact, botMessage);
        }

        messageTextField.clear();
        messagesListView.scrollTo(model.getMessagesForContact(selectedContact).size() - 1);
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
}