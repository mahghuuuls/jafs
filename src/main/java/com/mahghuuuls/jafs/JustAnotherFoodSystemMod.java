package com.mahghuuuls.jafs;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of Just Another Food System. Lifecycle wiring only; behavior lives in the
 * packages described by the architecture.
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:forge@[14.23.5.2847,);after:inhibited")
public class JustAnotherFoodSystemMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
    }
}
