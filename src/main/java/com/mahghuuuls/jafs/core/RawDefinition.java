package com.mahghuuuls.jafs.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A food definition exactly as the file said it, before validation. Null means "absent"; the
 * validator turns absences and bad values into problems with field names.
 */
public final class RawDefinition {

    public String id;
    public List<RawMatcher> match = new ArrayList<RawMatcher>();
    public Integer fullDuration;
    public Integer priority;
    public List<RawModifier> modifiers = new ArrayList<RawModifier>();
    /** Where the entry sits in the file, for messages when the id is missing. */
    public int index;
    /** Keys the reader did not recognise (a probable typo); reported as warnings. */
    public List<String> unknownKeys = new ArrayList<String>();

    public static final class RawMatcher {
        public String item;
        public String block;
        public Integer meta;
        public Map<String, String> nbt = new LinkedHashMap<String, String>();
    }

    public static final class RawModifier {
        public String property;
        public String operation;
        public Double amount;
    }
}
