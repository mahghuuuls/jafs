package com.mahghuuuls.jafs.network;

import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.DefinitionSet;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Server to client: the loaded definitions and the property metadata the client needs to show them. */
public final class DefinitionsSyncMessage implements IMessage {

    private DefinitionSet definitions = DefinitionSet.empty();
    private List<PropertySpec> properties = new ArrayList<PropertySpec>();

    public DefinitionsSyncMessage() {
    }

    public DefinitionsSyncMessage(DefinitionSet definitions, Collection<PropertySpec> properties) {
        this.definitions = definitions;
        this.properties = new ArrayList<PropertySpec>(properties);
    }

    public DefinitionSet definitions() {
        return definitions;
    }

    public List<PropertySpec> properties() {
        return properties;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        properties = DefinitionCodec.readProperties(buf);
        definitions = DefinitionCodec.readDefinitions(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DefinitionCodec.writeProperties(buf, properties);
        DefinitionCodec.writeDefinitions(buf, definitions);
    }
}
