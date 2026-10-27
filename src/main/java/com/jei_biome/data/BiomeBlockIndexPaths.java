package com.jei_biome.data;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class BiomeBlockIndexPaths {

    private BiomeBlockIndexPaths() {
    }

    public static Path getCachePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("jei_biome").resolve("biome_blocks.json");
    }

    // 返回 JEI Biome 功能开关配置文件路径。
    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("jei_biome").resolve("features.toml");
    }
}
