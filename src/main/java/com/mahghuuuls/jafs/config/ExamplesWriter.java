package com.mahghuuuls.jafs.config;

/**
 * The content of {@code foods-examples.json}. The game never reads this file; a pack maker
 * copies entries from it into {@code foods.json}. It is a valid definitions file on its own, so
 * copying everything works. JSON has no comments, so each entry explains itself through a
 * {@code "_about"} field, which the loader ignores.
 */
public final class ExamplesWriter {

    private ExamplesWriter() {
    }

    public static String content() {
        return "[\n"
                + "  {\n"
                + "    \"_about\": \"JAFS examples. This file is never loaded. Copy an entry into foods.json, edit it, then run /jafs reload. Fields: id (unique name), match (list of items or blocks), fullDuration (seconds, 1 to 86400, required), priority (higher wins when two entries match one item; equal priorities are a conflict), modifiers (property, operation flat or percent, amount). Properties shipped by JAFS: jafs:max_health (flat HP or percent), jafs:recovery_rate (percent), jafs:depletion_rate (percent). Addons add more.\",\n"
                + "    \"id\": \"example_steak\",\n"
                + "    \"match\": [ { \"item\": \"minecraft:cooked_beef\" } ],\n"
                + "    \"fullDuration\": 1200,\n"
                + "    \"priority\": 0,\n"
                + "    \"modifiers\": [\n"
                + "      { \"property\": \"jafs:max_health\", \"operation\": \"flat\", \"amount\": 4 }\n"
                + "    ]\n"
                + "  },\n"
                + "  {\n"
                + "    \"_about\": \"Bread: a long, mild food. Recovery rate +25 percent means recovery heals 25 percent faster than the base rate.\",\n"
                + "    \"id\": \"example_bread\",\n"
                + "    \"match\": [ { \"item\": \"minecraft:bread\" } ],\n"
                + "    \"fullDuration\": 1800,\n"
                + "    \"modifiers\": [\n"
                + "      { \"property\": \"jafs:recovery_rate\", \"operation\": \"percent\", \"amount\": 25 }\n"
                + "    ]\n"
                + "  },\n"
                + "  {\n"
                + "    \"_about\": \"Golden carrot: two bonuses at once. A negative depletion-rate percent makes every active food last longer.\",\n"
                + "    \"id\": \"example_golden_carrot\",\n"
                + "    \"match\": [ { \"item\": \"minecraft:golden_carrot\" } ],\n"
                + "    \"fullDuration\": 900,\n"
                + "    \"modifiers\": [\n"
                + "      { \"property\": \"jafs:max_health\", \"operation\": \"flat\", \"amount\": 2 },\n"
                + "      { \"property\": \"jafs:depletion_rate\", \"operation\": \"percent\", \"amount\": -20 }\n"
                + "    ]\n"
                + "  },\n"
                + "  {\n"
                + "    \"_about\": \"Mushroom stew: the bowl comes back as usual. Several items can share one definition; they count as the same food.\",\n"
                + "    \"id\": \"example_stews\",\n"
                + "    \"match\": [ { \"item\": \"minecraft:mushroom_stew\" }, { \"item\": \"minecraft:rabbit_stew\" }, { \"item\": \"minecraft:beetroot_soup\" } ],\n"
                + "    \"fullDuration\": 600,\n"
                + "    \"modifiers\": [\n"
                + "      { \"property\": \"jafs:max_health\", \"operation\": \"percent\", \"amount\": 10 }\n"
                + "    ]\n"
                + "  },\n"
                + "  {\n"
                + "    \"_about\": \"A potion matched by its NBT: only Strength potions match. The nbt object lists field names and the exact values they must have. Numbers are compared by value: write 1 for a byte flag such as Unbreakable, 3 for a short or an int, 2.5 for a float. For a metadata variant use \\\"meta\\\": 1 next to the item.\",\n"
                + "    \"id\": \"example_strength_potion\",\n"
                + "    \"match\": [ { \"item\": \"minecraft:potion\", \"nbt\": { \"Potion\": \"minecraft:strength\" } } ],\n"
                + "    \"fullDuration\": 300,\n"
                + "    \"priority\": 10,\n"
                + "    \"modifiers\": [\n"
                + "      { \"property\": \"jafs:recovery_rate\", \"operation\": \"percent\", \"amount\": 50 }\n"
                + "    ]\n"
                + "  },\n"
                + "  {\n"
                + "    \"_about\": \"A cake: eaten by right-clicking the block, so it is matched by block id. Block matchers need the cake support, which arrives in a later JAFS version; until then this entry loads but does nothing.\",\n"
                + "    \"id\": \"example_cake\",\n"
                + "    \"match\": [ { \"block\": \"minecraft:cake\" } ],\n"
                + "    \"fullDuration\": 300,\n"
                + "    \"modifiers\": [\n"
                + "      { \"property\": \"jafs:max_health\", \"operation\": \"flat\", \"amount\": 1 }\n"
                + "    ]\n"
                + "  }\n"
                + "]\n";
    }
}
