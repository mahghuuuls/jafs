package com.mahghuuuls.jafs.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structural rules of REQ-010 to REQ-012 and REQ-058, on raw entries. */
class DefinitionValidatorTest {

    private static RawDefinition steak(String id, Integer duration) {
        RawDefinition raw = new RawDefinition();
        raw.id = id;
        raw.fullDuration = duration;
        RawDefinition.RawMatcher matcher = new RawDefinition.RawMatcher();
        matcher.item = "minecraft:cooked_beef";
        raw.match.add(matcher);
        RawDefinition.RawModifier modifier = new RawDefinition.RawModifier();
        modifier.property = "jafs:max_health";
        modifier.operation = "flat";
        modifier.amount = 4.0;
        raw.modifiers.add(modifier);
        return raw;
    }

    private static DefinitionValidator.Result validate(RawDefinition... raws) {
        return DefinitionValidator.validate(Arrays.asList(raws), new TestCatalog());
    }

    private static String problems(DefinitionValidator.Result result) {
        StringBuilder b = new StringBuilder();
        for (Problem p : result.problems) {
            b.append(p).append(" | ");
        }
        return b.toString();
    }

    @Test
    void validEntryLoads() {
        DefinitionValidator.Result result = validate(steak("steak", 1200));
        assertEquals(1, result.definitions.size(), problems(result));
        assertEquals(24000, result.definitions.get(0).fullTicks());
        assertEquals(0, result.definitions.get(0).priority());
        assertTrue(result.problems.isEmpty(), problems(result));
    }

    @Test
    void missingDurationIsRejectedWithFieldName() {
        DefinitionValidator.Result result = validate(steak("steak", null));
        assertEquals(0, result.definitions.size());
        assertEquals(1, result.rejected);
        Problem problem = result.problems.get(0);
        assertTrue(problem.isError());
        assertEquals("steak", problem.definitionId());
        assertEquals("fullDuration", problem.field());
    }

    @Test
    void durationOutsideRangeIsRejected() {
        assertEquals(1, validate(steak("a", 0)).rejected);
        assertEquals(1, validate(steak("b", 86401)).rejected);
        assertEquals(0, validate(steak("c", 1)).rejected);
        assertEquals(0, validate(steak("d", 86400)).rejected);
    }

    @Test
    void unknownPropertyIsSkippedWithWarningAndDefinitionBecomesInert() {
        RawDefinition raw = steak("stamina_food", 100);
        raw.modifiers.get(0).property = "addon:stamina";
        DefinitionValidator.Result result = validate(raw);
        assertEquals(1, result.definitions.size());
        assertFalse(result.definitions.get(0).hasModifiers());
        assertEquals(1, result.inert);
        assertFalse(result.hasErrors(), problems(result));
        assertTrue(problems(result).contains("addon:stamina"));
    }

    @Test
    void unknownPropertyNextToValidOneKeepsTheValidOne() {
        RawDefinition raw = steak("mixed", 100);
        RawDefinition.RawModifier extra = new RawDefinition.RawModifier();
        extra.property = "addon:stamina";
        extra.operation = "flat";
        extra.amount = 10.0;
        raw.modifiers.add(extra);
        DefinitionValidator.Result result = validate(raw);
        assertEquals(1, result.definitions.get(0).modifiers().size());
        assertEquals("jafs:max_health", result.definitions.get(0).modifiers().get(0).propertyId());
        assertEquals(0, result.inert);
    }

    @Test
    void unsupportedOperationAndOutOfRangeAmountAreErrors() {
        RawDefinition raw = steak("bad_ops", 100);
        raw.modifiers.get(0).property = "jafs:recovery_rate";
        raw.modifiers.get(0).operation = "flat";
        RawDefinition.RawModifier tooBig = new RawDefinition.RawModifier();
        tooBig.property = "jafs:max_health";
        tooBig.operation = "percent";
        tooBig.amount = 5000.0;
        raw.modifiers.add(tooBig);
        DefinitionValidator.Result result = validate(raw);
        assertTrue(result.hasErrors());
        assertEquals(1, result.inert);
        String text = problems(result);
        assertTrue(text.contains("modifiers[0].operation"), text);
        assertTrue(text.contains("modifiers[1].amount"), text);
    }

    @Test
    void matcherNeedsExactlyOneOfItemOrBlockAndANamespacedId() {
        RawDefinition raw = steak("m", 100);
        raw.match.get(0).block = "minecraft:cake";
        RawDefinition.RawMatcher bare = new RawDefinition.RawMatcher();
        bare.item = "apple";
        raw.match.add(bare);
        DefinitionValidator.Result result = validate(raw);
        assertEquals(1, result.rejected);
        String text = problems(result);
        assertTrue(text.contains("match[0]"), text);
        assertTrue(text.contains("match[1]"), text);
    }

    @Test
    void duplicateIdRejectsTheLaterEntry() {
        DefinitionValidator.Result result = validate(steak("same", 100), steak("same", 200));
        assertEquals(1, result.definitions.size());
        assertEquals(2000, result.definitions.get(0).fullTicks());
        assertEquals(1, result.rejected);
    }

    @Test
    void blockMatcherWithNbtWarnsAndIgnoresIt() {
        RawDefinition raw = steak("cake", 100);
        raw.match.get(0).item = null;
        raw.match.get(0).block = "minecraft:cake";
        raw.match.get(0).nbt.put("X", "1");
        DefinitionValidator.Result result = validate(raw);
        assertEquals(1, result.definitions.size());
        Matcher matcher = result.definitions.get(0).matchers().get(0);
        assertEquals(Matcher.Kind.BLOCK, matcher.kind());
        assertTrue(matcher.nbt().isEmpty());
        assertFalse(result.hasErrors(), problems(result));
    }

    @Test
    void emptyInputIsValid() {
        DefinitionValidator.Result result = DefinitionValidator.validate(Collections.<RawDefinition>emptyList(), new TestCatalog());
        assertTrue(result.definitions.isEmpty());
        assertTrue(result.problems.isEmpty());
    }

    @Test
    void priorityIsKept() {
        RawDefinition raw = steak("p", 100);
        raw.priority = 7;
        List<FoodDefinition> defs = validate(raw).definitions;
        assertEquals(7, defs.get(0).priority());
    }
}
