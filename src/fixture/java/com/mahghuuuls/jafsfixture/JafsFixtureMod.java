package com.mahghuuuls.jafsfixture;

import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.api.IRateModifierProvider;
import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Development-only fixture addon: it compiles against the JAFS {@code api} package only and
 * proves the public contract (REQ-057, REQ-059, REQ-060). Never packaged with the mod.
 */
@Mod(modid = JafsFixtureMod.MOD_ID, name = "JAFS Fixture", version = "0.0.0", dependencies = "required-after:jafs")
public final class JafsFixtureMod {

    public static final String MOD_ID = "jafsfixture";
    public static final String STAMINA = "jafsfixture:stamina";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /** The contribution the rate command sets; applies to every player. */
    static volatile double depletionPercent;
    static volatile double recoveryPercent;
    /** When set, the fixture cancels the JAFS HUD event so the built-in HUD does not draw (IMP-012 card). */
    static volatile boolean cancelHud;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JafsApi.registerProperty(PropertySpec.builder(STAMINA)
                .displayNameKey("property.jafsfixture.stamina").unit("").color("aqua")
                .operation(PropertyOperation.FLAT, -100, 100)
                .operation(PropertyOperation.PERCENT, -100, 100)
                .build(), new IPropertyApplier() {
            @Override
            public void apply(EntityPlayerMP player, double flatSum, double percentSum) {
                LOGGER.info("fixture stamina applied for {}: flat={} percent={}", player.getName(), flatSum, percentSum);
            }
        });
        JafsApi.registerRateModifierProvider(MOD_ID, new IRateModifierProvider() {
            @Override
            public double recoveryRatePercent(EntityPlayerMP player) {
                return recoveryPercent;
            }

            @Override
            public double depletionRatePercent(EntityPlayerMP player) {
                return depletionPercent;
            }
        });
        LOGGER.info("JAFS fixture registered {} and its rate provider (api {})", STAMINA, JafsApi.API_VERSION);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            // The class is loaded only here, so a dedicated server never sees the client event type.
            MinecraftForge.EVENT_BUS.register(new FixtureHudCanceller());
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new FixtureCommand());
    }
}
