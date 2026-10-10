package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

/**
 * What an operator is told on login (REQ-072, REQ-097, REQ-074): how many definition and
 * configuration problems the last load left, until the next successful reload fixes them, and
 * the overlap notice when another healing mod is installed. Non-operators see nothing.
 */
public final class OperatorNotices {

    private CoexistenceNotice coexistence = CoexistenceNotice.NONE;

    public void setCoexistenceNotice(CoexistenceNotice notice) {
        this.coexistence = notice == null ? CoexistenceNotice.NONE : notice;
    }

    public CoexistenceNotice coexistenceNotice() {
        return coexistence;
    }

    /**
     * The lines for the counts and the overlap line; empty when there is nothing to say. Failed
     * definitions are the rejected and disabled ones (REQ-072); the problem count also carries
     * the warnings (REQ-097).
     */
    public static List<String> lines(int failedDefinitions, int definitionProblems, boolean definitionsFileUnreadable,
            int configProblems, String overlapLine) {
        List<String> lines = new ArrayList<String>();
        if (definitionsFileUnreadable) {
            lines.add("JAFS: foods.json could not be read; no definitions are loaded. See the log, fix the file, then run /jafs reload.");
        } else if (failedDefinitions > 0 || definitionProblems > 0) {
            lines.add("JAFS: " + failedDefinitions + " definition(s) failed to load, " + definitionProblems
                    + " problem(s) in foods.json. See the log, fix the file, then run /jafs reload.");
        }
        if (configProblems > 0) {
            lines.add("JAFS: " + configProblems + " value(s) in jafs.cfg fell back to defaults. See the log, fix the file, then run /jafs reload.");
        }
        if (overlapLine != null) {
            lines.add(overlapLine);
        }
        return lines;
    }

    /**
     * Sends the login lines to an operator, or to the owner of a single-player world (who can fix
     * the files even with cheats off); nothing to anyone else.
     */
    public void onLogin(EntityPlayerMP player) {
        if (player.getServer() == null || !isOperator(player)) {
            return;
        }
        FoodDefinitionFile.LoadReport report = FoodDefinitionFile.lastReport();
        List<String> lines = lines(report.rejected + report.disabled, report.problems.size(), report.fileLevelFailure,
                GameplayConfig.current().problems().size(), coexistence.loginLine());
        for (String line : lines) {
            player.sendMessage(new TextComponentString(line));
        }
        if (!lines.isEmpty()) {
            Diag.event("operator notices for {}: {} line(s)", player.getName(), lines.size());
        }
    }

    private static boolean isOperator(EntityPlayerMP player) {
        if (player.getServer().isSinglePlayer() && player.getName().equalsIgnoreCase(player.getServer().getServerOwner())) {
            return true;
        }
        return player.getServer().getPlayerList().canSendCommands(player.getGameProfile());
    }
}
