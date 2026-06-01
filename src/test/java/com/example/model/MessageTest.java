package com.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void getTextStripsSystemPrefix() {
        Message sys = Message.system("Alice joined");

        assertEquals("Alice joined", sys.getText());
        assertTrue(sys.getStorageText().startsWith("[SYSTEM] "));
        assertTrue(sys.isSystem());
    }

    @Test
    void plainMessageIsNotSystem() {
        Message m = new Message(1, "alice", "Alice", "hi");

        assertEquals("hi", m.getText());
        assertFalse(m.isSystem());
    }

    @Test
    void clientIdIsGeneratedAndUnique() {
        Message a = new Message(1, "a", "A", "x");
        Message b = new Message(1, "a", "A", "x");

        assertNotNull(a.getClientId());
        assertNotEquals(a.getClientId(), b.getClientId());
    }

    @Test
    void editChangesTextAndMarksEdited() {
        Message m = new Message(1, "a", "A", "old");
        assertFalse(m.isEdited());

        m.setText("new");
        m.setEdited(true);

        assertEquals("new", m.getText());
        assertTrue(m.isEdited());
    }

    @Test
    void readFlagFlips() {
        Message m = new Message(1, "a", "A", "x");
        assertFalse(m.isRead());

        m.setRead(true);

        assertTrue(m.isRead());
    }
}
