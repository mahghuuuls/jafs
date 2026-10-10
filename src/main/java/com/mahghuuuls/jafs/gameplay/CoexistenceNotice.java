package com.mahghuuuls.jafs.gameplay;

/** A notice about another mod that overlaps JAFS (Recovery and Hunger Tweaks), wired by the integrations. */
public interface CoexistenceNotice {

    CoexistenceNotice NONE = new CoexistenceNotice() {
        @Override
        public String loginLine() {
            return null;
        }
    };

    /** The line an operator sees on login, or null when nothing overlaps. */
    String loginLine();
}
