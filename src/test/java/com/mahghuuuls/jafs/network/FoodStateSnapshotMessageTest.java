package com.mahghuuuls.jafs.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodStateSnapshotMessageTest {

    @Test
    void roundTrips() {
        FoodStateSnapshotMessage original = new FoodStateSnapshotMessage(Arrays.asList(
                new FoodStateSnapshotMessage.Entry("steak", 1200, 24000, false),
                new FoodStateSnapshotMessage.Entry("bread", 10, 36000, true)), 1.5, 7, 15.5f);
        ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);
        FoodStateSnapshotMessage copy = new FoodStateSnapshotMessage();
        copy.fromBytes(buf);
        assertEquals(0, buf.readableBytes());
        assertEquals(7, copy.definitionsRevision());
        assertEquals(1.5, copy.effectiveRate(), 1e-9);
        assertEquals(15.5f, copy.health(), 1e-6f);
        assertEquals(2, copy.entries().size());
        assertEquals("steak", copy.entries().get(0).definitionId);
        assertEquals(24000, copy.entries().get(0).fullTicks);
        assertFalse(copy.entries().get(0).eligible);
        assertTrue(copy.entries().get(1).eligible);
        assertEquals(10, copy.entries().get(1).remainingTicks);
    }
}
