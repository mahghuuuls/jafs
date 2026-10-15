package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The report rows for a small registry against a small definition set (REQ-069b). */
class FoodReportTest {

    private static FoodDefinition def(String id, int seconds, int priority, List<Matcher> matchers, Modifier... modifiers) {
        return new FoodDefinition(id, matchers, seconds, priority, Arrays.asList(modifiers));
    }

    private static Map<String, String> nbt(String key, String value) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(key, value);
        return map;
    }

    private static final DefinitionSet SET = new DefinitionSet(1, Arrays.asList(
            def("steak", 1200, 0, Collections.singletonList(Matcher.item("minecraft:cooked_beef", Matcher.ANY_META, Collections.<String, String>emptyMap())),
                    new Modifier("jafs:max_health", PropertyOperation.FLAT, 4), new Modifier("jafs:recovery_rate", PropertyOperation.PERCENT, 12.5)),
            def("plain_fish", 60, 0, Collections.singletonList(Matcher.item("minecraft:fish", 1, Collections.<String, String>emptyMap()))),
            def("strength", 300, 0, Collections.singletonList(Matcher.item("minecraft:potion", Matcher.ANY_META, nbt("Potion", "minecraft:strength"))),
                    new Modifier("jafs:recovery_rate", PropertyOperation.PERCENT, 20)),
            def("cake", 300, 0, Collections.singletonList(Matcher.block("minecraft:cake", 0)),
                    new Modifier("jafs:max_health", PropertyOperation.FLAT, 2)),
            def("ghost", 10, 0, Collections.singletonList(Matcher.item("othermod:nothing", Matcher.ANY_META, Collections.<String, String>emptyMap())),
                    new Modifier("jafs:max_health", PropertyOperation.FLAT, -1.5))));

    private static FoodReport.Candidate item(FoodReport.Kind kind, String id, int meta, Map<String, String> nbt, String name, Integer hunger, Float sat) {
        return new FoodReport.Candidate(kind, new ConsumableKey(Matcher.Kind.ITEM, id, meta, nbt), name, id.substring(0, id.indexOf(':')), hunger, sat);
    }

    private static final List<FoodReport.Candidate> CANDIDATES = Arrays.asList(
            item(FoodReport.Kind.DRINK, "minecraft:potion", 0, nbt("Potion", "minecraft:swiftness"), "Potion of Swiftness", null, null),
            item(FoodReport.Kind.FOOD, "minecraft:fish", 1, Collections.<String, String>emptyMap(), "Raw Salmon", 2, 0.1f),
            item(FoodReport.Kind.FOOD, "minecraft:cooked_beef", 0, Collections.<String, String>emptyMap(), "Steak", 8, 0.8f),
            item(FoodReport.Kind.FOOD, "minecraft:apple", 0, Collections.<String, String>emptyMap(), "Apple", 4, 0.3f),
            item(FoodReport.Kind.DRINK, "minecraft:potion", 0, nbt("Potion", "minecraft:strength"), "Potion of Strength", null, null),
            new FoodReport.Candidate(FoodReport.Kind.BLOCK, new ConsumableKey(Matcher.Kind.BLOCK, "minecraft:cake", 0, Collections.<String, String>emptyMap()), "Cake", "minecraft", null, null),
            item(FoodReport.Kind.FOOD, "othermod:nothing", Matcher.ANY_META, Collections.<String, String>emptyMap(), "", null, null));

    @Test
    void rowsAreSortedFoodsThenDrinksThenBlocksAndByIdInsideAKind() {
        List<FoodReport.Row> rows = FoodReport.rows(CANDIDATES, SET);
        assertEquals(7, rows.size());
        assertEquals("minecraft:apple", rows.get(0).candidate.key.id());
        assertEquals("minecraft:cooked_beef", rows.get(1).candidate.key.id());
        assertEquals("minecraft:fish", rows.get(2).candidate.key.id());
        assertEquals("othermod:nothing", rows.get(3).candidate.key.id());
        assertEquals(FoodReport.Kind.DRINK, rows.get(4).candidate.kind);
        assertEquals(FoodReport.Kind.DRINK, rows.get(5).candidate.kind);
        assertEquals(FoodReport.Kind.BLOCK, rows.get(6).candidate.kind);
    }

    @Test
    void statusesAndCellsFollowTheRequirement() {
        List<FoodReport.Row> rows = FoodReport.rows(CANDIDATES, SET);
        assertArrayEquals(new String[] {"minecraft:apple", "0", "food", "Apple", "minecraft", "4", "0.3", "", "unconfigured", "", "", ""},
                rows.get(0).cells());
        assertArrayEquals(new String[] {"minecraft:cooked_beef", "0", "food", "Steak", "minecraft", "8", "0.8", "steak", "configured", "1200",
                "jafs:max_health:+4; jafs:recovery_rate:+12.5%", "item"}, rows.get(1).cells());
        assertArrayEquals(new String[] {"minecraft:fish", "1", "food", "Raw Salmon", "minecraft", "2", "0.1", "plain_fish", "inert", "60", "",
                "item meta=1"}, rows.get(2).cells());
        assertArrayEquals(new String[] {"othermod:nothing", "", "food", "", "othermod", "", "", "ghost", "configured", "10", "jafs:max_health:-1.5",
                "item"}, rows.get(3).cells());
        FoodReport.Row strength = rows.get(4).definition != null ? rows.get(4) : rows.get(5);
        FoodReport.Row swiftness = rows.get(4).definition != null ? rows.get(5) : rows.get(4);
        assertEquals("strength", strength.definition.id());
        assertEquals("item nbt", strength.matcher);
        assertEquals("unconfigured", swiftness.status);
        assertArrayEquals(new String[] {"minecraft:cake", "0", "block", "Cake", "minecraft", "", "", "cake", "configured", "300",
                "jafs:max_health:+2", "block"}, rows.get(6).cells());
    }

    @Test
    void countsListConfiguredAndUnconfigured() {
        FoodReport.Counts counts = FoodReport.count(FoodReport.rows(CANDIDATES, SET));
        assertEquals(7, counts.listed);
        assertEquals(4, counts.configured);
        assertEquals(2, counts.unconfigured);
    }

    @Test
    void aSamePriorityConflictIsReportedWithBothIds() {
        DefinitionSet set = new DefinitionSet(1, Arrays.asList(
                def("a", 10, 0, Collections.singletonList(Matcher.item("minecraft:apple", Matcher.ANY_META, Collections.<String, String>emptyMap())),
                        new Modifier("jafs:max_health", PropertyOperation.FLAT, 1)),
                def("b", 10, 0, Collections.singletonList(Matcher.item("minecraft:apple", 0, Collections.<String, String>emptyMap())),
                        new Modifier("jafs:max_health", PropertyOperation.FLAT, 2))));
        FoodReport.Row row = FoodReport.rows(Collections.singletonList(
                item(FoodReport.Kind.FOOD, "minecraft:apple", 0, Collections.<String, String>emptyMap(), "Apple", 4, 0.3f)), set).get(0);
        assertEquals("conflict", row.status);
        assertEquals("a|b", row.cells()[7]);
        assertEquals("", row.cells()[9]);
    }

    @Test
    void csvHasTheHeaderAndQuotesOnlyWhatNeedsIt() {
        FoodReport.Candidate odd = item(FoodReport.Kind.FOOD, "othermod:pie", 0, Collections.<String, String>emptyMap(), "Pie, \"Grandma's\"", 6, 0.6f);
        String csv = FoodReport.csv(FoodReport.rows(Collections.singletonList(odd), SET));
        String[] lines = csv.split("\r\n");
        assertEquals(2, lines.length);
        assertEquals(String.join(",", FoodReport.HEADER), lines[0]);
        assertTrue(lines[1].startsWith("othermod:pie,0,food,\"Pie, \"\"Grandma's\"\"\",othermod,6,0.6,,unconfigured,,,"), lines[1]);
        assertEquals("plain", FoodReport.quote("plain"));
    }
}
