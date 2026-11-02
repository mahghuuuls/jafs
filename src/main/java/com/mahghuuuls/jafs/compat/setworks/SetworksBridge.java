package com.mahghuuuls.jafs.compat.setworks;

import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import net.minecraftforge.common.MinecraftForge;

/**
 * The one entry point of the Setworks compatibility (ARC-010, REQ-056c), loaded only after
 * {@code Loader.isModLoaded("setworks")}: registers the {@code jafs:property} effect kind with
 * Setworks (at JAFS's post-init, before Setworks freezes its kinds at load complete) and the
 * contributions ledger with JAFS as the rate-modifier provider {@code jafs:setworks}.
 */
public final class SetworksBridge {

    public static final String MOD_ID = "setworks";
    public static final String PROVIDER_ID = "jafs:setworks";

    private SetworksBridge() {
    }

    /** Registers the kind and the provider; returns whether Setworks accepted the kind. */
    public static boolean register() {
        SetworksContributions contributions = new SetworksContributions();
        JafsPropertyEffectKind kind = new JafsPropertyEffectKind(contributions, PropertyRegistry.get());
        RegistrationResult result = SetworksApi.registerEffectKind(kind);
        if (!result.isOk()) {
            Diag.LOGGER.warn("Setworks refused the effect kind {}: {}; set bonuses cannot grant JAFS rates", JafsPropertyEffectKind.ID,
                    result.message());
            return false;
        }
        JafsApi.registerRateModifierProvider(PROVIDER_ID, contributions);
        MinecraftForge.EVENT_BUS.register(contributions);
        Diag.LOGGER.info("Setworks found: effect kind {} registered; set bonuses may grant {}", JafsPropertyEffectKind.ID,
                JafsPropertyEffectKind.ALLOWED);
        return true;
    }
}
