package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Priority, equal-priority conflicts, metadata, and NBT matching (REQ-011 to REQ-013). */
class DefinitionSetTest {

    private static FoodDefinition def(String id, int priority, Matcher... matchers) {
        return new FoodDefinition(id, Arrays.asList(matchers), 60, priority,
                Collections.singletonList(new Modifier("jafs:max_health", PropertyOperation.FLAT, 1)));
    }

    private static ConsumableKey item(String id, int meta, String... nbt) {
        Map<String, String> fields = new HashMap<String, String>();
        for (int i = 0; i + 1 < nbt.length; i += 2) {
            fields.put(nbt[i], nbt[i + 1]);
        }
        return new ConsumableKey(Matcher.Kind.ITEM, id, meta, fields);
    }

    @Test
    void higherPriorityWins() {
        DefinitionSet set = new DefinitionSet(1, Arrays.asList(
                def("all_potions", 0, Matcher.item("minecraft:potion", Matcher.ANY_META, null)),
                def("strength", 5, Matcher.item("minecraft:potion", Matcher.ANY_META,
                        Collections.singletonMap("Potion", "minecraft:strength")))));
        assertEquals("strength", set.resolve(item("minecraft:potion", 0, "Potion", "minecraft:strength")).definition().id());
        assertEquals("all_potions", set.resolve(item("minecraft:potion", 0, "Potion", "minecraft:speed")).definition().id());
    }

    @Test
    void equalPriorityIsAConflictAndNothingMatches() {
        DefinitionSet set = new DefinitionSet(1, Arrays.asList(
                def("c", 0, Matcher.item("minecraft:apple", Matcher.ANY_META, null)),
                def("d", 0, Matcher.item("minecraft:apple", Matcher.ANY_META, null))));
        DefinitionSet.Resolution resolution = set.resolve(item("minecraft:apple", 0));
        assertTrue(resolution.isConflict());
        assertNull(resolution.definition());
        assertEquals(2, resolution.conflicting().size());
    }

    @Test
    void metadataAndUnrelatedNbtAreHandled() {
        DefinitionSet set = new DefinitionSet(1, Arrays.asList(
                def("red_dye_food", 0, Matcher.item("minecraft:dye", 1, null)),
                def("steak", 0, Matcher.item("minecraft:cooked_beef", Matcher.ANY_META, null))));
        assertEquals("red_dye_food", set.resolve(item("minecraft:dye", 1)).definition().id());
        assertTrue(set.resolve(item("minecraft:dye", 2)).isNone());
        assertEquals("steak", set.resolve(item("minecraft:cooked_beef", 0, "display", "{Name:\"Fancy\"}")).definition().id());
        assertTrue(set.nbtFieldsFor("minecraft:cooked_beef").isEmpty());
    }

    @Test
    void nbtFieldsAreCollectedPerItem() {
        DefinitionSet set = new DefinitionSet(1, Collections.singletonList(
                def("strength", 0, Matcher.item("minecraft:potion", Matcher.ANY_META,
                        Collections.singletonMap("Potion", "minecraft:strength")))));
        assertEquals(Collections.singleton("Potion"), set.nbtFieldsFor("minecraft:potion"));
        assertTrue(set.resolve(item("minecraft:potion", 0)).isNone());
    }

    @Test
    void blocksAndItemsAreSeparateIndexes() {
        DefinitionSet set = new DefinitionSet(1, Collections.singletonList(def("cake", 0, Matcher.block("minecraft:cake", Matcher.ANY_META))));
        assertTrue(set.resolve(item("minecraft:cake", 0)).isNone());
        assertEquals("cake", set.resolve(new ConsumableKey(Matcher.Kind.BLOCK, "minecraft:cake", 3, null)).definition().id());
    }

    @Test
    void loadTimeConflictsAreFoundForOverlappingEqualPriorityMatchers() {
        DefinitionSet set = new DefinitionSet(1, Arrays.asList(
                def("c", 0, Matcher.item("minecraft:apple", Matcher.ANY_META, null)),
                def("d", 0, Matcher.item("minecraft:apple", Matcher.ANY_META, null)),
                def("e", 5, Matcher.item("minecraft:apple", Matcher.ANY_META, null)),
                def("f", 0, Matcher.item("minecraft:apple", 1, null)),
                def("g", 0, Matcher.item("minecraft:potion", Matcher.ANY_META, Collections.singletonMap("Potion", "minecraft:speed"))),
                def("h", 0, Matcher.item("minecraft:potion", Matcher.ANY_META, Collections.singletonMap("Potion", "minecraft:strength"))),
                def("i", 0, Matcher.item("minecraft:potion", Matcher.ANY_META, null))));
        java.util.List<DefinitionSet.Conflict> conflicts = set.conflicts();
        java.util.Set<String> pairs = new java.util.TreeSet<String>();
        for (DefinitionSet.Conflict conflict : conflicts) {
            pairs.add(conflict.first.id() + "+" + conflict.second.id());
        }
        // c and d overlap; both overlap f (any meta covers meta 1); e has another priority; g and h
        // are disjoint NBT; i (no NBT) overlaps both g and h.
        assertEquals(new java.util.TreeSet<String>(Arrays.asList("c+d", "c+f", "d+f", "g+i", "h+i")), pairs);
    }

    @Test
    void unknownIdResolvesToNone() {
        assertTrue(DefinitionSet.empty().resolve(item("minecraft:apple", 0)).isNone());
    }
}
