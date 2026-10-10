package com.mahghuuuls.jafs.config;

import com.google.gson.JsonParseException;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.DefinitionValidator;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.core.Problem;
import com.mahghuuuls.jafs.core.PropertyCatalog;
import com.mahghuuuls.jafs.core.RawDefinition;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owner of {@code config/jafs/foods.json}. Reads, validates, resolves item and block ids against
 * the registries, and swaps the immutable {@link DefinitionSet} held by {@link #current()}.
 * A file-level failure (unreadable, not valid JSON, wrong shape) keeps the previous set. Reading
 * ({@link #read}) and swapping ({@link #apply}) are separate so a reload can validate both files
 * before it swaps either.
 */
public final class FoodDefinitionFile {

    /** What one load produced; the command and the log report it. */
    public static final class LoadReport {
        public final boolean fileLevelFailure;
        public final String fileLevelMessage;
        public final int loaded;
        public final int inert;
        public final int disabled;
        public final int rejected;
        public final List<Problem> problems;

        LoadReport(boolean fileLevelFailure, String fileLevelMessage, int loaded, int inert, int disabled, int rejected,
                List<Problem> problems) {
            this.fileLevelFailure = fileLevelFailure;
            this.fileLevelMessage = fileLevelMessage;
            this.loaded = loaded;
            this.inert = inert;
            this.disabled = disabled;
            this.rejected = rejected;
            this.problems = Collections.unmodifiableList(problems);
        }

        public int errorCount() {
            int count = fileLevelFailure ? 1 : 0;
            for (Problem problem : problems) {
                if (problem.isError()) {
                    count++;
                }
            }
            return count;
        }

        public String summary() {
            if (fileLevelFailure) {
                return "foods.json not loaded (" + fileLevelMessage + "); the previous definitions stay active";
            }
            return "foods.json: loaded " + loaded + ", inert " + inert + ", disabled " + disabled + ", rejected " + rejected
                    + ", problems " + problems.size();
        }
    }

    /** One read of the file: the set to swap in (null on a file-level failure) and its report. */
    public static final class Candidate {
        public final DefinitionSet set;
        public final LoadReport report;

        Candidate(DefinitionSet set, LoadReport report) {
            this.set = set;
            this.report = report;
        }
    }

    private static File file;
    private static File examplesFile;
    private static volatile DefinitionSet current = DefinitionSet.empty();
    private static volatile LoadReport lastReport = new LoadReport(false, "", 0, 0, 0, 0, new ArrayList<Problem>());
    private static long revision;

    private FoodDefinitionFile() {
    }

    public static void setLocation(File configDirectory) {
        File folder = new File(configDirectory, "jafs");
        file = new File(folder, "foods.json");
        examplesFile = new File(folder, "foods-examples.json");
    }

    public static DefinitionSet current() {
        return current;
    }

    public static LoadReport lastReport() {
        return lastReport;
    }

    /** Reads, validates, and swaps in one step (startup, or a reload of this file alone). */
    public static LoadReport load(PropertyCatalog catalog) {
        Candidate candidate = read(catalog);
        if (!candidate.report.fileLevelFailure) {
            apply(candidate);
        }
        return candidate.report;
    }

    /**
     * Reads and validates the file without touching the current set. Creates an empty file and
     * the examples file when absent. Must run after registries are populated (load complete or later).
     */
    public static Candidate read(PropertyCatalog catalog) {
        if (file == null) {
            throw new IllegalStateException("FoodDefinitionFile.setLocation has not been called");
        }
        ensureFiles();
        List<RawDefinition> raws;
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            try {
                raws = DefinitionJson.read(reader);
            } finally {
                reader.close();
            }
        } catch (JsonParseException e) {
            return fail("invalid JSON: " + rootMessage(e));
        } catch (IOException e) {
            return fail("cannot read " + file.getName() + ": " + e.getMessage());
        } catch (RuntimeException e) {
            return fail("unexpected content: " + rootMessage(e));
        }
        DefinitionValidator.Result result = DefinitionValidator.validate(raws, catalog);
        List<FoodDefinition> resolved = resolveIds(result);
        int disabled = removeConflicts(resolved, result.problems);
        DefinitionSet set = new DefinitionSet(revision + 1, resolved);
        LoadReport report = new LoadReport(false, "", resolved.size(), result.inert, disabled, result.rejected, result.problems);
        for (Problem problem : result.problems) {
            if (problem.isError()) {
                Diag.LOGGER.error("foods.json: {}", problem);
            } else {
                Diag.LOGGER.warn("foods.json: {}", problem);
            }
        }
        return new Candidate(set, report);
    }

    /** Makes the candidate's set the working set. */
    public static void apply(Candidate candidate) {
        if (candidate.set == null) {
            return;
        }
        revision = candidate.set.revision();
        current = candidate.set;
        lastReport = candidate.report;
        Diag.LOGGER.info("{} (revision {})", candidate.report.summary(), revision);
    }

    private static Candidate fail(String message) {
        Diag.LOGGER.error("foods.json: {}", message);
        LoadReport report = new LoadReport(true, message, current.size(), 0, 0, 0, new ArrayList<Problem>());
        lastReport = report;
        return new Candidate(null, report);
    }

    /**
     * Drops matchers whose item or block id is not registered, with a warning; a definition left
     * without matchers is dropped with an error. Registry checks are the only Minecraft-aware
     * step of loading.
     */
    private static List<FoodDefinition> resolveIds(DefinitionValidator.Result result) {
        List<FoodDefinition> kept = new ArrayList<FoodDefinition>();
        for (FoodDefinition definition : result.definitions) {
            List<Matcher> matchers = new ArrayList<Matcher>();
            for (int i = 0; i < definition.matchers().size(); i++) {
                Matcher matcher = definition.matchers().get(i);
                boolean known = matcher.kind() == Matcher.Kind.ITEM
                        ? ForgeRegistries.ITEMS.containsKey(new ResourceLocation(matcher.id()))
                        : ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(matcher.id()));
                if (known) {
                    matchers.add(matcher);
                } else {
                    result.problems.add(Problem.warning(definition.id(), "match[" + i + "]", "no " + matcher.kind().name().toLowerCase()
                            + " named '" + matcher.id() + "' is registered; this matcher is inactive"));
                }
            }
            if (matchers.isEmpty()) {
                result.problems.add(Problem.error(definition.id(), "match", "no matcher names a registered item or block; rejected"));
                result.rejected++;
                continue;
            }
            kept.add(new FoodDefinition(definition.id(), matchers, definition.fullDurationSeconds(), definition.priority(),
                    definition.modifiers()));
        }
        return kept;
    }

    /**
     * Equal-priority definitions that can cover the same consumable: the colliding matchers are
     * removed from both (REQ-012), each pair is reported with both ids, and a definition left
     * without matchers is disabled. Returns the number of disabled definitions.
     */
    private static int removeConflicts(List<FoodDefinition> definitions, List<Problem> problems) {
        List<DefinitionSet.Conflict> conflicts = new DefinitionSet(0L, definitions).conflicts();
        if (conflicts.isEmpty()) {
            return 0;
        }
        Map<String, Set<Matcher>> removed = new HashMap<String, Set<Matcher>>();
        for (DefinitionSet.Conflict conflict : conflicts) {
            problems.add(Problem.error(conflict.first.id(), "match",
                    "conflicts with definition '" + conflict.second.id() + "' at the same priority (" + conflict.firstMatcher
                            + " overlaps " + conflict.secondMatcher + "); both matchers are disabled. Give one a higher priority."));
            mark(removed, conflict.first.id(), conflict.firstMatcher);
            mark(removed, conflict.second.id(), conflict.secondMatcher);
        }
        int disabled = 0;
        for (int i = 0; i < definitions.size(); i++) {
            FoodDefinition definition = definitions.get(i);
            Set<Matcher> gone = removed.get(definition.id());
            if (gone == null) {
                continue;
            }
            List<Matcher> kept = new ArrayList<Matcher>();
            for (Matcher matcher : definition.matchers()) {
                if (!gone.contains(matcher)) {
                    kept.add(matcher);
                }
            }
            if (kept.isEmpty()) {
                definitions.remove(i--);
                disabled++;
                problems.add(Problem.error(definition.id(), "match", "every matcher conflicted; the definition is disabled"));
            } else {
                definitions.set(i, new FoodDefinition(definition.id(), kept, definition.fullDurationSeconds(), definition.priority(),
                        definition.modifiers()));
            }
        }
        return disabled;
    }

    private static void mark(Map<String, Set<Matcher>> removed, String id, Matcher matcher) {
        Set<Matcher> set = removed.get(id);
        if (set == null) {
            set = new HashSet<Matcher>();
            removed.put(id, set);
        }
        set.add(matcher);
    }

    /** The deepest cause's message, where Gson puts the line and column of a syntax error. */
    static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static void ensureFiles() {
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                Diag.LOGGER.error("cannot create {}", file.getParentFile());
            }
            if (!file.exists()) {
                write(file, "[]\n");
                Diag.LOGGER.info("created an empty {}", file.getName());
            }
            if (!examplesFile.exists()) {
                write(examplesFile, ExamplesWriter.content());
                Diag.LOGGER.info("created {}", examplesFile.getName());
            }
        } catch (IOException e) {
            Diag.LOGGER.error("cannot create the definition files: {}", e.getMessage());
        }
    }

    private static void write(File target, String content) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8);
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

}
