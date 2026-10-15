package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.DisplayFormat;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Modifier;
import com.mahghuuuls.jafs.network.ItemKeys;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Food tooltips: one line per modifier (property name, signed amount, unit; percent and
 * percentage points told apart), then the full duration. Edible items without a definition get
 * the "no food bonuses configured" line.
 */
@SideOnly(Side.CLIENT)
public final class TooltipHandler {

    private static final java.util.Set<String> WARNED_KEYS = new java.util.HashSet<String>();

    private static final String NO_BONUSES_KEY = "tooltip.jafs.unconfigured";
    private static final String DURATION_KEY = "tooltip.jafs.duration";

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        DefinitionSet set = ClientDefinitions.definitions();
        ConsumableKey key = ItemKeys.of(stack, set);
        if (key == null) {
            return;
        }
        DefinitionSet.Resolution resolution = set.resolve(key);
        List<String> lines = event.getToolTip();
        FoodDefinition definition = resolution.definition();
        if (definition == null || !definition.hasModifiers()) {
            if (isEdible(stack) && ClientPreferences.showUnconfiguredTooltip()) {
                lines.add(TextFormatting.DARK_GRAY + I18n.format(NO_BONUSES_KEY));
            }
            return;
        }
        for (Modifier modifier : definition.modifiers()) {
            String line = formatModifier(modifier);
            if (line != null) {
                lines.add(line);
            }
        }
        lines.add(TextFormatting.GRAY + I18n.format(DURATION_KEY, formatDuration(definition.fullDurationSeconds())));
    }

    static boolean isEdible(ItemStack stack) {
        if (stack.getItem() instanceof ItemFood) {
            return true;
        }
        EnumAction action = stack.getItemUseAction();
        return action == EnumAction.EAT || action == EnumAction.DRINK;
    }

    static String formatModifier(Modifier modifier) {
        PropertySpec spec = ClientDefinitions.property(modifier.propertyId());
        if (spec == null) {
            return null;
        }
        String name = I18n.format(spec.displayNameKey());
        if (!I18n.hasKey(spec.displayNameKey()) && WARNED_KEYS.add(spec.displayNameKey())) {
            com.mahghuuuls.jafs.diagnostic.Diag.LOGGER.warn("no translation for property name key {} ({}) in the loaded language", spec.displayNameKey(), spec.id());
        }
        String color = ClientPreferences.colorFor(modifier.propertyId(), spec.color());
        TextFormatting formatting = TextFormatting.getValueByName(color);
        String prefix = formatting == null ? "" : formatting.toString();
        return prefix + DisplayFormat.amount(modifier.amount(), modifier.operation(), spec.unit()) + " " + name;
    }

    static String formatDuration(int seconds) {
        return DisplayFormat.duration(seconds);
    }
}
