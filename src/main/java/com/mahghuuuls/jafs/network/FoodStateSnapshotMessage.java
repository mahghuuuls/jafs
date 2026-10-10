package com.mahghuuuls.jafs.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

/** Server to client: the player's active foods, the rate their timers run at, and the definitions revision. */
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

    public FoodStateSnapshotMessage() {
    }

    public FoodStateSnapshotMessage(List<Entry> entries, double effectiveRate, long definitionsRevision) {
        this.entries = entries;
        this.effectiveRate = effectiveRate;
        this.definitionsRevision = definitionsRevision;
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

    @Override
    public void fromBytes(ByteBuf buf) {
        definitionsRevision = buf.readLong();
        effectiveRate = buf.readDouble();
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
        buf.writeByte(entries.size());
        for (Entry entry : entries) {
            ByteBufUtils.writeUTF8String(buf, entry.definitionId);
            buf.writeInt(entry.remainingTicks);
            buf.writeInt(entry.fullTicks);
            buf.writeBoolean(entry.eligible);
        }
    }
}
