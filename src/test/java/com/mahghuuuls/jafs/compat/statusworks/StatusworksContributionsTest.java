package com.mahghuuuls.jafs.compat.statusworks;

import com.mahghuuuls.jafs.property.PropertyRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ledger behind the Statusworks contribution types: a value per player and contribution key,
 * a repeated value that changes nothing, the end of a key, the logout clear, the sum per rate, and
 * the hold inside the rate's percent limits with one warning per player and key.
 */
class StatusworksContributionsTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID SAM = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final String WELL_FED = "status:mypack:well_fed#0";
    private static final String SLOW = "status:mypack:slow_hunger#0";

    private final List<String> warnings = new ArrayList<String>();

    private StatusworksContributions ledger() {
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        return new StatusworksContributions(registry, warnings::add);
    }

    @Test
    void aValueAddsToItsRateUntilItEnds() {
        StatusworksContributions ledger = ledger();
        assertTrue(ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20));
        assertEquals(20, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(0, ledger.percent(ALEX, PropertyRegistry.DEPLETION_RATE), 1e-9);
        assertTrue(ledger.remove(ALEX, WELL_FED));
        assertEquals(0, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertFalse(ledger.remove(ALEX, WELL_FED));
    }

    @Test
    void theSameValueAgainChangesNothing() {
        StatusworksContributions ledger = ledger();
        assertTrue(ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20));
        assertFalse(ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20));
        assertEquals(20, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(Arrays.asList(WELL_FED), ledger.keys(ALEX));
    }

    @Test
    void aNewValueForAKeyReplacesTheOldOne() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", SLOW, PropertyRegistry.DEPLETION_RATE, -30);
        assertTrue(ledger.put(ALEX, "Alex", SLOW, PropertyRegistry.DEPLETION_RATE, -40));
        assertEquals(-40, ledger.percent(ALEX, PropertyRegistry.DEPLETION_RATE), 1e-9);
    }

    @Test
    void keysAddUpPerRateAndPlayersStaySeparate() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20);
        ledger.put(ALEX, "Alex", "pool:mypack:focus#0", PropertyRegistry.RECOVERY_RATE, 12.5);
        ledger.put(ALEX, "Alex", SLOW, PropertyRegistry.DEPLETION_RATE, -30);
        ledger.put(SAM, "Sam", WELL_FED, PropertyRegistry.RECOVERY_RATE, 5);
        assertEquals(32.5, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(-30, ledger.percent(ALEX, PropertyRegistry.DEPLETION_RATE), 1e-9);
        assertEquals(5, ledger.percent(SAM, PropertyRegistry.RECOVERY_RATE), 1e-9);
    }

    @Test
    void logoutDropsEveryEntryOfThatPlayerOnly() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20);
        ledger.put(SAM, "Sam", WELL_FED, PropertyRegistry.RECOVERY_RATE, 5);
        assertTrue(ledger.clear(ALEX));
        assertFalse(ledger.clear(ALEX));
        assertEquals(0, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(5, ledger.percent(SAM, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertTrue(ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20));
    }

    @Test
    void aValuePastTheLimitIsHeldAtTheLimitWithOneWarningPerKey() {
        StatusworksContributions ledger = ledger();
        assertTrue(ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 1500));
        assertEquals(1000, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains(WELL_FED) && warnings.get(0).contains("Alex") && warnings.get(0).contains("1500"));
        assertFalse(ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 1600));
        assertEquals(1, warnings.size());
        ledger.put(ALEX, "Alex", SLOW, PropertyRegistry.DEPLETION_RATE, -5000);
        assertEquals(-1000, ledger.percent(ALEX, PropertyRegistry.DEPLETION_RATE), 1e-9);
        assertEquals(2, warnings.size());
    }

    @Test
    void theWarningComesAgainAfterTheKeyEndedOrThePlayerLeft() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 1500);
        ledger.remove(ALEX, WELL_FED);
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 1500);
        assertEquals(2, warnings.size());
        ledger.clear(ALEX);
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 1500);
        assertEquals(3, warnings.size());
    }

    @Test
    void aValueInsideTheLimitsLogsNothing() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 1000);
        ledger.put(ALEX, "Alex", SLOW, PropertyRegistry.DEPLETION_RATE, -1000);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void notANumberCountsAsZeroWithAWarning() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, Double.NaN);
        assertEquals(0, ledger.percent(ALEX, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertEquals(1, warnings.size());
    }

    @Test
    void describeNamesEachKeyWithItsRateAndPercent() {
        StatusworksContributions ledger = ledger();
        ledger.put(ALEX, "Alex", WELL_FED, PropertyRegistry.RECOVERY_RATE, 20);
        ledger.put(ALEX, "Alex", SLOW, PropertyRegistry.DEPLETION_RATE, -30);
        List<String> lines = ledger.describe(ALEX);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith(WELL_FED + "=jafs:recovery_rate +20"));
        assertTrue(lines.get(1).startsWith(SLOW + "=jafs:depletion_rate -30"));
        assertTrue(ledger.describe(SAM).isEmpty());
    }
}
