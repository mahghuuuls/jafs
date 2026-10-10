package com.mahghuuuls.jafs.gameplay;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The operator login lines (REQ-072, REQ-097, REQ-074): counts, the unreadable case, the overlap, and silence. */
class OperatorNoticesTest {

    @Test
    void nothingToSayGivesNoLines() {
        assertTrue(OperatorNotices.lines(0, 0, false, 0, null).isEmpty());
    }

    @Test
    void definitionProblemsAreCounted() {
        List<String> lines = OperatorNotices.lines(2, 3, false, 0, null);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("JAFS: 2 definition(s) failed to load, 3 problem(s) in foods.json"), lines.get(0));
        // warnings alone still produce the line (REQ-097)
        assertEquals(1, OperatorNotices.lines(0, 1, false, 0, null).size());
    }

    @Test
    void anUnreadableFileReplacesTheCount() {
        List<String> lines = OperatorNotices.lines(0, 0, true, 0, null);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("could not be read"), lines.get(0));
    }

    @Test
    void configFallbacksAndTheOverlapLineAreSeparateLines() {
        List<String> lines = OperatorNotices.lines(1, 1, false, 2, "overlap");
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).contains("2 value(s) in jafs.cfg"), lines.get(1));
        assertEquals("overlap", lines.get(2));
    }
}
