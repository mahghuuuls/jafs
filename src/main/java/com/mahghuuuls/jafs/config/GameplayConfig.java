package com.mahghuuuls.jafs.config;

import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/**
 * Owner of {@code config/jafs/jafs.cfg}. Uses the classic Forge {@link Configuration} so the
 * file can be re-read from disk by {@code /jafs reload} at any time. Readers hold the immutable
 * {@link GameplaySettings} snapshot returned by {@link #current()}.
 */
public final class GameplayConfig {

    private static File file;
    private static volatile GameplaySettings current = GameplaySettings.defaults();
    private static long revision;

    private GameplayConfig() {
    }

    /** First load, from the mod's configuration directory. */
    public static void load(File configDirectory) {
        file = new File(new File(configDirectory, "jafs"), "jafs.cfg");
        reload();
    }

    /** Re-reads the file from disk and swaps the snapshot. Returns the new snapshot. */
    public static GameplaySettings reload() {
        if (file == null) {
            throw new IllegalStateException("GameplayConfig.load has not been called");
        }
        Configuration configuration = new Configuration(file);
        configuration.load();
        SettingsValidator.Raw raw = new SettingsValidator.Raw();
        raw.activeFoodLimit = intProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.ACTIVE_FOOD_LIMIT);
        raw.eligibilityThreshold = intProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.ELIGIBILITY_THRESHOLD);
        raw.baseRecoveryRate = doubleProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.BASE_RECOVERY_RATE);
        raw.recoveryDelay = intProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.RECOVERY_DELAY);
        raw.depletionRate = doubleProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.DEPLETION_RATE);
        raw.hungerEffectIncrement = doubleProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.HUNGER_EFFECT_INCREMENT);
        raw.keepFoodsOnDeath = boolProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.KEEP_FOODS_ON_DEATH);
        raw.inhibitedBlocksRecovery = boolProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.INHIBITED_BLOCKS_RECOVERY);
        raw.diagnosticMode = boolProperty(configuration, SettingKeys.CATEGORY_DIAGNOSTICS, SettingKeys.DIAGNOSTIC_MODE);
        if (configuration.hasChanged()) {
            configuration.save();
        }
        revision++;
        GameplaySettings settings = SettingsValidator.validate(raw, revision);
        for (String problem : settings.problems()) {
            Diag.LOGGER.warn("jafs.cfg: {}", problem);
        }
        current = settings;
        Diag.setEnabled(settings.diagnosticMode());
        Diag.event("config loaded revision={} problems={}", revision, settings.problems().size());
        return settings;
    }

    public static GameplaySettings current() {
        return current;
    }

    private static int intProperty(Configuration configuration, String category, SettingKeys.IntKey key) {
        Property property = configuration.get(category, key.name, key.defaultValue,
                key.comment + " [range: " + key.min + " ~ " + key.max + ", default: " + key.defaultValue + "]");
        return property.getInt(key.defaultValue);
    }

    private static double doubleProperty(Configuration configuration, String category, SettingKeys.DoubleKey key) {
        Property property = configuration.get(category, key.name, key.defaultValue,
                key.comment + " [range: " + key.min + " ~ " + key.max + ", default: " + key.defaultValue + "]");
        return property.getDouble(key.defaultValue);
    }

    private static boolean boolProperty(Configuration configuration, String category, SettingKeys.BoolKey key) {
        Property property = configuration.get(category, key.name, key.defaultValue,
                key.comment + " [default: " + key.defaultValue + "]");
        return property.getBoolean(key.defaultValue);
    }
}
