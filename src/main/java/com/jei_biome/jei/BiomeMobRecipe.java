package com.jei_biome.jei;

import com.jei_biome.Jei_biome;
import com.jei_biome.data.BiomeBlockIndexCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BiomeMobRecipe {

    private final BiomeBlockIndexCache.BiomeEntry entry;
    private final Identifier id;
    private final List<MobDisplayEntry> mobEntries;
    private final List<ItemStack> lookupStacks;

    public BiomeMobRecipe(BiomeBlockIndexCache.BiomeEntry entry) {
        this.entry = entry;
        this.id = Identifier.fromNamespaceAndPath(Jei_biome.MODID, "mobs_" + entry.biomeId.toLowerCase(Locale.ROOT).replace(':', '_').replace('/', '_'));
        this.mobEntries = buildMobEntries(entry.mobSpawns);
        LinkedHashMap<Item, ItemStack> lookup = new LinkedHashMap<>();
        for (MobDisplayEntry mobEntry : mobEntries) {
            if (!mobEntry.stack().isEmpty()) {
                lookup.putIfAbsent(mobEntry.stack().getItem(), mobEntry.stack().copy());
            }
            for (BiomeBlockIndexCache.MobSpawnEntry spawnEntry : mobEntry.spawns()) {
                for (String itemId : spawnEntry.dropItems) {
                    Identifier id = Identifier.tryParse(itemId);
                    Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
                    if (item != null && item != Items.AIR) {
                        lookup.putIfAbsent(item, new ItemStack(item));
                    }
                }
            }
        }
        this.lookupStacks = List.copyOf(lookup.values());
    }

    public BiomeBlockIndexCache.BiomeEntry entry() {
        return entry;
    }

    public Identifier id() {
        return id;
    }

    public List<MobDisplayEntry> mobEntries() {
        return mobEntries;
    }

    public List<ItemStack> lookupStacks() {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : lookupStacks) {
            copies.add(stack.copy());
        }
        return List.copyOf(copies);
    }

    private static List<MobDisplayEntry> buildMobEntries(List<BiomeBlockIndexCache.MobSpawnEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, List<BiomeBlockIndexCache.MobSpawnEntry>> groupedEntries = new LinkedHashMap<>();
        for (BiomeBlockIndexCache.MobSpawnEntry entry : entries) {
            if (entry == null || entry.entityId == null || entry.entityId.isBlank()) {
                continue;
            }
            groupedEntries.computeIfAbsent(entry.entityId, ignored -> new ArrayList<>()).add(entry);
        }
        List<MobDisplayEntry> result = new ArrayList<>();
        for (Map.Entry<String, List<BiomeBlockIndexCache.MobSpawnEntry>> groupedEntry : groupedEntries.entrySet()) {
            BiomeBlockIndexCache.MobSpawnEntry firstEntry = groupedEntry.getValue().get(0);
            Identifier entityId = Identifier.tryParse(groupedEntry.getKey());
            EntityType<?> entityType = entityId == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
            ItemStack stack = ItemStack.EMPTY;
            if (entityType != null) {
                stack = SpawnEggItem.byId(entityType).map(ItemStack::new).orElse(ItemStack.EMPTY);
            }
            List<BiomeBlockIndexCache.MobSpawnEntry> spawns = new ArrayList<>(groupedEntry.getValue());
            spawns.sort(Comparator.comparing(BiomeMobRecipe::mobSpawnSortKey, String.CASE_INSENSITIVE_ORDER));
            result.add(new MobDisplayEntry(firstEntry, List.copyOf(spawns), stack));
        }
        result.sort(Comparator
                .comparing((MobDisplayEntry entry) -> entry.data().category, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> getEntityName(entry.data()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.data().entityId, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    static String getEntityName(BiomeBlockIndexCache.MobSpawnEntry entry) {
        Identifier entityId = Identifier.tryParse(entry.entityId);
        EntityType<?> entityType = entityId == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
        if (entityType != null) {
            return entityType.getDescription().getString();
        }
        return entry.entityId;
    }

    private static String mobSpawnSortKey(BiomeBlockIndexCache.MobSpawnEntry entry) {
        return entry.category + "|" + entry.weight + "|" + entry.minCount + "|" + entry.maxCount;
    }

    public record MobDisplayEntry(BiomeBlockIndexCache.MobSpawnEntry data, List<BiomeBlockIndexCache.MobSpawnEntry> spawns, ItemStack stack) {
    }
}
