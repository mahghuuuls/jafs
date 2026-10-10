package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.FoodTimers;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.FoodStateSnapshotMessage;
import com.mahghuuuls.jafs.network.JafsNetwork;
import net.minecraft.client.Minecraft;
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

    public static List<FoodStateSnapshotMessage.Entry> entries() {
        return entries;
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
                Diag.event("client snapshot foods={} rate={}", entries.size(), rate);
            }
        });
    }

    public static void clear() {
        entries = Collections.emptyList();
        rate = 1.0;
    }
}
