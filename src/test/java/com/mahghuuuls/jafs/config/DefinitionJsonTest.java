package com.mahghuuuls.jafs.config;

import com.google.gson.JsonParseException;
import com.mahghuuuls.jafs.core.DefinitionValidator;
import com.mahghuuuls.jafs.core.PropertyCatalog;
import com.mahghuuuls.jafs.core.RawDefinition;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The file shape, the examples file, and the problem messages for malformed input. */
class DefinitionJsonTest {

    private static final PropertyCatalog CATALOG = catalog();

    private static PropertyCatalog catalog() {
        PropertyRegistry registry = PropertyRegistry.get();
        if (registry.find(PropertyRegistry.MAX_HEALTH) == null) {
            PropertyRegistry.registerCoreProperties();
        }
        return registry;
    }

    @Test
    void readsEveryFieldAndLeavesAbsentOnesNull() {
        List<RawDefinition> raws = DefinitionJson.read(new StringReader(
                "[{\"id\":\"x\",\"match\":[{\"item\":\"minecraft:potion\",\"nbt\":{\"Potion\":\"minecraft:strength\",\"Custom\":3}}],"
                        + "\"fullDuration\":30,\"modifiers\":[{\"property\":\"jafs:max_health\",\"operation\":\"flat\",\"amount\":4}]}]"));
        assertEquals(1, raws.size());
        RawDefinition raw = raws.get(0);
        assertEquals("x", raw.id);
        assertNull(raw.priority);
        assertEquals(Integer.valueOf(30), raw.fullDuration);
        assertEquals("minecraft:strength", raw.match.get(0).nbt.get("Potion"));
        assertEquals("3", raw.match.get(0).nbt.get("Custom"));
        assertTrue(raw.unknownKeys.isEmpty(), raw.unknownKeys.toString());
        assertEquals(Double.valueOf(4), raw.modifiers.get(0).amount);
    }

    @Test
    void emptyArrayAndEmptyFileAreValid() {
        assertTrue(DefinitionJson.read(new StringReader("[]")).isEmpty());
        assertTrue(DefinitionJson.read(new StringReader("")).isEmpty());
    }

    @Test
    void wrongShapesAreNamed() {
        JsonParseException notArray = assertThrows(JsonParseException.class, () -> DefinitionJson.read(new StringReader("{}")));
        assertTrue(notArray.getMessage().contains("array"));
        JsonParseException badType = assertThrows(JsonParseException.class,
                () -> DefinitionJson.read(new StringReader("[{\"id\":\"x\",\"fullDuration\":\"soon\"}]")));
        assertTrue(badType.getMessage().contains("definition 'x'"), badType.getMessage());
        assertTrue(badType.getMessage().contains("fullDuration"), badType.getMessage());
        assertThrows(JsonParseException.class, () -> DefinitionJson.read(new StringReader("[{\"id\":\"x\",\"fullDuration\":1.5}]")));
        assertThrows(JsonParseException.class, () -> DefinitionJson.read(new StringReader("[{\"id\": }]")));
    }

    @Test
    void numbersAndBooleansInNbtUseTheSharedValueText() {
        RawDefinition raw = DefinitionJson.read(new StringReader(
                "[{\"id\":\"n\",\"match\":[{\"item\":\"minecraft:stick\",\"nbt\":{\"Unbreakable\":1,\"Damage\":3.0,\"Ratio\":2.5,\"Flag\":true,\"Name\":\"x\"}}]}]"))
                .get(0);
        assertEquals("1", raw.match.get(0).nbt.get("Unbreakable"));
        assertEquals("3", raw.match.get(0).nbt.get("Damage"));
        assertEquals("2.5", raw.match.get(0).nbt.get("Ratio"));
        assertEquals("1", raw.match.get(0).nbt.get("Flag"));
        assertEquals("x", raw.match.get(0).nbt.get("Name"));
    }

    @Test
    void unknownKeysAreRememberedAndUnderscoreKeysAreNot() {
        RawDefinition raw = DefinitionJson.read(new StringReader(
                "[{\"id\":\"t\",\"fullDuration\":100,\"_about\":\"note\",\"priorty\":5,\"match\":[{\"item\":\"minecraft:apple\",\"metadata\":1}],"
                        + "\"modifiers\":[{\"property\":\"jafs:max_health\",\"operation\":\"flat\",\"amount\":1,\"ammount\":2}]}]")).get(0);
        assertEquals(java.util.Arrays.asList("priorty", "match[0].metadata", "modifiers[0].ammount"), raw.unknownKeys);
        DefinitionValidator.Result result = DefinitionValidator.validate(java.util.Collections.singletonList(raw), CATALOG);
        assertEquals(3, result.problems.size());
        assertTrue(result.problems.get(0).toString().contains("priorty"), result.problems.toString());
        assertTrue(result.problems.get(0).severity() == com.mahghuuuls.jafs.core.Problem.Severity.WARNING);
    }

    @Test
    void syntaxErrorsCarryALineNumber() {
        try {
            DefinitionJson.read(new StringReader("[\n  { \"id\": \"broken\", \"fullDuration\": 100\n  { \"id\": \"x\" }\n]"));
        } catch (JsonParseException e) {
            String message = FoodDefinitionFile.rootMessage(e);
            assertTrue(message.contains("line 3"), message);
            return;
        }
        throw new AssertionError("no exception");
    }

    @Test
    void examplesFileParsesAndValidatesCleanly() {
        List<RawDefinition> raws = DefinitionJson.read(new StringReader(ExamplesWriter.content()));
        assertEquals(6, raws.size());
        DefinitionValidator.Result result = DefinitionValidator.validate(raws, CATALOG);
        assertEquals(6, result.definitions.size());
        assertTrue(result.problems.isEmpty(), result.problems.toString());
        assertEquals(10, result.definitions.get(4).priority());
    }
}
