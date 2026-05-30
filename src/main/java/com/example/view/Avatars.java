package com.example.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Builders for the circular initial-based avatars used across the UI.
 * Pure presentation: callers pass the online flag, this class knows nothing
 * about the model.
 */
public final class Avatars {

    private Avatars() {
    }

    private static final String[] COLORS = {
            "#5B9DFF",
            "#FF5B81",
            "#7D5FFF",
            "#2ED573",
            "#FFA502",
            "#FF4757",
            "#1E90FF",
            "#9C88FF"
    };

    /** Plain avatar with the first letter of {@code name}. */
    public static StackPane base(String name, double radius) {
        if (name == null || name.isBlank()) {
            name = "?";
        }

        StackPane avatar = new StackPane();

        avatar.setMinSize(radius * 2, radius * 2);
        avatar.setPrefSize(radius * 2, radius * 2);
        avatar.setMaxSize(radius * 2, radius * 2);

        avatar.setStyle(
                "-fx-background-color: " + color(name) + ";" +
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

    /** Avatar with an online dot scaled to the radius (chat list / header). */
    public static StackPane avatar(String name, double radius, boolean online) {
        StackPane avatar = base(name, radius);

        if (online) {
            avatar.getChildren().add(onlineDot(Math.max(12, radius * 0.52)));
        }

        return avatar;
    }

    /** Member avatar (radius 17) with a fixed 10px online dot. */
    public static StackPane member(String name, boolean online) {
        StackPane avatar = base(name, 17);

        if (online) {
            avatar.getChildren().add(onlineDot(10));
        }

        return avatar;
    }

    /** Small avatar shown next to incoming group messages. */
    public static StackPane smallMessage(String name) {
        return base(name, 15);
    }

    public static Label botBadge() {
        Label badge = new Label("BOT");
        badge.getStyleClass().add("bot-badge");
        return badge;
    }

    private static Region onlineDot(double size) {
        Region dot = new Region();
        dot.setPrefSize(size, size);
        dot.setMaxSize(size, size);
        dot.getStyleClass().add("online-status-dot");
        StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
        return dot;
    }

    private static String color(String name) {
        if (name == null || name.isBlank()) {
            return "#5B9DFF";
        }

        return COLORS[Math.abs(name.hashCode()) % COLORS.length];
    }
}
