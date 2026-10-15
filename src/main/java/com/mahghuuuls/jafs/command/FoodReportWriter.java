package com.mahghuuuls.jafs.command;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.FoodReport;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.ItemKeys;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Minecraft-aware half of {@code /jafs report} (REQ-069b): gathers every edible item from
 * the item registry (each sub-item of every item that is eaten or drunk) and every item or
 * block a definition names, hands them to {@link FoodReport}, and writes the file next to the
 * configuration. Runs on the server thread; reads the current definitions and no player state.
 */
final class FoodReportWriter {

    static final String FILE_NAME = "jafs-foods-report.csv";

    /** What one run produced, for the reply. */
    static final class Result {
        final File file;
        final FoodReport.Counts counts;
        /** Registry ids whose sub-item listing threw; named in the reply, never hidden (REQ-097). */
        final List<String> skipped;

        Result(File file, FoodReport.Counts counts, List<String> skipped) {
            this.file = file;
            this.counts = counts;
            this.skipped = skipped;
        }
    }

    private FoodReportWriter() {
    }

    static Result write() throws IOException {
        DefinitionSet set = FoodDefinitionFile.current();
        List<String> skipped = new ArrayList<String>();
        List<FoodReport.Row> rows = FoodReport.rows(gather(set, skipped), set);
        File file = new File(FoodDefinitionFile.folder(), FILE_NAME);
        if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
            throw new IOException("cannot create " + file.getParentFile());
        }
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            writer.write(FoodReport.csv(rows));
        } finally {
            writer.close();
        }
        FoodReport.Counts counts = FoodReport.count(rows);
        Diag.event("report written: {} ({} rows, {} items skipped)", file.getPath(), counts.listed, skipped.size());
        return new Result(file, counts, skipped);
    }

    /**
     * Every registry sub-item that is eaten or drunk, one row each (two potion variants share a
     * key when no matcher reads their NBT, so the sweep never merges stacks), then what the
     * definitions name and the sweep did not list. An item whose sub-item listing throws (a mod
     * item touching client classes on a dedicated server, for example) is skipped and named.
     */
    static List<FoodReport.Candidate> gather(DefinitionSet set, List<String> skipped) {
        List<FoodReport.Candidate> candidates = new ArrayList<FoodReport.Candidate>();
        List<ConsumableKey> swept = new ArrayList<ConsumableKey>();
        Set<String> namedKeys = new HashSet<String>();
        for (Item item : Item.REGISTRY) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            try {
                NonNullList<ItemStack> stacks = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, stacks);
                if (stacks.isEmpty()) {
                    stacks.add(new ItemStack(item));
                }
                for (ItemStack stack : stacks) {
                    FoodReport.Kind kind = kindOf(stack);
                    if (kind == null) {
                        continue;
                    }
                    FoodReport.Candidate candidate = candidate(kind, stack, set);
                    if (candidate != null) {
                        candidates.add(candidate);
                        swept.add(candidate.key);
                    }
                }
            } catch (Exception e) {
                skipped.add(String.valueOf(item.getRegistryName()) + " (" + e + ")");
            } catch (LinkageError e) {
                skipped.add(String.valueOf(item.getRegistryName()) + " (" + e + ")");
            }
        }
        for (FoodDefinition definition : set.all().values()) {
            for (Matcher matcher : definition.matchers()) {
                // A matcher some swept stack satisfies is already listed (the matcher's own rule decides,
                // not key text: a matcher without meta or with several NBT fields never equals a stack key).
                if (coveredBySweep(matcher, swept)) {
                    continue;
                }
                FoodReport.Candidate candidate = named(matcher, set);
                if (candidate != null && namedKeys.add(candidate.key.toString())) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    private static boolean coveredBySweep(Matcher matcher, List<ConsumableKey> swept) {
        for (ConsumableKey key : swept) {
            if (matcher.matches(key)) {
                return true;
            }
        }
        return false;
    }

    /** Eaten items are foods, drunk items are drinks, the rest are not listed. */
    static FoodReport.Kind kindOf(ItemStack stack) {
        EnumAction action = stack.getItemUseAction();
        if (action == EnumAction.EAT || stack.getItem() instanceof ItemFood) {
            return FoodReport.Kind.FOOD;
        }
        if (action == EnumAction.DRINK) {
            return FoodReport.Kind.DRINK;
        }
        return null;
    }

    private static FoodReport.Candidate candidate(FoodReport.Kind kind, ItemStack stack, DefinitionSet set) {
        ConsumableKey key = ItemKeys.of(stack, set);
        if (key == null) {
            return null;
        }
        Integer hunger = null;
        Float saturation = null;
        if (stack.getItem() instanceof ItemFood) {
            ItemFood food = (ItemFood) stack.getItem();
            hunger = food.getHealAmount(stack);
            saturation = food.getSaturationModifier(stack);
        }
        return new FoodReport.Candidate(kind, key, stack.getDisplayName(), modIdOf(key.id()), hunger, saturation);
    }

    /**
     * A matcher's own row when the registry sweep did not produce it: a block (the block's item
     * gives the name), an item the sweep skipped (neither eaten nor drunk), or an id the registry
     * does not know (empty name). A sub-item the sweep already listed is skipped by the caller.
     */
    private static FoodReport.Candidate named(Matcher matcher, DefinitionSet set) {
        ResourceLocation id = new ResourceLocation(matcher.id());
        int meta = matcher.meta() == Matcher.ANY_META ? 0 : matcher.meta();
        if (matcher.kind() == Matcher.Kind.BLOCK) {
            Block block = Block.REGISTRY.containsKey(id) ? Block.REGISTRY.getObject(id) : null;
            // The item registry answers air, never null, for an unknown id (Forge's namespaced wrapper).
            Item item = Item.REGISTRY.getObject(id);
            if ((item == null || item == Items.AIR) && block != null) {
                item = Item.getItemFromBlock(block);
            }
            String name = block == null ? "" : (item == null || item == Items.AIR ? block.getLocalizedName() : new ItemStack(item, 1, meta).getDisplayName());
            ConsumableKey key = new ConsumableKey(Matcher.Kind.BLOCK, matcher.id(), meta, java.util.Collections.<String, String>emptyMap());
            return new FoodReport.Candidate(FoodReport.Kind.BLOCK, key, name, modIdOf(matcher.id()), null, null);
        }
        Item item = Item.REGISTRY.getObject(id);
        if (item == null || item == Items.AIR) {
            ConsumableKey key = new ConsumableKey(Matcher.Kind.ITEM, matcher.id(), matcher.meta(), matcher.nbt());
            return new FoodReport.Candidate(FoodReport.Kind.FOOD, key, "", modIdOf(matcher.id()), null, null);
        }
        ItemStack stack = new ItemStack(item, 1, meta);
        FoodReport.Kind kind = kindOf(stack);
        if (!matcher.nbt().isEmpty()) {
            // An NBT matcher some sub-item satisfies was listed by the sweep and is skipped by the caller;
            // one no sub-item satisfies gets a row of its own under the item's plain name, so it is visible.
            ConsumableKey key = new ConsumableKey(Matcher.Kind.ITEM, matcher.id(), matcher.meta(), matcher.nbt());
            return new FoodReport.Candidate(kind == null ? FoodReport.Kind.FOOD : kind, key, stack.getDisplayName(), modIdOf(matcher.id()), null, null);
        }
        return candidate(kind == null ? FoodReport.Kind.FOOD : kind, stack, set);
    }

    private static String modIdOf(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }
}
