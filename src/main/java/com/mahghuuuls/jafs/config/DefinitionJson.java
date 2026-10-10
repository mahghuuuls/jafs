package com.mahghuuuls.jafs.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mahghuuuls.jafs.core.RawDefinition;
import com.mahghuuuls.jafs.core.ValueText;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * The JSON shape of {@code foods.json}: an array of definitions. Reads into raw entries without
 * judging them; the validator does that. Any structural surprise (not an array, a non-object
 * entry, a wrong value type) is a {@link JsonParseException} naming the entry.
 */
public final class DefinitionJson {

    private DefinitionJson() {
    }

    public static List<RawDefinition> read(Reader reader) {
        JsonElement root = new JsonParser().parse(reader);
        if (root == null || root.isJsonNull()) {
            return new ArrayList<RawDefinition>();
        }
        if (!root.isJsonArray()) {
            throw new JsonParseException("the file must be a JSON array of definitions");
        }
        List<RawDefinition> result = new ArrayList<RawDefinition>();
        JsonArray array = root.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                throw new JsonParseException("entry #" + i + " is not an object");
            }
            result.add(readDefinition(element.getAsJsonObject(), i));
        }
        return result;
    }

    private static final Set<String> DEFINITION_KEYS = new HashSet<String>(Arrays.asList("id", "match", "fullDuration", "priority", "modifiers"));
    private static final Set<String> MATCHER_KEYS = new HashSet<String>(Arrays.asList("item", "block", "meta", "nbt"));
    private static final Set<String> MODIFIER_KEYS = new HashSet<String>(Arrays.asList("property", "operation", "amount"));

    private static RawDefinition readDefinition(JsonObject object, int index) {
        RawDefinition raw = new RawDefinition();
        raw.index = index;
        String where = "entry #" + index;
        raw.id = string(object, "id", where);
        if (raw.id != null) {
            where = "definition '" + raw.id + "'";
        }
        unknownKeys(object, DEFINITION_KEYS, "", raw);
        raw.fullDuration = integer(object, "fullDuration", where);
        raw.priority = integer(object, "priority", where);
        JsonElement match = object.get("match");
        if (match != null && !match.isJsonNull()) {
            if (!match.isJsonArray()) {
                throw new JsonParseException(where + ": 'match' must be an array");
            }
            for (JsonElement entry : match.getAsJsonArray()) {
                if (!entry.isJsonObject()) {
                    throw new JsonParseException(where + ": every 'match' entry must be an object");
                }
                unknownKeys(entry.getAsJsonObject(), MATCHER_KEYS, "match[" + raw.match.size() + "].", raw);
                raw.match.add(readMatcher(entry.getAsJsonObject(), where));
            }
        }
        JsonElement modifiers = object.get("modifiers");
        if (modifiers != null && !modifiers.isJsonNull()) {
            if (!modifiers.isJsonArray()) {
                throw new JsonParseException(where + ": 'modifiers' must be an array");
            }
            for (JsonElement entry : modifiers.getAsJsonArray()) {
                if (!entry.isJsonObject()) {
                    throw new JsonParseException(where + ": every 'modifiers' entry must be an object");
                }
                JsonObject m = entry.getAsJsonObject();
                unknownKeys(m, MODIFIER_KEYS, "modifiers[" + raw.modifiers.size() + "].", raw);
                RawDefinition.RawModifier modifier = new RawDefinition.RawModifier();
                modifier.property = string(m, "property", where);
                modifier.operation = string(m, "operation", where);
                modifier.amount = number(m, "amount", where);
                raw.modifiers.add(modifier);
            }
        }
        return raw;
    }

    private static RawDefinition.RawMatcher readMatcher(JsonObject object, String where) {
        RawDefinition.RawMatcher matcher = new RawDefinition.RawMatcher();
        matcher.item = string(object, "item", where);
        matcher.block = string(object, "block", where);
        matcher.meta = integer(object, "meta", where);
        JsonElement nbt = object.get("nbt");
        if (nbt != null && !nbt.isJsonNull()) {
            if (!nbt.isJsonObject()) {
                throw new JsonParseException(where + ": 'nbt' must be an object of field names to values");
            }
            for (Map.Entry<String, JsonElement> field : nbt.getAsJsonObject().entrySet()) {
                JsonElement value = field.getValue();
                if (!value.isJsonPrimitive()) {
                    throw new JsonParseException(where + ": nbt field '" + field.getKey() + "' must be a string, number, or boolean");
                }
                matcher.nbt.put(field.getKey(), valueText(value.getAsJsonPrimitive()));
            }
        }
        return matcher;
    }

    /** Keys that are not part of the format and do not start with an underscore are remembered as warnings. */
    private static void unknownKeys(JsonObject object, Set<String> known, String prefix, RawDefinition raw) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (!known.contains(key) && !key.startsWith("_")) {
                raw.unknownKeys.add(prefix + key);
            }
        }
    }

    /** Numbers and booleans become the same text the item reader produces for NBT values. */
    static String valueText(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return ValueText.ofBoolean(primitive.getAsBoolean());
        }
        if (primitive.isNumber()) {
            return ValueText.ofNumber(primitive.getAsDouble());
        }
        return primitive.getAsString();
    }

    private static String string(JsonObject object, String key, String where) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(where + ": '" + key + "' must be a string");
        }
        return element.getAsString();
    }

    private static Integer integer(JsonObject object, String key, String where) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(where + ": '" + key + "' must be a whole number");
        }
        double value = element.getAsDouble();
        if (value != Math.rint(value) || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new JsonParseException(where + ": '" + key + "' must be a whole number");
        }
        return (int) value;
    }

    private static Double number(JsonObject object, String key, String where) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(where + ": '" + key + "' must be a number");
        }
        return element.getAsDouble();
    }
}
