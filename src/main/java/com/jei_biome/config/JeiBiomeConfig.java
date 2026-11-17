package com.jei_biome.config;

import com.jei_biome.Jei_biome;
import com.jei_biome.data.BiomeBlockIndexPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JeiBiomeConfig {

    private static final String DEFAULT_CONTENT = """
            # JEI Biome 功能开关 / JEI Biome feature toggles
            # 修改后需要重启游戏才能生效 / Restart the game after changing these options

            # 是否显示群系自身/地表候选方块 / Whether to display biome terrain/surface candidate blocks
            show_terrain_blocks=true

            # 是否显示地表地物方块 / Whether to display surface feature blocks
            show_surface_feature_blocks=true

            # 是否显示地下地物方块 / Whether to display underground feature blocks
            show_underground_feature_blocks=true

            # 是否显示矿石方块及矿物分布提示 / Whether to display ore blocks and ore distribution tooltips
            show_ore_blocks=true

            # 是否显示生物生成分类、刷怪蛋和生物掉落物 / Whether to display mob spawning, spawn eggs, and mob drops
            show_mob_spawns=true
            """;

    // 使用时间戳缓存实现热重载：修改配置文件后重新导出群系索引即可生效，无需重启游戏。
    private static volatile CachedSettings cachedSettings;

    private JeiBiomeConfig() {
    }

    private static Settings current() {
        Path path = BiomeBlockIndexPaths.getConfigPath();
        long lastModified = 0L;
        try {
            if (Files.exists(path)) {
                lastModified = Files.getLastModifiedTime(path).toMillis();
            }
        } catch (Exception ignored) {}
        CachedSettings snapshot = cachedSettings;
        if (snapshot != null && snapshot.lastModified == lastModified) {
            return snapshot.settings;
        }
        synchronized (JeiBiomeConfig.class) {
            snapshot = cachedSettings;
            if (snapshot == null || snapshot.lastModified != lastModified) {
                Settings settings = load();
                cachedSettings = new CachedSettings(settings, lastModified);
            }
            return cachedSettings.settings;
        }
    }

    public static void ensureLoaded() {
        current();
    }

    // 返回群系自身/地表候选方块开关状态。
    public static boolean showTerrainBlocks() {
        return current().showTerrainBlocks;
    }

    // 返回地表地物方块开关状态。
    public static boolean showSurfaceFeatureBlocks() {
        return current().showSurfaceFeatureBlocks;
    }

    // 返回地下地物方块开关状态。
    public static boolean showUndergroundFeatureBlocks() {
        return current().showUndergroundFeatureBlocks;
    }

    // 返回矿石方块开关状态。
    public static boolean showOreBlocks() {
        return current().showOreBlocks;
    }

    // 返回生物生成分类开关状态。
    public static boolean showMobSpawns() {
        return current().showMobSpawns;
    }

    // 读取配置文件；首次运行时写入带中英文注释的默认配置。
    private static Settings load() {
        Path path = BiomeBlockIndexPaths.getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.writeString(path, DEFAULT_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                return Settings.defaults();
            }
            Map<String, Boolean> values = parse(Files.readAllLines(path, StandardCharsets.UTF_8));
            return new Settings(
                    values.getOrDefault("show_terrain_blocks", true),
                    values.getOrDefault("show_surface_feature_blocks", true),
                    values.getOrDefault("show_underground_feature_blocks", true),
                    values.getOrDefault("show_ore_blocks", true),
                    values.getOrDefault("show_mob_spawns", true)
            );
        } catch (Exception exception) {
            Jei_biome.LOGGER.error("Failed to load JEI Biome feature configuration: {}", path, exception);
            return Settings.defaults();
        }
    }

    // 解析简单的键值配置，未知键和非法值会被忽略并使用默认值。
    private static Map<String, Boolean> parse(List<String> lines) {
        Map<String, Boolean> values = new HashMap<>();
        for (String line : lines) {
            String content = line.trim();
            if (content.isEmpty() || content.startsWith("#")) {
                continue;
            }
            int separator = content.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = content.substring(0, separator).trim();
            String value = content.substring(separator + 1).trim();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                values.put(key, Boolean.parseBoolean(value));
            }
        }
        return values;
    }

    private record Settings(
            boolean showTerrainBlocks,
            boolean showSurfaceFeatureBlocks,
            boolean showUndergroundFeatureBlocks,
            boolean showOreBlocks,
            boolean showMobSpawns
    ) {
        // 创建所有功能默认开启的配置快照。
        private static Settings defaults() {
            return new Settings(true, true, true, true, true);
        }
    }

    private record CachedSettings(Settings settings, long lastModified) {
    }
}
