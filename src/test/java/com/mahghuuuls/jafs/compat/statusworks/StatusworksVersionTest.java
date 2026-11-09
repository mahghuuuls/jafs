package com.mahghuuuls.jafs.compat.statusworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Statusworks version check: 1.2.0 and newer are used; older, empty, and unreadable versions are not. */
class StatusworksVersionTest {

    @Test
    void theMinimumAndNewerAreSupported() {
        assertTrue(StatusworksVersion.isSupported("1.2.0"));
        assertTrue(StatusworksVersion.isSupported("1.2.1"));
        assertTrue(StatusworksVersion.isSupported("1.3"));
        assertTrue(StatusworksVersion.isSupported("1.10.0"));
        assertTrue(StatusworksVersion.isSupported("2.0.0"));
        assertTrue(StatusworksVersion.isSupported(" 1.2.0 "));
    }

    @Test
    void olderVersionsAreNot() {
        assertFalse(StatusworksVersion.isSupported("1.1.0"));
        assertFalse(StatusworksVersion.isSupported("1.0.0"));
        assertFalse(StatusworksVersion.isSupported("1.1.9"));
    }

    @Test
    void missingOrUnreadableVersionsAreNot() {
        assertFalse(StatusworksVersion.isSupported(null));
        assertFalse(StatusworksVersion.isSupported(""));
        assertFalse(StatusworksVersion.isSupported("   "));
        assertFalse(StatusworksVersion.isSupported("unknown"));
    }
}
