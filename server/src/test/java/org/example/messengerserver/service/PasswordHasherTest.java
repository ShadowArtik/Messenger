package org.example.messengerserver.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashDiffersFromPlainAndVerifies() {
        String hash = hasher.hash("secret123");

        assertNotEquals("secret123", hash);
        assertTrue(hasher.matches("secret123", hash));
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = hasher.hash("secret123");
        assertFalse(hasher.matches("wrong", hash));
    }

    @Test
    void nullInputsDoNotMatch() {
        assertFalse(hasher.matches(null, "x"));
        assertFalse(hasher.matches("x", null));
    }

    @Test
    void samePasswordHashesDifferDueToSalt() {
        assertNotEquals(hasher.hash("same"), hasher.hash("same"));
    }
}
