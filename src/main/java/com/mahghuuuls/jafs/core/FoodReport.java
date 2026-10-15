package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The pack-maker report (REQ-069b, REV-010): one row per edible item the game knows, with the
 * definition that covers it and the bonuses it gives, or the fact that none does. Pure: the
 * command gathers the candidates from the item registry and hands them in as plain values; this
 * class only resolves, formats, sorts, and counts.
 */
public final class FoodReport {

    /** What the item is to the player, which also orders the file: foods, then drinks, then blocks. */
    public enum Kind {
        FOOD("food"), DRINK("drink"), BLOCK("block");

        public final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    /** One item the registry knows (or one a definition names and the registry does not). */
    public static final class Candidate {
        public final Kind kind;
        public final ConsumableKey key;
        /** Empty when the registry has no such item. */
        public final String displayName;
        public final String modId;
        /** Null when the item is not a vanilla-style food with hunger numbers. */
        public final Integer hunger;
        public final Float saturation;

        public Candidate(Kind kind, ConsumableKey key, String displayName, String modId, Integer hunger, Float saturation) {
            this.kind = kind;
            this.key = key;
            this.displayName = displayName == null ? "" : displayName;
            this.modId = modId == null ? "" : modId;
            this.hunger = hunger;
            this.saturation = saturation;
        }
    }

    /** One line of the file. */
    public static final class Row {
        public final Candidate candidate;
        public final String status;
        /** Null when no definition covers the item. */
        public final FoodDefinition definition;
        /** The resolved definition's matcher that covered the item, described; empty when none. */
        public final String matcher;
        /** The conflicting definition ids, joined by {@code |}; empty unless the status is conflict. */
        public final String conflicting;

        Row(Candidate candidate, String status, FoodDefinition definition, String matcher, String conflicting) {
            this.candidate = candidate;
            this.status = status;
            this.definition = definition;
            this.matcher = matcher;
            this.conflicting = conflicting;
        }

        public String[] cells() {
            ConsumableKey key = candidate.key;
            return new String[] {
                key.id(),
                key.meta() == Matcher.ANY_META ? "" : Integer.toString(key.meta()),
                candidate.kind.label,
                candidate.displayName,
                candidate.modId,
                candidate.hunger == null ? "" : Integer.toString(candidate.hunger),
                candidate.saturation == null ? "" : Float.toString(candidate.saturation),
                definition == null ? conflicting : definition.id(),
                status,
                definition == null ? "" : Integer.toString(definition.fullDurationSeconds()),
                definition == null ? "" : bonuses(definition),
                matcher,
            };
        }
    }

    /** The reply's numbers. */
    public static final class Counts {
        public final int listed;
        public final int configured;
        public final int unconfigured;

        Counts(int listed, int configured, int unconfigured) {
            this.listed = listed;
            this.configured = configured;
            this.unconfigured = unconfigured;
        }
    }

    public static final String STATUS_CONFIGURED = "configured";
    public static final String STATUS_UNCONFIGURED = "unconfigured";
    public static final String STATUS_INERT = "inert";
    public static final String STATUS_CONFLICT = "conflict";

    public static final String[] HEADER = {"item_id", "meta", "kind", "display_name", "mod_id", "vanilla_hunger",
        "vanilla_saturation", "definition_id", "status", "duration_seconds", "bonuses", "matcher"};

    private static final Comparator<Row> ORDER = new Comparator<Row>() {
        @Override
        public int compare(Row a, Row b) {
            int byKind = a.candidate.kind.compareTo(b.candidate.kind);
            if (byKind != 0) {
                return byKind;
            }
            int byId = a.candidate.key.id().compareTo(b.candidate.key.id());
            if (byId != 0) {
                return byId;
            }
            int byMeta = Integer.compare(a.candidate.key.meta(), b.candidate.key.meta());
            if (byMeta != 0) {
                return byMeta;
            }
            return a.candidate.key.nbt().toString().compareTo(b.candidate.key.nbt().toString());
        }
    };

    private FoodReport() {
    }

    /**
     * Resolves every candidate against the definitions and returns the rows in file order. The
     * status is {@code configured}, {@code inert} (a definition without modifiers),
     * {@code unconfigured}, or {@code conflict} (several definitions at the same priority cover the
     * item; the colliding ids go into the definition column). Definitions the loader disabled are not
     * in the set and cannot be listed; the load report names them.
     */
    public static List<Row> rows(List<Candidate> candidates, DefinitionSet set) {
        List<Row> rows = new ArrayList<Row>();
        for (Candidate candidate : candidates) {
            rows.add(row(candidate, set));
        }
        Collections.sort(rows, ORDER);
        return rows;
    }

    static Row row(Candidate candidate, DefinitionSet set) {
        DefinitionSet.Resolution resolution = set.resolve(candidate.key);
        if (resolution.isConflict()) {
            StringBuilder ids = new StringBuilder();
            for (FoodDefinition definition : resolution.conflicting()) {
                if (ids.length() > 0) {
                    ids.append('|');
                }
                ids.append(definition.id());
            }
            return new Row(candidate, STATUS_CONFLICT, null, "", ids.toString());
        }
        FoodDefinition definition = resolution.definition();
        if (definition == null) {
            return new Row(candidate, STATUS_UNCONFIGURED, null, "", "");
        }
        String status = definition.hasModifiers() ? STATUS_CONFIGURED : STATUS_INERT;
        return new Row(candidate, status, definition, describeMatcher(definition, candidate.key), "");
    }

    /** The first of the definition's matchers that covers the key, as the file shows it. */
    static String describeMatcher(FoodDefinition definition, ConsumableKey key) {
        for (Matcher matcher : definition.matchers()) {
            if (!matcher.matches(key)) {
                continue;
            }
            if (matcher.kind() == Matcher.Kind.BLOCK) {
                return "block";
            }
            if (!matcher.nbt().isEmpty()) {
                return "item nbt";
            }
            if (matcher.meta() != Matcher.ANY_META) {
                return "item meta=" + matcher.meta();
            }
            return "item";
        }
        return "";
    }

    /** {@code property:+flat} and {@code property:+percent%} entries, separated by {@code ; }. */
    static String bonuses(FoodDefinition definition) {
        StringBuilder text = new StringBuilder();
        for (Modifier modifier : definition.modifiers()) {
            if (text.length() > 0) {
                text.append("; ");
            }
            text.append(modifier.propertyId()).append(':').append(modifier.amount() >= 0 ? "+" : "").append(ValueText.ofNumber(modifier.amount()));
            if (modifier.operation() == PropertyOperation.PERCENT) {
                text.append('%');
            }
        }
        return text.toString();
    }

    public static Counts count(List<Row> rows) {
        int configured = 0;
        int unconfigured = 0;
        for (Row row : rows) {
            if (STATUS_CONFIGURED.equals(row.status)) {
                configured++;
            } else if (STATUS_UNCONFIGURED.equals(row.status)) {
                unconfigured++;
            }
        }
        return new Counts(rows.size(), configured, unconfigured);
    }

    /** The whole file: the header line, then one line per row, each cell quoted when it needs it. */
    public static String csv(List<Row> rows) {
        StringBuilder text = new StringBuilder();
        appendLine(text, HEADER);
        for (Row row : rows) {
            appendLine(text, row.cells());
        }
        return text.toString();
    }

    private static void appendLine(StringBuilder text, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(quote(cells[i]));
        }
        text.append("\r\n");
    }

    /** RFC 4180 quoting: a cell with a comma, a quote, or a line break is wrapped and its quotes doubled. */
    static String quote(String cell) {
        if (cell.indexOf(',') < 0 && cell.indexOf('"') < 0 && cell.indexOf('\n') < 0 && cell.indexOf('\r') < 0) {
            return cell;
        }
        return "\"" + cell.replace("\"", "\"\"") + "\"";
    }
}
