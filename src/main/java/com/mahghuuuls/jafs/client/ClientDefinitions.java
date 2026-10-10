package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.DefinitionsSyncMessage;
import com.mahghuuuls.jafs.network.JafsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** The client's copy of the server's definitions and property metadata, replaced on every sync. */
@SideOnly(Side.CLIENT)
public final class ClientDefinitions implements JafsNetwork.DefinitionsSink {

    private static volatile DefinitionSet definitions = DefinitionSet.empty();
    private static volatile Map<String, PropertySpec> properties = Collections.emptyMap();

    public static DefinitionSet definitions() {
        return definitions;
    }

    public static PropertySpec property(String id) {
        return properties.get(id);
    }

    @Override
    public void accept(final DefinitionsSyncMessage message) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Map<String, PropertySpec> map = new HashMap<String, PropertySpec>();
                for (PropertySpec spec : message.properties()) {
                    map.put(spec.id(), spec);
                }
                properties = Collections.unmodifiableMap(map);
                definitions = message.definitions();
                Diag.event("client received definitions={} revision={} properties={}", definitions.size(), definitions.revision(),
                        map.size());
            }
        });
    }

    /** Forgets the server's data when leaving a world. */
    public static void clear() {
        definitions = DefinitionSet.empty();
        properties = Collections.emptyMap();
    }
}
