package com.example.controller;

import com.example.model.Chat;
import com.example.view.Avatars;
import com.example.view.LastSeen;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

class ChatHeaderView {

    // =================== Rendering ===================

    private final MessengerController c;

    ChatHeaderView(MessengerController controller) {
        this.c = controller;
    }

    void update(Chat chat) {
        String name = chat.getName();

        c.chatTitleLabel.setText(name);
        c.chatTitleLabel.setGraphic(null);
        updateSubtitle(chat);

        if (c.model.isBotChat(chat)) {
            Label titleLabel = new Label(name);
            titleLabel.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-size: 24;" +
                            "-fx-font-weight: bold;"
            );

            HBox titleBox = new HBox(6, titleLabel, Avatars.botBadge());
            titleBox.setAlignment(Pos.CENTER_LEFT);

            c.chatTitleLabel.setText("");
            c.chatTitleLabel.setGraphic(titleBox);
        }

        c.chatAvatar.getChildren().clear();
        c.chatAvatar.getChildren().add(
                Avatars.avatar(chat.getName(), 20, c.model.isChatOnline(chat))
        );
    }

    private void updateSubtitle(Chat chat) {
        if (c.chatSubtitleLabel == null) {
            return;
        }

        if (c.model.isGroupChat(chat)) {
            int membersCount = c.model.getGroupMembers(chat).size();
            String membersText = membersCount == 1
                    ? "1 member"
                    : membersCount + " members";

            c.chatSubtitleLabel.setText(membersText);
            c.chatSubtitleLabel.setVisible(true);
            c.chatSubtitleLabel.setManaged(true);
            return;
        }

        if (chat.isPrivate() && chat.getCompanionUserId() != null) {
            c.chatSubtitleLabel.setText(companionPresence(chat.getCompanionUserId()));
            c.chatSubtitleLabel.setVisible(true);
            c.chatSubtitleLabel.setManaged(true);
            return;
        }

        c.chatSubtitleLabel.setText("");
        c.chatSubtitleLabel.setVisible(false);
        c.chatSubtitleLabel.setManaged(false);
    }

    private String companionPresence(int companionUserId) {
        if (c.model.isUserOnline(companionUserId)) {
            return "online";
        }

        Long lastSeen = c.userService.getLastSeen(companionUserId);
        return lastSeen == null ? "last seen recently" : LastSeen.format(lastSeen);
    }
}
