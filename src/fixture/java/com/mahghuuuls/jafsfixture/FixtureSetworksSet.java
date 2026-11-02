package com.mahghuuuls.jafsfixture;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.SetworksApi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The campaign set for the development runtimes, the same one the CraftTweaker script defines for
 * the test instance: an iron armor set whose bonuses grant JAFS rates through {@code jafs:property}.
 * Loaded only when Setworks is present.
 */
final class FixtureSetworksSet {

    static final String SET = "jafsf:iron_armor";

    private FixtureSetworksSet() {
    }

    static void register() {
        DefinitionSource source = DefinitionSource.mod(JafsFixtureMod.MOD_ID);
        report(SetworksApi.registerSet(SetBuilder.set(SET).displayName("Iron Armor (JAFS test)")
                .armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                        ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")), source));
        report(SetworksApi.registerBonus(BonusBuilder.bonus("jafsf:iron_recovery").requires(SET, 2)
                .description("Iron Recovery: +50% recovery rate")
                .effect("jafs:property", params("jafs:recovery_rate", "50")), source));
        report(SetworksApi.registerBonus(BonusBuilder.bonus("jafsf:iron_endurance").requiresAll(SET)
                .description("Iron Endurance: +25% recovery rate and slower food timers")
                .effect("jafs:property", params("jafs:recovery_rate", "25"))
                .effect("jafs:property", params("jafs:depletion_rate", "-30")), source));
    }

    private static Map<String, String> params(String property, String amount) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("property", property);
        params.put("amount", amount);
        return params;
    }

    private static void report(RegistrationResult result) {
        if (result.isOk()) {
            JafsFixtureMod.LOGGER.info("fixture Setworks registration ok");
        } else {
            JafsFixtureMod.LOGGER.warn("fixture Setworks registration failed: {}", result.message());
        }
    }
}
