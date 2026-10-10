package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The three core properties, their limits (REQ-056), and duplicate rejection. */
class PropertyRegistryTest {

    @Test
    void corePropertiesCarryTheApprovedOperationsAndLimits() {
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        PropertySpec maxHealth = registry.find(PropertyRegistry.MAX_HEALTH);
        assertNotNull(maxHealth);
        assertTrue(maxHealth.supports(PropertyOperation.FLAT));
        assertEquals(-1000, maxHealth.range(PropertyOperation.FLAT).min(), 1e-9);
        assertEquals(1000, maxHealth.range(PropertyOperation.FLAT).max(), 1e-9);
        assertEquals(-100, maxHealth.range(PropertyOperation.PERCENT).min(), 1e-9);
        assertEquals(1000, maxHealth.range(PropertyOperation.PERCENT).max(), 1e-9);
        PropertySpec recovery = registry.find(PropertyRegistry.RECOVERY_RATE);
        assertTrue(recovery.supports(PropertyOperation.PERCENT));
        assertTrue(!recovery.supports(PropertyOperation.FLAT));
        assertEquals(-1000, recovery.range(PropertyOperation.PERCENT).min(), 1e-9);
        assertTrue(registry.find(PropertyRegistry.DEPLETION_RATE).supports(PropertyOperation.PERCENT));
        assertEquals(3, registry.all().size());
    }

    @Test
    void duplicateIdIsRejected() {
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                PropertySpec.builder(PropertyRegistry.MAX_HEALTH).operation(PropertyOperation.FLAT, 0, 1).build(),
                (player, flat, percent) -> { }));
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                PropertySpec.builder("addon:other").operation(PropertyOperation.FLAT, 0, 1).build(), null));
    }

    @Test
    void specBuilderRejectsUnnamespacedIdsAndEmptyOperations() {
        assertThrows(IllegalArgumentException.class, () -> PropertySpec.builder("stamina"));
        assertThrows(IllegalStateException.class, () -> PropertySpec.builder("addon:stamina").build());
    }
}
