package com.mahghuuuls.jafs.compat.statusworks;

import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

/**
 * The oldest Statusworks the contribution types work with. Statusworks 1.2.0 is the first release
 * whose end notices are never lost inside long chains of its callbacks; with an older one a status
 * could end without telling JAFS, and its bonus would stay until the player logs out.
 */
public final class StatusworksVersion {

    public static final String MINIMUM = "1.2.0";

    private StatusworksVersion() {
    }

    /** Whether a Statusworks version text is {@link #MINIMUM} or newer; a missing or empty text is not. */
    public static boolean isSupported(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        DefaultArtifactVersion loaded = new DefaultArtifactVersion(StatusworksBridge.MOD_ID, version.trim());
        return loaded.compareTo(new DefaultArtifactVersion(StatusworksBridge.MOD_ID, MINIMUM)) >= 0;
    }
}
