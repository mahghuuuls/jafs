package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.config.GameplaySettings;
import com.mahghuuuls.jafs.core.Problem;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.JafsNetwork;
import com.mahghuuuls.jafs.property.PropertyRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owner of the reload sequence: re-read the gameplay settings and the definition file, then
 * tell every client about the definitions when the file loaded. The consequences for active
 * foods join this sequence when they exist.
 */
public final class ReloadService {

    private ReloadService() {
    }

    /** What one reload did, in the order the operator should read it. */
    public static final class Report {
        public final List<String> lines;

        Report(List<String> lines) {
            this.lines = Collections.unmodifiableList(lines);
        }
    }

    public static Report reloadAll() {
        GameplaySettings settings = GameplayConfig.reload();
        FoodDefinitionFile.LoadReport foods = FoodDefinitionFile.load(PropertyRegistry.get());
        List<String> lines = new ArrayList<String>();
        lines.add("JAFS reload: jafs.cfg revision " + settings.revision() + ", " + settings.problems().size() + " problem(s); "
                + foods.summary());
        for (String problem : settings.problems()) {
            lines.add("JAFS reload jafs.cfg: " + problem);
        }
        for (Problem problem : foods.problems) {
            lines.add("JAFS reload foods.json: " + problem);
        }
        if (!foods.fileLevelFailure) {
            JafsNetwork.sendDefinitionsToAll(FoodDefinitionFile.current(), PropertyRegistry.get().all());
            Diag.event("reload: sent definitions={} revision={} to all players", FoodDefinitionFile.current().size(),
                    FoodDefinitionFile.current().revision());
        }
        return new Report(lines);
    }
}
