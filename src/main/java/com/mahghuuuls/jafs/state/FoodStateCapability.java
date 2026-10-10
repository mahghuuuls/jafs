package com.mahghuuuls.jafs.state;

import com.mahghuuuls.jafs.Tags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/** Registration, attachment, and lookup of the per-player state. */
public final class FoodStateCapability {

    @CapabilityInject(IFoodState.class)
    public static final Capability<IFoodState> CAPABILITY = null;

    private static final ResourceLocation KEY = new ResourceLocation(Tags.MOD_ID, "food_state");

    private FoodStateCapability() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(IFoodState.class, new FoodStateStorage(), new Callable<IFoodState>() {
            @Override
            public IFoodState call() {
                return new FoodState();
            }
        });
    }

    /** The player's state, or null for an entity that has none (never null for a real player). */
    @Nullable
    public static IFoodState get(EntityPlayer player) {
        return player == null ? null : player.getCapability(CAPABILITY, null);
    }

    public static final class Attacher {
        @SubscribeEvent
        public void onAttach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof EntityPlayer) {
                event.addCapability(KEY, new FoodStateProvider());
            }
        }
    }
}
