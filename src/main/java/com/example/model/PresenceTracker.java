package com.example.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PresenceTracker {

    // =================== Presence ===================

    private final Set<Integer> onlineUserIds = new HashSet<>();

    public void setUserOnline(int userId) {
        onlineUserIds.add(userId);
    }

    public void setUserOffline(int userId) {
        onlineUserIds.remove(userId);
    }

    public void setOnlineUsers(List<Integer> userIds, int currentUserId) {
        onlineUserIds.clear();

        if (userIds == null) {
            return;
        }

        for (Integer userId : userIds) {
            if (userId != null && userId != currentUserId) {
                onlineUserIds.add(userId);
            }
        }
    }

    public boolean isUserOnline(Integer userId) {
        return userId != null && onlineUserIds.contains(userId);
    }
}
