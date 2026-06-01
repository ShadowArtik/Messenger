package com.example.controller;

import com.example.model.Chat;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class TypingIndicators {

    private final MessengerController c;

    private final Set<Integer> typingChatIds = new HashSet<>();
    private final Map<Integer, PauseTransition> chatTypingHideDelays = new HashMap<>();

    private PauseTransition headerHideDelay;

    TypingIndicators(MessengerController controller) {
        this.c = controller;
    }

    boolean isChatTyping(Chat chat) {
        return chat != null && typingChatIds.contains(chat.getId());
    }

    // =================== Header label (open chat) ===================

    void showHeaderLabel(String text) {
        Label typingLabel = c.typingLabel;
        if (typingLabel != null) {
            typingLabel.setText(text);
            typingLabel.setVisible(true);
            typingLabel.setManaged(true);
        }
    }

    void hideHeaderLabel() {
        if (headerHideDelay != null) {
            headerHideDelay.stop();
        }

        Label typingLabel = c.typingLabel;
        if (typingLabel != null) {
            typingLabel.setText("");
            typingLabel.setVisible(false);
            typingLabel.setManaged(true);
        }
    }

    void showHeaderLabelWithTimeout(String text) {
        showHeaderLabel(text);

        if (headerHideDelay != null) {
            headerHideDelay.stop();
        }

        headerHideDelay = new PauseTransition(Duration.seconds(2));
        headerHideDelay.setOnFinished(event -> hideHeaderLabel());
        headerHideDelay.playFromStart();
    }

    // =================== Contact-list previews ===================

    void showChatListTyping(int chatId) {
        typingChatIds.add(chatId);
        c.contactsListView.refresh();

        PauseTransition oldDelay = chatTypingHideDelays.remove(chatId);
        if (oldDelay != null) {
            oldDelay.stop();
        }

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event -> {
            typingChatIds.remove(chatId);
            chatTypingHideDelays.remove(chatId);
            c.contactsListView.refresh();
        });

        chatTypingHideDelays.put(chatId, delay);
        delay.playFromStart();
    }

    void hideChatListTyping(int chatId) {
        typingChatIds.remove(chatId);

        PauseTransition delay = chatTypingHideDelays.remove(chatId);
        if (delay != null) {
            delay.stop();
        }

        c.contactsListView.refresh();
    }
}
