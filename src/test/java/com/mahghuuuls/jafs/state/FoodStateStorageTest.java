package com.mahghuuuls.jafs.state;

import com.mahghuuuls.jafs.core.FoodSlot;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The NBT shape round-trips: foods in slot order with their times, plus the recovery delay. */
class FoodStateStorageTest {

    @Test
    void emptyStateRoundTrips() {
        FoodState state = new FoodState();
        state.setRecoveryDelayTicks(140);
        NBTTagCompound tag = FoodStateStorage.write(state);
        FoodState restored = new FoodState();
        FoodStateStorage.read(restored, tag);
        assertTrue(restored.slots().isEmpty());
        assertEquals(140, restored.recoveryDelayTicks());
    }

    @Test
    void foodsRoundTripInOrder() {
        FoodState state = new FoodState();
        state.slots().add(new FoodSlot("jafs_test:steak", 1200, 24000));
        state.slots().add(new FoodSlot("jafs_test:bread", 0, 600));
        NBTTagCompound tag = FoodStateStorage.write(state);
        FoodState restored = new FoodState();
        FoodStateStorage.read(restored, tag);
        assertEquals(2, restored.slots().size());
        assertEquals("jafs_test:steak", restored.slots().get(0).definitionId());
        assertEquals(1200, restored.slots().get(0).remainingTicks());
        assertEquals(24000, restored.slots().get(0).fullTicks());
        assertEquals("jafs_test:bread", restored.slots().get(1).definitionId());
        assertTrue(restored.slots().get(1).isExpired());
    }

    @Test
    void corruptEntriesAreSkippedAndRemainingIsClamped() {
        NBTTagCompound tag = new NBTTagCompound();
        net.minecraft.nbt.NBTTagList foods = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound bad = new NBTTagCompound();
        bad.setString("Id", "");
        bad.setInteger("Full", 100);
        foods.appendTag(bad);
        NBTTagCompound over = new NBTTagCompound();
        over.setString("Id", "x:y");
        over.setInteger("Remaining", 5000);
        over.setInteger("Full", 100);
        foods.appendTag(over);
        tag.setTag("Foods", foods);
        FoodState restored = new FoodState();
        FoodStateStorage.read(restored, tag);
        assertEquals(1, restored.slots().size());
        assertEquals(100, restored.slots().get(0).remainingTicks());
    }

    @Test
    void copyFromDuplicatesSlotsAndDelay() {
        FoodState a = new FoodState();
        a.slots().add(new FoodSlot("x:y", 10, 20));
        a.setRecoveryDelayTicks(3);
        FoodState b = new FoodState();
        b.copyFrom(a);
        a.slots().get(0).setRemainingTicks(1);
        assertEquals(10, b.slots().get(0).remainingTicks());
        assertEquals(3, b.recoveryDelayTicks());
    }
}
