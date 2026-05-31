package com.example.controller;

import com.example.model.Chat;
import com.example.model.Session;
import com.example.model.User;
import com.example.service.result.CreateGroupResponse;
import com.example.view.MessengerCells;
import com.example.view.MessengerOverlays;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.List;

public class GroupHandler {

    private final MessengerController c;

    public GroupHandler(MessengerController controller) {
        this.c = controller;
    }

    // =================== Setup ===================

    void setupGroupMembersList() {
        c.groupMembersListView.setCellFactory(listView ->
                MessengerCells.groupMemberSelectionCell(c.selectedGroupMemberIds)
        );
    }

    void setupAddMembersList() {
        c.addMembersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        c.addMembersListView.setCellFactory(listView ->
                MessengerCells.addMembersCell(c::createSmallMemberAvatar)
        );
    }

    void setupGroupInfoMembersList() {
        c.groupInfoMembersListView.setCellFactory(listView ->
                MessengerCells.groupInfoMemberCell(
                        c::createSmallMemberAvatar,
                        this::canShowRoleAction,
                        this::getRoleActionText,
                        this::onToggleMemberRoleClick,
                        user -> c.model.groupService().canTransferOwnership(c.selectedChat, user),
                        this::onTransferOwnerClick,
                        user -> c.model.groupService().canKickGroupMember(c.selectedChat, user),
                        this::onKickGroupMemberClick
                )
        );
    }

    // =================== Create group ===================

    void showCreateGroupOverlay() {
        MessengerOverlays.showCreateGroup(
                c.createGroupOverlay,
                c.groupNameField,
                c.selectedGroupMemberIds,
                c.groupMembersListView,
                getAvailableGroupMembers()
        );
    }

    void cancelCreateGroup() {
        MessengerOverlays.hideCreateGroup(
                c.createGroupOverlay,
                c.groupNameField,
                c.selectedGroupMemberIds,
                c.groupMembersListView
        );
    }

    void confirmCreateGroup() {
        String groupName = c.groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            c.showMessage("Invalid group name", "Please enter group name.");
            return;
        }

        if (c.selectedGroupMemberIds.isEmpty()) {
            c.showMessage("Invalid members", "Please choose at least one member.");
            return;
        }

        CreateGroupResponse response = c.model.createGroupChat(
                groupName,
                new ArrayList<>(c.selectedGroupMemberIds)
        );

        switch (response.getResult()) {
            case SUCCESS -> {
                c.selectedChat = response.getChat();
                c.selectChatInList(c.selectedChat);
                c.refreshContactList();
                c.openChat(c.selectedChat);

                c.webSocketClient.sendGroupCreatedMessage(
                        Session.getCurrentUser().getId(),
                        response.getChat().getId(),
                        response.getChat().getName(),
                        response.getMemberIds()
                );

                MessengerOverlays.hideCreateGroup(
                        c.createGroupOverlay,
                        c.groupNameField,
                        c.selectedGroupMemberIds,
                        c.groupMembersListView
                );
            }
            case EMPTY_GROUP_NAME -> c.showMessage("Invalid group name", "Please enter group name.");
            case EMPTY_MEMBERS, ONLY_SELF_SELECTED -> c.showMessage("Invalid members", "Please choose at least one member.");
            case DATABASE_ERROR -> c.showMessage("Error", "Could not create group chat.");
            case USER_NOT_FOUND -> c.showMessage("Error", "One of the selected users was not found.");
        }
    }

    // =================== Group info ===================

    void openGroupInfo() {
        c.hideChatMenu();

        if (c.selectedChat == null || !c.model.isGroupChat(c.selectedChat)) {
            return;
        }

        c.groupInfoNameLabel.setText(c.selectedChat.getName());
        boolean canAddMembers = c.model.canManageGroup(c.selectedChat);
        c.addMembersButton.setVisible(canAddMembers);
        c.addMembersButton.setManaged(canAddMembers);

        refreshGroupInfoMembers();
        MessengerOverlays.show(c.groupInfoOverlay);
    }

    void refreshGroupInfoMembers() {
        if (c.selectedChat == null || !c.model.isGroupChat(c.selectedChat)) {
            c.groupInfoMembersListView.getItems().clear();
            return;
        }

        c.groupInfoMembersListView.getItems().setAll(
                c.model.getGroupMembers(c.selectedChat)
        );
    }

    // =================== Add members ===================

    void openAddMembers() {
        if (c.selectedChat == null
                || !c.model.isGroupChat(c.selectedChat)
                || !c.model.canManageGroup(c.selectedChat)) {
            return;
        }

        List<User> users = c.model.groupService().getUsersNotInChat(c.selectedChat);
        c.addMembersListView.getItems().setAll(users);
        c.addMembersListView.getSelectionModel().clearSelection();

        MessengerOverlays.hide(c.groupInfoOverlay);
        MessengerOverlays.show(c.addMembersOverlay);
    }

    void cancelAddMembers() {
        c.addMembersListView.getItems().clear();
        MessengerOverlays.hide(c.addMembersOverlay);
    }

    void confirmAddMembers() {
        if (c.selectedChat == null || !c.model.isGroupChat(c.selectedChat)) {
            c.addMembersListView.getItems().clear();
            MessengerOverlays.hide(c.addMembersOverlay);
            return;
        }

        List<User> selectedUsers = new ArrayList<>(
                c.addMembersListView.getSelectionModel().getSelectedItems()
        );

        if (selectedUsers.isEmpty()) {
            c.showMessage("Add members", "Please select at least one user.");
            return;
        }

        c.model.groupService().addMembersToGroup(c.selectedChat, selectedUsers);

        List<Integer> newMemberIds = selectedUsers.stream().map(User::getId).toList();
        String joinedText = buildMembersJoinedText(selectedUsers);

        // Notify other clients and let the server persist the system message FIRST,
        // so the inviter's own history reload (triggered by addSystemMessage below)
        // already contains it. Otherwise LOAD_HISTORY races ahead of the save and
        // the inviter never sees the "joined the group" message.
        c.webSocketClient.sendGroupCreatedMessage(
                Session.getCurrentUser().getId(),
                c.selectedChat.getId(),
                c.selectedChat.getName(),
                newMemberIds
        );

        List<Integer> updatedMemberIds = c.model.getGroupReceiverIds(c.selectedChat);

        if (!updatedMemberIds.isEmpty()) {
            c.webSocketClient.sendGroupMembersUpdatedMessage(
                    Session.getCurrentUser().getId(),
                    c.selectedChat.getId(),
                    updatedMemberIds,
                    joinedText
            );
        }

        Chat updatedChat = c.model.addSystemMessage(
                c.selectedChat, joinedText, Session.getCurrentUser().getId()
        );

        if (updatedChat != null) {
            c.selectedChat = updatedChat;
            c.selectChatInList(c.selectedChat);
            c.showMessagesForCurrentChat();
            c.scrollMessagesToBottom();
        }

        c.addMembersListView.getItems().clear();
        MessengerOverlays.hide(c.addMembersOverlay);
        openGroupInfo();
    }

    // =================== Leave group ===================

    void openLeaveGroup() {
        c.hideChatMenu();

        if (c.selectedChat == null || !c.model.isGroupChat(c.selectedChat)) {
            return;
        }

        MessengerOverlays.show(c.leaveGroupOverlay);
    }

    void confirmLeaveGroup() {
        if (c.selectedChat == null) {
            MessengerOverlays.hide(c.leaveGroupOverlay);
            return;
        }

        if (!c.model.groupService().canCurrentOwnerLeaveGroup(c.selectedChat)) {
            MessengerOverlays.hide(c.leaveGroupOverlay);
            c.showMessage("Leave group",
                    "You are the owner of this group. Make another member the owner before leaving.");
            return;
        }

        List<Integer> remainingMemberIds = c.model.getGroupReceiverIds(c.selectedChat);
        int leftChatId = c.selectedChat.getId();
        String leftText = Session.getCurrentUser().getDisplayName() + " left the group";

        Chat updatedChat = c.model.addSystemMessage(
                c.selectedChat, leftText, Session.getCurrentUser().getId()
        );

        if (updatedChat != null) {
            c.selectedChat = updatedChat;
        }

        boolean success = c.model.leaveGroup(c.selectedChat);

        if (!success) {
            MessengerOverlays.hide(c.leaveGroupOverlay);
            c.showMessage("Error", "Cannot leave this group.");
            return;
        }

        if (!remainingMemberIds.isEmpty()) {
            c.webSocketClient.sendGroupMembersUpdatedMessage(
                    Session.getCurrentUser().getId(),
                    leftChatId,
                    remainingMemberIds,
                    leftText
            );
        }

        c.contactsListView.getSelectionModel().clearSelection();
        c.refreshContactList();
        c.messagesListView.setItems(FXCollections.observableArrayList());
        c.showEmptyChatState();

        MessengerOverlays.hide(c.leaveGroupOverlay);
    }

    // =================== Role management ===================

    private boolean canShowRoleAction(User user) {
        return c.selectedChat != null
                && c.model.groupService().canManageGroupRoles(c.selectedChat)
                && user.getId() != Session.getCurrentUser().getId()
                && !"OWNER".equalsIgnoreCase(user.getMemberRole());
    }

    private String getRoleActionText(User user) {
        return "ADMIN".equalsIgnoreCase(user.getMemberRole()) ? "Remove admin" : "Make admin";
    }

    private void onToggleMemberRoleClick(User user) {
        if (c.selectedChat == null || user == null) return;

        String newRole = "ADMIN".equalsIgnoreCase(user.getMemberRole()) ? "MEMBER" : "ADMIN";
        boolean updated = c.model.groupService().updateGroupMemberRole(c.selectedChat, user, newRole);

        if (!updated) {
            c.showMessage("Group roles", "Could not update this member role.");
            return;
        }

        String roleText = "ADMIN".equalsIgnoreCase(newRole)
                ? user.getDisplayName() + " is now an admin"
                : user.getDisplayName() + " is no longer an admin";

        c.model.addSystemMessage(c.selectedChat, roleText, Session.getCurrentUser().getId());

        List<Integer> receiverIds = c.model.getGroupReceiverIds(c.selectedChat);
        if (!receiverIds.isEmpty()) {
            c.webSocketClient.sendGroupMembersUpdatedMessage(
                    Session.getCurrentUser().getId(), c.selectedChat.getId(), receiverIds, roleText
            );
        }

        refreshGroupInfoMembers();
        c.refreshContactList();
    }

    private void onTransferOwnerClick(User user) {
        if (c.selectedChat == null || user == null) return;

        boolean transferred = c.model.groupService().transferOwnership(c.selectedChat, user);

        if (!transferred) {
            c.showMessage("Group owner", "Could not transfer ownership.");
            return;
        }

        String systemText = Session.getCurrentUser().getDisplayName()
                + " made " + user.getDisplayName() + " the group owner";

        Chat updatedChat = c.model.addSystemMessage(
                c.selectedChat, systemText, Session.getCurrentUser().getId()
        );

        if (updatedChat != null) {
            c.selectedChat = updatedChat;
        }

        List<Integer> receiverIds = c.model.getGroupReceiverIds(c.selectedChat);
        if (!receiverIds.isEmpty()) {
            c.webSocketClient.sendGroupMembersUpdatedMessage(
                    Session.getCurrentUser().getId(), c.selectedChat.getId(), receiverIds, systemText
            );
        }

        refreshGroupInfoMembers();
        c.refreshContactList();
        c.showMessagesForCurrentChat();
        c.scrollMessagesToBottom();
    }

    private void onKickGroupMemberClick(User user) {
        if (c.selectedChat == null || user == null) return;

        List<Integer> receiverIds = c.model.getGroupReceiverIds(c.selectedChat);
        String systemText = user.getDisplayName() + " was removed from the group";

        Chat updatedChat = c.model.addSystemMessage(
                c.selectedChat, systemText, Session.getCurrentUser().getId()
        );

        if (updatedChat != null) {
            c.selectedChat = updatedChat;
        }

        boolean kicked = c.model.groupService().kickGroupMember(c.selectedChat, user);

        if (!kicked) {
            c.showMessage("Group members", "Could not remove this member.");
            return;
        }

        if (!receiverIds.isEmpty()) {
            c.webSocketClient.sendGroupMembersUpdatedMessage(
                    Session.getCurrentUser().getId(), c.selectedChat.getId(), receiverIds, systemText
            );
        }

        refreshGroupInfoMembers();
        c.refreshContactList();
        c.showMessagesForCurrentChat();
        c.scrollMessagesToBottom();
    }

    // =================== Helpers ===================

    private ObservableList<Chat> getAvailableGroupMembers() {
        ObservableList<Chat> users = FXCollections.observableArrayList();

        for (Chat chat : c.model.getChats()) {
            if ("PRIVATE".equalsIgnoreCase(chat.getType()) && chat.getCompanionUserId() != null) {
                users.add(chat);
            }
        }

        return users;
    }

    private String buildMembersJoinedText(List<User> users) {
        if (users.size() == 1) {
            return users.get(0).getDisplayName() + " joined the group";
        }

        String names = users.stream()
                .map(User::getDisplayName)
                .reduce((first, second) -> first + ", " + second)
                .orElse("Members");

        return names + " joined the group";
    }
}
