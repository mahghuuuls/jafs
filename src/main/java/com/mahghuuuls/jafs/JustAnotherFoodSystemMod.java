package com.mahghuuuls.jafs;

import com.mahghuuuls.jafs.command.JafsCommand;
import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.gameplay.JafsGameplay;
import com.mahghuuuls.jafs.gameplay.LifecycleHandlers;
import com.mahghuuuls.jafs.gameplay.hooks.CakeHook;
import com.mahghuuuls.jafs.gameplay.hooks.DamageHook;
import com.mahghuuuls.jafs.gameplay.hooks.ItemUseHook;
import com.mahghuuuls.jafs.gameplay.hooks.TickHook;
import com.mahghuuuls.jafs.item.BitterBerryDropHook;
import com.mahghuuuls.jafs.item.JafsItems;
import com.mahghuuuls.jafs.network.JafsNetwork;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Entry point of Just Another Food System. Lifecycle wiring only; behavior lives in the
 * packages described by the architecture.
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:forge@[14.23.5.2847,);after:inhibited")
public class JustAnotherFoodSystemMod {

    @SidedProxy(clientSide = "com.mahghuuuls.jafs.client.ClientProxy", serverSide = "com.mahghuuuls.jafs.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new JafsItems());
        MinecraftForge.EVENT_BUS.register(new BitterBerryDropHook());
        GameplayConfig.load(event.getModConfigurationDirectory());
        FoodDefinitionFile.setLocation(event.getModConfigurationDirectory());
        PropertyRegistry.registerCoreProperties();
        JafsNetwork.register();
        FoodStateCapability.register();
        MinecraftForge.EVENT_BUS.register(new FoodStateCapability.Attacher());
        JafsGameplay gameplay = JafsGameplay.create();
        MinecraftForge.EVENT_BUS.register(new TickHook(gameplay.ticker()));
        MinecraftForge.EVENT_BUS.register(new DamageHook(gameplay.recovery()));
        MinecraftForge.EVENT_BUS.register(new ItemUseHook(gameplay.gate()));
        MinecraftForge.EVENT_BUS.register(new CakeHook(gameplay.gate(), proxy));
        MinecraftForge.EVENT_BUS.register(new LifecycleHandlers(gameplay));
        proxy.preInit();
        Diag.LOGGER.info("{} {} loaded", Tags.MOD_NAME, Tags.VERSION);
    }

    /** Every mod is loaded: detect the optional ones by id and wire their notices and gates. */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Integrations.wire(JafsGameplay.get());
    }

    /** Addons have registered their properties by now, and every item and block id is known. */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        PropertyRegistry.get().freeze();
        FoodDefinitionFile.load(PropertyRegistry.get());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new JafsCommand());
    }
}
