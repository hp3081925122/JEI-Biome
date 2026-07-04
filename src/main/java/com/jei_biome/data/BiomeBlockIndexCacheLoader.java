package com.jei_biome.data;

import com.jei_biome.Jei_biome;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BiomeBlockIndexCacheLoader {

    private static volatile CachedIndex cached;

    private BiomeBlockIndexCacheLoader() {
    }

    public static BiomeBlockIndexCache load() {
        Path path = BiomeBlockIndexPaths.getCachePath();
        long lastModified = 0L;
        try {
            if (Files.exists(path)) {
                lastModified = Files.getLastModifiedTime(path).toMillis();
            }
        } catch (Exception exception) {
            Jei_biome.LOGGER.error("Failed to read biome block index timestamp: {}", path, exception);
            lastModified = -1L;
        }
        CachedIndex snapshot = cached;
        if (snapshot != null && snapshot.lastModified == lastModified) {
            return snapshot.cache;
        }
        synchronized (BiomeBlockIndexCacheLoader.class) {
            snapshot = cached;
            if (snapshot == null || snapshot.lastModified != lastModified) {
                BiomeBlockIndexCache cache = new BiomeBlockIndexCache();
                if (Files.exists(path)) {
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        BiomeBlockIndexCache loaded = BiomeBlockIndexCache.GSON.fromJson(reader, BiomeBlockIndexCache.class);
                        if (loaded != null && loaded.version == BiomeBlockIndexCache.CURRENT_VERSION) {
                            cache = loaded;
                        }
                    } catch (Exception exception) {
                        Jei_biome.LOGGER.error("Failed to read biome block index: {}", path, exception);
                    }
                }
                cached = new CachedIndex(cache, lastModified);
            }
            return cached.cache;
        }
    }

    private record CachedIndex(BiomeBlockIndexCache cache, long lastModified) {
    }
}
