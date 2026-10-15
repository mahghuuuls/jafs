package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.FoodTimers;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.FoodStateSnapshotMessage;
import com.mahghuuuls.jafs.network.JafsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;

/**
 * The client's view of its own active foods: the last snapshot plus a local countdown at the
 * snapshot's rate, so the HUD moves between server updates.
 */
@SideOnly(Side.CLIENT)
public final class ClientFoodState implements JafsNetwork.SnapshotSink {

    private static volatile List<FoodStateSnapshotMessage.Entry> entries = Collections.emptyList();
    private static volatile double rate = 1.0;
    private static volatile long receivedAtMillis;

    /** The largest value {@link #realTicksLeft} returns; leaves room for the rounding in the display format. */
    static final int MAX_DISPLAY_TICKS = Integer.MAX_VALUE - FoodDefinition.TICKS_PER_SECOND;

    public static List<FoodStateSnapshotMessage.Entry> entries() {
        return entries;
    }

    /** The rate the timers run at, from the last snapshot (0 while paused). */
    public static double rate() {
        return rate;
    }

    /**
     * The real seconds until the entry expires at the current rate, in ticks: the remaining fuel
     * divided by the rate (REV-009); -1 while the rate is 0 (paused). Capped so that
     * {@code DisplayFormat.durationTicks}, which adds a second minus one tick, cannot overflow.
     */
    public static int realTicksLeft(FoodStateSnapshotMessage.Entry entry) {
        double r = rate;
        if (r <= 0) {
            return -1;
        }
        long ticks = Math.round(remainingNow(entry) / r);
        return ticks > MAX_DISPLAY_TICKS ? MAX_DISPLAY_TICKS : (int) ticks;
    }

    /** Remaining ticks of an entry now, counted down locally since the snapshot arrived. */
    public static int remainingNow(FoodStateSnapshotMessage.Entry entry) {
        double elapsedTicks = (System.currentTimeMillis() - receivedAtMillis) / 50.0;
        return FoodTimers.remainingAfter(entry.remainingTicks, elapsedTicks, rate);
    }

    @Override
    public void accept(final FoodStateSnapshotMessage message) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                entries = Collections.unmodifiableList(message.entries());
                rate = message.effectiveRate();
                receivedAtMillis = System.currentTimeMillis();
                preApplyLowerHealth(message.health());
                Diag.event("client snapshot foods={} rate={}", entries.size(), rate);
            }
        });
    }

    /**
     * A snapshot arrives before vanilla's health packet for the same tick. When the server's
     * health is lower than ours (a cap after a maximum-health decrease), setting it now makes
     * vanilla see no decrease when its packet arrives, so it does not roll the camera as for a
     * hit. Real damage keeps its hit animation: the server sends a separate hurt status for it.
     */
    private static void preApplyLowerHealth(float serverHealth) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && serverHealth >= 0 && serverHealth < player.getHealth()) {
            player.setHealth(serverHealth);
        }
    }

    /** The addon-facing view of the local snapshot (api.client), counted down to now. */
    public static final class ApiSource implements com.mahghuuuls.jafs.api.client.ClientFoodApi.Source {
        @Override
        public java.util.List<com.mahghuuuls.jafs.api.ActiveFoodView> snapshot() {
            java.util.List<com.mahghuuuls.jafs.api.ActiveFoodView> views = new java.util.ArrayList<com.mahghuuuls.jafs.api.ActiveFoodView>();
            for (FoodStateSnapshotMessage.Entry entry : entries()) {
                java.util.List<com.mahghuuuls.jafs.api.ModifierView> modifiers = new java.util.ArrayList<com.mahghuuuls.jafs.api.ModifierView>();
                com.mahghuuuls.jafs.core.FoodDefinition definition = ClientDefinitions.definitions().get(entry.definitionId);
                if (definition != null) {
                    for (com.mahghuuuls.jafs.core.Modifier modifier : definition.modifiers()) {
                        modifiers.add(new com.mahghuuuls.jafs.api.ModifierView(modifier.propertyId(), modifier.operation(), modifier.amount()));
                    }
                }
                views.add(new com.mahghuuuls.jafs.api.ActiveFoodView(entry.definitionId, remainingNow(entry), entry.fullTicks, entry.eligible, modifiers));
            }
            return views;
        }
    }

    public static void clear() {
        entries = Collections.emptyList();
        rate = 1.0;
    }
}
