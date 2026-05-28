package com.example.view.cell;

import com.example.model.Chat;
import com.example.model.User;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MessengerCells {

    private MessengerCells() {
    }

    public static ListCell<Chat> groupMemberSelectionCell(
            Set<Integer> selectedMemberIds
    ) {
        return new ListCell<>() {
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
                        selectedMemberIds.contains(chat.getCompanionUserId())
                );

                checkBox.selectedProperty().addListener((observable, oldValue, selected) -> {
                    if (selected) {
                        selectedMemberIds.add(chat.getCompanionUserId());
                    } else {
                        selectedMemberIds.remove(chat.getCompanionUserId());
                    }
                });

                checkBox.getStyleClass().add("group-member-checkbox");

                setText(null);
                setGraphic(checkBox);
            }
        };
    }

    public static ListCell<User> addMembersCell(
            Function<User, StackPane> avatarFactory
    ) {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox root = new HBox(10);
                root.setAlignment(Pos.CENTER_LEFT);

                StackPane avatar = avatarFactory.apply(user);

                VBox textBox = new VBox(2);

                Label displayNameLabel = new Label(user.getDisplayName());
                displayNameLabel.getStyleClass().add("group-member-name");

                Label usernameLabel = new Label("@" + user.getUsername());
                usernameLabel.getStyleClass().add("group-member-username");

                textBox.getChildren().addAll(displayNameLabel, usernameLabel);
                root.getChildren().addAll(avatar, textBox);

                setText(null);
                setGraphic(root);
            }
        };
    }

    public static ListCell<User> groupInfoMemberCell(
            Function<User, StackPane> avatarFactory,
            Predicate<User> canShowRoleAction,
            Function<User, String> roleActionTextProvider,
            Consumer<User> roleActionHandler,
            Predicate<User> canTransferOwnership,
            Consumer<User> transferOwnerHandler,
            Predicate<User> canKickMember,
            Consumer<User> kickMemberHandler
    ) {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                setGraphic(createMemberRow(
                        user,
                        avatarFactory,
                        canShowRoleAction,
                        roleActionTextProvider,
                        roleActionHandler,
                        canTransferOwnership,
                        transferOwnerHandler,
                        canKickMember,
                        kickMemberHandler
                ));
            }
        };
    }

    private static HBox createMemberRow(
            User user,
            Function<User, StackPane> avatarFactory,
            Predicate<User> canShowRoleAction,
            Function<User, String> roleActionTextProvider,
            Consumer<User> roleActionHandler,
            Predicate<User> canTransferOwnership,
            Consumer<User> transferOwnerHandler,
            Predicate<User> canKickMember,
            Consumer<User> kickMemberHandler
    ) {
        HBox root = new HBox(10);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setMaxWidth(Double.MAX_VALUE);

        StackPane avatar = avatarFactory.apply(user);

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(user.getDisplayName());
        nameLabel.getStyleClass().add("group-member-name");

        nameRow.getChildren().add(nameLabel);

        Label roleBadge = createRoleBadge(user.getMemberRole());

        if (roleBadge != null) {
            nameRow.getChildren().add(roleBadge);
        }

        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.getStyleClass().add("group-member-username");

        textBox.getChildren().addAll(nameRow, usernameLabel);
        root.getChildren().addAll(avatar, textBox);

        HBox actionsBox = new HBox(6);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        if (canShowRoleAction.test(user)) {
            Button roleButton = new Button(roleActionTextProvider.apply(user));
            roleButton.getStyleClass().add("member-role-button");
            roleButton.setOnAction(event -> roleActionHandler.accept(user));
            actionsBox.getChildren().add(roleButton);
        }

        if (canTransferOwnership.test(user)) {
            Button ownerButton = new Button("Make owner");
            ownerButton.getStyleClass().add("member-role-button");
            ownerButton.setOnAction(event -> transferOwnerHandler.accept(user));
            actionsBox.getChildren().add(ownerButton);
        }

        if (canKickMember.test(user)) {
            Button kickButton = new Button("Kick");
            kickButton.getStyleClass().add("member-kick-button");
            kickButton.setOnAction(event -> kickMemberHandler.accept(user));
            actionsBox.getChildren().add(kickButton);
        }

        if (!actionsBox.getChildren().isEmpty()) {
            root.getChildren().add(actionsBox);
        }

        return root;
    }

    private static Label createRoleBadge(String role) {
        if (role == null || "MEMBER".equalsIgnoreCase(role)) {
            return null;
        }

        Label roleBadge = new Label(role.toUpperCase());
        roleBadge.getStyleClass().add("group-member-role-badge");

        if ("OWNER".equalsIgnoreCase(role)) {
            roleBadge.getStyleClass().add("owner-role-badge");
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            roleBadge.getStyleClass().add("admin-role-badge");
        }

        return roleBadge;
    }
}
