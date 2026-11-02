package com.mahghuuuls.jafs;

import com.mahghuuuls.jafs.compat.inhibited.InhibitedRecoveryGate;
import com.mahghuuuls.jafs.compat.rht.RhtNotice;
import com.mahghuuuls.jafs.compat.setworks.SetworksBridge;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.gameplay.JafsGameplay;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.Loader;

/**
 * Composition root for optional mods (ARC-010): detects them by mod id after every mod has
 * loaded and wires the compat classes through the gameplay interfaces. JAFS never references a
 * class of those mods.
 */
public final class Integrations {

    private Integrations() {
    }

    public static void wire(JafsGameplay gameplay) {
        if (Loader.isModLoaded(RhtNotice.MOD_ID)) {
            Diag.LOGGER.warn("{} Operators are told on login.", RhtNotice.LINE);
            gameplay.notices().setCoexistenceNotice(new RhtNotice());
        }
        if (Loader.isModLoaded(InhibitedRecoveryGate.MOD_ID)) {
            Potion effect = InhibitedRecoveryGate.lookup();
            if (effect == null) {
                Diag.LOGGER.warn("Inhibited is loaded but registers no effect {}; recovery is never blocked by it", InhibitedRecoveryGate.EFFECT_ID);
            } else {
                gameplay.recovery().setGate(new InhibitedRecoveryGate(effect));
                Diag.LOGGER.info("Inhibited found: recovery is blocked while {} is active when inhibitedBlocksRecovery is on", InhibitedRecoveryGate.EFFECT_ID);
            }
        }
        if (Loader.isModLoaded(SetworksBridge.MOD_ID)) {
            // The bridge class is loaded only here, so a game without Setworks never touches its API.
            SetworksBridge.register();
        }
    }
}
