package com.example.controller;

import com.example.model.Message;
import com.example.model.MessengerModel;
import com.example.service.result.CreateChatResponse;
import com.example.service.result.CreateChatResult;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.Optional;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.example.model.Session;
import com.example.model.Chat;


public class MessengerController {

    @FXML
    private ListView<Chat> contactsListView;
    @FXML
    private StackPane createChatOverlay;
    @FXML
    private TextField createChatUsernameField;
    @FXML
    private Label createChatErrorLabel;
    @FXML
    private StackPane renameChatOverlay;
    @FXML
    private TextField renameChatNameField;
    @FXML
    private Label renameChatErrorLabel;
    @FXML
    private StackPane deleteChatOverlay;
    @FXML
    private StackPane clearChatOverlay;

    @FXML private ListView<Message> messagesListView;
    @FXML private Label chatTitleLabel;
    @FXML private StackPane chatAvatar;
    @FXML private TextField messageTextField;
    @FXML private Button menuButton;
    @FXML private VBox dropdownMenu;
    private ContextMenu chatMenu;

    private MessengerModel model;
    private Chat selectedChat;

    @FXML
    public void initialize() {
        model = new MessengerModel();

        contactsListView.setItems(model.getChats());
        contactsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);

                if (empty || chat == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String name = chat.getName();

                StackPane avatar = createAvatar(name, 18);

                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                HBox nameBox = new HBox(6, nameLabel);
                nameBox.setAlignment(Pos.CENTER_LEFT);

                if (model.isBotChat(chat)) {
                    Label botBadge = createBotBadge();
                    nameBox.getChildren().add(botBadge);
                }

                HBox container = new HBox(10, avatar, nameBox);
                container.setAlignment(Pos.CENTER_LEFT);

                setText(null);
                setGraphic(container);

                chatMenu = new ContextMenu();

                MenuItem clearItem = new MenuItem("Clear chat");
                clearItem.setOnAction(event -> onClearChatClick());

                MenuItem renameItem = new MenuItem("Rename");
                renameItem.setOnAction(event -> onRenameChatClick());

                MenuItem deleteItem = new MenuItem("Delete chat");
                deleteItem.setOnAction(event -> onDeleteChatClick());

                chatMenu.getItems().addAll(clearItem, renameItem, deleteItem);
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

        if (!model.getChats().isEmpty()) {
            selectedChat = model.getChats().get(0);
            contactsListView.getSelectionModel().select(selectedChat);
            openChat(selectedChat);
        }

        contactsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedChat = newValue;
                        openChat(selectedChat);
                        hideDropdownMenu();
                    }
                }
        );

        messageTextField.setOnAction(event -> onSendButtonClick());
    }

    private void openChat(Chat chat) {
        updateChatHeader(chat);
        messagesListView.setItems(model.getMessagesForChat(chat));
    }

    private void updateChatHeader(Chat chat) {
        String name = chat.getName();

        chatTitleLabel.setText(name);
        chatTitleLabel.setGraphic(null);

        if (model.isBotChat(chat)) {
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
        if (chatMenu.isShowing()) {
            chatMenu.hide();
        } else {
            chatMenu.show(menuButton, javafx.geometry.Side.BOTTOM, 0, 6);
        }
    }

    @FXML
    private void onClearChatClick() {
        hideDropdownMenu();

        if (selectedChat == null) {
            return;
        }

        clearChatOverlay.setVisible(true);
        clearChatOverlay.toFront();
    }

    @FXML
    private void onCancelClearChatClick() {
        clearChatOverlay.setVisible(false);
    }

    @FXML
    private void onConfirmClearChatClick() {
        if (selectedChat == null) {
            clearChatOverlay.setVisible(false);
            return;
        }

        model.clearChat(selectedChat);

        messagesListView.setItems(
                model.getMessagesForChat(selectedChat)
        );

        clearChatOverlay.setVisible(false);
    }

    @FXML
    private void onRenameChatClick() {
        hideDropdownMenu();

        if (selectedChat == null) {
            return;
        }

        renameChatNameField.setText(selectedChat.getName());

        renameChatErrorLabel.setText("");
        renameChatErrorLabel.setVisible(false);
        renameChatErrorLabel.setManaged(false);

        renameChatOverlay.setVisible(true);
        renameChatOverlay.toFront();

        renameChatNameField.requestFocus();
        renameChatNameField.selectAll();
    }

    @FXML
    private void onCancelRenameChatClick() {
        renameChatOverlay.setVisible(false);

        renameChatNameField.clear();

        renameChatErrorLabel.setText("");
        renameChatErrorLabel.setVisible(false);
        renameChatErrorLabel.setManaged(false);
    }

    @FXML
    private void onConfirmRenameChatClick() {
        if (selectedChat == null) {
            return;
        }

        String newName = renameChatNameField.getText().trim();

        if (newName.isEmpty()) {
            showRenameChatError("Please enter chat name.");
            return;
        }

        if (newName.equals(selectedChat.getName())) {
            renameChatOverlay.setVisible(false);
            return;
        }

        model.renameChat(selectedChat, newName);

        selectedChat = contactsListView.getSelectionModel().getSelectedItem();

        openChat(selectedChat);
        contactsListView.refresh();

        renameChatOverlay.setVisible(false);
        renameChatNameField.clear();
    }

    private void showRenameChatError(String message) {
        renameChatErrorLabel.setText(message);
        renameChatErrorLabel.setVisible(true);
        renameChatErrorLabel.setManaged(true);
    }

    @FXML
    private void onDeleteChatClick() {
        hideDropdownMenu();

        if (selectedChat == null) {
            return;
        }

        deleteChatOverlay.setVisible(true);
        deleteChatOverlay.toFront();
    }

    @FXML
    private void onCancelDeleteChatClick() {
        deleteChatOverlay.setVisible(false);
    }

    @FXML
    private void onConfirmDeleteChatClick() {
        if (selectedChat == null) {
            deleteChatOverlay.setVisible(false);
            return;
        }

        Chat chatToDelete = selectedChat;

        model.deleteChat(chatToDelete);

        selectedChat = null;

        messagesListView.setItems(null);
        chatTitleLabel.setText("");

        deleteChatOverlay.setVisible(false);

        if (!contactsListView.getItems().isEmpty()) {
            contactsListView.getSelectionModel().selectFirst();
            selectedChat = contactsListView.getSelectionModel().getSelectedItem();
            openChat(selectedChat);
        }
    }

    @FXML
    private void onCreateChatClick() {
        hideDropdownMenu();

        createChatUsernameField.clear();

        createChatErrorLabel.setText("");
        createChatErrorLabel.setVisible(false);
        createChatErrorLabel.setManaged(false);

        createChatOverlay.setVisible(true);
        createChatOverlay.toFront();

        createChatUsernameField.requestFocus();
    }

    @FXML
    private void onCancelCreateChatClick() {
        createChatOverlay.setVisible(false);

        createChatUsernameField.clear();

        createChatErrorLabel.setText("");
        createChatErrorLabel.setVisible(false);
        createChatErrorLabel.setManaged(false);
    }

    @FXML
    private void onConfirmCreateChatClick() {
        String username = createChatUsernameField.getText().trim();

        if (username.isEmpty()) {
            showCreateChatError("Please enter username.");
            return;
        }

        CreateChatResponse response = model.addChat(username);

        switch (response.getResult()) {
            case SUCCESS -> {
                createChatOverlay.setVisible(false);
                createChatOverlay.setManaged(false);

                createChatUsernameField.clear();

                createChatErrorLabel.setText("");
                createChatErrorLabel.setVisible(false);
                createChatErrorLabel.setManaged(false);

                selectedChat = response.getChat();
                contactsListView.getSelectionModel().select(selectedChat);
                openChat(selectedChat);
            }

            case USER_NOT_FOUND -> showCreateChatError(
                    "No user with this username was found."
            );

            case SELF_CHAT -> showCreateChatError(
                    "You cannot create a chat with yourself."
            );

            case CHAT_ALREADY_EXISTS -> showCreateChatError(
                    "You already have a chat with this user."
            );

            case DATABASE_ERROR -> showCreateChatError(
                    "Could not create chat. Please try again."
            );
        }
    }

    private void showCreateChatError(String message) {
        createChatErrorLabel.setText(message);
        createChatErrorLabel.setVisible(true);
        createChatErrorLabel.setManaged(true);
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
        model.addMessage(selectedChat, userMessage);

        if (model.isBotChat(selectedChat)) {
            Message botMessage = model.generateBotResponse(text);
            model.addBotMessage(selectedChat, botMessage);
        }

        messageTextField.clear();
        messagesListView.scrollTo(
                model.getMessagesForChat(selectedChat).size() - 1
        );
    }

    private void showMessage(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void hideDropdownMenu() {
        if (chatMenu != null) {
            chatMenu.hide();
        }
    }
}
