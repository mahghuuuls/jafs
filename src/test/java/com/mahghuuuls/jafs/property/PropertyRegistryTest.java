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

    private static final com.mahghuuuls.jafs.api.IPropertyApplier NO_OP = new com.mahghuuuls.jafs.api.IPropertyApplier() {
        @Override
        public void apply(net.minecraft.entity.player.EntityPlayerMP player, double flatSum, double percentSum) {
        }
    };

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
        int unconditional = 0;
        for (AttributeProperties.Row row : AttributeProperties.rows()) {
            if (row.requiresMod() == null) {
                unconditional++;
            }
        }
        assertEquals(3 + unconditional, registry.all().size(), "the three originals plus the attribute-backed rows that need no mod");
    }

    @Test
    void registrationIsRefusedAfterTheFreezeNamingTheProperty() {
        PropertyRegistry registry = PropertyRegistry.forTests();
        registry.register(PropertySpec.builder("addon:early").displayNameKey("x").unit("").color("aqua")
                .operation(PropertyOperation.FLAT, -1, 1).build(), NO_OP);
        registry.freeze();
        assertTrue(registry.isFrozen());
        IllegalStateException error = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        registry.register(PropertySpec.builder("addon:late").displayNameKey("x").unit("").color("aqua")
                                .operation(PropertyOperation.FLAT, -1, 1).build(), NO_OP);
                    }
                });
        assertTrue(error.getMessage().contains("addon:late"), error.getMessage());
        assertTrue(error.getMessage().contains("closed"), error.getMessage());
        org.junit.jupiter.api.Assertions.assertNotNull(registry.find("addon:early"));
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
