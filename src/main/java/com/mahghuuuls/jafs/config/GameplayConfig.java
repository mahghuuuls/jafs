package com.mahghuuuls.jafs.config;

import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner of {@code config/jafs/jafs.cfg}. Uses the classic Forge {@link Configuration} so the
 * file can be re-read from disk by {@code /jafs reload} at any time. Readers hold the immutable
 * {@link GameplaySettings} snapshot returned by {@link #current()}.
 *
 * <p>Reading and applying are separate steps so a reload can validate both files before it swaps
 * either. A value that is not a number or boolean at all is a file-level failure: a reload keeps
 * the previous snapshot; the first load uses the default for that value and reports it. A value
 * of the right kind but outside its range falls back to the default with a report (REQ-077).
 */
public final class GameplayConfig {

    private static File file;
    private static volatile GameplaySettings current = GameplaySettings.defaults();
    private static long revision;

    private GameplayConfig() {
    }

    /** One read of the file: the validated settings and whether any value was unreadable. */
    public static final class Candidate {
        public final GameplaySettings settings;
        /** Values that are not a number or boolean at all; non-empty means file-level failure. */
        public final List<String> malformed;

        Candidate(GameplaySettings settings, List<String> malformed) {
            this.settings = settings;
            this.malformed = malformed;
        }

        public boolean fileLevelFailure() {
            return !malformed.isEmpty();
        }

        public String summary() {
            if (fileLevelFailure()) {
                return "jafs.cfg not loaded (" + malformed.get(0) + "); the previous settings stay active";
            }
            return "jafs.cfg revision " + settings.revision() + ", " + settings.problems().size() + " problem(s)";
        }
    }

    /** First load, from the mod's configuration directory: malformed values use their defaults and are reported. */
    public static void load(File configDirectory) {
        file = new File(new File(configDirectory, "jafs"), "jafs.cfg");
        Candidate candidate = read();
        for (String problem : candidate.malformed) {
            Diag.LOGGER.error("jafs.cfg: {}; using the default", problem);
        }
        apply(candidate);
    }

    /** Re-reads the file from disk and swaps the snapshot unless a value is unreadable. Returns the candidate. */
    public static Candidate reload() {
        Candidate candidate = read();
        if (!candidate.fileLevelFailure()) {
            apply(candidate);
        }
        return candidate;
    }

    /** Reads and validates the file without touching the current snapshot. */
    public static Candidate read() {
        if (file == null) {
            throw new IllegalStateException("GameplayConfig.load has not been called");
        }
        return read(file, revision + 1);
    }

    static Candidate read(File source, long candidateRevision) {
        Configuration configuration = new Configuration(source);
        configuration.load();
        Map<String, String> values = new HashMap<String, String>();
        values.put(SettingKeys.ACTIVE_FOOD_LIMIT.name, intProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.ACTIVE_FOOD_LIMIT));
        values.put(SettingKeys.ELIGIBILITY_THRESHOLD.name, intProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.ELIGIBILITY_THRESHOLD));
        values.put(SettingKeys.BASE_RECOVERY_RATE.name, doubleProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.BASE_RECOVERY_RATE));
        values.put(SettingKeys.RECOVERY_DELAY.name, intProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.RECOVERY_DELAY));
        values.put(SettingKeys.DEPLETION_RATE.name, doubleProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.DEPLETION_RATE));
        values.put(SettingKeys.HUNGER_EFFECT_INCREMENT.name, doubleProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.HUNGER_EFFECT_INCREMENT));
        values.put(SettingKeys.KEEP_FOODS_ON_DEATH.name, boolProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.KEEP_FOODS_ON_DEATH));
        values.put(SettingKeys.INHIBITED_BLOCKS_RECOVERY.name, boolProperty(configuration, SettingKeys.CATEGORY_GAMEPLAY, SettingKeys.INHIBITED_BLOCKS_RECOVERY));
        values.put(SettingKeys.DIAGNOSTIC_MODE.name, boolProperty(configuration, SettingKeys.CATEGORY_DIAGNOSTICS, SettingKeys.DIAGNOSTIC_MODE));
        if (configuration.hasChanged()) {
            configuration.save();
        }
        SettingsValidator.Parsed parsed = SettingsValidator.parse(values);
        return new Candidate(SettingsValidator.validate(parsed.raw, candidateRevision), parsed.malformed);
    }

    /** Makes the candidate the working snapshot and reports its range fallbacks. */
    public static void apply(Candidate candidate) {
        GameplaySettings settings = candidate.settings;
        for (String problem : settings.problems()) {
            Diag.LOGGER.warn("jafs.cfg: {}", problem);
        }
        revision = settings.revision();
        current = settings;
        Diag.setEnabled(settings.diagnosticMode());
        Diag.event("config loaded revision={} problems={}", revision, settings.problems().size());
    }

    public static GameplaySettings current() {
        return current;
    }

    /**
     * The property's text as written in the file. The typed Forge accessors replace a value of the
     * wrong kind with the default and mark the file for saving, which would erase a typo silently;
     * the string-and-type accessor returns an existing value untouched and creates only absent keys.
     */
    private static String intProperty(Configuration configuration, String category, SettingKeys.IntKey key) {
        Property property = configuration.get(category, key.name, String.valueOf(key.defaultValue),
                key.comment + " [range: " + key.min + " ~ " + key.max + ", default: " + key.defaultValue + "]", Property.Type.INTEGER);
        return property.getString();
    }

    private static String doubleProperty(Configuration configuration, String category, SettingKeys.DoubleKey key) {
        Property property = configuration.get(category, key.name, String.valueOf(key.defaultValue),
                key.comment + " [range: " + key.min + " ~ " + key.max + ", default: " + key.defaultValue + "]", Property.Type.DOUBLE);
        return property.getString();
    }

    private static String boolProperty(Configuration configuration, String category, SettingKeys.BoolKey key) {
        Property property = configuration.get(category, key.name, String.valueOf(key.defaultValue),
                key.comment + " [default: " + key.defaultValue + "]", Property.Type.BOOLEAN);
        return property.getString();
    }
}
