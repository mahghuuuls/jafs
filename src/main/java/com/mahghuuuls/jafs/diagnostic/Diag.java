package com.mahghuuuls.jafs.diagnostic;

import com.mahghuuuls.jafs.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The mod logger and the diagnostic mode: one log line per JAFS decision when enabled, nothing
 * otherwise. Callers write diagnostic lines at state transitions only, never per tick.
 */
public final class Diag {

    /** The mod's logger; every package logs through it. */
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);
    private static volatile boolean enabled;

    private Diag() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Writes one diagnostic line when diagnostic mode is on. Log4j-style {} placeholders. */
    public static void event(String message, Object... args) {
        if (enabled) {
            LOGGER.info("[JAFS] " + message, args);
        }
    }
}
