package com.mahghuuuls.jafs.compat.setworks;

import com.mahghuuuls.jafs.property.PropertyRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ledger of Setworks contributions (REQ-056c): instances add up per property, the same
 * instance recorded twice counts once, a changed amount replaces the old one, withdrawal of an
 * absent instance changes nothing, and logout forgets the player.
 */
class SetworksContributionsTest {

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void instancesAddUpPerPropertyAndPerPlayer() {
        SetworksContributions book = new SetworksContributions();
        assertTrue(book.put(ALICE, "s/a/0", PropertyRegistry.RECOVERY_RATE, 50));
        assertTrue(book.put(ALICE, "s/b/0", PropertyRegistry.RECOVERY_RATE, 25));
        assertTrue(book.put(ALICE, "s/b/1", PropertyRegistry.DEPLETION_RATE, -30));
        assertEquals(75, book.percent(ALICE, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(-30, book.percent(ALICE, PropertyRegistry.DEPLETION_RATE), 1e-9);
        assertEquals(0, book.percent(BOB, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(Arrays.asList("s/a/0", "s/b/0", "s/b/1"), book.instances(ALICE));
        assertTrue(book.instances(BOB).isEmpty());
    }

    @Test
    void reapplyingAnInstanceCountsOnceAndAChangedAmountReplaces() {
        SetworksContributions book = new SetworksContributions();
        assertTrue(book.put(ALICE, "s/a/0", PropertyRegistry.RECOVERY_RATE, 50));
        assertFalse(book.put(ALICE, "s/a/0", PropertyRegistry.RECOVERY_RATE, 50));
        assertEquals(50, book.percent(ALICE, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertTrue(book.put(ALICE, "s/a/0", PropertyRegistry.RECOVERY_RATE, 60));
        assertEquals(60, book.percent(ALICE, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertTrue(book.put(ALICE, "s/a/0", PropertyRegistry.DEPLETION_RATE, 60));
        assertEquals(0, book.percent(ALICE, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(60, book.percent(ALICE, PropertyRegistry.DEPLETION_RATE), 1e-9);
    }

    @Test
    void describeListsEveryInstanceWithItsPropertyAndPercent() {
        SetworksContributions book = new SetworksContributions();
        assertTrue(book.describe(ALICE).isEmpty());
        book.put(ALICE, "jafsf:iron_armor/jafsf:iron_recovery/0", PropertyRegistry.RECOVERY_RATE, 50);
        book.put(ALICE, "jafsf:iron_armor/jafsf:iron_endurance/1", PropertyRegistry.DEPLETION_RATE, -30);
        assertEquals(Arrays.asList("jafsf:iron_armor/jafsf:iron_recovery/0=jafs:recovery_rate +50%",
                "jafsf:iron_armor/jafsf:iron_endurance/1=jafs:depletion_rate -30%"), book.describe(ALICE));
    }

    @Test
    void withdrawalAndLogoutForget() {
        SetworksContributions book = new SetworksContributions();
        assertFalse(book.remove(ALICE, "s/a/0"));
        book.put(ALICE, "s/a/0", PropertyRegistry.RECOVERY_RATE, 50);
        book.put(ALICE, "s/b/0", PropertyRegistry.RECOVERY_RATE, 25);
        assertTrue(book.remove(ALICE, "s/a/0"));
        assertFalse(book.remove(ALICE, "s/a/0"));
        assertEquals(25, book.percent(ALICE, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertTrue(book.clear(ALICE));
        assertFalse(book.clear(ALICE));
        assertEquals(0, book.percent(ALICE, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertTrue(book.instances(ALICE).isEmpty());
    }
}
