package com.jei_biome.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public final class BiomeBlockIndexCache {

    public static final int CURRENT_VERSION = 10;
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public int version = CURRENT_VERSION;
    public String generatedAt = "";
    public List<BiomeEntry> biomes = new ArrayList<>();

    public static final class BiomeEntry {
        public String biomeId = "";
        public int placedFeatureCount;
        public List<String> placedFeatureIds = new ArrayList<>();
        public List<String> configuredFeatureIds = new ArrayList<>();
        public List<String> terrainBlocks = new ArrayList<>();
        public List<String> surfaceFeatureBlocks = new ArrayList<>();
        public List<String> undergroundFeatureBlocks = new ArrayList<>();
        public List<String> oreBlocks = new ArrayList<>();
        public List<OreDistributionEntry> oreDistributions = new ArrayList<>();
        public List<MobSpawnEntry> mobSpawns = new ArrayList<>();
        public List<String> featureBlocks = new ArrayList<>();
        public List<String> allBlocks = new ArrayList<>();
    }

    public static final class MobSpawnEntry {
        public String entityId = "";
        public String category = "";
        public int weight;
        public int minCount;
        public int maxCount;
        public String placementType = "";
        public String heightmapType = "";
        public boolean hasPlacement;
        public String spawnCharge = "";
        public String spawnEnergyBudget = "";
        public List<String> dropItems = new ArrayList<>();
    }

    public static final class OreDistributionEntry {
        public String blockId = "";
        public List<OreDistributionLine> lines = new ArrayList<>();
    }

    public static final class OreDistributionLine {
        public String sourceId = "";
        public String sourceKind = "";
        public int veinSize;
        public String attemptsPerChunk = "";
        public String averageEveryChunks = "";
        public String minHeight = "";
        public String maxHeight = "";
        public String heightMode = "";
    }
}
