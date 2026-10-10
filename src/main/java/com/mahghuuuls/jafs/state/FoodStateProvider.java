package com.mahghuuuls.jafs.state;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FoodStateProvider implements ICapabilitySerializable<NBTBase> {

    private final FoodState instance = new FoodState();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == FoodStateCapability.CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == FoodStateCapability.CAPABILITY ? FoodStateCapability.CAPABILITY.<T>cast(instance) : null;
    }

    @Override
    public NBTBase serializeNBT() {
        return FoodStateStorage.write(instance);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        if (nbt instanceof net.minecraft.nbt.NBTTagCompound) {
            FoodStateStorage.read(instance, (net.minecraft.nbt.NBTTagCompound) nbt);
        }
    }
}
