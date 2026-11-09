package com.mahghuuuls.jafs.compat.statusworks;

import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.statusworks.api.RegistrationResult;
import com.mahghuuuls.statusworks.api.StatusworksApi;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The one entry point of the Statusworks compatibility, loaded only after
 * {@code Loader.isModLoaded("statusworks")}: checks the Statusworks version, registers the two
 * contribution types with Statusworks (at JAFS's post-init, before Statusworks freezes its types at
 * load complete) and the ledger with JAFS as the rate-modifier provider {@code jafs:statusworks}.
 */
public final class StatusworksBridge {

    public static final String MOD_ID = "statusworks";
    public static final String PROVIDER_ID = "jafs:statusworks";
    static final List<String> RATES = Collections.unmodifiableList(Arrays.asList(PropertyRegistry.RECOVERY_RATE, PropertyRegistry.DEPLETION_RATE));

    private StatusworksBridge() {
    }

    /** Registers the types and the provider; returns whether Statusworks accepted at least one type. */
    public static boolean register() {
        ModContainer container = Loader.instance().getIndexedModList().get(MOD_ID);
        String version = container == null ? null : container.getVersion();
        if (!StatusworksVersion.isSupported(version)) {
            Diag.LOGGER.warn("Statusworks {} is loaded, but JAFS needs Statusworks {} or newer for its contribution types; "
                    + "Statusworks states cannot change JAFS rates", version, StatusworksVersion.MINIMUM);
            return false;
        }
        StatusworksContributions contributions = new StatusworksContributions(PropertyRegistry.get(), message -> Diag.LOGGER.warn(message));
        List<String> accepted = new ArrayList<String>();
        for (String rateId : RATES) {
            RegistrationResult result = StatusworksApi.registerContributionType(new RateContributionType(rateId, contributions));
            if (result.isAccepted()) {
                accepted.add(rateId);
            } else {
                Diag.LOGGER.warn("Statusworks refused the contribution type {}: {}", rateId, result.error());
            }
        }
        if (accepted.isEmpty()) {
            return false;
        }
        JafsApi.registerRateModifierProvider(PROVIDER_ID, contributions);
        MinecraftForge.EVENT_BUS.register(contributions);
        Diag.LOGGER.info("Statusworks {} found: contribution types registered; statuses, pools, and marks may change {}", version, accepted);
        return true;
    }
}
