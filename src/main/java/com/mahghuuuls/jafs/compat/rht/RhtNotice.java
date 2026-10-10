package com.mahghuuuls.jafs.compat.rht;

import com.mahghuuuls.jafs.gameplay.CoexistenceNotice;

/**
 * Recovery and Hunger Tweaks also heals players on its own schedule. JAFS keeps running; it logs
 * the overlap once at startup and tells operators on login (REQ-074). No class of that mod is
 * referenced: the integrations detect it by mod id only.
 */
public final class RhtNotice implements CoexistenceNotice {

    public static final String MOD_ID = "recoveryhungertweaks";
    public static final String LINE = "JAFS: Recovery and Hunger Tweaks is installed; both mods heal players. Remove one of them.";

    @Override
    public String loginLine() {
        return LINE;
    }
}
