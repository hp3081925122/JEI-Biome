package com.jei_biome.export;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jei_biome.Jei_biome;
import com.jei_biome.data.BiomeBlockIndexCache;
import com.jei_biome.data.BiomeBlockIndexPaths;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import net.minecraft.world.level.levelgen.feature.BlockPileFeature;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.DeltaFeature;
import net.minecraft.world.level.levelgen.feature.DiskFeature;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.IcebergFeature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import net.minecraft.world.level.levelgen.feature.OverlayFeature;
import net.minecraft.world.level.levelgen.feature.ProjectedRandomPatchySquare;
import net.minecraft.world.level.levelgen.feature.RandomBooleanSelectorFeature;
import net.minecraft.world.level.levelgen.feature.RandomNeighborSpreadFeature;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.ReplaceBlobsFeature;
import net.minecraft.world.level.levelgen.feature.ReplaceBlockFeature;
import net.minecraft.world.level.levelgen.feature.RootSystemFeature;
import net.minecraft.world.level.levelgen.feature.SequenceFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.SimpleRandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.VegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.WeightedRandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomBlockProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.Tags;

import java.io.BufferedReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BiomeBlockIndexExporter {

    private static final int MAX_FEATURE_DEPTH = 16;
    public static final TagKey<Block> BIOME_BLOCK_BLACKLIST = BlockTags.create(Identifier.fromNamespaceAndPath("jei_biome", "biome_block_blacklist"));
    private static final Field VEGETATION_GROUND_STATE_FIELD = resolveVegetationField("groundState");
    private static final Field VEGETATION_FEATURE_FIELD = resolveVegetationField("vegetationFeature");

    private BiomeBlockIndexExporter() {
    }

    private static Field resolveVegetationField(String fieldName) {
        try {
            Field field = VegetationPatchFeature.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception exception) {
            Jei_biome.LOGGER.error("Failed to access VegetationPatchFeature.{}", fieldName, exception);
            return null;
        }
    }

    public static Path export(MinecraftServer server) throws Exception {
        Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Registry<PlacedFeature> placedFeatureRegistry = server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Registry<Feature> featureRegistry = server.registryAccess().lookupOrThrow(Registries.FEATURE);
        RegistryOps<JsonElement> registryOps = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        BiomeBlockIndexCache cache = new BiomeBlockIndexCache();
        cache.generatedAt = Instant.now().toString();
        for (var biomeEntry : biomeRegistry.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString())).toList()) {
            BiomeBlockIndexCache.BiomeEntry exportedBiome = new BiomeBlockIndexCache.BiomeEntry();
            exportedBiome.biomeId = biomeEntry.getKey().identifier().toString();
            LinkedHashSet<String> placedFeatureIds = new LinkedHashSet<>();
            LinkedHashSet<String> configuredFeatureIds = new LinkedHashSet<>();
            LinkedHashSet<String> terrainBlocks = new LinkedHashSet<>();
            LinkedHashSet<String> surfaceFeatureBlocks = new LinkedHashSet<>();
            LinkedHashSet<String> undergroundFeatureBlocks = new LinkedHashSet<>();
            LinkedHashSet<String> oreBlocks = new LinkedHashSet<>();
            Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines = new LinkedHashMap<>();
            List<BiomeBlockIndexCache.MobSpawnEntry> mobSpawns = new ArrayList<>();
            Set<PlacedFeature> visitedPlacedFeatures = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<Feature> visitedFeatures = Collections.newSetFromMap(new IdentityHashMap<>());
            collectMobSpawns(server, biomeEntry.getValue(), mobSpawns);
            List<HolderSet<PlacedFeature>> featureSteps = biomeEntry.getValue().getGenerationSettings().features();
            GenerationStep.Decoration[] decorationSteps = GenerationStep.Decoration.values();
            for (int stepIndex = 0; stepIndex < featureSteps.size(); stepIndex++) {
                HolderSet<PlacedFeature> stepFeatures = featureSteps.get(stepIndex);
                GenerationStep.Decoration decorationStep = stepIndex < decorationSteps.length ? decorationSteps[stepIndex] : GenerationStep.Decoration.RAW_GENERATION;
                for (Holder<PlacedFeature> placedFeatureHolder : stepFeatures) {
                    PlacedFeature placedFeature = placedFeatureHolder.value();
                    exportedBiome.placedFeatureCount++;
                    placedFeatureHolder.unwrapKey().ifPresent(key -> placedFeatureIds.add(key.identifier().toString()));
                    placedFeatureRegistry.getResourceKey(placedFeature).ifPresent(key -> placedFeatureIds.add(key.identifier().toString()));
                    String placedFeatureId = placedFeatureHolder.unwrapKey().map(key -> key.identifier().toString())
                            .or(() -> placedFeatureRegistry.getResourceKey(placedFeature).map(key -> key.identifier().toString()))
                            .orElse("unknown");
                    collectPlacedFeatureBlocks(placedFeature, featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, 0);
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
        MobSpawnSettings mobSettings = biome.getModifiedEnvironmentAttributes()
                .applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, EnvironmentAttributes.NATURAL_MOB_SPAWNS.defaultValue());
        for (MobCategory category : mobSettings.definedCategories()) {
            for (Weighted<MobSpawnSettings.SpawnerData> weightedSpawner : mobSettings.getMobsToSpawn(category).unwrap()) {
                MobSpawnSettings.SpawnerData spawnerData = weightedSpawner.value();
                Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(spawnerData.type());
                if (entityId == null) {
                    continue;
                }
                BiomeBlockIndexCache.MobSpawnEntry entry = new BiomeBlockIndexCache.MobSpawnEntry();
                entry.entityId = entityId.toString();
                entry.category = category.getSerializedName();
                entry.weight = weightedSpawner.weight();
                entry.minCount = spawnerData.count().minInclusive();
                entry.maxCount = spawnerData.count().maxInclusive();
                entry.placementType = summarizeSpawnPlacementType(SpawnPlacements.getPlacementType(spawnerData.type()));
                entry.heightmapType = SpawnPlacements.getHeightmapType(spawnerData.type()).getSerializedName();
                entry.hasPlacement = SpawnPlacements.hasPlacement(spawnerData.type());
                MobSpawnSettings.MobSpawnCost spawnCost = mobSettings.getMobSpawnCost(spawnerData.type());
                if (spawnCost != null) {
                    entry.spawnCharge = formatDouble(spawnCost.charge());
                    entry.spawnEnergyBudget = formatDouble(spawnCost.energyBudget());
                }
                entry.dropItems = collectEntityDropItems(server, spawnerData.type().getDefaultLootTable().orElse(null));
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
        if (lootTableId == null || "minecraft:empty".equals(lootTableId.identifier().toString())) {
            return List.of();
        }
        LinkedHashSet<String> itemIds = new LinkedHashSet<>();
        // 使用访问集合防止循环引用
        collectLootTableItems(server, lootTableId.identifier(), itemIds, new LinkedHashSet<>());
        return sortedList(itemIds);
    }

    // 递归收集战利品表中的物品，支持跨表引用（minecraft:loot_table 类型条目）。
    private static void collectLootTableItems(MinecraftServer server, Identifier lootLocation, Set<String> itemIds, Set<String> visitedTables) {
        if (!visitedTables.add(lootLocation.toString())) {
            return;
        }
        Identifier resourceId = Identifier.fromNamespaceAndPath(lootLocation.getNamespace(), "loot_table/" + lootLocation.getPath() + ".json");
        server.getResourceManager().getResource(resourceId).ifPresent(resource -> {
            try (BufferedReader reader = resource.openAsReader()) {
                collectItemEntries(JsonParser.parseReader(reader), itemIds, server, visitedTables);
            } catch (Exception ignored) {}
        });
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

    private static void collectItemEntries(JsonElement element, Set<String> itemIds, MinecraftServer server, Set<String> visitedTables) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectItemEntries(child, itemIds, server, visitedTables);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("type")) {
            String type = object.get("type").getAsString();
            if ("minecraft:item".equals(type) && object.has("name")) {
                Identifier itemId = Identifier.tryParse(object.get("name").getAsString());
                Item item = itemId == null ? null : BuiltInRegistries.ITEM.getValue(itemId);
                if (item != null) {
                    itemIds.add(itemId.toString());
                }
            } else if ("minecraft:loot_table".equals(type) && object.has("value")) {
                JsonElement valueElement = object.get("value");
                if (valueElement.isJsonPrimitive()) {
                    Identifier refLocation = Identifier.tryParse(valueElement.getAsString());
                    if (refLocation != null) {
                        collectLootTableItems(server, refLocation, itemIds, visitedTables);
                    }
                }
            }
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectItemEntries(entry.getValue(), itemIds, server, visitedTables);
        }
    }

    private static void collectPlacedFeatureBlocks(
            PlacedFeature placedFeature,
            Registry<Feature> featureRegistry,
            RegistryOps<JsonElement> registryOps,
            Set<String> terrainBlocks,
            Set<String> surfaceFeatureBlocks,
            Set<String> undergroundFeatureBlocks,
            Set<String> oreBlocks,
            Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines,
            Set<String> configuredFeatureIds,
            Set<PlacedFeature> visitedPlacedFeatures,
            Set<Feature> visitedFeatures,
            GenerationStep.Decoration decorationStep,
            String placedFeatureId,
            int depth
    ) {
        if (placedFeature == null || !visitedPlacedFeatures.add(placedFeature)) {
            return;
        }
        if (depth > MAX_FEATURE_DEPTH) {
            Jei_biome.LOGGER.warn("Biome block index: placed feature nesting exceeded max depth {} at {}, some blocks may be missing", MAX_FEATURE_DEPTH, placedFeatureId);
            return;
        }
        Holder<Feature> featureHolder = placedFeature.feature();
        Feature feature = featureHolder.value();
        featureHolder.unwrapKey().ifPresent(key -> configuredFeatureIds.add(key.identifier().toString()));
        featureRegistry.getResourceKey(feature).ifPresent(key -> configuredFeatureIds.add(key.identifier().toString()));
        collectFeatureBlocks(feature, placedFeature, featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
    }

    @SuppressWarnings("unchecked")
    private static void collectFeatureBlocks(
            Feature feature,
            PlacedFeature sourcePlacedFeature,
            Registry<Feature> featureRegistry,
            RegistryOps<JsonElement> registryOps,
            Set<String> terrainBlocks,
            Set<String> surfaceFeatureBlocks,
            Set<String> undergroundFeatureBlocks,
            Set<String> oreBlocks,
            Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines,
            Set<String> configuredFeatureIds,
            Set<PlacedFeature> visitedPlacedFeatures,
            Set<Feature> visitedFeatures,
            GenerationStep.Decoration decorationStep,
            String placedFeatureId,
            int depth
    ) {
        if (feature == null || !visitedFeatures.add(feature)) {
            return;
        }
        if (depth > MAX_FEATURE_DEPTH) {
            Jei_biome.LOGGER.warn("Biome block index: feature nesting exceeded max depth {} at {}, some blocks may be missing", MAX_FEATURE_DEPTH, placedFeatureId);
            return;
        }
        Set<String> stepTarget = resolveFeatureTarget(decorationStep, surfaceFeatureBlocks, undergroundFeatureBlocks);
        if (feature instanceof AbstractOreFeature oreFeature) {
            for (BlockReplacement targetState : oreFeature.targetStates()) {
                addBlockState(targetState.state(), oreBlocks);
                addOreDistributionLine(targetState.state(), sourcePlacedFeature, registryOps, placedFeatureId, oreFeature.size(), oreDistributionLines);
            }
        } else if (feature instanceof ReplaceBlockFeature replaceBlockFeature) {
            for (BlockReplacement replacement : replaceBlockFeature.replacements()) {
                addBlockState(replacement.state(), stepTarget);
            }
        } else if (feature instanceof ReplaceBlobsFeature replaceBlobsFeature) {
            addBlockState(replaceBlobsFeature.replaceState(), stepTarget);
        } else if (feature instanceof SimpleBlockFeature simpleBlockFeature) {
            addProviderBlock(registryOps, simpleBlockFeature.toPlace(), stepTarget);
        } else if (feature instanceof ProjectedRandomPatchySquare patchySquare) {
            addProviderBlock(registryOps, patchySquare.block(), stepTarget);
        } else if (feature instanceof RandomNeighborSpreadFeature neighborSpread) {
            addProviderBlock(registryOps, neighborSpread.block(), stepTarget);
        } else if (feature instanceof BlockColumnFeature blockColumnFeature) {
            for (BlockColumnFeature.Layer layer : blockColumnFeature.layers()) {
                addProviderBlock(registryOps, layer.state(), stepTarget);
            }
        } else if (feature instanceof BlockBlobFeature blockBlobFeature) {
            addBlockState(blockBlobFeature.state(), stepTarget);
        } else if (feature instanceof IcebergFeature icebergFeature) {
            addBlockState(icebergFeature.state(), surfaceFeatureBlocks);
        } else if (feature instanceof TreeFeature treeFeature) {
            addProviderBlock(registryOps, treeFeature.trunkProvider(), surfaceFeatureBlocks);
            addProviderBlock(registryOps, treeFeature.belowTrunkProvider(), terrainBlocks);
            addProviderBlock(registryOps, treeFeature.foliageProvider(), surfaceFeatureBlocks);
        } else if (feature instanceof HugeFungusFeature hugeFungusFeature) {
            addBlockState(hugeFungusFeature.validBaseState(), terrainBlocks);
            addBlockState(hugeFungusFeature.stemState(), surfaceFeatureBlocks);
            addBlockState(hugeFungusFeature.hatState(), surfaceFeatureBlocks);
            addBlockState(hugeFungusFeature.decorState(), surfaceFeatureBlocks);
        } else if (feature instanceof AbstractHugeMushroomFeature mushroomFeature) {
            addProviderBlock(registryOps, mushroomFeature.capProvider(), surfaceFeatureBlocks);
            addProviderBlock(registryOps, mushroomFeature.stemProvider(), surfaceFeatureBlocks);
        } else if (feature instanceof BlockPileFeature blockPileFeature) {
            addProviderBlock(registryOps, blockPileFeature.stateProvider(), stepTarget);
        } else if (feature instanceof DiskFeature diskFeature) {
            addProviderBlock(registryOps, diskFeature.stateProvider(), terrainBlocks);
        } else if (feature instanceof DeltaFeature deltaFeature) {
            addBlockState(deltaFeature.contents(), stepTarget);
            addBlockState(deltaFeature.rim(), stepTarget);
        } else if (feature instanceof SpringFeature springFeature) {
            addFluidState(springFeature.state(), stepTarget);
        } else if (feature instanceof LakeFeature lakeFeature) {
            addProviderBlock(registryOps, lakeFeature.fluid(), stepTarget);
            addProviderBlock(registryOps, lakeFeature.barrier(), stepTarget);
        } else if (feature instanceof MultifaceGrowthFeature multifaceGrowthFeature) {
            addBlock(multifaceGrowthFeature.placeBlock(), stepTarget);
        } else if (feature instanceof FallenTreeFeature fallenTreeFeature) {
            addProviderBlock(registryOps, fallenTreeFeature.trunkProvider(), surfaceFeatureBlocks);
        } else if (feature instanceof GeodeFeature geodeFeature) {
            GeodeBlockSettings settings = geodeFeature.blockSettings();
            addProviderOreOrUndergroundBlock(registryOps, settings.fillingProvider(), oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(registryOps, settings.innerLayerProvider(), oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(registryOps, settings.alternateInnerLayerProvider(), oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(registryOps, settings.middleLayerProvider(), oreBlocks, undergroundFeatureBlocks);
            addProviderOreOrUndergroundBlock(registryOps, settings.outerLayerProvider(), oreBlocks, undergroundFeatureBlocks);
            for (BlockState state : settings.innerPlacements()) {
                addOreOrUndergroundBlockState(state, oreBlocks, undergroundFeatureBlocks);
                addOreDistributionLine(state, sourcePlacedFeature, registryOps, placedFeatureId, 0, oreDistributionLines);
            }
        } else if (feature instanceof VegetationPatchFeature vegetationPatchFeature) {
            try {
                if (VEGETATION_GROUND_STATE_FIELD != null) {
                    Holder<BlockStateProvider> groundState = (Holder<BlockStateProvider>) VEGETATION_GROUND_STATE_FIELD.get(vegetationPatchFeature);
                    addProviderBlock(registryOps, groundState, terrainBlocks);
                }
                if (VEGETATION_FEATURE_FIELD != null) {
                    Holder<PlacedFeature> vegFeature = (Holder<PlacedFeature>) VEGETATION_FEATURE_FIELD.get(vegetationPatchFeature);
                    if (vegFeature != null) {
                        collectPlacedFeatureBlocks(vegFeature.value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
                    }
                }
            } catch (Exception exception) {
                Jei_biome.LOGGER.warn("Failed to read VegetationPatchFeature at {}", placedFeatureId, exception);
            }
        } else if (feature instanceof RootSystemFeature rootSystemFeature) {
            addProviderBlock(registryOps, rootSystemFeature.rootStateProvider(), undergroundFeatureBlocks);
            addProviderBlock(registryOps, rootSystemFeature.hangingRootStateProvider(), undergroundFeatureBlocks);
            collectPlacedFeatureBlocks(rootSystemFeature.treeFeature().value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
        } else if (feature instanceof RandomSelectorFeature randomSelectorFeature) {
            for (WeightedPlacedFeature weightedPlacedFeature : randomSelectorFeature.features()) {
                collectPlacedFeatureBlocks(weightedPlacedFeature.feature().value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
            }
            collectPlacedFeatureBlocks(randomSelectorFeature.defaultFeature().value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
        } else if (feature instanceof WeightedRandomSelectorFeature weightedRandomSelectorFeature) {
            for (Weighted<Holder<PlacedFeature>> weightedFeature : weightedRandomSelectorFeature.features().unwrap()) {
                collectPlacedFeatureBlocks(weightedFeature.value().value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
            }
        } else if (feature instanceof SimpleRandomSelectorFeature simpleRandomSelectorFeature) {
            for (Holder<PlacedFeature> featureHolder : simpleRandomSelectorFeature.features()) {
                collectPlacedFeatureBlocks(featureHolder.value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
            }
        } else if (feature instanceof SequenceFeature sequenceFeature) {
            for (Holder<PlacedFeature> featureHolder : sequenceFeature.features()) {
                collectPlacedFeatureBlocks(featureHolder.value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
            }
        } else if (feature instanceof OverlayFeature overlayFeature) {
            for (Holder<PlacedFeature> featureHolder : overlayFeature.features()) {
                collectPlacedFeatureBlocks(featureHolder.value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
            }
        } else if (feature instanceof RandomBooleanSelectorFeature randomBooleanSelectorFeature) {
            collectPlacedFeatureBlocks(randomBooleanSelectorFeature.featureTrue().value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
            collectPlacedFeatureBlocks(randomBooleanSelectorFeature.featureFalse().value(), featureRegistry, registryOps, terrainBlocks, surfaceFeatureBlocks, undergroundFeatureBlocks, oreBlocks, oreDistributionLines, configuredFeatureIds, visitedPlacedFeatures, visitedFeatures, decorationStep, placedFeatureId, depth + 1);
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

    private static void addProviderBlock(RegistryOps<JsonElement> registryOps, Holder<BlockStateProvider> providerHolder, Set<String> target) {
        addProviderBlockRecursive(registryOps, providerHolder, target, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void addProviderBlockRecursive(RegistryOps<JsonElement> registryOps, Holder<BlockStateProvider> providerHolder, Set<String> target, Set<BlockStateProvider> visited) {
        if (providerHolder == null) {
            return;
        }
        BlockStateProvider provider = providerHolder.value();
        if (provider == null || !visited.add(provider)) {
            return;
        }
        if (provider instanceof RuleBasedStateProvider ruleProvider) {
            if (ruleProvider.fallback() != null) {
                addProviderBlockRecursive(registryOps, ruleProvider.fallback(), target, visited);
            }
            for (RuleBasedStateProvider.Rule rule : ruleProvider.rules()) {
                addProviderBlockRecursive(registryOps, rule.then(), target, visited);
            }
            return;
        }
        if (provider instanceof RandomBlockProvider randomBlockProvider) {
            for (Holder<Block> blockHolder : randomBlockProvider.blocks()) {
                addBlock(blockHolder.value(), target);
            }
            return;
        }
        BlockStateProvider.DIRECT_CODEC.encodeStart(registryOps, provider).result().ifPresent(element -> collectProviderBlocks(element, target));
    }

    private static void addProviderOreOrUndergroundBlock(RegistryOps<JsonElement> registryOps, Holder<BlockStateProvider> providerHolder, Set<String> oreBlocks, Set<String> undergroundFeatureBlocks) {
        addProviderOreRecursive(registryOps, providerHolder, oreBlocks, undergroundFeatureBlocks, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void addProviderOreRecursive(RegistryOps<JsonElement> registryOps, Holder<BlockStateProvider> providerHolder, Set<String> oreBlocks, Set<String> undergroundFeatureBlocks, Set<BlockStateProvider> visited) {
        if (providerHolder == null) {
            return;
        }
        BlockStateProvider provider = providerHolder.value();
        if (provider == null || !visited.add(provider)) {
            return;
        }
        if (provider instanceof RuleBasedStateProvider ruleProvider) {
            if (ruleProvider.fallback() != null) {
                addProviderOreRecursive(registryOps, ruleProvider.fallback(), oreBlocks, undergroundFeatureBlocks, visited);
            }
            for (RuleBasedStateProvider.Rule rule : ruleProvider.rules()) {
                addProviderOreRecursive(registryOps, rule.then(), oreBlocks, undergroundFeatureBlocks, visited);
            }
            return;
        }
        if (provider instanceof RandomBlockProvider randomBlockProvider) {
            for (Holder<Block> blockHolder : randomBlockProvider.blocks()) {
                addOreOrUndergroundBlockState(blockHolder.value().defaultBlockState(), oreBlocks, undergroundFeatureBlocks);
            }
            return;
        }
        BlockStateProvider.DIRECT_CODEC.encodeStart(registryOps, provider).result().ifPresent(element -> collectProviderOreBlocks(element, oreBlocks, undergroundFeatureBlocks));
    }

    private static void collectProviderBlocks(JsonElement element, Set<String> target) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            Identifier id = Identifier.tryParse(element.getAsString());
            Block block = id == null ? null : BuiltInRegistries.BLOCK.getValue(id);
            if (block != null) {
                addBlock(block, target);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectProviderBlocks(child, target);
            }
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectProviderBlocks(entry.getValue(), target);
            }
        }
    }

    private static void collectProviderOreBlocks(JsonElement element, Set<String> oreBlocks, Set<String> undergroundFeatureBlocks) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            Identifier id = Identifier.tryParse(element.getAsString());
            Block block = id == null ? null : BuiltInRegistries.BLOCK.getValue(id);
            if (block != null) {
                addOreOrUndergroundBlockState(block.defaultBlockState(), oreBlocks, undergroundFeatureBlocks);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectProviderOreBlocks(child, oreBlocks, undergroundFeatureBlocks);
            }
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectProviderOreBlocks(entry.getValue(), oreBlocks, undergroundFeatureBlocks);
            }
        }
    }

    private static void addOreOrUndergroundBlockState(BlockState state, Set<String> oreBlocks, Set<String> undergroundFeatureBlocks) {
        if (state != null && state.is(Tags.BlockItems.ORES.block())) {
            addBlockState(state, oreBlocks);
        } else {
            addBlockState(state, undergroundFeatureBlocks);
        }
    }

    private static void addOreDistributionLine(BlockState state, PlacedFeature placedFeature, RegistryOps<JsonElement> registryOps, String placedFeatureId, int veinSize, Map<String, LinkedHashMap<String, BiomeBlockIndexCache.OreDistributionLine>> oreDistributionLines) {
        if (state == null || state.is(BIOME_BLOCK_BLACKLIST)) {
            return;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return;
        }
        BiomeBlockIndexCache.OreDistributionLine line = new BiomeBlockIndexCache.OreDistributionLine();
        line.sourceId = placedFeatureId;
        line.sourceKind = resolveOreSourceKind(placedFeatureId);
        line.veinSize = veinSize;
        if (placedFeature != null) {
            for (PlacementModifier modifier : placedFeature.placement()) {
                PlacementModifier.CODEC.encodeStart(registryOps, modifier)
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
        String lowerId = placedFeatureId.toLowerCase(Locale.ROOT);
        // 优先匹配更具体的模式，避免 _large_buried 被误判为 large
        if (lowerId.contains("_buried")) {
            return "buried";
        }
        if (lowerId.contains("_large")) {
            return "large";
        }
        if (lowerId.contains("_extra")) {
            return "extra";
        }
        if (lowerId.contains("_small")) {
            return "small";
        }
        if (lowerId.contains("_rare") || lowerId.contains("_uncommon") || lowerId.contains("_scattered")) {
            return "rare";
        }
        if (lowerId.contains("geode")) {
            return "geode";
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
            int offset = object.get("above_bottom").getAsInt();
            if (offset == 0) {
                return "世界底部";
            }
            return offset > 0 ? "世界底部+" + offset : "世界底部" + offset;
        }
        if (object.has("below_top")) {
            int offset = object.get("below_top").getAsInt();
            if (offset == 0) {
                return "世界顶部";
            }
            return offset > 0 ? "世界顶部-" + offset : "世界顶部+" + Math.abs(offset);
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
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            target.add(id.toString());
        }
    }

    private static List<String> sortedList(Set<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
