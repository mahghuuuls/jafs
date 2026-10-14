package com.mahghuuuls.jafs;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.DefinitionSet;
import net.minecraft.world.World;

/** Side-specific registration hook and the side's view of the definitions. */
public class CommonProxy {

    public void preInit() {
    }

    /** The definition set a world should consult: the server's working set here; the client overrides for remote worlds. */
    public DefinitionSet definitionsFor(World world) {
        return FoodDefinitionFile.current();
    }
}
