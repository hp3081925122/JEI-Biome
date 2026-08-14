package com.jei_biome.jei;

import com.jei_biome.Jei_biome;
import com.jei_biome.data.BiomeBlockIndexCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BiomeBlockRecipe {

    private final BiomeBlockIndexCache.BiomeEntry entry;
    private final ResourceLocation id;
    private final List<ItemStack> terrainStacks;
    private final List<ItemStack> surfaceFeatureStacks;
    private final List<ItemStack> undergroundFeatureStacks;
    private final List<ItemStack> oreStacks;
    private final List<ItemStack> mobDropStacks;
    private final List<ItemStack> mobSpawnEggStacks;
    private final Map<String, List<BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines;
    private final List<ItemStack> lookupStacks;

    public BiomeBlockRecipe(BiomeBlockIndexCache.BiomeEntry entry) {
        this.entry = entry;
        this.id = new ResourceLocation(Jei_biome.MODID, entry.biomeId.toLowerCase(Locale.ROOT).replace(':', '_').replace('/', '_'));
        this.terrainStacks = toStacks(entry.terrainBlocks);
        this.surfaceFeatureStacks = toStacks(entry.surfaceFeatureBlocks);
        this.undergroundFeatureStacks = toStacks(entry.undergroundFeatureBlocks);
        this.oreStacks = toStacks(entry.oreBlocks);
        BiomeMobRecipe mobRecipe = new BiomeMobRecipe(entry);
        LinkedHashMap<Item, ItemStack> spawnEggs = new LinkedHashMap<>();
        for (BiomeMobRecipe.MobDisplayEntry mobEntry : mobRecipe.mobEntries()) {
            if (!mobEntry.stack().isEmpty()) {
                spawnEggs.putIfAbsent(mobEntry.stack().getItem(), mobEntry.stack().copy());
            }
        }
        LinkedHashMap<Item, ItemStack> mobDrops = new LinkedHashMap<>();
        for (ItemStack stack : mobRecipe.lookupStacks()) {
            if (!spawnEggs.containsKey(stack.getItem())) {
                mobDrops.putIfAbsent(stack.getItem(), stack.copy());
            }
        }
        this.mobSpawnEggStacks = List.copyOf(spawnEggs.values());
        this.mobDropStacks = List.copyOf(mobDrops.values());
        this.oreDistributionLines = buildOreDistributionLines(entry.oreDistributions);
        LinkedHashMap<Item, ItemStack> lookup = new LinkedHashMap<>();
        for (ItemStack stack : terrainStacks) {
            lookup.putIfAbsent(stack.getItem(), stack.copy());
        }
        for (ItemStack stack : surfaceFeatureStacks) {
            lookup.putIfAbsent(stack.getItem(), stack.copy());
        }
        for (ItemStack stack : undergroundFeatureStacks) {
            lookup.putIfAbsent(stack.getItem(), stack.copy());
        }
        for (ItemStack stack : oreStacks) {
            lookup.putIfAbsent(stack.getItem(), stack.copy());
        }
        for (ItemStack stack : mobDropStacks) {
            lookup.putIfAbsent(stack.getItem(), stack.copy());
        }
        for (ItemStack stack : mobSpawnEggStacks) {
            lookup.putIfAbsent(stack.getItem(), stack.copy());
        }
        this.lookupStacks = List.copyOf(lookup.values());
    }

    public BiomeBlockIndexCache.BiomeEntry entry() {
        return entry;
    }

    public ResourceLocation id() {
        return id;
    }

    public List<ItemStack> terrainStacks() {
        return copyStacks(terrainStacks);
    }

    public List<ItemStack> surfaceFeatureStacks() {
        return copyStacks(surfaceFeatureStacks);
    }

    public List<ItemStack> undergroundFeatureStacks() {
        return copyStacks(undergroundFeatureStacks);
    }

    public List<ItemStack> oreStacks() {
        return copyStacks(oreStacks);
    }

    public List<ItemStack> mobDropStacks() {
        return copyStacks(mobDropStacks);
    }

    public List<ItemStack> mobSpawnEggStacks() {
        return copyStacks(mobSpawnEggStacks);
    }

    public boolean containsBlockStack(ItemStack target) {
        return containsItem(terrainStacks, target)
                || containsItem(surfaceFeatureStacks, target)
                || containsItem(undergroundFeatureStacks, target)
                || containsItem(oreStacks, target);
    }

    public List<BiomeBlockIndexCache.OreDistributionLine> getOreDistributionLines(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        if (blockId == null) {
            return List.of();
        }
        return oreDistributionLines.getOrDefault(blockId.toString(), List.of());
    }

    public List<ItemStack> lookupStacks() {
        return copyStacks(lookupStacks);
    }

    private static List<ItemStack> toStacks(List<String> blockIds) {
        if (blockIds == null || blockIds.isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (String rawId : blockIds) {
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
            Item item = block == null ? Items.AIR : block.asItem();
            if (item != Items.AIR) {
                stacks.add(new ItemStack(item));
            }
        }
        stacks.sort(Comparator
                .comparing((ItemStack stack) -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(stack -> String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(stacks);
    }

    private static Map<String, List<BiomeBlockIndexCache.OreDistributionLine>> buildOreDistributionLines(List<BiomeBlockIndexCache.OreDistributionEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Map.of();
        }
        Map<String, List<BiomeBlockIndexCache.OreDistributionLine>> linesByBlock = new LinkedHashMap<>();
        for (BiomeBlockIndexCache.OreDistributionEntry entry : entries) {
            if (entry == null || entry.blockId == null || entry.blockId.isBlank()) {
                continue;
            }
            List<BiomeBlockIndexCache.OreDistributionLine> lines = new ArrayList<>();
            for (BiomeBlockIndexCache.OreDistributionLine line : entry.lines) {
                if (line != null) {
                    lines.add(line);
                }
            }
            lines.sort(Comparator.comparing(BiomeBlockRecipe::oreDistributionSortKey, String.CASE_INSENSITIVE_ORDER));
            linesByBlock.put(entry.blockId, List.copyOf(lines));
        }
        return Collections.unmodifiableMap(linesByBlock);
    }

    private static String oreDistributionSortKey(BiomeBlockIndexCache.OreDistributionLine line) {
        return line.sourceKind + "|" + line.sourceId + "|" + line.veinSize + "|" + line.attemptsPerChunk + "|" + line.averageEveryChunks + "|" + line.minHeight + "|" + line.maxHeight + "|" + line.heightMode;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : stacks) {
            copies.add(stack.copy());
        }
        return List.copyOf(copies);
    }

    private static boolean containsItem(List<ItemStack> stacks, ItemStack target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        for (ItemStack stack : stacks) {
            if (stack.getItem() == target.getItem()) {
                return true;
            }
        }
        return false;
    }
}
