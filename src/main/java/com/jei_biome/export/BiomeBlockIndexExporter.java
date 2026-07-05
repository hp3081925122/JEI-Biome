package com.jei_biome.export;

import com.jei_biome.data.BiomeBlockIndexCache;
import com.jei_biome.data.BiomeBlockIndexPaths;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.Tags;

import java.io.BufferedReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

public final class BiomeBlockIndexExporter {

    private static final int MAX_FEATURE_DEPTH = 8;
    public static final TagKey<Block> BIOME_BLOCK_BLACKLIST = BlockTags.create(ResourceLocation.fromNamespaceAndPath("jei_biome", "biome_block_blacklist"));

    private BiomeBlockIndexExporter() {
    }

    public static Path export(MinecraftServer server) throws Exception {
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Registry<net.minecraft.world.level.levelgen.placement.PlacedFeature> placedFeatureRegistry = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Registry<ConfiguredFeature<?, ?>> configuredFeatureRegistry = server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        BiomeBlockIndexCache cache = new BiomeBlockIndexCache();
        cache.generatedAt = Instant.now().toString();
        for (var biomeEntry : biomeRegistry.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().location().toString())).toList()) {
            BiomeBlockIndexCache.BiomeEntry exportedBiome = new BiomeBlockIndexCache.BiomeEntry();
            exportedBiome.biomeId = biomeEntry.getKey().location().toString();
            LinkedHashSet<String> placedFeatureIds = new LinkedHashSet<>();
            LinkedHashSet<String> configuredFeatureIds = new LinkedHashSet<>();
            LinkedHashSet<String> terrainBlocks = new LinkedHashSet<>();
            LinkedHashSet<String> surfaceFeatureBlocks = new LinkedHashSet<>();
            LinkedHashSet<String> undergroundFeatureBlocks = new LinkedHashSet<>();
            LinkedHashSet<String> oreBlocks = new LinkedHashSet<>();
            Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines = new LinkedHashMap<>();
            List<BiomeBlockIndexCache.MobSpawnEntry> mobSpawns = new java.util.ArrayList<>();
            Set<net.minecraft.world.level.levelgen.placement.PlacedFeature> visitedPlacedFeatures = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            Set<ConfiguredFeature<?, ?>> visitedConfiguredFeatures = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            collectMobSpawns(server, biomeEntry.getValue(), mobSpawns);
            List<HolderSet<net.minecraft.world.level.levelgen.placement.PlacedFeature>> featureSteps = biomeEntry.getValue().getGenerationSettings().features();
            GenerationStep.Decoration[] decorationSteps = GenerationStep.Decoration.values();
            for (int stepIndex = 0; stepIndex < featureSteps.size(); stepIndex++) {
                HolderSet<net.minecraft.world.level.levelgen.placement.PlacedFeature> stepFeatures = featureSteps.get(stepIndex);
                GenerationStep.Decoration decorationStep = stepIndex < decorationSteps.length ? decorationSteps[stepIndex] : GenerationStep.Decoration.RAW_GENERATION;
                for (Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> placedFeatureHolder : stepFeatures) {
                    net.minecraft.world.level.levelgen.placement.PlacedFeature placedFeature = placedFeatureHolder.value();
                    exportedBiome.placedFeatureCount++;
                    placedFeatureHolder.unwrapKey().ifPresent(key -> placedFeatureIds.add(key.location().toString()));
                    placedFeatureRegistry.getResourceKey(placedFeature).ifPresent(key -> placedFeatureIds.add(key.location().toString()));
                    String placedFeatureId = placedFeatureHolder.unwrapKey().map(key -> key.location().toString())
                            .or(() -> placedFeatureRegistry.getResourceKey(placedFeature).map(key -> key.location().toString()))
                            .orElse("unknown");
                    collectPlacedFeatureBlocks(placedFeature, configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, 0);
                }
            }
            exportedBiome.placedFeatureIds = sortedList(placedFeatureIds);
            exportedBiome.configuredFeatureIds = sortedList(configuredFeatureIds);
            exportedBiome.terrainBlocks = sortedList(terrainBlocks);
            exportedBiome.surfaceFeatureBlocks = sortedList(surfaceFeatureBlocks);
            exportedBiome.undergroundFeatureBlocks = sortedList(undergroundFeatureBlocks);
            exportedBiome.oreBlocks = sortedList(oreBlocks);
            exportedBiome.mobSpawns = mobSpawns.stream()
                    .sorted(Comparator.comparing(BiomeBlockIndexExporter::mobSpawnSortKey, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (Map.Entry<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> entry : oreDistributionLines.entrySet()) {
                BiomeBlockIndexCache.OreDistributionEntry distributionEntry = new BiomeBlockIndexCache.OreDistributionEntry();
                distributionEntry.blockId = entry.getKey();
                distributionEntry.lines = entry.getValue().values().stream()
                        .sorted(Comparator.comparing(BiomeBlockIndexExporter::oreDistributionSortKey, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                exportedBiome.oreDistributions.add(distributionEntry);
            }
            LinkedHashSet<String> oldFeatureBlocks = new LinkedHashSet<>();
            oldFeatureBlocks.addAll(exportedBiome.surfaceFeatureBlocks);
            oldFeatureBlocks.addAll(exportedBiome.undergroundFeatureBlocks);
            exportedBiome.featureBlocks = sortedList(oldFeatureBlocks);
            LinkedHashSet<String> allBlocks = new LinkedHashSet<>();
            allBlocks.addAll(exportedBiome.terrainBlocks);
            allBlocks.addAll(exportedBiome.surfaceFeatureBlocks);
            allBlocks.addAll(exportedBiome.undergroundFeatureBlocks);
            allBlocks.addAll(exportedBiome.oreBlocks);
            exportedBiome.allBlocks = sortedList(allBlocks);
            cache.biomes.add(exportedBiome);
        }
        Path path = BiomeBlockIndexPaths.getCachePath();
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            BiomeBlockIndexCache.GSON.toJson(cache, writer);
        }
        return path;
    }

    private static void collectMobSpawns(MinecraftServer server, Biome biome, List<BiomeBlockIndexCache.MobSpawnEntry> target) {
        MobSpawnSettings mobSettings = biome.getMobSettings();
        for (MobCategory category : mobSettings.getSpawnerTypes()) {
            for (MobSpawnSettings.SpawnerData spawnerData : mobSettings.getMobs(category).unwrap()) {
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(spawnerData.type);
                if (entityId == null) {
                    continue;
                }
                BiomeBlockIndexCache.MobSpawnEntry entry = new BiomeBlockIndexCache.MobSpawnEntry();
                entry.entityId = entityId.toString();
                entry.category = category.getSerializedName();
                entry.weight = spawnerData.getWeight().asInt();
                entry.minCount = spawnerData.minCount;
                entry.maxCount = spawnerData.maxCount;
                entry.placementType = summarizeSpawnPlacementType(SpawnPlacements.getPlacementType(spawnerData.type));
                entry.heightmapType = SpawnPlacements.getHeightmapType(spawnerData.type).getSerializedName();
                entry.hasPlacement = SpawnPlacements.hasPlacement(spawnerData.type);
                MobSpawnSettings.MobSpawnCost spawnCost = mobSettings.getMobSpawnCost(spawnerData.type);
                if (spawnCost != null) {
                    entry.spawnCharge = formatDouble(spawnCost.charge());
                    entry.spawnEnergyBudget = formatDouble(spawnCost.energyBudget());
                }
                entry.dropItems = collectEntityDropItems(server, spawnerData.type.getDefaultLootTable());
                target.add(entry);
            }
        }
    }

    private static String formatDouble(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static List<String> collectEntityDropItems(MinecraftServer server, ResourceKey<LootTable> lootTableId) {
        if (lootTableId == null || "minecraft:empty".equals(lootTableId.location().toString())) {
            return List.of();
        }
        ResourceLocation lootLocation = lootTableId.location();
        ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(lootLocation.getNamespace(), "loot_tables/" + lootLocation.getPath() + ".json");
        return server.getResourceManager().getResource(resourceId)
                .map(resource -> {
                    LinkedHashSet<String> itemIds = new LinkedHashSet<>();
                    try (BufferedReader reader = resource.openAsReader()) {
                        collectItemEntries(JsonParser.parseReader(reader), itemIds);
                    } catch (Exception ignored) {
                        return List.<String>of();
                    }
                    return sortedList(itemIds);
                })
                .orElse(List.of());
    }

    private static String summarizeSpawnPlacementType(SpawnPlacementType placementType) {
        if (placementType == SpawnPlacementTypes.ON_GROUND) {
            return "on_ground";
        }
        if (placementType == SpawnPlacementTypes.IN_WATER) {
            return "in_water";
        }
        if (placementType == SpawnPlacementTypes.IN_LAVA) {
            return "in_lava";
        }
        if (placementType == SpawnPlacementTypes.NO_RESTRICTIONS) {
            return "no_restrictions";
        }
        return "custom";
    }

    private static void collectItemEntries(JsonElement element, Set<String> itemIds) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                collectItemEntries(child, itemIds);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("type") && "minecraft:item".equals(object.get("type").getAsString()) && object.has("name")) {
            ResourceLocation itemId = ResourceLocation.tryParse(object.get("name").getAsString());
            Item item = itemId == null ? null : BuiltInRegistries.ITEM.get(itemId);
            if (item != null) {
                itemIds.add(itemId.toString());
            }
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectItemEntries(entry.getValue(), itemIds);
        }
    }

    private static void collectPlacedFeatureBlocks(
            net.minecraft.world.level.levelgen.placement.PlacedFeature placedFeature,
            Registry<ConfiguredFeature<?, ?>> configuredFeatureRegistry,
            Set<String> terrainBlocks,
            Set<String> surfaceFeatureBlocks,
            Set<String> undergroundFeatureBlocks,
            Set<String> oreBlocks,
            Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines,
            Set<String> configuredFeatureIds,
            Set<net.minecraft.world.level.levelgen.placement.PlacedFeature> visitedPlacedFeatures,
            Set<ConfiguredFeature<?, ?>> visitedConfiguredFeatures,
            GenerationStep.Decoration decorationStep,
            String placedFeatureId,
            int depth
    ) {
        if (placedFeature == null || depth > MAX_FEATURE_DEPTH || !visitedPlacedFeatures.add(placedFeature)) {
            return;
        }
        ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();
        configuredFeatureRegistry.getResourceKey(configuredFeature).ifPresent(key -> configuredFeatureIds.add(key.location().toString()));
        collectConfiguredFeatureBlocks(configuredFeature, placedFeature, configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, depth + 1);
    }

    private static void collectConfiguredFeatureBlocks(
            ConfiguredFeature<?, ?> configuredFeature,
            net.minecraft.world.level.levelgen.placement.PlacedFeature sourcePlacedFeature,
            Registry<ConfiguredFeature<?, ?>> configuredFeatureRegistry,
            Set<String> terrainBlocks,
            Set<String> surfaceFeatureBlocks,
            Set<String> undergroundFeatureBlocks,
            Set<String> oreBlocks,
            Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines,
            Set<String> configuredFeatureIds,
            Set<net.minecraft.world.level.levelgen.placement.PlacedFeature> visitedPlacedFeatures,
            Set<ConfiguredFeature<?, ?>> visitedConfiguredFeatures,
            GenerationStep.Decoration decorationStep,
            String placedFeatureId,
            int depth
    ) {
        if (configuredFeature == null || depth > MAX_FEATURE_DEPTH || !visitedConfiguredFeatures.add(configuredFeature)) {
            return;
        }
        FeatureConfiguration config = configuredFeature.config();
        if (config instanceof OreConfiguration oreConfiguration) {
            for (OreConfiguration.TargetBlockState targetState : oreConfiguration.targetStates) {
                addBlockState(targetState.state, oreBlocks);
                addOreDistributionLine(targetState.state, sourcePlacedFeature, placedFeatureId, oreConfiguration.size, oreDistributionLines);
            }
        } else if (config instanceof ReplaceBlockConfiguration replaceBlockConfiguration) {
            for (OreConfiguration.TargetBlockState targetState : replaceBlockConfiguration.targetStates) {
                addBlockState(targetState.state, resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
            }
        } else if (config instanceof SimpleBlockConfiguration simpleBlockConfiguration) {
            addProviderBlock(simpleBlockConfiguration.toPlace(), resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
        } else if (config instanceof BlockStateConfiguration blockStateConfiguration) {
            addBlockState(blockStateConfiguration.state, resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
        } else if (config instanceof TreeConfiguration treeConfiguration) {
            addProviderBlock(treeConfiguration.trunkProvider, surfaceFeatureBlocks);
            addProviderBlock(treeConfiguration.dirtProvider, terrainBlocks);
            addProviderBlock(treeConfiguration.foliageProvider, surfaceFeatureBlocks);
        } else if (config instanceof HugeMushroomFeatureConfiguration mushroomConfiguration) {
            addProviderBlock(mushroomConfiguration.capProvider, surfaceFeatureBlocks);
            addProviderBlock(mushroomConfiguration.stemProvider, surfaceFeatureBlocks);
        } else if (config instanceof BlockPileConfiguration blockPileConfiguration) {
            addProviderBlock(blockPileConfiguration.stateProvider, resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
        } else if (config instanceof DiskConfiguration diskConfiguration) {
            addProviderBlock(diskConfiguration.stateProvider().fallback(), terrainBlocks);
        } else if (config instanceof DeltaFeatureConfiguration deltaFeatureConfiguration) {
            addBlockState(deltaFeatureConfiguration.contents(), resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
            addBlockState(deltaFeatureConfiguration.rim(), resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
        } else if (config instanceof SpringConfiguration springConfiguration) {
            addFluidState(springConfiguration.state, resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
        } else if (config instanceof MultifaceGrowthConfiguration multifaceGrowthConfiguration) {
            addBlock(multifaceGrowthConfiguration.placeBlock, resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks));
        } else if (config instanceof GeodeConfiguration geodeConfiguration) {
            GeodeBlockSettings settings = geodeConfiguration.geodeBlockSettings;
            addProviderOreOrUndergroundBlock(settings.fillingProvider, oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(settings.innerLayerProvider, oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(settings.alternateInnerLayerProvider, oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(settings.middleLayerProvider, oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(settings.outerLayerProvider, oreBlocks, undergroundFeatureBlocks);
            for (BlockState state : settings.innerPlacements) {
                addOreOrUndergroundBlockState(state, oreBlocks, undergroundFeatureBlocks);
                addOreDistributionLine(state, sourcePlacedFeature, placedFeatureId, 0, oreDistributionLines);
            }
        } else if (config instanceof RandomPatchConfiguration randomPatchConfiguration) {
            collectPlacedFeatureBlocks(randomPatchConfiguration.feature().value(), configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, depth + 1);
        } else if (config instanceof VegetationPatchConfiguration vegetationPatchConfiguration) {
            addProviderBlock(vegetationPatchConfiguration.groundState, terrainBlocks);
            collectPlacedFeatureBlocks(vegetationPatchConfiguration.vegetationFeature.value(), configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, depth + 1);
        } else if (config instanceof RandomFeatureConfiguration randomFeatureConfiguration) {
            for (WeightedPlacedFeature weightedPlacedFeature : randomFeatureConfiguration.features) {
                collectPlacedFeatureBlocks(weightedPlacedFeature.feature.value(), configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, depth + 1);
            }
            collectPlacedFeatureBlocks(randomFeatureConfiguration.defaultFeature.value(), configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, depth + 1);
        } else if (config instanceof SimpleRandomFeatureConfiguration simpleRandomFeatureConfiguration) {
            for (Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> featureHolder : simpleRandomFeatureConfiguration.features) {
                collectPlacedFeatureBlocks(featureHolder.value(), configuredFeatureRegistry, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedConfiguredFeatures, decorationStep, placedFeatureId, depth + 1);
            }
        }
    }

    private static Set<String> resolveFeatureTarget(GenerationStep.Decoration decorationStep, Set<String> surfaceFeatureBlocks, Set<String> undergroundFeatureBlocks) {
        if (decorationStep == GenerationStep.Decoration.UNDERGROUND_ORES
                || decorationStep == GenerationStep.Decoration.UNDERGROUND_DECORATION
                || decorationStep == GenerationStep.Decoration.UNDERGROUND_STRUCTURES
                || decorationStep == GenerationStep.Decoration.FLUID_SPRINGS) {
            return undergroundFeatureBlocks;
        }
        return surfaceFeatureBlocks;
    }

    private static void addProviderBlock(BlockStateProvider provider, Set<String> target) {
        if (provider == null) {
            return;
        }
        for (long seed = 1L; seed <= 5L; seed++) {
            addBlockState(provider.getState(RandomSource.create(seed), net.minecraft.core.BlockPos.ZERO), target);
        }
    }

    private static void addProviderOreOrUndergroundBlock(BlockStateProvider provider, Set<String> oreBlocks, Set<String> undergroundFeatureBlocks) {
        if (provider == null) {
            return;
        }
        for (long seed = 1L; seed <= 5L; seed++) {
            addOreOrUndergroundBlockState(provider.getState(RandomSource.create(seed), net.minecraft.core.BlockPos.ZERO), oreBlocks, undergroundFeatureBlocks);
        }
    }

    private static void addOreOrUndergroundBlockState(BlockState state, Set<String> oreBlocks, Set<String> undergroundFeatureBlocks) {
        if (state != null && state.is(Tags.Blocks.ORES)) {
            addBlockState(state, oreBlocks);
        } else {
            addBlockState(state, undergroundFeatureBlocks);
        }
    }

    private static void addOreDistributionLine(BlockState state, net.minecraft.world.level.levelgen.placement.PlacedFeature placedFeature, String placedFeatureId, int veinSize, Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines) {
        if (state == null || state.is(BIOME_BLOCK_BLACKLIST)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return;
        }
        BiomeBlockIndexCache.OreDistributionLine line = new BiomeBlockIndexCache.OreDistributionLine();
        line.sourceId = placedFeatureId;
        line.sourceKind = resolveOreSourceKind(placedFeatureId);
        line.veinSize = veinSize;
        if (placedFeature != null) {
            for (net.minecraft.world.level.levelgen.placement.PlacementModifier modifier : placedFeature.placement()) {
                net.minecraft.world.level.levelgen.placement.PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier)
                        .result()
                        .ifPresent(element -> readPlacementModifier(element, line));
            }
        }
        String key = oreDistributionSortKey(line);
        oreDistributionLines.computeIfAbsent(id.toString(), ignored -> new LinkedHashMap<>()).putIfAbsent(key, line);
    }

    private static void readPlacementModifier(JsonElement element, BiomeBlockIndexCache.OreDistributionLine line) {
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        if ("minecraft:count".equals(type) && object.has("count")) {
            line.attemptsPerChunk = summarizeJsonValue(object.get("count"));
            return;
        }
        if ("minecraft:rarity_filter".equals(type) && object.has("chance")) {
            line.averageEveryChunks = summarizeJsonValue(object.get("chance"));
            return;
        }
        if ("minecraft:height_range".equals(type) && object.has("height") && object.get("height").isJsonObject()) {
            JsonObject height = object.getAsJsonObject("height");
            String heightType = height.has("type") ? height.get("type").getAsString() : "";
            line.minHeight = height.has("min_inclusive") ? summarizeVerticalAnchor(height.get("min_inclusive")) : "";
            line.maxHeight = height.has("max_inclusive") ? summarizeVerticalAnchor(height.get("max_inclusive")) : "";
            line.heightMode = switch (heightType) {
                case "minecraft:trapezoid" -> "middle_common";
                case "minecraft:uniform" -> "even";
                case "minecraft:constant" -> "fixed";
                default -> "custom";
            };
        }
    }

    private static String resolveOreSourceKind(String placedFeatureId) {
        if (placedFeatureId == null) {
            return "common";
        }
        String lowerId = placedFeatureId.toLowerCase(java.util.Locale.ROOT);
        if (lowerId.contains("_extra")) {
            return "extra";
        }
        if (lowerId.contains("_large")) {
            return "large";
        }
        if (lowerId.contains("_buried")) {
            return "buried";
        }
        if (lowerId.contains("_small")) {
            return "small";
        }
        return "common";
    }

    private static String oreDistributionSortKey(BiomeBlockIndexCache.OreDistributionLine line) {
        return line.sourceKind + "|" + line.sourceId + "|" + line.veinSize + "|" + line.attemptsPerChunk + "|" + line.averageEveryChunks + "|" + line.minHeight + "|" + line.maxHeight + "|" + line.heightMode;
    }

    private static String mobSpawnSortKey(BiomeBlockIndexCache.MobSpawnEntry entry) {
        return entry.category + "|" + entry.entityId + "|" + entry.weight + "|" + entry.minCount + "|" + entry.maxCount;
    }

    private static String summarizeJsonValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("value")) {
                return summarizeJsonValue(object.get("value"));
            }
            if (object.has("min_inclusive") && object.has("max_inclusive")) {
                return summarizeJsonValue(object.get("min_inclusive")) + " 到 " + summarizeJsonValue(object.get("max_inclusive"));
            }
            if (object.has("min") && object.has("max")) {
                return summarizeJsonValue(object.get("min")) + " 到 " + summarizeJsonValue(object.get("max"));
            }
        }
        return "";
    }

    private static String summarizeVerticalAnchor(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (!element.isJsonObject()) {
            return "?";
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("absolute")) {
            return object.get("absolute").getAsString();
        }
        if (object.has("above_bottom")) {
            return "世界底部+" + object.get("above_bottom").getAsString();
        }
        if (object.has("below_top")) {
            return "世界顶部-" + object.get("below_top").getAsString();
        }
        return object.toString();
    }

    private static void addFluidState(FluidState state, Set<String> target) {
        if (state != null && !state.isEmpty()) {
            addBlockState(state.createLegacyBlock(), target);
        }
    }

    private static void addBlockState(BlockState state, Set<String> target) {
        if (state != null) {
            addBlock(state.getBlock(), target);
        }
    }

    private static void addBlock(Block block, Set<String> target) {
        if (block == null || block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR || block.defaultBlockState().is(BIOME_BLOCK_BLACKLIST)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            target.add(id.toString());
        }
    }

    private static java.util.List<String> sortedList(Set<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
