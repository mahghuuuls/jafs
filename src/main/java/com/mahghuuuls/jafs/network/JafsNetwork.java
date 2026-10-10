package com.mahghuuuls.jafs.network;

import com.mahghuuuls.jafs.Tags;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.DefinitionSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;

/**
 * The mod's channel. Handlers run on the Netty thread; the client-side sink is installed by the
 * client proxy and schedules onto the main thread itself.
 */
public final class JafsNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    /** What the client does with received definitions; set by the client proxy, absent on a server. */
    public interface DefinitionsSink {
        void accept(DefinitionsSyncMessage message);
    }

    /** What the client does with a received food snapshot; set by the client proxy. */
    public interface SnapshotSink {
        void accept(FoodStateSnapshotMessage message);
    }

    private static volatile DefinitionsSink definitionsSink;
    private static volatile SnapshotSink snapshotSink;

    private JafsNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(DefinitionsHandler.class, DefinitionsSyncMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(SnapshotHandler.class, FoodStateSnapshotMessage.class, 1, Side.CLIENT);
    }

    public static void setDefinitionsSink(DefinitionsSink sink) {
        definitionsSink = sink;
    }

    public static void setSnapshotSink(SnapshotSink sink) {
        snapshotSink = sink;
    }

    public static void sendSnapshot(EntityPlayerMP player, FoodStateSnapshotMessage message) {
        CHANNEL.sendTo(message, player);
    }

    public static void sendDefinitions(EntityPlayerMP player, DefinitionSet definitions, Collection<PropertySpec> properties) {
        CHANNEL.sendTo(new DefinitionsSyncMessage(definitions, properties), player);
    }

    public static void sendDefinitionsToAll(DefinitionSet definitions, Collection<PropertySpec> properties) {
        CHANNEL.sendToAll(new DefinitionsSyncMessage(definitions, properties));
    }

    public static final class SnapshotHandler implements IMessageHandler<FoodStateSnapshotMessage, IMessage> {
        @Override
        public IMessage onMessage(FoodStateSnapshotMessage message, MessageContext ctx) {
            SnapshotSink sink = snapshotSink;
            if (sink != null) {
                sink.accept(message);
            }
            return null;
        }
    }

    public static final class DefinitionsHandler implements IMessageHandler<DefinitionsSyncMessage, IMessage> {
        @Override
        public IMessage onMessage(DefinitionsSyncMessage message, MessageContext ctx) {
            DefinitionsSink sink = definitionsSink;
            if (sink != null) {
                sink.accept(message);
            }
            return null;
        }
    }
}
