package com.mahghuuuls.jafs.compat.setworks;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.setworks.api.CombinationRule;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The effect kind's pure parts (REQ-056c): id and combination rule, validation of the property
 * (the two rates only) and of the amount (a number inside the property's percent limits), and the
 * description line Setworks shows.
 */
class JafsPropertyEffectKindTest {

    private static JafsPropertyEffectKind kind() {
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        return new JafsPropertyEffectKind(new SetworksContributions(), registry);
    }

    private static Map<String, String> params(String property, String amount) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        if (property != null) {
            params.put("property", property);
        }
        if (amount != null) {
            params.put("amount", amount);
        }
        return params;
    }

    @Test
    void idAndCombinationRule() {
        JafsPropertyEffectKind kind = kind();
        assertEquals("jafs:property", kind.id());
        assertEquals(CombinationRule.SUM, kind.combinationRule());
    }

    @Test
    void onlyTheTwoRatesAreAccepted() {
        JafsPropertyEffectKind kind = kind();
        assertNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, "50")));
        assertNull(kind.validate(params(PropertyRegistry.DEPLETION_RATE, "-30")));
        assertNotNull(kind.validate(params(PropertyRegistry.MAX_HEALTH, "4")));
        assertNotNull(kind.validate(params("jafs:armor", "2")));
        assertNotNull(kind.validate(params(null, "2")));
        assertTrue(kind.validate(params("jafs:armor", "2")).contains("setworks:attribute"));
    }

    @Test
    void theAmountMustBeANumberInsideTheLimits() {
        JafsPropertyEffectKind kind = kind();
        assertNotNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, null)));
        assertNotNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, "lots")));
        assertNotNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, "NaN")));
        assertNotNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, "1001")));
        assertNotNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, "-1001")));
        assertNull(kind.validate(params(PropertyRegistry.RECOVERY_RATE, " 1000 ")));
        PropertySpec spec = PropertyRegistry.get().find(PropertyRegistry.RECOVERY_RATE);
        assertEquals(1000, spec.range(PropertyOperation.PERCENT).max(), 1e-9, "the limit the test relies on");
    }

    @Test
    void theRecomputeFollowsOnlyAChange() {
        // apply() recomputes only when record() reports a change, and withdraw() only when forget() does (one line each).
        SetworksContributions ledger = new SetworksContributions();
        JafsPropertyEffectKind kind = new JafsPropertyEffectKind(ledger, PropertyRegistry.get());
        java.util.UUID alice = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        assertTrue(kind.record(alice, "alice", "s/a/0", params(PropertyRegistry.RECOVERY_RATE, "50")));
        assertTrue(!kind.record(alice, "alice", "s/a/0", params(PropertyRegistry.RECOVERY_RATE, "50")));
        assertTrue(kind.record(alice, "alice", "s/a/0", params(PropertyRegistry.RECOVERY_RATE, "60")));
        assertEquals(60, ledger.percent(alice, PropertyRegistry.RECOVERY_RATE), 1e-9);
        assertTrue(kind.forget(alice, "alice", "s/a/0"));
        assertTrue(!kind.forget(alice, "alice", "s/a/0"));
        assertEquals(0, ledger.percent(alice, PropertyRegistry.RECOVERY_RATE), 1e-9);
    }

    @Test
    void anUnregisteredPropertyIsRefusedEvenWhenAllowed() {
        JafsPropertyEffectKind bare = new JafsPropertyEffectKind(new SetworksContributions(), PropertyRegistry.forTestsWithoutProperties());
        String problem = bare.validate(params(PropertyRegistry.RECOVERY_RATE, "50"));
        assertNotNull(problem);
        assertTrue(problem.contains("not registered"), problem);
    }

    @Test
    void describeNamesThePropertyAndTheSignedPercent() {
        JafsPropertyEffectKind kind = kind();
        assertEquals("jafs:recovery_rate +50%", kind.describe(params(PropertyRegistry.RECOVERY_RATE, "50")));
        assertEquals("jafs:depletion_rate -30%", kind.describe(params(PropertyRegistry.DEPLETION_RATE, "-30")));
        assertEquals("jafs:recovery_rate +12.5%", kind.describe(params(PropertyRegistry.RECOVERY_RATE, "12.5")));
        assertEquals(0, JafsPropertyEffectKind.parse(params(PropertyRegistry.RECOVERY_RATE, "bad")), 1e-9);
    }
}
