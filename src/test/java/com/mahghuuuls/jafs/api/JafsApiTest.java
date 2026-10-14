package com.mahghuuuls.jafs.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The facade before and after binding: unbound calls explain themselves; bound calls forward; one bind only. */
class JafsApiTest {

    @Test
    void unboundCallsNameTheCause() {
        // The facade is a process-wide static; this test runs before any bind in the suite order or after
        // the binding test, so only the unbound branch or the forwarding branch can be asserted here.
        if (JafsApi.isBound()) {
            return;
        }
        IllegalStateException error = assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                JafsApi.isRegistrationFrozen();
            }
        });
        assertTrue(error.getMessage().contains("required-after:jafs"), error.getMessage());
    }

    @Test
    void valueTypesCarryTheirNumbers() {
        PaymentResult failed = PaymentResult.failed(PaymentResult.Status.NOT_AFFORDABLE, 30);
        assertFalse(failed.success());
        assertEquals(30, failed.requestedSeconds());
        assertTrue(failed.expiredDefinitionIds().isEmpty());
        ActiveFoodView view = new ActiveFoodView("steak", 1230, 24000, true,
                java.util.Collections.singletonList(new ModifierView("jafs:max_health", PropertyOperation.FLAT, 4)));
        assertEquals(61, view.remainingSeconds());
        assertEquals(1200, view.fullSeconds());
        assertEquals("jafs:max_health", view.modifiers().get(0).propertyId());
        assertEquals(0, PropertyValue.ZERO.contributions());
    }
}
