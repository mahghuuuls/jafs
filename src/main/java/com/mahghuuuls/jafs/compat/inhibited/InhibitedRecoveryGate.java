package com.mahghuuuls.jafs.compat.inhibited;

import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.gameplay.RecoveryGate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Blocks recovery while the player has the Inhibited effect and the option is on (REQ-040). The
 * effect is looked up by registry id once; no class of that mod is referenced. The option is
 * read on every check so a reload turns the block on or off at once.
 */
public final class InhibitedRecoveryGate implements RecoveryGate {

    public static final String MOD_ID = "inhibited";
    public static final ResourceLocation EFFECT_ID = new ResourceLocation(MOD_ID, "inhibited");

    private final Potion effect;

    public InhibitedRecoveryGate(Potion effect) {
        this.effect = effect;
    }

    /** The registered effect, or null when the mod is absent or registers no such effect. */
    public static Potion lookup() {
        return ForgeRegistries.POTIONS.getValue(EFFECT_ID);
    }

    @Override
    public boolean blocksRecovery(EntityPlayerMP player) {
        return effect != null && GameplayConfig.current().inhibitedBlocksRecovery() && player.isPotionActive(effect);
    }
}
