package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every structural rule of a food definition, applied to raw entries. Produces the valid
 * definitions and the problems. Errors reject the entry (or the matcher or modifier they name);
 * warnings keep it. Registry existence of item and block ids is checked by the loader, not here.
 */
public final class DefinitionValidator {

    public static final int MIN_DURATION_SECONDS = 1;
    public static final int MAX_DURATION_SECONDS = 86400;

    private DefinitionValidator() {
    }

    /** The validator's output: definitions that survived, and everything worth reporting. */
    public static final class Result {
        public final List<FoodDefinition> definitions = new ArrayList<FoodDefinition>();
        public final List<Problem> problems = new ArrayList<Problem>();
        /** Entries rejected outright (bad id, bad duration, no matchers). */
        public int rejected;
        /** Entries kept but inert because no modifier was valid. */
        public int inert;

        public boolean hasErrors() {
            for (Problem problem : problems) {
                if (problem.isError()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static Result validate(List<RawDefinition> raws, PropertyCatalog catalog) {
        Result result = new Result();
        Set<String> seen = new HashSet<String>();
        for (RawDefinition raw : raws) {
            String id = raw.id == null ? "#" + raw.index : raw.id;
            boolean reject = false;
            for (String key : raw.unknownKeys) {
                result.problems.add(Problem.warning(id, key, "unknown field; ignored (a typo?)"));
            }
            if (raw.id == null || raw.id.trim().isEmpty()) {
                result.problems.add(Problem.error(id, "id", "missing or empty"));
                reject = true;
            } else if (!seen.add(raw.id)) {
                result.problems.add(Problem.error(id, "id", "duplicate id; the later entry is rejected"));
                reject = true;
            }
            if (raw.fullDuration == null) {
                result.problems.add(Problem.error(id, "fullDuration", "missing; seconds, " + MIN_DURATION_SECONDS + " to "
                        + MAX_DURATION_SECONDS));
                reject = true;
            } else if (raw.fullDuration < MIN_DURATION_SECONDS || raw.fullDuration > MAX_DURATION_SECONDS) {
                result.problems.add(Problem.error(id, "fullDuration", raw.fullDuration + " is outside " + MIN_DURATION_SECONDS
                        + ".." + MAX_DURATION_SECONDS + " seconds"));
                reject = true;
            }
            List<Matcher> matchers = new ArrayList<Matcher>();
            if (raw.match == null || raw.match.isEmpty()) {
                result.problems.add(Problem.error(id, "match", "missing; at least one item or block matcher is required"));
                reject = true;
            } else {
                for (int i = 0; i < raw.match.size(); i++) {
                    Matcher matcher = validateMatcher(id, i, raw.match.get(i), result);
                    if (matcher != null) {
                        matchers.add(matcher);
                    }
                }
                if (matchers.isEmpty() && !reject) {
                    result.problems.add(Problem.error(id, "match", "no valid matcher"));
                    reject = true;
                }
            }
            List<Modifier> modifiers = new ArrayList<Modifier>();
            if (raw.modifiers != null) {
                for (int i = 0; i < raw.modifiers.size(); i++) {
                    Modifier modifier = validateModifier(id, i, raw.modifiers.get(i), catalog, result);
                    if (modifier != null) {
                        modifiers.add(modifier);
                    }
                }
            }
            if (reject) {
                result.rejected++;
                continue;
            }
            if (modifiers.isEmpty()) {
                result.inert++;
                result.problems.add(Problem.warning(id, "modifiers", "no valid modifier; the definition grants no active food"));
            }
            result.definitions.add(new FoodDefinition(raw.id, matchers, raw.fullDuration, raw.priority == null ? 0 : raw.priority,
                    modifiers));
        }
        return result;
    }

    private static Matcher validateMatcher(String id, int index, RawDefinition.RawMatcher raw, Result result) {
        String field = "match[" + index + "]";
        boolean item = raw.item != null && !raw.item.isEmpty();
        boolean block = raw.block != null && !raw.block.isEmpty();
        if (item == block) {
            result.problems.add(Problem.error(id, field, "exactly one of 'item' or 'block' is required"));
            return null;
        }
        String target = item ? raw.item : raw.block;
        if (!target.contains(":")) {
            result.problems.add(Problem.error(id, field, "'" + target + "' is not a namespaced id (expected modid:name)"));
            return null;
        }
        int meta = raw.meta == null ? Matcher.ANY_META : raw.meta;
        if (raw.meta != null && raw.meta < 0) {
            result.problems.add(Problem.error(id, field + ".meta", "must be 0 or more (omit it for any)"));
            return null;
        }
        if (block) {
            if (raw.nbt != null && !raw.nbt.isEmpty()) {
                result.problems.add(Problem.warning(id, field + ".nbt", "ignored on a block matcher"));
            }
            return Matcher.block(target, meta);
        }
        if (raw.nbt != null) {
            for (String key : raw.nbt.keySet()) {
                if (key == null || key.isEmpty() || raw.nbt.get(key) == null) {
                    result.problems.add(Problem.error(id, field + ".nbt", "every entry needs a name and a value"));
                    return null;
                }
            }
        }
        return Matcher.item(target, meta, raw.nbt);
    }

    private static Modifier validateModifier(String id, int index, RawDefinition.RawModifier raw, PropertyCatalog catalog,
            Result result) {
        String field = "modifiers[" + index + "]";
        if (raw.property == null || raw.property.isEmpty()) {
            result.problems.add(Problem.error(id, field + ".property", "missing"));
            return null;
        }
        PropertyOperation operation = PropertyOperation.fromKey(raw.operation);
        if (operation == null) {
            result.problems.add(Problem.error(id, field + ".operation", "'" + raw.operation + "' is not 'flat' or 'percent'"));
            return null;
        }
        if (raw.amount == null || raw.amount.isNaN()) {
            result.problems.add(Problem.error(id, field + ".amount", "missing or not a number"));
            return null;
        }
        PropertySpec spec = catalog.find(raw.property);
        if (spec == null) {
            result.problems.add(Problem.warning(id, field + ".property", "property '" + raw.property
                    + "' is not available (no provider registered it); this modifier is skipped"));
            return null;
        }
        if (!spec.supports(operation)) {
            result.problems.add(Problem.error(id, field + ".operation", "property '" + raw.property + "' does not support '"
                    + operation.key() + "'"));
            return null;
        }
        PropertySpec.Range range = spec.range(operation);
        if (!range.contains(raw.amount)) {
            result.problems.add(Problem.error(id, field + ".amount", raw.amount + " is outside " + range + " for '" + raw.property
                    + "' " + operation.key()));
            return null;
        }
        return new Modifier(raw.property, operation, raw.amount);
    }
}
