package com.mahghuuuls.jafs.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the player's active foods, the rate their timers run at, the definitions
 * revision, and the player's current health. The health lets the client lower its own health
 * before vanilla's health packet arrives, so a cap after a maximum-health decrease is not
 * mistaken for a hit (vanilla rolls the camera for any decrease it receives).
 */
public final class FoodStateSnapshotMessage implements IMessage {

    public static final class Entry {
        public final String definitionId;
        public final int remainingTicks;
        public final int fullTicks;
        public final boolean eligible;

        public Entry(String definitionId, int remainingTicks, int fullTicks, boolean eligible) {
            this.definitionId = definitionId;
            this.remainingTicks = remainingTicks;
            this.fullTicks = fullTicks;
            this.eligible = eligible;
        }
    }

    private List<Entry> entries = new ArrayList<Entry>();
    private double effectiveRate = 1.0;
    private long definitionsRevision;
    private float health;

    public FoodStateSnapshotMessage() {
    }

    public FoodStateSnapshotMessage(List<Entry> entries, double effectiveRate, long definitionsRevision, float health) {
        this.entries = entries;
        this.effectiveRate = effectiveRate;
        this.definitionsRevision = definitionsRevision;
        this.health = health;
    }

    public List<Entry> entries() {
        return entries;
    }

    public double effectiveRate() {
        return effectiveRate;
    }

    public long definitionsRevision() {
        return definitionsRevision;
    }

    /** The server's current health of the player when the snapshot was taken. */
    public float health() {
        return health;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        definitionsRevision = buf.readLong();
        effectiveRate = buf.readDouble();
        health = buf.readFloat();
        int count = buf.readByte();
        entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(ByteBufUtils.readUTF8String(buf), buf.readInt(), buf.readInt(), buf.readBoolean()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(definitionsRevision);
        buf.writeDouble(effectiveRate);
        buf.writeFloat(health);
        buf.writeByte(entries.size());
        for (Entry entry : entries) {
            ByteBufUtils.writeUTF8String(buf, entry.definitionId);
            buf.writeInt(entry.remainingTicks);
            buf.writeInt(entry.fullTicks);
            buf.writeBoolean(entry.eligible);
        }
    }
}
