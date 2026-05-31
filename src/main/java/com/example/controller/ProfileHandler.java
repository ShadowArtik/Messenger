package com.example.controller;

import com.example.model.Session;
import com.example.model.User;
import com.example.view.Avatars;
import com.example.view.overlay.MessengerOverlays;

/**
 * Current-user profile: the sidebar card and the "edit display name" overlay,
 * extracted from {@link MessengerController}. The controller keeps thin @FXML
 * delegates that call into this handler.
 */
public class ProfileHandler {

    private final MessengerController c;

    public ProfileHandler(MessengerController controller) {
        this.c = controller;
    }

    void setupCurrentUserProfile() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        String displayName = Session.getCurrentUser().getDisplayName();
        String username = Session.getCurrentUser().getUsername();

        c.currentUserDisplayNameLabel.setText(displayName);
        c.currentUserUsernameLabel.setText("@" + username);

        c.currentUserAvatar.getChildren().clear();
        c.currentUserAvatar.getChildren().add(Avatars.base(displayName, 18));
    }

    void openProfile() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        c.hideAllOverlays();

        MessengerOverlays.showProfile(
                c.profileOverlay,
                c.profileUsernameLabel,
                c.profileDisplayNameField,
                Session.getCurrentUser()
        );
    }

    void saveProfile() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        String newDisplayName = c.profileDisplayNameField.getText().trim();

        if (newDisplayName.isEmpty()) {
            c.showMessage("Invalid name", "Display name cannot be empty.");
            return;
        }

        boolean updated = c.userService.updateDisplayName(
                Session.getCurrentUser().getId(),
                newDisplayName
        );

        if (!updated) {
            c.showMessage("Error", "Could not update display name.");
            return;
        }

        User updatedUser = new User(
                Session.getCurrentUser().getId(),
                Session.getCurrentUser().getUsername(),
                newDisplayName
        );

        Session.setCurrentUser(updatedUser);

        setupCurrentUserProfile();
        c.webSocketClient.sendProfileUpdatedMessage(
                updatedUser.getId(),
                updatedUser.getDisplayName()
        );

        c.hideAllOverlays();
    }
}
