package com.mahghuuuls.jafs.state;

import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

/**
 * NBT shape of the saved state (the persistence contract of the architecture):
 * {@code Foods: [{Id, Remaining, Full}...]}, {@code RecoveryDelay}. Ticks throughout.
 */
public final class FoodStateStorage implements Capability.IStorage<IFoodState> {

    static final String FOODS = "Foods";
    static final String ID = "Id";
    static final String REMAINING = "Remaining";
    static final String FULL = "Full";
    static final String RECOVERY_DELAY = "RecoveryDelay";

    @Nullable
    @Override
    public NBTBase writeNBT(Capability<IFoodState> capability, IFoodState instance, EnumFacing side) {
        return write(instance);
    }

    @Override
    public void readNBT(Capability<IFoodState> capability, IFoodState instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            read(instance, (NBTTagCompound) nbt);
        }
    }

    public static NBTTagCompound write(IFoodState state) {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList foods = new NBTTagList();
        for (FoodSlot slot : state.slots()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString(ID, slot.definitionId());
            entry.setInteger(REMAINING, slot.remainingTicks());
            entry.setInteger(FULL, slot.fullTicks());
            foods.appendTag(entry);
        }
        tag.setTag(FOODS, foods);
        tag.setInteger(RECOVERY_DELAY, state.recoveryDelayTicks());
        return tag;
    }

    public static void read(IFoodState state, NBTTagCompound tag) {
        state.slots().clear();
        NBTTagList foods = tag.getTagList(FOODS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < foods.tagCount(); i++) {
            NBTTagCompound entry = foods.getCompoundTagAt(i);
            String id = entry.getString(ID);
            int full = entry.getInteger(FULL);
            if (id.isEmpty() || full <= 0) {
                Diag.LOGGER.warn("Dropped a corrupt saved food entry (id='{}', full={}); the rest of the state loads", id, full);
                continue;
            }
            state.slots().add(new FoodSlot(id, entry.getInteger(REMAINING), full));
        }
        state.setRecoveryDelayTicks(tag.getInteger(RECOVERY_DELAY));
        state.bumpRevision();
    }
}
