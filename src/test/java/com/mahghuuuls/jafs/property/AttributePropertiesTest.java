package com.mahghuuuls.jafs.property;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The property table against the authority file ({@code config-defaults.json}, {@code
 * properties} section): every row has an entry with the same operations and limits, every entry
 * with an attribute has a row (or is one of the originals, whose registered specs are compared
 * too), the vanilla and Forge upper limits sit inside the attribute's own range while the lower
 * side clamps, every row has a display name in the language file, and registration honors the
 * required mod (REQ-056, REQ-056a, REQ-056b, REQ-078).
 */
class AttributePropertiesTest {

    private static final Map<String, IAttribute> VANILLA = new HashMap<String, IAttribute>();

    static {
        for (IAttribute attribute : new IAttribute[] {SharedMonsterAttributes.MAX_HEALTH, SharedMonsterAttributes.MOVEMENT_SPEED,
                SharedMonsterAttributes.ATTACK_DAMAGE, SharedMonsterAttributes.ATTACK_SPEED, SharedMonsterAttributes.ARMOR,
                SharedMonsterAttributes.ARMOR_TOUGHNESS, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, SharedMonsterAttributes.LUCK,
                EntityPlayer.REACH_DISTANCE}) {
            VANILLA.put(attribute.getName(), attribute);
        }
    }

    @Test
    void everyRowMatchesTheAuthorityFile() throws Exception {
        JsonObject properties = load().getAsJsonObject("properties");
        for (AttributeProperties.Row row : AttributeProperties.rows()) {
            JsonObject entry = properties.getAsJsonObject(row.id());
            assertNotNull(entry, row.id() + " has no entry in config-defaults.json");
            assertEquals(row.attribute(), entry.get("attribute").getAsString(), row.id() + " attribute");
            assertRange(row.id() + " flat", row.flat(), entry.get("flat"));
            assertRange(row.id() + " percent", row.percent(), entry.get("percent"));
            JsonElement requires = entry.get("requiresMod");
            assertEquals(row.requiresMod(), requires == null ? null : requires.getAsString(), row.id() + " requiresMod");
        }
    }

    @Test
    void theOriginalsMatchTheAuthorityFile() throws Exception {
        JsonObject properties = load().getAsJsonObject("properties");
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        for (String id : new String[] {PropertyRegistry.MAX_HEALTH, PropertyRegistry.RECOVERY_RATE, PropertyRegistry.DEPLETION_RATE}) {
            PropertySpec spec = registry.find(id);
            JsonObject entry = properties.getAsJsonObject(id);
            assertNotNull(entry, id + " has no entry in config-defaults.json");
            assertRange(id + " flat", spec.range(PropertyOperation.FLAT), entry.get("flat"));
            assertRange(id + " percent", spec.range(PropertyOperation.PERCENT), entry.get("percent"));
        }
    }

    @Test
    void everyAttributeEntryInTheAuthorityFileHasARow() throws Exception {
        JsonObject properties = load().getAsJsonObject("properties");
        for (Map.Entry<String, JsonElement> e : properties.entrySet()) {
            if (e.getKey().startsWith("_")) {
                continue;
            }
            JsonObject entry = e.getValue().getAsJsonObject();
            if (!entry.has("attribute") || PropertyRegistry.MAX_HEALTH.equals(e.getKey())) {
                continue; // the two rates and max health have their own appliers (checked above)
            }
            assertNotNull(AttributeProperties.row(e.getKey()), e.getKey() + " is in config-defaults.json but has no table row");
        }
    }

    @Test
    void vanillaAndForgeUpperLimitsStayInsideTheAttributeRange() {
        for (AttributeProperties.Row row : AttributeProperties.rows()) {
            if (row.requiresMod() != null) {
                continue;
            }
            IAttribute attribute = VANILLA.get(row.attribute());
            assertNotNull(attribute, row.attribute() + " is not a vanilla or Forge player attribute");
            RangedAttribute ranged = (RangedAttribute) attribute;
            double base = attribute.getDefaultValue();
            if (row.flat() != null) {
                double top = base + row.flat().max();
                assertEquals(top, ranged.clampValue(top), 1e-9, row.id() + " flat max would be clamped by the attribute range");
            }
            if (row.percent() != null) {
                double top = base * (1 + row.percent().max() / 100.0);
                assertEquals(top, ranged.clampValue(top), 1e-9, row.id() + " percent max would be clamped by the attribute range");
                assertTrue(row.percent().min() >= -100, row.id() + " percent min below -100 would invert the value");
            }
            // The lower side is allowed to clamp (REQ-056a): a food cannot take an attribute below the attribute's own floor.
        }
    }

    /**
     * Potion Core 1.9's attribute ranges, read from its {@code PotionCoreAttributes} bytecode (no class of it is
     * referenced): default, minimum, maximum. The table's upper limits must not be clamped by them.
     */
    private static final Object[][] POTION_CORE_RANGES = {
            {"potioncore.jumpHeight", 1.0, 0.0, 2147483647.0},
            {"potioncore.digSpeed", 1.0, 8.1E-4, 2147483647.0},
            {"potioncore.projectileDamage", 1.0, 0.0, 2147483647.0},
            {"potioncore.magicDamage", 1.0, 0.0, 2147483647.0},
            {"potioncore.damageResistance", 1.0, -2147483648.0, 2.0},
            {"potioncore.magicShielding", 0.0, 0.0, 20.0},
            {"potioncore.stepHeight", 0.6, 0.0, 256.0},
    };

    @Test
    void potionCoreUpperLimitsStayInsidePotionCoresRanges() {
        int checked = 0;
        for (AttributeProperties.Row row : AttributeProperties.rows()) {
            if (!AttributeProperties.POTION_CORE.equals(row.requiresMod())) {
                continue;
            }
            Object[] found = null;
            for (Object[] candidate : POTION_CORE_RANGES) {
                if (candidate[0].equals(row.attribute())) {
                    found = candidate;
                }
            }
            assertNotNull(found, row.attribute() + " is not a Potion Core 1.9 attribute");
            double base = (Double) found[1];
            double max = (Double) found[3];
            if (row.flat() != null) {
                assertTrue(base + row.flat().max() <= max + 1e-9, row.id() + " flat max would be clamped by Potion Core's range");
            }
            if (row.percent() != null) {
                assertTrue(base * (1 + row.percent().max() / 100.0) <= max + 1e-9, row.id() + " percent max would be clamped by Potion Core's range");
                assertTrue(row.percent().min() >= -100, row.id() + " percent min below -100 would invert the value");
            }
            checked++;
        }
        assertEquals(7, checked, "the seven Potion Core rows");
    }

    @Test
    void everyRowHasADisplayNameInTheLanguageFile() throws Exception {
        File lang = new File("src/main/resources/assets/jafs/lang/en_us.lang");
        assertTrue(lang.isFile(), "the language file was not found from the project directory: " + lang.getAbsolutePath());
        List<String> lines = Files.readAllLines(lang.toPath(), StandardCharsets.UTF_8);
        for (AttributeProperties.Row row : AttributeProperties.rows()) {
            String key = row.spec().displayNameKey() + "=";
            boolean found = false;
            for (String line : lines) {
                if (line.startsWith(key) && line.length() > key.length()) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, row.id() + " has no display name under " + key + " in en_us.lang");
        }
    }

    @Test
    void specsCarryTheRowValues() {
        AttributeProperties.Row armor = AttributeProperties.row("jafs:armor");
        PropertySpec spec = armor.spec();
        assertEquals("jafs:armor", spec.id());
        assertEquals("property.jafs.armor", spec.displayNameKey());
        assertEquals("gray", spec.color());
        assertTrue(spec.supports(PropertyOperation.FLAT));
        assertTrue(spec.supports(PropertyOperation.PERCENT));
        assertEquals(-30, spec.range(PropertyOperation.FLAT).min(), 1e-9);
        AttributeProperties.Row luck = AttributeProperties.row("jafs:luck");
        assertFalse(luck.spec().supports(PropertyOperation.PERCENT));
        assertNull(AttributeProperties.row("jafs:nothing"));
    }

    @Test
    void registrationHonorsTheRequiredMod() {
        AttributeProperties.Row optional = new AttributeProperties.Row("jafs:test_optional", "test.attribute", "", "white",
                new PropertySpec.Range(0, 1), null, "somemod");
        AttributeProperties.Row plain = AttributeProperties.row("jafs:armor");
        List<AttributeProperties.Row> rows = new java.util.ArrayList<AttributeProperties.Row>();
        rows.add(plain);
        rows.add(optional);
        PropertyRegistry absent = PropertyRegistry.forTests();
        List<String> registered = AttributeProperties.registerAll(rows, absent, AttributeProperties.NONE);
        assertEquals(Collections.singletonList("jafs:armor"), registered);
        assertNull(absent.find("jafs:test_optional"));
        assertTrue(absent.applier("jafs:armor") instanceof AttributeApplier);
        PropertyRegistry present = PropertyRegistry.forTests();
        registered = AttributeProperties.registerAll(rows, present, new AttributeProperties.ModPresence() {
            @Override
            public boolean isLoaded(String modId) {
                return "somemod".equals(modId);
            }
        });
        assertEquals(2, registered.size());
        assertNotNull(present.find("jafs:test_optional"));
    }

    @Test
    void theExamplesValidateWithoutAProblemWhenPotionCoreIsPresent() {
        PropertyRegistry registry = PropertyRegistry.forTests();
        registry.register(PropertySpec.builder(PropertyRegistry.MAX_HEALTH).unit("HP").color("red")
                .operation(PropertyOperation.FLAT, -1000, 1000).operation(PropertyOperation.PERCENT, -100, 1000).build(), new MaxHealthApplier());
        registry.register(PropertySpec.builder(PropertyRegistry.RECOVERY_RATE).operation(PropertyOperation.PERCENT, -1000, 1000).build(),
                registry.rates().recoveryApplier());
        registry.register(PropertySpec.builder(PropertyRegistry.DEPLETION_RATE).operation(PropertyOperation.PERCENT, -1000, 1000).build(),
                registry.rates().depletionApplier());
        AttributeProperties.registerAll(registry, new AttributeProperties.ModPresence() {
            @Override
            public boolean isLoaded(String modId) {
                return AttributeProperties.POTION_CORE.equals(modId);
            }
        });
        List<com.mahghuuuls.jafs.core.RawDefinition> raws = com.mahghuuuls.jafs.config.DefinitionJson.read(
                new java.io.StringReader(com.mahghuuuls.jafs.config.ExamplesWriter.content()));
        com.mahghuuuls.jafs.core.DefinitionValidator.Result result = com.mahghuuuls.jafs.core.DefinitionValidator.validate(raws, registry);
        assertTrue(result.problems.isEmpty(), result.problems.toString());
        assertEquals(8, result.definitions.size());
    }

    @Test
    void potionCoreRowsRegisterOnlyWithPotionCore() {
        PropertyRegistry without = PropertyRegistry.forTests();
        AttributeProperties.registerAll(without, AttributeProperties.NONE);
        assertNull(without.find("jafs:jump_height"));
        assertEquals(8, without.all().size(), "the eight vanilla and Forge rows");
        PropertyRegistry with = PropertyRegistry.forTests();
        AttributeProperties.registerAll(with, new AttributeProperties.ModPresence() {
            @Override
            public boolean isLoaded(String modId) {
                return AttributeProperties.POTION_CORE.equals(modId);
            }
        });
        assertNotNull(with.find("jafs:jump_height"));
        assertNotNull(with.find("jafs:damage_resistance"));
        assertEquals(15, with.all().size(), "all fifteen attribute-backed rows");
    }

    private static void assertRange(String what, PropertySpec.Range range, JsonElement element) {
        if (range == null) {
            assertNull(element, what + " is unsupported in the table but present in config-defaults.json");
            return;
        }
        assertNotNull(element, what + " is in the table but absent from config-defaults.json");
        JsonArray pair = element.getAsJsonArray();
        assertEquals(pair.get(0).getAsDouble(), range.min(), 1e-9, what + " min");
        assertEquals(pair.get(1).getAsDouble(), range.max(), 1e-9, what + " max");
    }

    private static JsonObject load() throws Exception {
        InputStream stream = AttributePropertiesTest.class.getResourceAsStream("/config-defaults.json");
        assertNotNull(stream, "config-defaults.json is missing from test resources");
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }
}
