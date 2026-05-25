package com.example.controller;

import com.example.model.Chat;
import com.example.model.Message;
import com.example.model.MessengerModel;
import com.example.model.Session;
import com.example.model.User;
import com.example.service.UserService;
import com.example.service.result.CreateChatResponse;
import com.example.service.result.CreateGroupResponse;
import com.example.network.WebSocketClient;
import com.example.network.dto.IncomingMessage;
import com.google.gson.Gson;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @FXML
    private StackPane profileOverlay;
    @FXML
    private StackPane createGroupOverlay;
    @FXML
    private VBox chatArea;
    @FXML
    private VBox emptyChatBox;
    @FXML
    private TextField chatSearchField;
    @FXML
    private StackPane currentUserAvatar;
    @FXML
    private Label currentUserDisplayNameLabel;
    @FXML
    private Label currentUserUsernameLabel;
    @FXML
    private Label profileUsernameLabel;
    @FXML
    private TextField profileDisplayNameField;
    @FXML
    private TextField groupNameField;
    @FXML
    private ListView<Chat> groupMembersListView;

    @FXML private ListView<Message> messagesListView;
    @FXML private Label chatTitleLabel;
    @FXML private Label typingLabel;
    @FXML private Label clearChatSearchLabel;
    @FXML private StackPane chatAvatar;
    @FXML private TextField messageTextField;
    @FXML private Button menuButton;
    @FXML private Button sendButton;
    @FXML private Button addChatButton;

    private ContextMenu chatContextMenu;
    private ContextMenu addChatContextMenu;
    private MessengerModel model;
    private Chat selectedChat;
    private WebSocketClient webSocketClient;
    private final UserService userService = new UserService();
    private FilteredList<Chat> filteredChats;
    private PauseTransition typingHideDelay;
    private final Set<Integer> typingChatIds = new HashSet<>();
    private final Set<Integer> selectedGroupMemberIds = new HashSet<>();
    private final Map<Integer, PauseTransition> chatTypingHideDelays = new HashMap<>();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        model = new MessengerModel();

        webSocketClient = new WebSocketClient();

        webSocketClient.setMessageHandler(this::handleIncomingWebSocketMessage);

        webSocketClient.connect(
                Session.getCurrentUser().getId(),
                Session.getCurrentUser().getUsername(),
                Session.getCurrentUser().getDisplayName()
        );

        filteredChats = new FilteredList<>(
                model.getChats(),
                chat -> true
        );

        contactsListView.setItems(filteredChats);

        chatSearchField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    filterChats(newValue);
                    updateClearChatSearchLabel(newValue);
                }
        );

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
                root.setMaxWidth(Double.MAX_VALUE);
                root.getStyleClass().add("chat-cell-root");

                StackPane avatar = createAvatar(chat, 20);

                VBox textBox = new VBox(3);
                textBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                HBox nameRow = new HBox(6);
                nameRow.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(chat.getName());
                nameLabel.getStyleClass().add("chat-name-label");
                nameLabel.setMaxWidth(95);
                nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

                nameRow.getChildren().add(nameLabel);

                if (model.isBotChat(chat)) {
                    Label botBadge = createBotBadge();
                    nameRow.getChildren().add(botBadge);
                }

                Label lastMessageLabel = new Label();

                if (isChatTyping(chat)) {
                    lastMessageLabel.setText("is typing...");
                    lastMessageLabel.getStyleClass().add("chat-typing-preview-label");
                } else if (chat.getLastMessageText() == null || chat.getLastMessageText().isBlank()) {
                    lastMessageLabel.setText("No messages yet");
                } else {
                    lastMessageLabel.setText(chat.getLastMessageText());
                }

                lastMessageLabel.getStyleClass().add("chat-last-message-label");
                lastMessageLabel.setMaxWidth(95);
                lastMessageLabel.setWrapText(false);
                lastMessageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

                textBox.getChildren().addAll(nameRow, lastMessageLabel);

                VBox infoBox = new VBox(5);
                infoBox.setAlignment(Pos.TOP_RIGHT);
                infoBox.setMinWidth(38);
                infoBox.setPrefWidth(38);
                infoBox.setMaxWidth(38);

                Label timeLabel = new Label();

                if (chat.getLastMessageTime() == null || chat.getLastMessageTime().isBlank()) {
                    timeLabel.setText("");
                } else {
                    timeLabel.setText(chat.getLastMessageTime());
                }

                timeLabel.getStyleClass().add("chat-time-label");

                infoBox.getChildren().add(timeLabel);

                if (chat.getUnreadCount() > 0) {
                    Label unreadLabel = new Label(
                            String.valueOf(chat.getUnreadCount())
                    );
                    unreadLabel.getStyleClass().add("unread-badge");

                    infoBox.getChildren().add(unreadLabel);
                }

                root.getChildren().addAll(avatar, textBox, infoBox);

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

        showEmptyChatState();

        contactsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedChat = newValue;
                        openChat(selectedChat);
                    }
                }
        );

        messageTextField.setOnAction(event -> onSendButtonClick());
        sendButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> messageTextField.getText().trim().isEmpty(),
                        messageTextField.textProperty()
                )
        );
        messageTextField.textProperty().addListener(
                (observable, oldValue, newValue) -> sendTypingStatus()
        );

        Platform.runLater(() -> {
            Scene scene = contactsListView.getScene();

            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        hideAllOverlays();
                    }
                });
            }
        });

        setupChatContextMenu();
        setupAddChatContextMenu();
        setupGroupMembersList();
        setupCurrentUserProfile();
    }

    private void openChat(Chat chat) {
        if (chat == null) {
            return;
        }

        hideTypingLabel();

        int unreadCountBeforeOpen = chat.getUnreadCount();

        Chat updatedChat = model.resetUnreadCount(chat);

        if (updatedChat != null) {
            selectedChat = updatedChat;
        } else {
            selectedChat = chat;
        }

        emptyChatBox.setVisible(false);
        emptyChatBox.setManaged(false);

        chatArea.setVisible(true);
        chatArea.setManaged(true);

        contactsListView.getSelectionModel().select(selectedChat);
        contactsListView.refresh();

        updateChatHeader(selectedChat);

        messagesListView.setItems(
                model.getMessagesForChat(selectedChat)
        );

        int messageCount = model.getMessagesForChat(selectedChat).size();

        if (messageCount == 0) {
            return;
        }

        int scrollIndex;

        if (unreadCountBeforeOpen > 0) {
            scrollIndex = messageCount - unreadCountBeforeOpen;

            if (scrollIndex < 0) {
                scrollIndex = 0;
            }

            scrollMessagesTo(scrollIndex);
        } else {
            scrollMessagesToBottom();
        }
    }

    private void showEmptyChatState() {
        hideTypingLabel();

        selectedChat = null;

        contactsListView.getSelectionModel().clearSelection();

        chatArea.setVisible(false);
        chatArea.setManaged(false);

        emptyChatBox.setVisible(true);
        emptyChatBox.setManaged(true);

        messagesListView.setItems(null);
        messageTextField.clear();
        chatTitleLabel.setText("");
        chatTitleLabel.setGraphic(null);
        chatAvatar.getChildren().clear();
    }

    private void filterChats(String text) {
        String searchText = text == null
                ? ""
                : text.trim().toLowerCase();

        filteredChats.setPredicate(chat -> {
            if (searchText.isEmpty()) {
                return true;
            }

            return chat.getName()
                    .toLowerCase()
                    .contains(searchText);
        });
    }

    private void updateClearChatSearchLabel(String text) {
        boolean hasSearchText = text != null && !text.isBlank();

        clearChatSearchLabel.setVisible(hasSearchText);
        clearChatSearchLabel.setManaged(hasSearchText);
    }

    @FXML
    private void onClearChatSearchClick() {
        chatSearchField.clear();
        chatSearchField.requestFocus();
    }

    private void scrollMessagesTo(int index) {
        Platform.runLater(() -> Platform.runLater(
                () -> messagesListView.scrollTo(index)
        ));
    }

    private void scrollMessagesToBottom() {
        Platform.runLater(() -> Platform.runLater(() -> {
            if (messagesListView.getItems() == null) {
                return;
            }

            int messageCount = messagesListView.getItems().size();

            if (messageCount == 0) {
                return;
            }

            int lastIndex = messageCount - 1;

            messagesListView.scrollTo(lastIndex);
            messagesListView.applyCss();
            messagesListView.layout();

            Platform.runLater(() -> {
                messagesListView.scrollTo(lastIndex);

                ScrollBar verticalScrollBar =
                        (ScrollBar) messagesListView.lookup(".scroll-bar:vertical");

                if (verticalScrollBar != null) {
                    verticalScrollBar.setValue(verticalScrollBar.getMax());
                }
            });
        }));
    }

    private void hideTypingLabel() {
        if (typingHideDelay != null) {
            typingHideDelay.stop();
        }

        if (typingLabel != null) {
            typingLabel.setText("");
            typingLabel.setVisible(false);
            typingLabel.setManaged(true);
        }
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
        chatAvatar.getChildren().add(createAvatar(chat, 20));
    }

    private StackPane createAvatar(Chat chat, double radius) {
        String name = chat.getName();

        StackPane avatar = createBaseAvatar(name, radius);

        if (!model.isBotChat(chat)
                && chat.getCompanionUserId() != null
                && model.isUserOnline(chat.getCompanionUserId())) {

            double statusDotSize = Math.max(12, radius * 0.52);

            Region statusDot = new Region();
            statusDot.setPrefSize(statusDotSize, statusDotSize);
            statusDot.setMaxSize(statusDotSize, statusDotSize);
            statusDot.getStyleClass().add("online-status-dot");

            StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(statusDot, new Insets(0, 0, 0, 0));

            avatar.getChildren().add(statusDot);
        }

        return avatar;
    }

    private StackPane createBaseAvatar(String name, double radius) {
        StackPane avatar = new StackPane();

        avatar.setMinSize(radius * 2, radius * 2);
        avatar.setPrefSize(radius * 2, radius * 2);
        avatar.setMaxSize(radius * 2, radius * 2);

        avatar.setStyle(
                "-fx-background-color: " + getAvatarColor(name) + ";" +
                        "-fx-background-radius: " + radius + ";"
        );

        Label letter = new Label(name.substring(0, 1).toUpperCase());
        letter.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        avatar.getChildren().add(letter);

        return avatar;
    }

    private String getAvatarColor(String name) {
        if (name == null || name.isBlank()) {
            return "#5B9DFF";
        }

        String[] colors = {
                "#5B9DFF",
                "#FF5B81",
                "#7D5FFF",
                "#2ED573",
                "#FFA502",
                "#FF4757",
                "#1E90FF",
                "#9C88FF"
        };

        int index = Math.abs(name.hashCode()) % colors.length;

        return colors[index];
    }

    private Label createBotBadge() {
        Label botBadge = new Label("BOT");
        botBadge.getStyleClass().add("bot-badge");
        return botBadge;
    }

    private void setupCurrentUserProfile() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        String displayName = Session.getCurrentUser().getDisplayName();
        String username = Session.getCurrentUser().getUsername();

        currentUserDisplayNameLabel.setText(displayName);
        currentUserUsernameLabel.setText("@" + username);

        currentUserAvatar.getChildren().clear();
        currentUserAvatar.getChildren().add(
                createBaseAvatar(displayName, 18)
        );
    }

    @FXML
    private void onEditProfileClick() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        hideAllOverlays();

        profileUsernameLabel.setText(
                "@" + Session.getCurrentUser().getUsername()
        );

        profileDisplayNameField.setText(
                Session.getCurrentUser().getDisplayName()
        );

        profileOverlay.setVisible(true);
        profileOverlay.setManaged(true);
        profileOverlay.toFront();

        Platform.runLater(() -> {
            profileDisplayNameField.requestFocus();
            profileDisplayNameField.positionCaret(
                    profileDisplayNameField.getText().length()
            );
        });
    }

    @FXML
    private void onCancelProfileClick() {
        hideAllOverlays();
    }

    @FXML
    private void onSaveProfileClick() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        String newDisplayName = profileDisplayNameField.getText().trim();

        if (newDisplayName.isEmpty()) {
            showMessage(
                    "Invalid name",
                    "Display name cannot be empty."
            );
            return;
        }

        boolean updated = userService.updateDisplayName(
                Session.getCurrentUser().getId(),
                newDisplayName
        );

        if (!updated) {
            showMessage(
                    "Error",
                    "Could not update display name."
            );
            return;
        }

        User updatedUser = new User(
                Session.getCurrentUser().getId(),
                Session.getCurrentUser().getUsername(),
                newDisplayName
        );

        Session.setCurrentUser(updatedUser);

        setupCurrentUserProfile();
        webSocketClient.sendProfileUpdatedMessage(
                updatedUser.getId(),
                updatedUser.getDisplayName()
        );
        hideAllOverlays();
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
        clearChatOverlay.setManaged(true);
        clearChatOverlay.toFront();
    }

    @FXML
    private void onCancelClearChatClick() {
        hideAllOverlays();
    }

    @FXML
    private void onConfirmClearChatClick() {
        if (selectedChat == null) {
            hideAllOverlays();
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

        hideAllOverlays();
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
        renameChatOverlay.setManaged(true);
        renameChatOverlay.toFront();

        renameChatNameField.requestFocus();
        renameChatNameField.selectAll();
    }

    @FXML
    private void onCancelRenameChatClick() {
        hideAllOverlays();

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
            hideAllOverlays();
            return;
        }

        model.renameChat(selectedChat, newName);

        selectedChat = contactsListView.getSelectionModel().getSelectedItem();

        openChat(selectedChat);
        contactsListView.refresh();

        hideAllOverlays();
        renameChatNameField.clear();
    }

    @FXML
    private void onResetRenameChatClick() {
        if (selectedChat == null) {
            return;
        }

        Chat updatedChat = model.resetChatName(selectedChat);

        if (updatedChat != null) {
            selectedChat = updatedChat;
            contactsListView.getSelectionModel().select(selectedChat);
            contactsListView.refresh();
            openChat(selectedChat);
        }

        hideAllOverlays();
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
        deleteChatOverlay.setManaged(true);
        deleteChatOverlay.toFront();
    }

    @FXML
    private void onCancelDeleteChatClick() {
        hideAllOverlays();
    }

    @FXML
    private void onConfirmDeleteChatClick() {
        if (selectedChat == null) {
            hideAllOverlays();
            return;
        }

        Chat chatToDelete = selectedChat;

        model.deleteChat(chatToDelete);

        hideAllOverlays();
        showEmptyChatState();
    }

    @FXML
    private void onCreateChatClick() {

        createChatUsernameField.clear();

        createChatErrorLabel.setText("");
        createChatErrorLabel.setVisible(false);
        createChatErrorLabel.setManaged(false);

        createChatOverlay.setVisible(true);
        createChatOverlay.setManaged(true);
        createChatOverlay.toFront();

        createChatUsernameField.requestFocus();
    }

    private void showCreateGroupChatOverlay() {
        groupNameField.clear();
        selectedGroupMemberIds.clear();
        groupMembersListView.setItems(getAvailableGroupMembers());

        createGroupOverlay.setVisible(true);
        createGroupOverlay.setManaged(true);
        createGroupOverlay.toFront();

        groupNameField.requestFocus();
    }

    @FXML
    private void onCancelCreateGroupClick() {
        groupNameField.clear();
        selectedGroupMemberIds.clear();
        groupMembersListView.getItems().clear();

        createGroupOverlay.setVisible(false);
        createGroupOverlay.setManaged(false);
    }

    @FXML
    private void onConfirmCreateGroupClick() {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            showMessage("Invalid group name", "Please enter group name.");
            return;
        }

        if (selectedGroupMemberIds.isEmpty()) {
            showMessage("Invalid members", "Please choose at least one member.");
            return;
        }

        CreateGroupResponse response = model.createGroupChat(
                groupName,
                new ArrayList<>(selectedGroupMemberIds)
        );

        switch (response.getResult()) {
            case SUCCESS -> {
                selectedChat = response.getChat();
                contactsListView.getSelectionModel().select(selectedChat);
                contactsListView.refresh();
                openChat(selectedChat);

                webSocketClient.sendGroupCreatedMessage(
                        Session.getCurrentUser().getId(),
                        response.getChat().getId(),
                        response.getChat().getName(),
                        response.getMemberIds()
                );

                groupNameField.clear();
                selectedGroupMemberIds.clear();
                groupMembersListView.getItems().clear();

                createGroupOverlay.setVisible(false);
                createGroupOverlay.setManaged(false);
            }
            case EMPTY_GROUP_NAME -> showMessage(
                    "Invalid group name",
                    "Please enter group name."
            );
            case EMPTY_MEMBERS, ONLY_SELF_SELECTED -> showMessage(
                    "Invalid members",
                    "Please choose at least one member."
            );
            case DATABASE_ERROR -> showMessage(
                    "Error",
                    "Could not create group chat."
            );
            case USER_NOT_FOUND -> showMessage(
                    "Error",
                    "One of the selected users was not found."
            );
        }
    }

    private ObservableList<Chat> getAvailableGroupMembers() {
        ObservableList<Chat> users = FXCollections.observableArrayList();

        for (Chat chat : model.getChats()) {
            if ("PRIVATE".equalsIgnoreCase(chat.getType())
                    && chat.getCompanionUserId() != null) {
                users.add(chat);
            }
        }

        return users;
    }

    private void setupGroupMembersList() {
        groupMembersListView.setCellFactory(listView -> new ListCell<>() {

            @Override
            protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);

                if (empty || chat == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                CheckBox checkBox = new CheckBox(chat.getName());
                checkBox.setSelected(
                        selectedGroupMemberIds.contains(chat.getCompanionUserId())
                );

                checkBox.selectedProperty().addListener((observable, oldValue, selected) -> {
                    if (selected) {
                        selectedGroupMemberIds.add(chat.getCompanionUserId());
                    } else {
                        selectedGroupMemberIds.remove(chat.getCompanionUserId());
                    }
                });

                checkBox.getStyleClass().add("group-member-checkbox");

                setText(null);
                setGraphic(checkBox);
            }
        });
    }

    @FXML
    private void onCancelCreateChatClick() {
        hideAllOverlays();

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

    private void sendTypingStatus() {
        if (selectedChat == null) {
            return;
        }

        if (model.isBotChat(selectedChat)) {
            return;
        }

        String text = messageTextField.getText();

        if (text == null || text.isBlank()) {
            return;
        }

        List<Integer> receiverIds = model.getReceiverIdsForChat(selectedChat);

        if (receiverIds.isEmpty()) {
            return;
        }

        webSocketClient.sendTypingMessage(
                selectedChat.getId(),
                Session.getCurrentUser().getId(),
                receiverIds,
                Session.getCurrentUser().getDisplayName()
        );
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
            contactsListView.getSelectionModel().select(selectedChat);
        }

        if (model.isBotChat(selectedChat)) {
            Message botMessage = model.generateBotResponse(text);
            Chat updatedBotChat = model.addBotMessage(selectedChat, botMessage);

            if (updatedBotChat != null) {
                selectedChat = updatedBotChat;
                contactsListView.getSelectionModel().select(selectedChat);
            }
        } else if (model.isGroupChat(selectedChat)) {
            List<Integer> receiverIds = model.getGroupReceiverIds(selectedChat);

            if (!receiverIds.isEmpty()) {
                webSocketClient.sendGroupMessage(
                        selectedChat.getId(),
                        Session.getCurrentUser().getId(),
                        receiverIds,
                        Session.getCurrentUser().getUsername(),
                        Session.getCurrentUser().getDisplayName(),
                        text
                );
            }
        } else {
            Integer companionUserId = selectedChat.getCompanionUserId();

            if (companionUserId != null) {
                webSocketClient.sendPrivateMessage(
                        selectedChat.getId(),
                        Session.getCurrentUser().getId(),
                        companionUserId,
                        Session.getCurrentUser().getUsername(),
                        Session.getCurrentUser().getDisplayName(),
                        text
                );
            }
        }

        contactsListView.refresh();

        messageTextField.clear();
        messageTextField.requestFocus();

        scrollMessagesToBottom();
    }

    private void handleIncomingWebSocketMessage(String json) {
        try {
            IncomingMessage incoming = gson.fromJson(json, IncomingMessage.class);

            if (incoming == null) {
                return;
            }

            if (incoming.isTyping()) {
                handleTypingMessage(incoming);
                return;
            }

            if (incoming.isOnlineUsers()) {
                model.setOnlineUsers(
                        incoming.getUserIds(),
                        Session.getCurrentUser().getId()
                );

                contactsListView.refresh();

                if (selectedChat != null) {
                    updateChatHeader(selectedChat);
                }

                return;
            }

            if (incoming.isUserOnline()) {
                int userId = incoming.getUserId();

                if (userId != Session.getCurrentUser().getId()) {
                    model.setUserOnline(userId);
                    contactsListView.refresh();

                    if (selectedChat != null) {
                        updateChatHeader(selectedChat);
                    }
                }

                return;
            }

            if (incoming.isUserOffline()) {
                int userId = incoming.getUserId();

                if (userId != Session.getCurrentUser().getId()) {
                    model.setUserOffline(userId);
                    contactsListView.refresh();

                    if (selectedChat != null) {
                        updateChatHeader(selectedChat);
                    }
                }

                return;
            }

            if (incoming.isUserProfileUpdated()) {
                handleUserProfileUpdated(incoming);
                return;
            }

            if (incoming.isGroupCreated()) {
                handleGroupCreatedMessage(incoming);
                return;
            }

            if (!incoming.isPrivateMessage() && !incoming.isGroupMessage()) {
                return;
            }

            int chatId = incoming.getChatId();
            int senderId = incoming.getSenderId();

            if (senderId == Session.getCurrentUser().getId()) {
                return;
            }

            hideChatListTyping(chatId);

            if (selectedChat != null && selectedChat.getId() == chatId) {
                hideTypingLabel();
            }

            int previouslySelectedChatId =
                    selectedChat != null ? selectedChat.getId() : -1;

            Chat incomingChat = model.findChatById(chatId);
            boolean messageLoadedByReload = false;

            if (incomingChat == null) {
                model.reloadChats();
                messageLoadedByReload = true;

                incomingChat = model.findChatById(chatId);

                if (incomingChat == null) {
                    return;
                }
            }

            Chat updatedIncomingChat = incomingChat;

            if (!messageLoadedByReload) {
                Message incomingMessage = new Message(
                        incoming.getSenderUsername(),
                        incoming.getText()
                );

                updatedIncomingChat =
                        model.addIncomingMessage(incomingChat, incomingMessage);
            }

            if (updatedIncomingChat == null) {
                return;
            }

            boolean messageForOpenedChat =
                    previouslySelectedChatId == updatedIncomingChat.getId();

            if (!messageForOpenedChat) {
                updatedIncomingChat =
                        model.increaseUnreadCount(updatedIncomingChat);
            }

            contactsListView.refresh();

            if (messageForOpenedChat) {
                selectedChat = updatedIncomingChat;
                contactsListView.getSelectionModel().select(selectedChat);

                messagesListView.setItems(
                        model.getMessagesForChat(selectedChat)
                );

                scrollMessagesToBottom();

                return;
            }

            Chat selectedAfterUpdate =
                    model.findChatById(previouslySelectedChatId);

            if (selectedAfterUpdate != null) {
                selectedChat = selectedAfterUpdate;
                contactsListView.getSelectionModel().select(selectedChat);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleUserProfileUpdated(IncomingMessage incoming) {
        if (incoming.getUserId() == Session.getCurrentUser().getId()) {
            return;
        }

        int selectedChatId = selectedChat != null ? selectedChat.getId() : -1;

        Chat updatedProfileChat = model.updateCompanionDisplayName(
                incoming.getUserId(),
                incoming.getDisplayName()
        );

        contactsListView.refresh();

        if (selectedChatId == -1) {
            return;
        }

        Chat updatedSelectedChat =
                updatedProfileChat != null && updatedProfileChat.getId() == selectedChatId
                        ? updatedProfileChat
                        : model.findChatById(selectedChatId);

        if (updatedSelectedChat == null) {
            showEmptyChatState();
            return;
        }

        selectedChat = updatedSelectedChat;
        contactsListView.getSelectionModel().select(selectedChat);
        updateChatHeader(selectedChat);
        messagesListView.setItems(model.getMessagesForChat(selectedChat));
    }

    private void handleGroupCreatedMessage(IncomingMessage incoming) {
        if (incoming.getMemberIds() == null) {
            return;
        }

        int currentUserId = Session.getCurrentUser().getId();

        if (!incoming.getMemberIds().contains(currentUserId)) {
            return;
        }

        int selectedChatId = selectedChat != null ? selectedChat.getId() : -1;

        model.reloadChats();
        contactsListView.refresh();

        if (selectedChatId == -1) {
            return;
        }

        Chat updatedSelectedChat = model.findChatById(selectedChatId);

        if (updatedSelectedChat == null) {
            showEmptyChatState();
            return;
        }

        selectedChat = updatedSelectedChat;
        contactsListView.getSelectionModel().select(selectedChat);
        updateChatHeader(selectedChat);
        messagesListView.setItems(model.getMessagesForChat(selectedChat));
    }

    private void handleTypingMessage(IncomingMessage incoming) {
        if (incoming.getSenderId() == Session.getCurrentUser().getId()) {
            return;
        }

        showChatListTyping(incoming.getChatId());

        if (selectedChat == null || selectedChat.getId() != incoming.getChatId()) {
            return;
        }

        typingLabel.setText("is typing...");
        typingLabel.setVisible(true);
        typingLabel.setManaged(true);

        if (typingHideDelay != null) {
            typingHideDelay.stop();
        }

        typingHideDelay = new PauseTransition(Duration.seconds(2));
        typingHideDelay.setOnFinished(event -> hideTypingLabel());
        typingHideDelay.playFromStart();
    }

    private boolean isChatTyping(Chat chat) {
        return chat != null && typingChatIds.contains(chat.getId());
    }

    private void showChatListTyping(int chatId) {
        typingChatIds.add(chatId);
        contactsListView.refresh();

        PauseTransition oldDelay = chatTypingHideDelays.remove(chatId);

        if (oldDelay != null) {
            oldDelay.stop();
        }

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event -> {
            typingChatIds.remove(chatId);
            chatTypingHideDelays.remove(chatId);
            contactsListView.refresh();
        });

        chatTypingHideDelays.put(chatId, delay);
        delay.playFromStart();
    }

    private void hideChatListTyping(int chatId) {
        typingChatIds.remove(chatId);

        PauseTransition delay = chatTypingHideDelays.remove(chatId);

        if (delay != null) {
            delay.stop();
        }

        contactsListView.refresh();
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

    private void setupAddChatContextMenu() {
        MenuItem createPrivateChatItem = new MenuItem("Create private chat");
        MenuItem createGroupChatItem = new MenuItem("Create group chat");

        createPrivateChatItem.setOnAction(event -> onCreateChatClick());
        createGroupChatItem.setOnAction(event -> showCreateGroupChatOverlay());

        addChatContextMenu = new ContextMenu(
                createPrivateChatItem,
                createGroupChatItem
        );

        addChatContextMenu.getStyleClass().add("chat-context-menu");
    }

    @FXML
    private void onAddChatButtonClick() {
        hideChatMenu();

        if (addChatContextMenu.isShowing()) {
            addChatContextMenu.hide();
            return;
        }

        addChatContextMenu.show(
                addChatButton,
                Side.BOTTOM,
                -150,
                6
        );
    }

    @FXML
    private void onLogoutClick() {
        hideChatMenu();

        if (logoutOverlay != null) {
            logoutOverlay.setVisible(true);
            logoutOverlay.setManaged(true);
            logoutOverlay.toFront();
        }
    }

    @FXML
    private void onCancelLogoutClick() {
        hideAllOverlays();
    }

    @FXML
    private void onConfirmLogoutClick() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }

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

        if (addChatContextMenu != null && addChatContextMenu.isShowing()) {
            addChatContextMenu.hide();
        }
    }

    @FXML
    private void onOverlayBackgroundClick() {
        hideAllOverlays();
    }

    @FXML
    private void onModalWindowClick(MouseEvent event) {
        event.consume();
    }

    private void hideAllOverlays() {
        hideChatMenu();

        hideOverlay(createChatOverlay);
        hideOverlay(createGroupOverlay);
        hideOverlay(renameChatOverlay);
        hideOverlay(deleteChatOverlay);
        hideOverlay(clearChatOverlay);
        hideOverlay(logoutOverlay);
        hideOverlay(profileOverlay);
    }

    private void hideOverlay(StackPane overlay) {
        if (overlay != null) {
            overlay.setVisible(false);
            overlay.setManaged(false);
        }
    }
}
