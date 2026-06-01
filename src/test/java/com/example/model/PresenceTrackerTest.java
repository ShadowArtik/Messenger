package com.example.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresenceTrackerTest {

    @Test
    void onlineThenOffline() {
        PresenceTracker p = new PresenceTracker();
        assertFalse(p.isUserOnline(5));

        p.setUserOnline(5);
        assertTrue(p.isUserOnline(5));

        p.setUserOffline(5);
        assertFalse(p.isUserOnline(5));
    }

    @Test
    void setOnlineUsersReplacesAndExcludesCurrent() {
        PresenceTracker p = new PresenceTracker();
        p.setUserOnline(99);

        p.setOnlineUsers(List.of(1, 2, 3), 2);

        assertTrue(p.isUserOnline(1));
        assertFalse(p.isUserOnline(2));   // current user excluded
        assertTrue(p.isUserOnline(3));
        assertFalse(p.isUserOnline(99));  // previous state cleared
    }

    @Test
    void nullUserIsNeverOnline() {
        assertFalse(new PresenceTracker().isUserOnline(null));
    }
}
