package com.mahghuuuls.jafs.network;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.core.Modifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The wire encoding reproduces the definition set and the property metadata exactly. */
class DefinitionCodecTest {

    @Test
    void definitionsRoundTrip() {
        FoodDefinition potion = new FoodDefinition("strength", Collections.singletonList(
                Matcher.item("minecraft:potion", 2, Collections.singletonMap("Potion", "minecraft:strength"))), 300, 10,
                Arrays.asList(new Modifier("jafs:recovery_rate", PropertyOperation.PERCENT, 50.5),
                        new Modifier("addon:stamina", PropertyOperation.FLAT, -3)));
        FoodDefinition cake = new FoodDefinition("cake", Collections.singletonList(Matcher.block("minecraft:cake", Matcher.ANY_META)),
                60, 0, Collections.<Modifier>emptyList());
        DefinitionSet original = new DefinitionSet(42, Arrays.asList(potion, cake));
        ByteBuf buf = Unpooled.buffer();
        DefinitionCodec.writeDefinitions(buf, original);
        DefinitionSet copy = DefinitionCodec.readDefinitions(buf);
        assertEquals(0, buf.readableBytes());
        assertEquals(42, copy.revision());
        assertEquals(2, copy.size());
        FoodDefinition p = copy.get("strength");
        assertEquals(6000, p.fullTicks());
        assertEquals(10, p.priority());
        assertEquals(2, p.matchers().get(0).meta());
        assertEquals("minecraft:strength", p.matchers().get(0).nbt().get("Potion"));
        assertEquals(50.5, p.modifiers().get(0).amount(), 1e-9);
        assertEquals(PropertyOperation.FLAT, p.modifiers().get(1).operation());
        assertEquals(Matcher.Kind.BLOCK, copy.get("cake").matchers().get(0).kind());
        assertTrue(copy.get("cake").modifiers().isEmpty());
    }

    @Test
    void propertiesRoundTrip() {
        PropertySpec spec = PropertySpec.builder("addon:stamina").displayNameKey("property.addon.stamina").unit("SP").color("aqua")
                .operation(PropertyOperation.FLAT, -50, 50).operation(PropertyOperation.PERCENT, -100, 100).build();
        ByteBuf buf = Unpooled.buffer();
        DefinitionCodec.writeProperties(buf, Collections.singletonList(spec));
        List<PropertySpec> copy = DefinitionCodec.readProperties(buf);
        assertEquals(1, copy.size());
        assertEquals("addon:stamina", copy.get(0).id());
        assertEquals("property.addon.stamina", copy.get(0).displayNameKey());
        assertEquals("SP", copy.get(0).unit());
        assertEquals("aqua", copy.get(0).color());
        assertEquals(-50, copy.get(0).range(PropertyOperation.FLAT).min(), 1e-9);
        assertEquals(100, copy.get(0).range(PropertyOperation.PERCENT).max(), 1e-9);
    }
}
