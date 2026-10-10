package com.mahghuuuls.jafs.network;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.core.Modifier;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire encoding of the definition set and the property metadata, so the client resolves and
 * displays with the same core code the server uses.
 */
public final class DefinitionCodec {

    private DefinitionCodec() {
    }

    public static void writeDefinitions(ByteBuf buf, DefinitionSet set) {
        buf.writeLong(set.revision());
        buf.writeInt(set.size());
        for (FoodDefinition definition : set.all().values()) {
            ByteBufUtils.writeUTF8String(buf, definition.id());
            buf.writeInt(definition.fullDurationSeconds());
            buf.writeInt(definition.priority());
            buf.writeInt(definition.matchers().size());
            for (Matcher matcher : definition.matchers()) {
                buf.writeByte(matcher.kind() == Matcher.Kind.ITEM ? 0 : 1);
                ByteBufUtils.writeUTF8String(buf, matcher.id());
                buf.writeInt(matcher.meta());
                buf.writeInt(matcher.nbt().size());
                for (Map.Entry<String, String> field : matcher.nbt().entrySet()) {
                    ByteBufUtils.writeUTF8String(buf, field.getKey());
                    ByteBufUtils.writeUTF8String(buf, field.getValue());
                }
            }
            buf.writeInt(definition.modifiers().size());
            for (Modifier modifier : definition.modifiers()) {
                ByteBufUtils.writeUTF8String(buf, modifier.propertyId());
                buf.writeByte(modifier.operation().ordinal());
                buf.writeDouble(modifier.amount());
            }
        }
    }

    public static DefinitionSet readDefinitions(ByteBuf buf) {
        long revision = buf.readLong();
        int count = buf.readInt();
        List<FoodDefinition> definitions = new ArrayList<FoodDefinition>(count);
        for (int i = 0; i < count; i++) {
            String id = ByteBufUtils.readUTF8String(buf);
            int fullSeconds = buf.readInt();
            int priority = buf.readInt();
            int matcherCount = buf.readInt();
            List<Matcher> matchers = new ArrayList<Matcher>(matcherCount);
            for (int m = 0; m < matcherCount; m++) {
                Matcher.Kind kind = buf.readByte() == 0 ? Matcher.Kind.ITEM : Matcher.Kind.BLOCK;
                String matcherId = ByteBufUtils.readUTF8String(buf);
                int meta = buf.readInt();
                int fieldCount = buf.readInt();
                Map<String, String> nbt = new LinkedHashMap<String, String>();
                for (int f = 0; f < fieldCount; f++) {
                    nbt.put(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
                }
                matchers.add(new Matcher(kind, matcherId, meta, nbt));
            }
            int modifierCount = buf.readInt();
            List<Modifier> modifiers = new ArrayList<Modifier>(modifierCount);
            for (int m = 0; m < modifierCount; m++) {
                String propertyId = ByteBufUtils.readUTF8String(buf);
                PropertyOperation operation = PropertyOperation.values()[buf.readByte()];
                modifiers.add(new Modifier(propertyId, operation, buf.readDouble()));
            }
            definitions.add(new FoodDefinition(id, matchers, fullSeconds, priority, modifiers));
        }
        return new DefinitionSet(revision, definitions);
    }

    public static void writeProperties(ByteBuf buf, Collection<PropertySpec> specs) {
        buf.writeInt(specs.size());
        for (PropertySpec spec : specs) {
            ByteBufUtils.writeUTF8String(buf, spec.id());
            ByteBufUtils.writeUTF8String(buf, spec.displayNameKey());
            ByteBufUtils.writeUTF8String(buf, spec.unit());
            ByteBufUtils.writeUTF8String(buf, spec.color());
            buf.writeInt(spec.operations().size());
            for (PropertyOperation operation : spec.operations()) {
                buf.writeByte(operation.ordinal());
                buf.writeDouble(spec.range(operation).min());
                buf.writeDouble(spec.range(operation).max());
            }
        }
    }

    public static List<PropertySpec> readProperties(ByteBuf buf) {
        int count = buf.readInt();
        List<PropertySpec> specs = new ArrayList<PropertySpec>(count);
        for (int i = 0; i < count; i++) {
            PropertySpec.Builder builder = PropertySpec.builder(ByteBufUtils.readUTF8String(buf))
                    .displayNameKey(ByteBufUtils.readUTF8String(buf))
                    .unit(ByteBufUtils.readUTF8String(buf))
                    .color(ByteBufUtils.readUTF8String(buf));
            int operations = buf.readInt();
            for (int o = 0; o < operations; o++) {
                PropertyOperation operation = PropertyOperation.values()[buf.readByte()];
                builder.operation(operation, buf.readDouble(), buf.readDouble());
            }
            specs.add(builder.build());
        }
        return specs;
    }
}
