package com.example.controller;

import com.example.model.Chat;
import com.example.model.Message;
import com.example.model.MessengerModel;
import com.example.model.Session;
import com.example.model.User;
import com.example.service.UserService;
import com.example.network.WebSocketClient;
import com.example.view.Avatars;
import com.example.view.MessengerCells;
import com.example.view.MessengerOverlays;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class MessengerController {

    @FXML
    ListView<Chat> contactsListView;
    @FXML
    StackPane createChatOverlay;
    @FXML
    TextField createChatUsernameField;
    @FXML
    Label createChatErrorLabel;
    @FXML
    StackPane renameChatOverlay;
    @FXML
    TextField renameChatNameField;
    @FXML
    Label renameChatErrorLabel;
    @FXML
    Button resetChatNameButton;
    @FXML
    private StackPane deleteChatOverlay;
    @FXML
    private Label deleteChatTitleLabel;
    @FXML
    private Label deleteChatSubtitleLabel;
    @FXML
    private StackPane clearChatOverlay;
    @FXML
    private StackPane logoutOverlay;
    @FXML
    StackPane profileOverlay;
    @FXML
    StackPane createGroupOverlay;
    @FXML
    StackPane groupInfoOverlay;
    @FXML
    StackPane addMembersOverlay;
    @FXML
    StackPane leaveGroupOverlay;
    @FXML
    private VBox chatArea;
    @FXML
    private VBox emptyChatBox;
    @FXML
    private TextField chatSearchField;
    @FXML
    StackPane currentUserAvatar;
    @FXML
    Label currentUserDisplayNameLabel;
    @FXML
    Label currentUserUsernameLabel;
    @FXML
    Label profileUsernameLabel;
    @FXML
    TextField profileDisplayNameField;
    @FXML
    TextField groupNameField;
    @FXML
    ListView<Chat> groupMembersListView;
    @FXML
    Label groupInfoNameLabel;
    @FXML
    ListView<User> groupInfoMembersListView;
    @FXML
    ListView<User> addMembersListView;

    @FXML ListView<Message> messagesListView;
    @FXML Label chatTitleLabel;
    @FXML Label chatSubtitleLabel;
    @FXML Label typingLabel;
    @FXML private Label clearChatSearchLabel;
    @FXML StackPane chatAvatar;
    @FXML TextField messageTextField;
    @FXML private Button menuButton;
    @FXML private Button sendButton;
    @FXML private Button addChatButton;
    @FXML Button addMembersButton;

    private ContextMenu chatContextMenu;
    private ContextMenu addChatContextMenu;
    private MenuItem groupInfoItem;
    private MenuItem leaveGroupItem;
    private MenuItem renameChatItem;
    private MenuItem deleteChatItem;
    private MenuItem clearChatItem;
    MessengerModel model;
    Chat selectedChat;
    WebSocketClient webSocketClient;

    private int unreadOnOpen = 0;
    private int unreadOnOpenChatId = -1;
    private GroupHandler groupHandler;
    private ChatActionsHandler chatActions;
    private ProfileHandler profileHandler;
    final TypingIndicators typing = new TypingIndicators(this);
    final ChatHeaderView chatHeader = new ChatHeaderView(this);
    final UserService userService = new UserService();
    private FilteredList<Chat> filteredChats;
    final Set<Integer> selectedGroupMemberIds = new HashSet<>();

    // =================== Setup ===================

    @FXML
    public void initialize() {
        model = new MessengerModel();

        webSocketClient = new WebSocketClient();

        WebSocketMessageHandler wsHandler = new WebSocketMessageHandler(this);
        webSocketClient.setMessageHandler(wsHandler::handle);

        model.setWebSocketClient(webSocketClient);
        groupHandler = new GroupHandler(this);
        chatActions = new ChatActionsHandler(this);
        profileHandler = new ProfileHandler(this);

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

        contactsListView.setCellFactory(listView -> MessengerCells.contactCell(
                model::isBotChat,
                model::isChatOnline,
                typing::isChatTyping
        ));

        messagesListView.setCellFactory(listView -> MessengerCells.messageCell(
                () -> selectedChat != null && selectedChat.isGroup(),
                () -> selectedChat != null && !selectedChat.isBot(),
                () -> selectedChat != null && !selectedChat.isBot(),
                this::onEditMessage,
                this::onDeleteMessage
        ));

        messagesListView.setSelectionModel(new NoSelectionModel<>());
        messagesListView.setFocusTraversable(false);

        showEmptyChatState();

        contactsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && !model.isReordering()) {
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
                        chatActions.cancelEdit();
                        messageTextField.clear();
                        hideAllOverlays();
                    }
                });
            }
        });

        setupChatContextMenu();
        setupAddChatContextMenu();
        setupGroupMembersList();
        setupGroupInfoMembersList();
        setupAddMembersList();
        profileHandler.setupCurrentUserProfile();
    }

    // =================== Chat navigation ===================

    void openChat(Chat chat) {
        if (chat == null) {
            return;
        }

        chatActions.cancelEdit();
        typing.hideHeaderLabel();

        // Remember how many were unread; the scroll is applied when LOAD_HISTORY
        // arrives (messages are not loaded yet at this point).
        unreadOnOpen = chat.getUnreadCount();
        unreadOnOpenChatId = chat.getId();

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

        webSocketClient.sendLoadHistory(selectedChat.getId());

        if (!selectedChat.isBot()) {
            webSocketClient.sendMessageRead(
                    selectedChat.getId(),
                    Session.getCurrentUser().getId()
            );
        }

    }

    void scrollAfterHistoryLoad(int chatId) {
        int messageCount = model.getMessagesForChat(selectedChat).size();

        if (messageCount == 0) {
            return;
        }

        if (unreadOnOpenChatId == chatId && unreadOnOpen > 0) {
            scrollMessagesTo(Math.max(0, messageCount - unreadOnOpen));
        } else {
            scrollMessagesToBottom();
        }

        unreadOnOpenChatId = -1;
        unreadOnOpen = 0;
    }

    void showEmptyChatState() {
        typing.hideHeaderLabel();

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

    // =================== Search ===================

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
        Platform.runLater(() -> Platform.runLater(() -> {
            messagesListView.scrollTo(index);
            messagesListView.applyCss();
            messagesListView.layout();
            Platform.runLater(() -> messagesListView.scrollTo(index));
        }));
    }

    void scrollMessagesToBottom() {
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

    // =================== View helpers ===================

    void refreshContactList() {
        contactsListView.refresh();
    }

    void selectChatInList(Chat chat) {
        contactsListView.getSelectionModel().select(chat);
    }

    void showMessagesForCurrentChat() {
        if (selectedChat != null) {
            messagesListView.setItems(model.getMessagesForChat(selectedChat));
        }
    }

    void refreshMessages() {
        messagesListView.refresh();
    }

    boolean isGroupInfoVisible() {
        return groupInfoOverlay != null && groupInfoOverlay.isVisible();
    }

    void updateGroupInfoName(String name) {
        if (groupInfoNameLabel != null) {
            groupInfoNameLabel.setText(name);
        }
    }

    void updateChatHeader(Chat chat) {
        chatHeader.update(chat);
    }

    // =================== FXML actions ===================

    @FXML
    private void onEditProfileClick() { profileHandler.openProfile(); }

    @FXML
    private void onSaveProfileClick() { profileHandler.saveProfile(); }

    @FXML
    private void onMenuButtonClick() {
        if (selectedChat == null) {
            return;
        }

        if (groupInfoItem != null) {
            groupInfoItem.setVisible(model.isGroupChat(selectedChat));
        }

        if (leaveGroupItem != null) {
            leaveGroupItem.setVisible(model.isGroupChat(selectedChat));
        }

        if (deleteChatItem != null) {
            boolean isGroup = model.isGroupChat(selectedChat);
            boolean showDelete = !isGroup || model.groupService().isGroupOwner(selectedChat);
            deleteChatItem.setVisible(showDelete);
            deleteChatItem.setText(isGroup ? "Delete group" : "Delete chat");
        }

        if (renameChatItem != null) {
            renameChatItem.setVisible(
                    !model.isGroupChat(selectedChat)
                            || model.canManageGroup(selectedChat)
            );
        }

        if (clearChatItem != null) {
            clearChatItem.setVisible(
                    !model.isGroupChat(selectedChat)
                            || model.canManageGroup(selectedChat)
            );
        }

        if (chatContextMenu.isShowing()) {
            chatContextMenu.hide();
        } else {
            chatContextMenu.show(menuButton, Side.BOTTOM, -100, 0);
        }
    }

    @FXML
    private void onClearChatClick() {

        if (selectedChat == null) {
            return;
        }

        if (model.isGroupChat(selectedChat) && !model.canManageGroup(selectedChat)) {
            return;
        }

        MessengerOverlays.show(clearChatOverlay);
    }

    @FXML
    private void onConfirmClearChatClick() { chatActions.confirmClearChat(); }

    @FXML
    private void onRenameChatClick() { chatActions.openRename(); }

    @FXML
    private void onConfirmRenameChatClick() { chatActions.confirmRenameChat(); }

    @FXML
    private void onResetRenameChatClick() { chatActions.resetRenameChat(); }

    @FXML
    private void onDeleteChatClick() {

        if (selectedChat == null) {
            return;
        }

        if (model.isGroupChat(selectedChat)) {
            deleteChatTitleLabel.setText("Delete group");
            deleteChatSubtitleLabel.setText("The group will be deleted for everyone.");
        } else {
            deleteChatTitleLabel.setText("Delete chat");
            deleteChatSubtitleLabel.setText("This chat will be removed from your chat list.");
        }

        MessengerOverlays.show(deleteChatOverlay);
    }

    @FXML
    private void onConfirmDeleteChatClick() { chatActions.confirmDeleteChat(); }

    @FXML
    private void onCreateChatClick() {
        MessengerOverlays.showCreateChat(
                createChatOverlay,
                createChatUsernameField,
                createChatErrorLabel
        );
    }

    private void showCreateGroupChatOverlay() { groupHandler.showCreateGroupOverlay(); }

    @FXML
    private void onCancelCreateGroupClick() { groupHandler.cancelCreateGroup(); }

    @FXML
    private void onConfirmCreateGroupClick() { groupHandler.confirmCreateGroup(); }

    private void setupGroupMembersList() { groupHandler.setupGroupMembersList(); }

    private void setupAddMembersList() { groupHandler.setupAddMembersList(); }

    private void setupGroupInfoMembersList() { groupHandler.setupGroupInfoMembersList(); }

    @FXML
    private void onConfirmCreateChatClick() { chatActions.confirmCreateChat(); }

    private void sendTypingStatus() { chatActions.sendTypingStatus(); }

    @FXML
    private void onSendButtonClick() { chatActions.sendMessage(); }

    @FXML
    private void onAttachClick() { chatActions.attachImage(); }

    private static final class NoSelectionModel<T> extends MultipleSelectionModel<T> {
        @Override public ObservableList<Integer> getSelectedIndices() { return FXCollections.emptyObservableList(); }
        @Override public ObservableList<T> getSelectedItems() { return FXCollections.emptyObservableList(); }
        @Override public void selectIndices(int index, int... indices) { }
        @Override public void selectAll() { }
        @Override public void selectFirst() { }
        @Override public void selectLast() { }
        @Override public void clearAndSelect(int index) { }
        @Override public void select(int index) { }
        @Override public void select(T obj) { }
        @Override public void clearSelection(int index) { }
        @Override public void clearSelection() { }
        @Override public boolean isSelected(int index) { return false; }
        @Override public boolean isEmpty() { return true; }
        @Override public void selectPrevious() { }
        @Override public void selectNext() { }
    }

    private void onEditMessage(Message message) { chatActions.startEdit(message); }

    private void onDeleteMessage(Message message) { chatActions.deleteMessage(message); }

    void setComposerEditing(boolean editing) {
        if (editing) {
            if (!messageTextField.getStyleClass().contains("composer-editing")) {
                messageTextField.getStyleClass().add("composer-editing");
            }
            messageTextField.setPromptText("Editing message — Esc to cancel");
        } else {
            messageTextField.getStyleClass().remove("composer-editing");
            messageTextField.setPromptText("Enter message");
        }
    }

    // =================== Dialogs & menus ===================

    void showMessage(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void setupChatContextMenu() {
        groupInfoItem = new MenuItem("Group info");
        leaveGroupItem = new MenuItem("Leave group");
        clearChatItem = new MenuItem("Clear chat");
        renameChatItem = new MenuItem("Rename");
        deleteChatItem = new MenuItem("Delete chat");

        groupInfoItem.setOnAction(event -> onGroupInfoClick());
        leaveGroupItem.setOnAction(event -> onLeaveGroupClick());
        clearChatItem.setOnAction(event -> onClearChatClick());
        renameChatItem.setOnAction(event -> onRenameChatClick());
        deleteChatItem.setOnAction(event -> onDeleteChatClick());

        chatContextMenu = new ContextMenu(
                groupInfoItem,
                clearChatItem,
                renameChatItem,
                deleteChatItem,
                leaveGroupItem
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
                0,
                6
        );
    }

    @FXML
    private void onGroupInfoClick() { groupHandler.openGroupInfo(); }

    void refreshGroupInfoMembers() { groupHandler.refreshGroupInfoMembers(); }

    @FXML
    private void onAddMembersClick() { groupHandler.openAddMembers(); }

    @FXML
    private void onCancelAddMembersClick() { groupHandler.cancelAddMembers(); }

    @FXML
    private void onConfirmAddMembersClick() { groupHandler.confirmAddMembers(); }

    @FXML
    private void onLeaveGroupClick() { groupHandler.openLeaveGroup(); }

    @FXML
    private void onConfirmLeaveGroupClick() { groupHandler.confirmLeaveGroup(); }

    // =================== Session ===================

    @FXML
    private void onLogoutClick() {
        hideChatMenu();

        MessengerOverlays.show(logoutOverlay);
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

    void hideChatMenu() {
        if (chatContextMenu != null && chatContextMenu.isShowing()) {
            chatContextMenu.hide();
        }

        if (addChatContextMenu != null && addChatContextMenu.isShowing()) {
            addChatContextMenu.hide();
        }
    }

    @FXML
    private void onCloseOverlay() {
        hideAllOverlays();
    }

    @FXML
    private void onModalWindowClick(MouseEvent event) {
        event.consume();
    }

    void hideAllOverlays() {
        hideChatMenu();

        MessengerOverlays.hideAll(
                createChatOverlay,
                createGroupOverlay,
                groupInfoOverlay,
                addMembersOverlay,
                leaveGroupOverlay,
                renameChatOverlay,
                deleteChatOverlay,
                clearChatOverlay,
                logoutOverlay,
                profileOverlay
        );
    }

    StackPane createSmallMemberAvatar(User user) {
        return Avatars.member(user.getDisplayName(), model.isUserOnline(user.getId()));
    }
}
