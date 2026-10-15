package com.mahghuuuls.jafs.item;

import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Where Bitter Berries come from (REQ-053, REV-008): tall grass, fern, and the double tall grass
 * and fern drop one with a 1 in 16 chance when a player breaks them without shears. With shears
 * Forge's {@code ItemShears.onBlockStartBreak} shears the plant and ends the harvest before any
 * harvest-drops event, so shears drop the plant only; silk touch and non-player harvesting
 * (explosions, machines) drop nothing extra.
 *
 * <p>A double plant is two blocks. Breaking the lower half fires one harvest event for it, with
 * the variant readable. Breaking the upper half first destroys the lower half without a harvester
 * and then fires the upper half's event, when the lower block is already air; so the variant is
 * remembered at the break event, which fires first with both halves intact, and consumed there.
 * Either way one roll per plant.
 */
public final class BitterBerryDropHook {

    /** One drop per this many broken grass blocks on average; vanilla seeds are 1 in 8. */
    public static final int CHANCE_DENOMINATOR = 16;

    /** The upper-half double plants a player is breaking, remembered at the break event: player id to position. */
    private final Map<UUID, BlockPos> upperHalfBreaks = new HashMap<UUID, BlockPos>();

    /** Whether the broken block is one of the grasses that can hold berries. */
    public static boolean dropsBerries(IBlockAccess world, BlockPos pos, IBlockState state) {
        if (state.getBlock() == Blocks.TALLGRASS) {
            BlockTallGrass.EnumType type = state.getValue(BlockTallGrass.TYPE);
            return type == BlockTallGrass.EnumType.GRASS || type == BlockTallGrass.EnumType.FERN;
        }
        if (state.getBlock() == Blocks.DOUBLE_PLANT) {
            IBlockState lower = state.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.UPPER
                    ? world.getBlockState(pos.down()) : state;
            if (lower.getBlock() != Blocks.DOUBLE_PLANT) {
                return false;
            }
            BlockDoublePlant.EnumPlantType type = lower.getValue(BlockDoublePlant.VARIANT);
            return type == BlockDoublePlant.EnumPlantType.GRASS || type == BlockDoublePlant.EnumPlantType.FERN;
        }
        return false;
    }

    /** A player breaking with shears in the main hand never gets berries, whichever half of a plant (REQ-053). */
    static boolean usesShears(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() == Items.SHEARS;
    }

    /** The roll, separated so the chance is one number in one place. */
    public static boolean rolls(Random random) {
        return random.nextInt(CHANCE_DENOMINATOR) == 0;
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (event.getWorld().isRemote || player == null || player.capabilities.isCreativeMode || usesShears(player)
                || event.getState().getBlock() != Blocks.DOUBLE_PLANT
                || event.getState().getValue(BlockDoublePlant.HALF) != BlockDoublePlant.EnumBlockHalf.UPPER) {
            return;
        }
        if (dropsBerries(event.getWorld(), event.getPos(), event.getState())) {
            upperHalfBreaks.put(player.getUniqueID(), event.getPos());
        }
    }

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote || event.getHarvester() == null || event.isSilkTouching() || usesShears(event.getHarvester())) {
            return;
        }
        boolean grassy;
        if (event.getState().getBlock() == Blocks.DOUBLE_PLANT
                && event.getState().getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.UPPER) {
            BlockPos remembered = upperHalfBreaks.remove(event.getHarvester().getUniqueID());
            grassy = remembered != null && remembered.equals(event.getPos());
        } else {
            grassy = dropsBerries(event.getWorld(), event.getPos(), event.getState());
        }
        if (!grassy || !rolls(event.getWorld().rand)) {
            return;
        }
        event.getDrops().add(new ItemStack(JafsItems.BITTER_BERRIES));
        Diag.event("bitter berries dropped from {} at {} for {}", event.getState().getBlock().getRegistryName(), event.getPos(),
                event.getHarvester().getName());
    }
}
