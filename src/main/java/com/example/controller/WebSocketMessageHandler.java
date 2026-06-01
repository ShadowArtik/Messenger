package com.example.controller;

import com.example.model.Chat;
import com.example.model.Message;
import com.example.model.Session;
import com.example.network.XmlProtocol;
import com.example.network.XmlProtocol.IncomingMessage;

public class WebSocketMessageHandler {

    private final MessengerController controller;

    public WebSocketMessageHandler(MessengerController controller) {
        this.controller = controller;
    }

    // =================== Dispatch ===================

    public void handle(String xml) {
        try {
            IncomingMessage incoming = XmlProtocol.parse(xml);

            if (incoming == null) {
                return;
            }

            if (incoming.isTyping()) {
                handleTypingMessage(incoming);
                return;
            }

            if (incoming.isOnlineUsers()) {
                handleOnlineUsersMessage(incoming);
                return;
            }

            if (incoming.isUserOnline()) {
                handleUserOnlineMessage(incoming);
                return;
            }

            if (incoming.isUserOffline()) {
                handleUserOfflineMessage(incoming);
                return;
            }

            if (incoming.isUserProfileUpdated()) {
                handleUserProfileUpdated(incoming);
                return;
            }

            if (incoming.isGroupCreated()) {
                handleGroupCreatedMessage(incoming);
                return;
            }

            if (incoming.isGroupMembersUpdated()) {
                handleGroupMembersUpdatedMessage(incoming);
                return;
            }

            if (incoming.isGroupRenamed()) {
                handleGroupRenamedMessage(incoming);
                return;
            }

            if (incoming.isGroupDeleted()) {
                handleGroupDeletedMessage(incoming);
                return;
            }

            if (incoming.isHistory()) {
                handleHistory(incoming);
                return;
            }

            if (incoming.isClearChat()) {
                handleClearChat(incoming);
                return;
            }

            if (incoming.isMessagesRead()) {
                handleMessagesRead(incoming);
                return;
            }

            if (incoming.isMessageDeleted()) {
                handleMessageDeleted(incoming);
                return;
            }

            if (incoming.isMessageEdited()) {
                handleMessageEdited(incoming);
                return;
            }

            if (incoming.isPrivateMessage() || incoming.isGroupMessage()) {
                handleChatMessage(incoming);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================== Presence ===================

    private void handleOnlineUsersMessage(IncomingMessage incoming) {
        controller.model.setOnlineUsers(
                incoming.getUserIds(),
                Session.getCurrentUser().getId()
        );

        refreshOnlineState();
    }

    private void handleUserOnlineMessage(IncomingMessage incoming) {
        int userId = incoming.getUserId();

        if (userId == Session.getCurrentUser().getId()) {
            return;
        }

        controller.model.setUserOnline(userId);
        refreshOnlineState();
    }

    private void handleUserOfflineMessage(IncomingMessage incoming) {
        int userId = incoming.getUserId();

        if (userId == Session.getCurrentUser().getId()) {
            return;
        }

        controller.model.setUserOffline(userId);
        refreshOnlineState();
    }

    private void refreshOnlineState() {
        controller.refreshContactList();

        if (controller.selectedChat != null) {
            controller.updateChatHeader(controller.selectedChat);
        }
    }

    // =================== Chat messages ===================

    private void handleChatMessage(IncomingMessage incoming) {
        int chatId = incoming.getChatId();
        int senderId = incoming.getSenderId();

        controller.typing.hideChatListTyping(chatId);

        if (controller.selectedChat != null && controller.selectedChat.getId() == chatId) {
            controller.typing.hideHeaderLabel();
        }

        int previouslySelectedChatId =
                controller.selectedChat != null ? controller.selectedChat.getId() : -1;

        Chat incomingChat = controller.model.findChatById(chatId);
        boolean messageLoadedByReload = false;

        if (incomingChat == null) {
            controller.model.reloadChats();
            messageLoadedByReload = true;

            incomingChat = controller.model.findChatById(chatId);

            if (incomingChat == null) {
                return;
            }
        }

        Chat updatedIncomingChat = incomingChat;

        if (!messageLoadedByReload) {
            Message incomingMessage = new Message(
                    incoming.getSenderId(),
                    incoming.getSenderUsername(),
                    incoming.getSenderDisplayName(),
                    incoming.getText()
            );

            if (incoming.getMsgId() != null && !incoming.getMsgId().isBlank()) {
                incomingMessage.setClientId(incoming.getMsgId());
            }

            updatedIncomingChat =
                    controller.model.addIncomingMessage(incomingChat, incomingMessage);
        }

        if (updatedIncomingChat == null) {
            return;
        }

        boolean messageForOpenedChat =
                previouslySelectedChatId == updatedIncomingChat.getId();

        boolean isOwnMessage = senderId == Session.getCurrentUser().getId();

        // When the chat was just (re)loaded from the server, its unreadCount already
        // counts this message (it is is_read=false in the DB), so do not add again.
        if (!messageForOpenedChat && !isOwnMessage && !messageLoadedByReload) {
            updatedIncomingChat =
                    controller.model.increaseUnreadCount(updatedIncomingChat);
        }

        controller.refreshContactList();

        if (messageForOpenedChat) {
            controller.selectedChat = updatedIncomingChat;
            controller.selectChatInList(updatedIncomingChat);
            controller.showMessagesForCurrentChat();
            controller.scrollMessagesToBottom();

            if (!updatedIncomingChat.isBot() && !isOwnMessage) {
                controller.webSocketClient.sendMessageRead(
                        updatedIncomingChat.getId(),
                        Session.getCurrentUser().getId()
                );
            }

            return;
        }

        Chat selectedAfterUpdate =
                controller.model.findChatById(previouslySelectedChatId);

        if (selectedAfterUpdate != null) {
            controller.selectedChat = selectedAfterUpdate;
            controller.selectChatInList(selectedAfterUpdate);
        }
    }

    // =================== Profile ===================

    private void handleUserProfileUpdated(IncomingMessage incoming) {
        if (incoming.getUserId() == Session.getCurrentUser().getId()) {
            return;
        }

        int selectedChatId = controller.selectedChat != null
                ? controller.selectedChat.getId() : -1;

        Chat updatedProfileChat = controller.model.updateCompanionDisplayName(
                incoming.getUserId(),
                incoming.getDisplayName()
        );

        controller.refreshContactList();

        if (selectedChatId == -1) {
            return;
        }

        Chat updatedSelectedChat =
                updatedProfileChat != null && updatedProfileChat.getId() == selectedChatId
                        ? updatedProfileChat
                        : controller.model.findChatById(selectedChatId);

        if (updatedSelectedChat == null) {
            controller.showEmptyChatState();
            return;
        }

        controller.selectedChat = updatedSelectedChat;
        controller.selectChatInList(updatedSelectedChat);
        controller.updateChatHeader(updatedSelectedChat);
        controller.showMessagesForCurrentChat();
    }

    // =================== Groups ===================

    private void handleGroupCreatedMessage(IncomingMessage incoming) {
        if (incoming.getMemberIds() == null) {
            return;
        }

        int currentUserId = Session.getCurrentUser().getId();

        if (!incoming.getMemberIds().contains(currentUserId)) {
            return;
        }

        int selectedChatId = controller.selectedChat != null
                ? controller.selectedChat.getId() : -1;

        controller.model.reloadChats();
        controller.refreshContactList();

        if (selectedChatId == -1) {
            return;
        }

        Chat updatedSelectedChat = controller.model.findChatById(selectedChatId);

        if (updatedSelectedChat == null) {
            controller.showEmptyChatState();
            return;
        }

        controller.selectedChat = updatedSelectedChat;
        controller.selectChatInList(updatedSelectedChat);
        controller.updateChatHeader(updatedSelectedChat);
        controller.showMessagesForCurrentChat();
    }

    private void handleGroupMembersUpdatedMessage(IncomingMessage incoming) {
        if (incoming.getMemberIds() == null) {
            return;
        }

        int currentUserId = Session.getCurrentUser().getId();

        if (!incoming.getMemberIds().contains(currentUserId)) {
            return;
        }

        int selectedChatId = controller.selectedChat != null
                ? controller.selectedChat.getId() : -1;

        controller.model.reloadChats();
        controller.refreshContactList();

        Chat eventChat = controller.model.findChatById(incoming.getChatId());

        Chat updatedSelectedChat = selectedChatId == -1
                ? null
                : controller.model.findChatById(selectedChatId);

        if (selectedChatId != -1 && updatedSelectedChat == null) {
            controller.showEmptyChatState();
            return;
        }

        if (eventChat != null && selectedChatId == eventChat.getId()) {
            controller.selectedChat = eventChat;
            controller.selectChatInList(eventChat);
            controller.updateChatHeader(eventChat);
            controller.showMessagesForCurrentChat();
            controller.scrollMessagesToBottom();
        } else if (updatedSelectedChat != null) {
            controller.selectedChat = updatedSelectedChat;
            controller.selectChatInList(updatedSelectedChat);
            controller.updateChatHeader(updatedSelectedChat);
            controller.showMessagesForCurrentChat();
        }

        if (controller.selectedChat != null
                && controller.selectedChat.getId() == incoming.getChatId()
                && controller.isGroupInfoVisible()) {
            controller.refreshGroupInfoMembers();
        }

        String systemMessageText = incoming.getSystemMessageText();

        if (eventChat != null
                && selectedChatId != eventChat.getId()
                && systemMessageText != null
                && !systemMessageText.isBlank()) {
            controller.model.increaseUnreadCount(eventChat);
        }

        controller.refreshContactList();
    }

    private void handleGroupRenamedMessage(IncomingMessage incoming) {
        if (incoming.getMemberIds() == null) {
            return;
        }

        int currentUserId = Session.getCurrentUser().getId();

        if (!incoming.getMemberIds().contains(currentUserId)) {
            return;
        }

        int selectedChatId = controller.selectedChat != null
                ? controller.selectedChat.getId() : -1;

        controller.model.reloadChats();
        controller.refreshContactList();

        Chat eventChat = controller.model.findChatById(incoming.getChatId());
        Chat updatedSelectedChat = selectedChatId == -1
                ? null
                : controller.model.findChatById(selectedChatId);

        if (selectedChatId != -1 && updatedSelectedChat == null) {
            controller.showEmptyChatState();
            return;
        }

        if (eventChat != null && selectedChatId == eventChat.getId()) {
            controller.selectedChat = eventChat;
            controller.selectChatInList(eventChat);
            controller.updateChatHeader(eventChat);
            controller.showMessagesForCurrentChat();
            controller.scrollMessagesToBottom();
        } else if (updatedSelectedChat != null) {
            controller.selectedChat = updatedSelectedChat;
            controller.selectChatInList(updatedSelectedChat);
            controller.updateChatHeader(updatedSelectedChat);
            controller.showMessagesForCurrentChat();
        }

        if (controller.selectedChat != null
                && controller.selectedChat.getId() == incoming.getChatId()
                && controller.isGroupInfoVisible()) {
            controller.updateGroupInfoName(controller.selectedChat.getName());
            controller.refreshGroupInfoMembers();
        }

        String systemMessageText = incoming.getSystemMessageText();

        if (eventChat != null
                && selectedChatId != eventChat.getId()
                && systemMessageText != null
                && !systemMessageText.isBlank()) {
            controller.model.increaseUnreadCount(eventChat);
        }

        controller.refreshContactList();
    }

    private void handleGroupDeletedMessage(IncomingMessage incoming) {
        int chatId = incoming.getChatId();

        controller.model.removeChatLocally(chatId);
        controller.refreshContactList();

        if (controller.selectedChat != null
                && controller.selectedChat.getId() == chatId) {
            controller.showEmptyChatState();
        }
    }

    // =================== Read receipts ===================

    private void handleMessagesRead(IncomingMessage incoming) {
        int chatId = incoming.getChatId();

        boolean changed = controller.model.markOutgoingMessagesRead(chatId);

        if (changed
                && controller.selectedChat != null
                && controller.selectedChat.getId() == chatId) {
            controller.refreshMessages();
        }
    }

    // =================== Edit / delete ===================

    private void handleMessageDeleted(IncomingMessage incoming) {
        int chatId = incoming.getChatId();

        boolean changed = controller.model.deleteMessageLocal(chatId, incoming.getMsgId());

        if (!changed) {
            controller.model.refreshChatPreviewFromServer(chatId);
        }

        controller.refreshContactList();

        if (controller.selectedChat != null
                && controller.selectedChat.getId() == chatId) {
            controller.refreshMessages();
        }
    }

    private void handleMessageEdited(IncomingMessage incoming) {
        int chatId = incoming.getChatId();

        boolean changed = controller.model.editMessageLocal(
                chatId,
                incoming.getMsgId(),
                incoming.getText()
        );

        if (!changed) {
            controller.model.refreshChatPreviewFromServer(chatId);
        }

        controller.refreshContactList();

        if (controller.selectedChat != null
                && controller.selectedChat.getId() == chatId) {
            controller.refreshMessages();
        }
    }

    // =================== Clear & history ===================

    private void handleClearChat(IncomingMessage incoming) {
        int chatId = incoming.getChatId();

        Chat updatedChat = controller.model.applyRemoteClear(chatId);

        controller.refreshContactList();

        if (updatedChat != null
                && controller.selectedChat != null
                && controller.selectedChat.getId() == chatId) {
            controller.selectedChat = updatedChat;
            controller.selectChatInList(updatedChat);
            controller.showMessagesForCurrentChat();
        }
    }

    private void handleHistory(IncomingMessage incoming) {
        controller.model.setMessagesForChat(
                incoming.getChatId(),
                incoming.getMessages()
        );

        if (controller.selectedChat != null
                && controller.selectedChat.getId() == incoming.getChatId()) {
            controller.showMessagesForCurrentChat();
            controller.scrollAfterHistoryLoad(incoming.getChatId());
        }
    }

    // =================== Typing ===================

    private void handleTypingMessage(IncomingMessage incoming) {
        if (incoming.getSenderId() == Session.getCurrentUser().getId()) {
            return;
        }

        controller.typing.showChatListTyping(incoming.getChatId());

        if (controller.selectedChat == null
                || controller.selectedChat.getId() != incoming.getChatId()) {
            return;
        }

        String text = controller.model.isGroupChat(controller.selectedChat)
                && incoming.getSenderDisplayName() != null
                && !incoming.getSenderDisplayName().isBlank()
                ? incoming.getSenderDisplayName() + " is typing..."
                : "typing...";

        controller.typing.showHeaderLabelWithTimeout(text);
    }
}
