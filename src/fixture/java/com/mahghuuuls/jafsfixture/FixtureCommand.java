package com.mahghuuuls.jafsfixture;

import com.mahghuuuls.jafs.api.ActiveFoodView;
import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.api.PaymentResult;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * {@code /jafsfixture pay <seconds> | canpay <seconds> | rate <depletionPercent> [recoveryPercent] | rate clear | foods | value <propertyId> | hud cancel|restore}.
 * Every reply is also logged so the agent can read it.
 */
public final class FixtureCommand extends CommandBase {

    private static final String USAGE = "/jafsfixture pay <seconds> | canpay <seconds> | rate <depletionPercent> [recoveryPercent] | rate clear | foods | value <propertyId> | hud cancel|restore";

    @Override
    public String getName() {
        return "jafsfixture";
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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(USAGE);
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        String sub = args[0];
        if ("pay".equals(sub) && args.length == 2) {
            PaymentResult result = JafsApi.getFoodService().pay(player, parseInt(args[1], 0, 86400), "fixture command");
            reply(sender, "jafsfixture pay " + args[1] + ": " + result);
        } else if ("canpay".equals(sub) && args.length == 2) {
            reply(sender, "jafsfixture canpay " + args[1] + ": " + JafsApi.getFoodService().canPay(player, parseInt(args[1], 0, 86400)));
        } else if ("rate".equals(sub) && args.length >= 2) {
            if ("clear".equals(args[1])) {
                JafsFixtureMod.depletionPercent = 0;
                JafsFixtureMod.recoveryPercent = 0;
            } else {
                JafsFixtureMod.depletionPercent = parseDouble(args[1]);
                JafsFixtureMod.recoveryPercent = args.length > 2 ? parseDouble(args[2]) : 0;
            }
            JafsApi.markDirty(player);
            reply(sender, "jafsfixture rate: depletion " + JafsFixtureMod.depletionPercent + "% recovery " + JafsFixtureMod.recoveryPercent + "% (marked dirty)");
        } else if ("foods".equals(sub)) {
            StringBuilder text = new StringBuilder("jafsfixture foods:");
            for (ActiveFoodView food : JafsApi.getFoodService().getActiveFoods(player)) {
                text.append(' ').append(food).append(' ').append(food.modifiers()).append(';');
            }
            reply(sender, text.toString());
        } else if ("hud".equals(sub) && args.length == 2 && ("cancel".equals(args[1]) || "restore".equals(args[1]))) {
            // Single-player only: the flag lives in this JVM, which is also the client's.
            JafsFixtureMod.cancelHud = "cancel".equals(args[1]);
            reply(sender, "jafsfixture hud: " + (JafsFixtureMod.cancelHud ? "cancelling the JAFS HUD event" : "JAFS HUD restored"));
        } else if ("value".equals(sub) && args.length == 2) {
            reply(sender, "jafsfixture value " + args[1] + ": " + JafsApi.getFoodService().getPropertyValue(player, args[1]));
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    private static void reply(ICommandSender sender, String line) {
        sender.sendMessage(new TextComponentString(line));
        JafsFixtureMod.LOGGER.info(line);
    }
}
