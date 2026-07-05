package com.jei_biome.data;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class BiomeBlockIndexPaths {

    private BiomeBlockIndexPaths() {
    }

    public static Path getCachePath() {
        return FMLPaths.CONFIGDIR.get().resolve("jei_biome").resolve("biome_blocks.json");
    }
}
