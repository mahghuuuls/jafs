package com.mahghuuuls.jafs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The package rules a green build would otherwise not notice: another mod's classes are named only
 * by the compat package written for it, so a game without that mod never loads them, and the rules
 * core stays free of Minecraft and Forge so its tests run without a game. Inhibited is never named
 * at all; its effect is looked up by registry name. A class counts as named when an import or any
 * code line outside a comment mentions its package, so a fully qualified name written inline is
 * caught as well.
 */
class ImportRulesTest {

    private static final Path MAIN = Paths.get("src", "main", "java", "com", "mahghuuuls", "jafs");

    private static List<Path> sources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static String relative(Path file) {
        return MAIN.relativize(file).toString().replace('\\', '/');
    }

    private static List<String> imports(Path file) throws IOException {
        List<String> result = new ArrayList<String>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                result.add(trimmed.substring("import ".length()).replace("static ", "").replace(";", "").trim());
            }
        }
        return result;
    }

    /** The lines of a file that are code: no blank lines and no line of a line or block comment. */
    private static List<String> codeLines(Path file) throws IOException {
        List<String> result = new ArrayList<String>();
        boolean inBlockComment = false;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }
            if (trimmed.startsWith("/*")) {
                inBlockComment = !trimmed.contains("*/");
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                continue;
            }
            int comment = trimmed.indexOf("//");
            result.add(comment >= 0 ? trimmed.substring(0, comment) : trimmed);
        }
        return result;
    }

    private static List<String> violations(String forbiddenPackage, String allowedFolder) throws IOException {
        List<String> found = new ArrayList<String>();
        for (Path file : sources()) {
            String name = relative(file);
            if (allowedFolder != null && name.startsWith(allowedFolder)) {
                continue;
            }
            for (String line : codeLines(file)) {
                if (line.contains(forbiddenPackage + ".")) {
                    found.add(name + ": " + line);
                }
            }
        }
        return found;
    }

    @Test
    void theSourceTreeIsFound() throws IOException {
        assertTrue(Files.isDirectory(MAIN), "run from the mod repository: " + MAIN.toAbsolutePath());
        assertFalse(sources().isEmpty());
    }

    @Test
    void onlyTheSetworksCompatImportsSetworks() throws IOException {
        List<String> found = violations("com.mahghuuuls.setworks", "compat/setworks/");
        assertTrue(found.isEmpty(), String.join("; ", found));
    }

    @Test
    void onlyTheStatusworksCompatImportsStatusworks() throws IOException {
        List<String> found = violations("com.mahghuuuls.statusworks", "compat/statusworks/");
        assertTrue(found.isEmpty(), String.join("; ", found));
    }

    @Test
    void nothingImportsInhibited() throws IOException {
        List<String> found = violations("mod.acgaming", null);
        assertTrue(found.isEmpty(), String.join("; ", found));
    }

    @Test
    void theCoreImportsNoMinecraftOrForge() throws IOException {
        List<String> found = new ArrayList<String>();
        for (Path file : sources()) {
            String name = relative(file);
            if (!name.startsWith("core/")) {
                continue;
            }
            for (String imported : imports(file)) {
                if (imported.startsWith("net.minecraft") || imported.startsWith("net.minecraftforge")) {
                    found.add(name + " imports " + imported);
                }
            }
        }
        assertTrue(found.isEmpty(), String.join("; ", found));
    }
}
