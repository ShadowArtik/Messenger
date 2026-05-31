package com.example.view.cell;

import com.example.model.Chat;
import com.example.model.Message;
import com.example.model.Session;
import com.example.model.User;
import com.example.view.Avatars;
import com.example.view.ImageStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MessengerCells {

    private MessengerCells() {
    }

    public static ListCell<Chat> contactCell(
            Predicate<Chat> isBot,
            Predicate<Chat> isOnline,
            Predicate<Chat> isTyping
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

                HBox root = new HBox(10);
                root.setAlignment(Pos.CENTER_LEFT);
                root.setMaxWidth(Double.MAX_VALUE);
                root.getStyleClass().add("chat-cell-root");

                StackPane avatar = Avatars.avatar(chat.getName(), 20, isOnline.test(chat));

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

                if (isBot.test(chat)) {
                    nameRow.getChildren().add(Avatars.botBadge());
                }

                Label lastMessageLabel = new Label();

                if (isTyping.test(chat)) {
                    lastMessageLabel.setText("is typing...");
                    lastMessageLabel.getStyleClass().add("chat-typing-preview-label");
                } else if (chat.getLastMessageText() == null || chat.getLastMessageText().isBlank()) {
                    lastMessageLabel.setText("No messages yet");
                } else {
                    lastMessageLabel.setText(previewText(chat.getLastMessageText()));
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
        };
    }

    public static ListCell<Message> messageCell(
            BooleanSupplier currentChatIsGroup,
            BooleanSupplier currentChatShowsReceipts
    ) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);

                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                boolean showDateHeader;
                int index = getIndex();
                if (index <= 0) {
                    showDateHeader = true;
                } else {
                    Message previous = getListView().getItems().get(index - 1);
                    showDateHeader = previous == null
                            || previous.getDate() == null
                            || !previous.getDate().equals(message.getDate());
                }

                boolean isMine = message.getSenderUsername().equals(
                        Session.getCurrentUser().getUsername()
                );

                boolean isGroupChat = currentChatIsGroup.getAsBoolean();

                Node content;
                Integer imageId = imageId(message.getText());

                if (imageId != null) {
                    ImageView imageView = new ImageView();
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(220);
                    imageView.setFitHeight(280);

                    Image image = ImageStore.get(imageId);
                    if (image != null) {
                        imageView.setImage(image);
                    }

                    content = imageView;
                } else {
                    Label textLabel = new Label(message.getText());
                    textLabel.setWrapText(true);
                    textLabel.setMaxWidth(280);
                    textLabel.getStyleClass().add("message-text");
                    content = textLabel;
                }

                Label timeLabel = new Label(message.getFormattedTime());
                timeLabel.getStyleClass().add("message-time");

                Node bubbleFooter = timeLabel;

                if (isMine && currentChatShowsReceipts.getAsBoolean()) {
                    Label receipt = new Label(message.isRead() ? "✓✓" : "✓");
                    receipt.getStyleClass().add(
                            message.isRead() ? "read-receipt-read" : "read-receipt-sent"
                    );

                    HBox footer = new HBox(4, timeLabel, receipt);
                    footer.setAlignment(Pos.CENTER_RIGHT);
                    bubbleFooter = footer;
                }

                VBox bubble = new VBox(3, content, bubbleFooter);
                bubble.getStyleClass().add("message-bubble");

                VBox bubbleBox = new VBox(1);
                HBox messageBox = new HBox(8);
                messageBox.setFillHeight(false);

                if (message.isSystem()) {
                    bubble.getStyleClass().add("message-bubble-system");
                    messageBox.setAlignment(Pos.CENTER);
                    messageBox.getChildren().add(bubble);
                } else if (isMine) {
                    bubble.getStyleClass().add("message-bubble-outgoing");
                    bubbleBox.getChildren().add(bubble);

                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    messageBox.getChildren().add(bubbleBox);
                } else if (isGroupChat) {
                    Label senderLabel = new Label(message.getSenderDisplayName());
                    senderLabel.getStyleClass().add("message-sender-label");

                    bubble.getStyleClass().add("message-bubble-incoming");

                    StackPane avatar = Avatars.smallMessage(
                            message.getSenderDisplayName()
                    );
                    HBox.setMargin(avatar, new Insets(4, 0, 0, 0));

                    Region senderNameIndent = new Region();
                    senderNameIndent.setPrefWidth(46);
                    senderNameIndent.setMinWidth(46);
                    senderNameIndent.setMaxWidth(46);

                    HBox senderRow = new HBox(senderNameIndent, senderLabel);
                    senderRow.setAlignment(Pos.CENTER_LEFT);

                    HBox bubbleRow = new HBox(8, avatar, bubble);
                    bubbleRow.setAlignment(Pos.TOP_LEFT);

                    bubbleBox.getChildren().addAll(senderRow, bubbleRow);
                    bubbleBox.getStyleClass().add("group-message-content");

                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageBox.getStyleClass().add("group-message-row");
                    messageBox.getChildren().add(bubbleBox);
                } else {
                    bubbleBox.getChildren().add(bubble);

                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    bubble.getStyleClass().add("message-bubble-incoming");
                    messageBox.getChildren().add(bubbleBox);
                }

                setText(null);

                if (showDateHeader) {
                    Label dateLabel = new Label(formatDay(message.getDate()));
                    dateLabel.getStyleClass().add("date-separator");

                    HBox dateRow = new HBox(dateLabel);
                    dateRow.setAlignment(Pos.CENTER);

                    setGraphic(new VBox(8, dateRow, messageBox));
                } else {
                    setGraphic(messageBox);
                }
            }
        };
    }

    /** Parse the attachment id from an "[IMG]42" message marker, or null if not an image. */
    private static Integer imageId(String text) {
        if (text == null || !text.startsWith("[IMG]")) {
            return null;
        }

        try {
            return Integer.parseInt(text.substring(5).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String previewText(String text) {
        return imageId(text) != null ? "📷 Photo" : text;
    }

    private static String formatDay(LocalDate date) {
        if (date == null) {
            return "";
        }

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Kyiv"));

        if (date.equals(today)) {
            return "Today";
        }

        if (date.equals(today.minusDays(1))) {
            return "Yesterday";
        }

        if (date.getYear() == today.getYear()) {
            return date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH));
        }

        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
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
