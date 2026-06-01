package com.example.controller;

import com.example.model.Chat;
import com.example.model.Message;
import com.example.model.Session;
import com.example.network.ServerApi;
import com.example.service.result.CreateChatResponse;
import com.example.view.MessengerOverlays;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class ChatActionsHandler {

    private final MessengerController c;

    private String editingClientId;

    public ChatActionsHandler(MessengerController controller) {
        this.c = controller;
    }

    // =================== Create private chat ===================

    void confirmCreateChat() {
        String username = c.createChatUsernameField.getText().trim();

        if (username.isEmpty()) {
            showCreateChatError("Please enter username.");
            return;
        }

        CreateChatResponse response = c.model.addChat(username);

        switch (response.getResult()) {
            case SUCCESS -> {
                MessengerOverlays.hide(c.createChatOverlay);

                c.createChatUsernameField.clear();
                MessengerOverlays.clearError(c.createChatErrorLabel);

                c.selectedChat = response.getChat();
                c.contactsListView.getSelectionModel().select(c.selectedChat);
                c.openChat(c.selectedChat);
            }
            case USER_NOT_FOUND -> showCreateChatError("No user with this username was found.");
            case SELF_CHAT -> showCreateChatError("You cannot create a chat with yourself.");
            case CHAT_ALREADY_EXISTS -> showCreateChatError("You already have a chat with this user.");
            case DATABASE_ERROR -> showCreateChatError("Could not create chat. Please try again.");
        }
    }

    void showCreateChatError(String message) {
        MessengerOverlays.showError(c.createChatErrorLabel, message);
    }

    // =================== Clear ===================

    void confirmClearChat() {
        if (c.selectedChat == null) {
            c.hideAllOverlays();
            return;
        }

        Chat updatedChat = c.model.clearChat(c.selectedChat);

        if (updatedChat != null) {
            c.selectedChat = updatedChat;

            c.messagesListView.setItems(c.model.getMessagesForChat(c.selectedChat));

            c.contactsListView.getSelectionModel().select(c.selectedChat);
            c.contactsListView.refresh();
            c.openChat(c.selectedChat);
        }

        c.hideAllOverlays();
    }

    // =================== Rename ===================

    void openRename() {
        if (c.selectedChat == null) {
            return;
        }

        if (c.model.isGroupChat(c.selectedChat) && !c.model.canManageGroup(c.selectedChat)) {
            showRenameChatError("Only group owner or admins can rename this group.");
            return;
        }

        boolean showReset = !c.model.isGroupChat(c.selectedChat);
        c.resetChatNameButton.setVisible(showReset);
        c.resetChatNameButton.setManaged(showReset);

        MessengerOverlays.showRenameChat(
                c.renameChatOverlay,
                c.renameChatNameField,
                c.renameChatErrorLabel,
                c.selectedChat.getName()
        );
    }

    void confirmRenameChat() {
        if (c.selectedChat == null) {
            return;
        }

        String newName = c.renameChatNameField.getText().trim();

        if (newName.isEmpty()) {
            showRenameChatError("Please enter chat name.");
            return;
        }

        if (newName.equals(c.selectedChat.getName())) {
            c.hideAllOverlays();
            return;
        }

        boolean renamedGroup = c.model.isGroupChat(c.selectedChat);
        String oldName = c.selectedChat.getName();
        List<Integer> receiverIds = renamedGroup
                ? c.model.getGroupReceiverIds(c.selectedChat)
                : List.of();

        Chat renamedChat = c.model.renameChat(c.selectedChat, newName);

        if (renamedChat == null) {
            showRenameChatError("Could not rename this chat.");
            return;
        }

        c.selectedChat = renamedChat;

        if (renamedGroup) {
            String systemText = Session.getCurrentUser().getDisplayName()
                    + " renamed the group to " + newName;

            Chat updatedChat = c.model.addSystemMessage(
                    c.selectedChat,
                    systemText,
                    Session.getCurrentUser().getId()
            );

            if (updatedChat != null) {
                c.selectedChat = updatedChat;
            }

            if (!receiverIds.isEmpty()) {
                c.webSocketClient.sendGroupRenamedMessage(
                        Session.getCurrentUser().getId(),
                        c.selectedChat.getId(),
                        oldName,
                        newName,
                        receiverIds,
                        systemText
                );
            }
        }

        c.contactsListView.getSelectionModel().select(c.selectedChat);
        c.openChat(c.selectedChat);
        c.contactsListView.refresh();

        c.hideAllOverlays();
        c.renameChatNameField.clear();
    }

    void resetRenameChat() {
        if (c.selectedChat == null) {
            return;
        }

        Chat updatedChat = c.model.resetChatName(c.selectedChat);

        if (updatedChat != null) {
            c.selectedChat = updatedChat;
            c.contactsListView.getSelectionModel().select(c.selectedChat);
            c.contactsListView.refresh();
            c.openChat(c.selectedChat);
        }

        c.hideAllOverlays();
        c.renameChatNameField.clear();
    }

    void showRenameChatError(String message) {
        MessengerOverlays.showError(c.renameChatErrorLabel, message);
    }

    // =================== Delete ===================

    void confirmDeleteChat() {
        if (c.selectedChat == null) {
            c.hideAllOverlays();
            return;
        }

        if (c.model.isGroupChat(c.selectedChat)) {
            c.model.deleteGroupForAll(c.selectedChat);
        } else {
            c.model.deleteChat(c.selectedChat);
        }

        c.hideAllOverlays();
        c.showEmptyChatState();
    }

    // =================== Messaging ===================

    void sendTypingStatus() {
        if (c.selectedChat == null) {
            return;
        }

        if (c.model.isBotChat(c.selectedChat)) {
            return;
        }

        String text = c.messageTextField.getText();

        if (text == null || text.isBlank()) {
            return;
        }

        List<Integer> receiverIds = c.model.getReceiverIdsForChat(c.selectedChat);

        if (receiverIds.isEmpty()) {
            return;
        }

        c.webSocketClient.sendTypingMessage(
                c.selectedChat.getId(),
                Session.getCurrentUser().getId(),
                receiverIds,
                Session.getCurrentUser().getDisplayName()
        );
    }

    void sendMessage() {
        if (c.selectedChat == null) {
            return;
        }

        String text = c.messageTextField.getText().trim();

        if (text.isEmpty()) {
            cancelEdit();
            c.messageTextField.clear();
            return;
        }

        if (editingClientId != null) {
            commitEdit(text);
            return;
        }

        sendText(text);

        c.messageTextField.clear();
        c.messageTextField.requestFocus();
    }

    // =================== Edit / delete a message ===================

    void startEdit(Message message) {
        if (message == null || c.selectedChat == null || c.selectedChat.isBot()) {
            return;
        }

        editingClientId = message.getClientId();
        c.setComposerEditing(true);
        c.messageTextField.setText(message.getText());
        c.messageTextField.requestFocus();
        c.messageTextField.positionCaret(c.messageTextField.getText().length());
    }

    void cancelEdit() {
        if (editingClientId == null) {
            return;
        }

        editingClientId = null;
        c.setComposerEditing(false);
    }

    private void commitEdit(String newText) {
        Chat chat = c.selectedChat;
        String clientId = editingClientId;

        c.webSocketClient.sendEditMessage(
                chat.getId(),
                Session.getCurrentUser().getId(),
                clientId,
                newText
        );

        cancelEdit();
        c.messageTextField.clear();
        c.messageTextField.requestFocus();
    }

    void deleteMessage(Message message) {
        if (message == null || c.selectedChat == null || c.selectedChat.isBot()) {
            return;
        }

        String clientId = message.getClientId();
        Chat chat = c.selectedChat;

        if (clientId != null && clientId.equals(editingClientId)) {
            cancelEdit();
            c.messageTextField.clear();
        }

        c.webSocketClient.sendDeleteMessage(
                chat.getId(),
                Session.getCurrentUser().getId(),
                clientId
        );
    }

    void attachImage() {
        if (c.selectedChat == null || c.selectedChat.isBot()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File file = chooser.showOpenDialog(c.messageTextField.getScene().getWindow());

        if (file == null) {
            return;
        }

        try {
            byte[] data = Files.readAllBytes(file.toPath());
            String contentType = Files.probeContentType(file.toPath());

            int attachmentId = new ServerApi().uploadFile(data, contentType);

            if (attachmentId < 0) {
                c.showMessage("Error", "Could not upload the image.");
                return;
            }

            sendText("[IMG]" + attachmentId);

        } catch (Exception e) {
            e.printStackTrace();
            c.showMessage("Error", "Could not send the image.");
        }
    }

    private void sendText(String text) {
        Message userMessage = new Message(
                Session.getCurrentUser().getId(),
                Session.getCurrentUser().getUsername(),
                Session.getCurrentUser().getDisplayName(),
                text
        );

        if (c.model.isBotChat(c.selectedChat)) {
            Chat updatedChat = c.model.addMessage(c.selectedChat, userMessage);

            if (updatedChat != null) {
                c.selectedChat = updatedChat;
                c.contactsListView.getSelectionModel().select(c.selectedChat);
            }

            Message botMessage = c.model.generateBotResponse(text);
            Chat updatedBotChat = c.model.addBotMessage(c.selectedChat, botMessage);

            if (updatedBotChat != null) {
                c.selectedChat = updatedBotChat;
                c.contactsListView.getSelectionModel().select(c.selectedChat);
            }

            c.contactsListView.refresh();
            c.scrollMessagesToBottom();
        } else if (c.model.isGroupChat(c.selectedChat)) {
            c.webSocketClient.sendGroupMessage(
                    c.selectedChat.getId(),
                    Session.getCurrentUser().getId(),
                    Session.getCurrentUser().getUsername(),
                    Session.getCurrentUser().getDisplayName(),
                    text,
                    userMessage.getClientId()
            );
        } else {
            Integer companionUserId = c.selectedChat.getCompanionUserId();

            if (companionUserId != null) {
                c.webSocketClient.sendPrivateMessage(
                        c.selectedChat.getId(),
                        Session.getCurrentUser().getId(),
                        companionUserId,
                        Session.getCurrentUser().getUsername(),
                        Session.getCurrentUser().getDisplayName(),
                        text,
                        userMessage.getClientId()
                );
            }
        }
    }
}
