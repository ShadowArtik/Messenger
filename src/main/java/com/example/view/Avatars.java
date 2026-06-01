package com.example.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class Avatars {

    // =================== Avatars ===================

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

    public static StackPane avatar(String name, double radius, boolean online) {
        StackPane avatar = base(name, radius);

        if (online) {
            avatar.getChildren().add(onlineDot(Math.max(12, radius * 0.52)));
        }

        return avatar;
    }

    public static StackPane member(String name, boolean online) {
        StackPane avatar = base(name, 17);

        if (online) {
            avatar.getChildren().add(onlineDot(10));
        }

        return avatar;
    }

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
