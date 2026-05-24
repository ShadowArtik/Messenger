package com.example.controller;

import com.example.model.Chat;
import com.example.model.Message;
import com.example.model.MessengerModel;
import com.example.model.Session;
import com.example.service.result.CreateChatResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

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
    @FXML
    private StackPane logoutOverlay;

    @FXML private ListView<Message> messagesListView;
    @FXML private Label chatTitleLabel;
    @FXML private StackPane chatAvatar;
    @FXML private TextField messageTextField;
    @FXML private Button menuButton;

    private ContextMenu chatContextMenu;
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

                HBox root = new HBox(10);
                root.setAlignment(Pos.CENTER_LEFT);

                StackPane avatar = createAvatar(chat.getName(), 20);

                VBox textBox = new VBox(3);
                textBox.setMaxWidth(130);

                HBox topRow = new HBox(6);
                topRow.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(chat.getName());
                nameLabel.setStyle(
                        "-fx-text-fill: white;" +
                                "-fx-font-size: 14px;" +
                                "-fx-font-weight: bold;"
                );

                nameLabel.setMaxWidth(80);
                nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

                topRow.getChildren().add(nameLabel);

                if (model.isBotChat(chat)) {
                    Label botBadge = new Label("BOT");
                    botBadge.getStyleClass().add("bot-badge");
                    topRow.getChildren().add(botBadge);
                }

                HBox bottomRow = new HBox(6);
                bottomRow.setAlignment(Pos.CENTER_LEFT);

                Label lastMessageLabel = new Label();

                if (chat.getLastMessageText() == null || chat.getLastMessageText().isBlank()) {
                    lastMessageLabel.setText("No messages yet");
                } else {
                    lastMessageLabel.setText(chat.getLastMessageText());
                }

                lastMessageLabel.setStyle(
                        "-fx-text-fill: #B8BDC7;" +
                                "-fx-font-size: 12px;"
                );

                lastMessageLabel.setMaxWidth(90);
                lastMessageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

                Label lastMessageTimeLabel = new Label();

                if (chat.getLastMessageTime() != null && !chat.getLastMessageTime().isBlank()) {
                    lastMessageTimeLabel.setText(chat.getLastMessageTime());
                }

                lastMessageTimeLabel.setStyle(
                        "-fx-text-fill: #8A8F99;" +
                                "-fx-font-size: 11px;"
                );

                bottomRow.getChildren().addAll(
                        lastMessageLabel,
                        lastMessageTimeLabel
                );

                textBox.getChildren().addAll(topRow, bottomRow);

                root.getChildren().addAll(avatar, textBox);

                setText(null);
                setGraphic(root);
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
                    }
                }
        );

        messageTextField.setOnAction(event -> onSendButtonClick());

        setupChatContextMenu();
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
        if (selectedChat == null) {
            return;
        }

        if (chatContextMenu.isShowing()) {
            chatContextMenu.hide();
        } else {
            chatContextMenu.show(menuButton, Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    private void onClearChatClick() {

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

        Chat updatedChat = model.clearChat(selectedChat);

        if (updatedChat != null) {
            selectedChat = updatedChat;

            messagesListView.setItems(
                    model.getMessagesForChat(selectedChat)
            );

            contactsListView.getSelectionModel().select(selectedChat);
            contactsListView.refresh();
            openChat(selectedChat);
        }

        clearChatOverlay.setVisible(false);
    }

    @FXML
    private void onRenameChatClick() {

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
        if (selectedChat == null) {
            return;
        }

        String text = messageTextField.getText().trim();

        if (text.isEmpty()) {
            messageTextField.clear();
            return;
        }

        Message userMessage = new Message(
                Session.getCurrentUser().getUsername(),
                text
        );

        Chat updatedChat = model.addMessage(selectedChat, userMessage);

        if (updatedChat != null) {
            selectedChat = updatedChat;
        }

        if (model.isBotChat(selectedChat)) {
            Message botMessage = model.generateBotResponse(text);

            Chat updatedBotChat = model.addBotMessage(selectedChat, botMessage);

            if (updatedBotChat != null) {
                selectedChat = updatedBotChat;
            }
        }

        contactsListView.getSelectionModel().select(selectedChat);
        openChat(selectedChat);

        messageTextField.clear();

        if (model.getMessagesForChat(selectedChat) != null &&
                !model.getMessagesForChat(selectedChat).isEmpty()) {

            messagesListView.scrollTo(
                    model.getMessagesForChat(selectedChat).size() - 1
            );
        }
    }

    private void showMessage(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void setupChatContextMenu() {
        MenuItem clearChatItem = new MenuItem("Clear chat");
        MenuItem renameChatItem = new MenuItem("Rename");
        MenuItem deleteChatItem = new MenuItem("Delete chat");

        clearChatItem.setOnAction(event -> onClearChatClick());
        renameChatItem.setOnAction(event -> onRenameChatClick());
        deleteChatItem.setOnAction(event -> onDeleteChatClick());

        chatContextMenu = new ContextMenu(
                clearChatItem,
                renameChatItem,
                deleteChatItem
        );

        chatContextMenu.getStyleClass().add("chat-context-menu");
    }

    @FXML
    private void onLogoutClick() {
        hideChatMenu();

        if (logoutOverlay != null) {
            logoutOverlay.setVisible(true);
            logoutOverlay.setManaged(true);
        }
    }

    @FXML
    private void onCancelLogoutClick() {
        if (logoutOverlay != null) {
            logoutOverlay.setVisible(false);
            logoutOverlay.setManaged(false);
        }
    }

    @FXML
    private void onConfirmLogoutClick() {
        Session.clear();
        openLoginScreen();
    }

    private void openLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Login.fxml")
            );

            Scene scene = new Scene(loader.load(), 800, 500);

            scene.getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm()
            );

            Stage stage = (Stage) contactsListView.getScene().getWindow();
            stage.setTitle("Messenger");
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error", "Cannot return to login screen.");
        }
    }

    private void hideChatMenu() {
        if (chatContextMenu != null && chatContextMenu.isShowing()) {
            chatContextMenu.hide();
        }
    }
}
