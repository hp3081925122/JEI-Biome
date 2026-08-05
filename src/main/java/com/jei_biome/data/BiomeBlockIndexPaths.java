package com.jei_biome.data;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class BiomeBlockIndexPaths {

    private BiomeBlockIndexPaths() {
    }

    public static Path getCachePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("jei_biome").resolve("biome_blocks.json");
    }
}
