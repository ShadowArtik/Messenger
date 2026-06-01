package com.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageStoreTest {

    private Message msg(String clientId, int senderId, String text) {
        Message m = new Message(senderId, "u", "U", text);
        m.setClientId(clientId);
        return m;
    }

    @Test
    void addCreatesListAndLastReturnsTail() {
        MessageStore store = new MessageStore();
        assertFalse(store.has(1));

        store.add(1, msg("a", 1, "hello"));

        assertTrue(store.has(1));
        assertEquals("hello", store.last(1).getText());
    }

    @Test
    void deleteByClientIdRemovesOnlyMatch() {
        MessageStore store = new MessageStore();
        store.add(1, msg("a", 1, "one"));
        store.add(1, msg("b", 1, "two"));

        assertTrue(store.deleteByClientId(1, "a"));
        assertFalse(store.deleteByClientId(1, "missing"));

        assertEquals(1, store.getOrEmpty(1).size());
        assertEquals("two", store.last(1).getText());
    }

    @Test
    void editByClientIdChangesTextAndFlag() {
        MessageStore store = new MessageStore();
        store.add(1, msg("a", 1, "old"));

        assertTrue(store.editByClientId(1, "a", "new"));
        assertFalse(store.editByClientId(1, "nope", "x"));

        Message m = store.last(1);
        assertEquals("new", m.getText());
        assertTrue(m.isEdited());
    }

    @Test
    void markOutgoingReadOnlyOwnUnreadMessages() {
        MessageStore store = new MessageStore();
        store.add(1, msg("a", 1, "mine"));
        store.add(1, msg("b", 2, "theirs"));

        assertTrue(store.markOutgoingRead(1, 1));
        // Second pass: nothing left to change for user 1.
        assertFalse(store.markOutgoingRead(1, 1));
    }

    @Test
    void clearEmptiesButKeepsList_removeDeletesIt() {
        MessageStore store = new MessageStore();
        store.add(1, msg("a", 1, "x"));

        store.clear(1);
        assertTrue(store.has(1));
        assertEquals(0, store.getOrEmpty(1).size());

        store.remove(1);
        assertFalse(store.has(1));
    }

    @Test
    void getOrEmptyDoesNotStoreUnknownChat() {
        MessageStore store = new MessageStore();
        store.getOrEmpty(99);
        assertFalse(store.has(99));
    }
}
