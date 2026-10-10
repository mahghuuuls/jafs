package com.mahghuuuls.jafs.command;

import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.config.GameplaySettings;
import com.mahghuuuls.jafs.core.DisplayFormat;
import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.gameplay.ActiveFoodService;
import com.mahghuuuls.jafs.gameplay.HungerPin;
import com.mahghuuuls.jafs.gameplay.JafsGameplay;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.gameplay.ReloadService;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /jafs inspect [player] | reload}: operator tools. Every reply is also written to the
 * server log so it can be read after a test.
 */
public final class JafsCommand extends CommandBase {

    private static final String USAGE = "/jafs inspect [player] | reload | clear <player>";

    @Override
    public String getName() {
        return "jafs";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "inspect", "reload", "clear");
        }
        if (args.length == 2 && ("inspect".equalsIgnoreCase(args[0]) || "clear".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(USAGE);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("inspect".equals(sub)) {
            EntityPlayerMP target = args.length >= 2 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
            for (String line : describe(target)) {
                reply(sender, line);
            }
        } else if ("clear".equals(sub)) {
            if (args.length < 2) {
                throw new WrongUsageException(USAGE);
            }
            EntityPlayerMP target = getPlayer(server, sender, args[1]);
            int cleared = JafsGameplay.get().service().clearAll(target, "command");
            reply(sender, "JAFS clear player=" + target.getName() + " cleared=" + cleared);
        } else if ("reload".equals(sub)) {
            for (String line : ReloadService.reloadAll().lines) {
                reply(sender, line);
            }
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    static List<String> describe(EntityPlayerMP player) {
        GameplaySettings settings = GameplayConfig.current();
        IFoodState state = FoodStateCapability.get(player);
        String foods;
        if (state == null) {
            foods = "state=missing";
        } else if (state.slots().isEmpty()) {
            foods = "foods=none";
        } else {
            StringBuilder builder = new StringBuilder("foods=");
            for (FoodSlot slot : state.slots()) {
                builder.append(slot.definitionId()).append(' ').append(DisplayFormat.durationTicks(slot.remainingTicks())).append('/')
                        .append(DisplayFormat.durationTicks(slot.fullTicks())).append(ActiveFoodService.isEligible(slot) ? " eligible;" : ";");
            }
            foods = builder.toString();
        }
        String recovery = state == null ? "recovery=unknown"
                : "recovery=not-implemented delayTicks=" + state.recoveryDelayTicks();
        JafsGameplay gameplay = JafsGameplay.get();
        StringBuilder bonuses = new StringBuilder("bonuses=");
        List<String> described = gameplay.bonuses().describe(player);
        bonuses.append(described.isEmpty() ? "none" : String.join("; ", described));
        double effectiveRecovery = PropertyRegistry.get().rates().recoveryRatePercentPerSecond(player, settings.baseRecoveryRatePercent());
        double effectiveDepletion = gameplay.ticker().effectiveRate(player);
        return Arrays.asList(
                "JAFS inspect player=" + player.getName() + " foodLevel=" + player.getFoodStats().getFoodLevel()
                        + " pinned=" + HungerPin.PINNED_FOOD_LEVEL + " health=" + fmt(player.getHealth()) + "/"
                        + fmt(player.getMaxHealth()),
                "JAFS inspect player=" + player.getName() + " " + foods + " limit=" + settings.activeFoodLimit()
                        + " threshold=" + settings.eligibilityThresholdPercent() + "%",
                "JAFS inspect player=" + player.getName() + " " + bonuses,
                "JAFS inspect player=" + player.getName() + " " + recovery + " baseRate=" + settings.baseRecoveryRatePercent()
                        + "%/s effectiveRecoveryRate=" + fmt((float) effectiveRecovery) + "%/s delay=" + settings.recoveryDelaySeconds()
                        + "s depletionRate=" + settings.depletionRate() + " effectiveDepletionRate=" + fmt((float) effectiveDepletion)
                        + " diagnostics=" + settings.diagnosticMode());
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void reply(ICommandSender sender, String line) {
        sender.sendMessage(new TextComponentString(line));
        Diag.LOGGER.info(line);
    }
}
