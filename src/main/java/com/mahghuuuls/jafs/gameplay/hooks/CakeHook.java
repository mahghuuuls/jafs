package com.mahghuuuls.jafs.gameplay.hooks;

import com.mahghuuuls.jafs.CommonProxy;
import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.gameplay.MealGate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The second interception path (REQ-023): a right-click on a block that a block matcher names
 * (cake) is a meal. The gate decides on the server exactly as for an item.
 *
 * <p>Client: for a configured block the event is cancelled with a success result, which keeps
 * vanilla from biting the cake locally and from falling through to the held item, while the
 * use-on-block packet still goes to the server; the server's answer arrives as the block update,
 * so a refusal shows nothing and an acceptance shows the slice vanish once.
 *
 * <p>Server: an accepted click lets vanilla eat the slice. Forge has no hook after the block's
 * own handling, and a task posted from the server thread runs at once, so the check waits for
 * the end of the same server tick: only when the block changed does the food activate (REQ-022);
 * a slice vanilla refused activates nothing.
 */
public final class CakeHook {

    private static final class PendingSlice {
        final EntityPlayerMP player;
        final BlockPos pos;
        final IBlockState before;
        final ConsumableKey key;

        PendingSlice(EntityPlayerMP player, BlockPos pos, IBlockState before, ConsumableKey key) {
            this.player = player;
            this.pos = pos;
            this.before = before;
            this.key = key;
        }
    }

    private final MealGate gate;
    private final CommonProxy proxy;
    private final List<PendingSlice> pending = new ArrayList<PendingSlice>();

    public CakeHook(MealGate gate, CommonProxy proxy) {
        this.gate = gate;
        this.proxy = proxy;
    }

    /** The consumable key of a block state: its registry id and metadata; block matchers carry no NBT. */
    public static ConsumableKey keyOf(IBlockState state) {
        Block block = state.getBlock();
        ResourceLocation name = block.getRegistryName();
        if (name == null) {
            return null;
        }
        return new ConsumableKey(Matcher.Kind.BLOCK, name.toString(), block.getMetaFromState(state),
                Collections.<String, String>emptyMap());
    }

    /** Whether the click ate a slice: the state at the position is no longer the one clicked. */
    public static boolean sliceEaten(IBlockState before, IBlockState after) {
        return before != after && !before.equals(after);
    }

    /** Vanilla activates a block only when the player is not sneaking or both held items let a sneak bypass the use. */
    static boolean vanillaWouldActivate(EntityPlayer player, World world, BlockPos pos) {
        return !player.isSneaking() || (player.getHeldItemMainhand().doesSneakBypassUse(world, pos, player)
                && player.getHeldItemOffhand().doesSneakBypassUse(world, pos, player));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        ConsumableKey key = keyOf(state);
        if (key == null || !vanillaWouldActivate(event.getEntityPlayer(), world, event.getPos())) {
            return;
        }
        DefinitionSet definitions = proxy.definitionsFor(world);
        FoodDefinition definition = definitions.configured(key);
        if (definition == null) {
            return;
        }
        if (world.isRemote) {
            // The server decides; the client neither bites the cake nor uses the held item.
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        MealGate.Decision decision = gate.begin(player, key,
                new TextComponentTranslation(state.getBlock().getTranslationKey() + ".name"));
        if (!decision.accepted) {
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
            player.sendStatusMessage(decision.message, true);
            return;
        }
        pending.add(new PendingSlice(player, event.getPos(), state, key));
    }

    /** End of the server tick: vanilla has handled the click by now, so the block tells whether the slice went. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) {
            return;
        }
        List<PendingSlice> checks = new ArrayList<PendingSlice>(pending);
        pending.clear();
        for (PendingSlice check : checks) {
            IBlockState after = check.player.world.getBlockState(check.pos);
            if (sliceEaten(check.before, after)) {
                gate.finish(check.player, check.key);
            } else {
                gate.cancel(check.player);
                Diag.event("block meal for {} at {} not eaten by the game; nothing activated", check.player.getName(), check.pos);
            }
        }
    }
}
