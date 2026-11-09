package com.mahghuuuls.jafs.compat.statusworks;

import com.mahghuuuls.jafs.property.PropertyRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The parts of the contribution type that need no running game: its id is the rate's property id,
 * the bridge registers one type per rate, anything that is not a server player is ignored, and the
 * player's rates are marked for recomputing only when a value or an end changed the ledger.
 */
class RateContributionTypeTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final String WELL_FED = "status:mypack:well_fed#0";

    private static StatusworksContributions ledger() {
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        return new StatusworksContributions(registry, message -> { });
    }

    @Test
    void theIdIsTheRatePropertyId() {
        assertEquals("jafs:recovery_rate", new RateContributionType(PropertyRegistry.RECOVERY_RATE, ledger()).id());
        assertEquals("jafs:depletion_rate", new RateContributionType(PropertyRegistry.DEPLETION_RATE, ledger()).id());
    }

    @Test
    void theBridgeRegistersOneTypePerRate() {
        assertEquals(Arrays.asList(PropertyRegistry.RECOVERY_RATE, PropertyRegistry.DEPLETION_RATE), StatusworksBridge.RATES);
    }

    @Test
    void anEntityThatIsNotAServerPlayerIsIgnored() {
        StatusworksContributions ledger = ledger();
        RateContributionType type = new RateContributionType(PropertyRegistry.RECOVERY_RATE, ledger);
        assertFalse(type.appliesTo(null));
        type.onValue(null, WELL_FED, 20);
        type.onEnd(null, WELL_FED);
    }

    @Test
    void aNewOrChangedValueMarksTheRatesForRecomputingAndARepeatDoesNot() {
        RateContributionType type = new RateContributionType(PropertyRegistry.RECOVERY_RATE, ledger());
        AtomicInteger marks = new AtomicInteger();
        type.value(ALEX, "Alex", WELL_FED, 20, marks::incrementAndGet);
        assertEquals(1, marks.get());
        type.value(ALEX, "Alex", WELL_FED, 20, marks::incrementAndGet);
        assertEquals(1, marks.get());
        type.value(ALEX, "Alex", WELL_FED, 30, marks::incrementAndGet);
        assertEquals(2, marks.get());
    }

    @Test
    void anEndMarksOnlyWhenThereWasAnEntry() {
        RateContributionType type = new RateContributionType(PropertyRegistry.RECOVERY_RATE, ledger());
        AtomicInteger marks = new AtomicInteger();
        type.end(ALEX, WELL_FED, marks::incrementAndGet);
        assertEquals(0, marks.get());
        type.value(ALEX, "Alex", WELL_FED, 20, () -> { });
        type.end(ALEX, WELL_FED, marks::incrementAndGet);
        assertEquals(1, marks.get());
        type.end(ALEX, WELL_FED, marks::incrementAndGet);
        assertEquals(1, marks.get());
    }
}
