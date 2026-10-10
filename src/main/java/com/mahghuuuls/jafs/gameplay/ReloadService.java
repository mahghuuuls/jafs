package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.core.Problem;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.JafsNetwork;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owner of the reload sequence (REQ-070, REQ-071): read and validate both files; a file-level
 * failure of either keeps both previous snapshots; otherwise swap both, apply the consequences
 * to every online player's active foods through the food service, and send the definitions to
 * every client. Nothing is ever half-swapped, and no configuration content can throw past here
 * (REQ-098).
 */
public final class ReloadService {

    private ReloadService() {
    }

    /** What one reload did, in the order the operator should read it. */
    public static final class Report {
        public final List<String> lines;
        public final boolean applied;

        Report(List<String> lines, boolean applied) {
            this.lines = Collections.unmodifiableList(lines);
            this.applied = applied;
        }
    }

    public static Report reloadAll() {
        GameplayConfig.Candidate settings;
        FoodDefinitionFile.Candidate foods;
        try {
            settings = GameplayConfig.read();
            foods = FoodDefinitionFile.read(PropertyRegistry.get());
        } catch (RuntimeException e) {
            Diag.LOGGER.error("reload failed while reading; the previous settings and definitions stay active", e);
            return new Report(Collections.singletonList("JAFS reload failed while reading (" + e
                    + "); the previous settings and definitions stay active"), false);
        }
        try {
            return reload(settings, foods);
        } catch (RuntimeException e) {
            Diag.LOGGER.error("reload failed while applying; the new settings and definitions may be partly applied", e);
            return new Report(Collections.singletonList("JAFS reload failed while applying (" + e
                    + "); run it again after fixing the cause"), false);
        }
    }

    private static Report reload(GameplayConfig.Candidate settings, FoodDefinitionFile.Candidate foods) {
        List<String> lines = new ArrayList<String>();
        boolean applied = !settings.fileLevelFailure() && !foods.report.fileLevelFailure;
        String summary = "JAFS reload: " + settings.summary() + "; " + foods.report.summary();
        if (!applied) {
            summary = "JAFS reload not applied: " + settings.summary() + "; " + foods.report.summary()
                    + "; the previous settings and definitions stay active";
        }
        lines.add(summary);
        for (String problem : settings.malformed) {
            lines.add("JAFS reload jafs.cfg: " + problem);
        }
        for (String problem : settings.settings.problems()) {
            lines.add("JAFS reload jafs.cfg: " + problem);
        }
        for (Problem problem : foods.report.problems) {
            lines.add("JAFS reload foods.json: " + problem);
        }
        if (!applied) {
            Diag.LOGGER.warn("reload not applied: settings {}, definitions {}", settings.fileLevelFailure() ? "unreadable" : "ok",
                    foods.report.fileLevelFailure ? "unreadable" : "ok");
            return new Report(lines, false);
        }
        GameplayConfig.apply(settings);
        FoodDefinitionFile.apply(foods);
        int players = 0;
        int changes = 0;
        if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
            ActiveFoodService service = JafsGameplay.get().service();
            for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
                players++;
                changes += service.applyReloadChanges(player, FoodDefinitionFile.current());
            }
        }
        JafsNetwork.sendDefinitionsToAll(FoodDefinitionFile.current(), PropertyRegistry.get().all());
        Diag.event("reload: applied; {} active food change(s) over {} player(s); sent definitions={} revision={} to all players",
                changes, players, FoodDefinitionFile.current().size(), FoodDefinitionFile.current().revision());
        return new Report(lines, true);
    }
}
