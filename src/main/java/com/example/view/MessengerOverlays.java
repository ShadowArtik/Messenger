package com.example.view;

import com.example.model.Chat;
import com.example.model.User;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.util.Set;

public final class MessengerOverlays {

    private MessengerOverlays() {
    }

    // =================== Generic show/hide ===================

    public static void show(StackPane overlay) {
        if (overlay == null) {
            return;
        }

        overlay.setVisible(true);
        overlay.setManaged(true);
        overlay.toFront();
    }

    public static void hide(StackPane overlay) {
        if (overlay == null) {
            return;
        }

        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    public static void hideAll(StackPane... overlays) {
        for (StackPane overlay : overlays) {
            hide(overlay);
        }
    }

    // =================== Specific overlays ===================

    public static void showCreateChat(
            StackPane overlay,
            TextField usernameField,
            Label errorLabel
    ) {
        usernameField.clear();
        clearError(errorLabel);
        show(overlay);
        usernameField.requestFocus();
    }

    public static void showCreateGroup(
            StackPane overlay,
            TextField groupNameField,
            Set<Integer> selectedMemberIds,
            ListView<Chat> membersListView,
            ObservableList<Chat> availableMembers
    ) {
        groupNameField.clear();
        selectedMemberIds.clear();
        membersListView.setItems(availableMembers);
        show(overlay);
        groupNameField.requestFocus();
    }

    public static void hideCreateGroup(
            StackPane overlay,
            TextField groupNameField,
            Set<Integer> selectedMemberIds,
            ListView<Chat> membersListView
    ) {
        groupNameField.clear();
        selectedMemberIds.clear();
        membersListView.getItems().clear();
        hide(overlay);
    }

    public static void showRenameChat(
            StackPane overlay,
            TextField nameField,
            Label errorLabel,
            String currentName
    ) {
        nameField.setText(currentName);
        clearError(errorLabel);
        show(overlay);
        nameField.requestFocus();
        nameField.selectAll();
    }

    public static void showProfile(
            StackPane overlay,
            Label usernameLabel,
            TextField displayNameField,
            User user
    ) {
        usernameLabel.setText("@" + user.getUsername());
        displayNameField.setText(user.getDisplayName());
        show(overlay);

        Platform.runLater(() -> {
            displayNameField.requestFocus();
            displayNameField.positionCaret(displayNameField.getText().length());
        });
    }

    // =================== Errors ===================

    public static void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public static void clearError(Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
