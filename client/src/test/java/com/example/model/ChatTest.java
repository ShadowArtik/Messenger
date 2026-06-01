package com.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatTest {

    private Chat sample() {
        return new Chat(7, "Bob", "PRIVATE", "hi", "10:00", 42, 3, false);
    }

    @Test
    void typeFlags() {
        assertTrue(new Chat(1, "x", "PRIVATE", null, null, null, 0, false).isPrivate());
        assertTrue(new Chat(1, "x", "GROUP", null, null, null, 0, false).isGroup());
        assertTrue(new Chat(1, "x", "BOT", null, null, null, 0, false).isBot());
    }

    @Test
    void withUnreadCountKeepsOtherFields() {
        Chat c = sample().withUnreadCount(0);

        assertEquals(0, c.getUnreadCount());
        assertEquals(7, c.getId());
        assertEquals("Bob", c.getName());
        assertEquals(42, c.getCompanionUserId());
    }

    @Test
    void withLastMessageUpdatesPreviewOnly() {
        Chat c = sample().withLastMessage("bye", "11:00");

        assertEquals("bye", c.getLastMessageText());
        assertEquals("11:00", c.getLastMessageTime());
        assertEquals(3, c.getUnreadCount());
    }

    @Test
    void withNameSetsCustomFlag() {
        Chat c = sample().withName("Bobby", true);

        assertEquals("Bobby", c.getName());
        assertTrue(c.hasCustomName());
    }

    @Test
    void withersReturnNewInstance() {
        Chat c = sample();
        assertNotSame(c, c.withUnreadCount(9));
    }
}
