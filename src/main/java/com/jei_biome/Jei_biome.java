package com.jei_biome;

import com.jei_biome.data.BiomeBlockIndexCacheLoader;
import com.jei_biome.export.BiomeBlockIndexExporter;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;

public final class Jei_biome implements ModInitializer {

    public static final String MODID = "jei_biome";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                BiomeBlockIndexExporter.export(server);
                BiomeBlockIndexCacheLoader.load();
                LOGGER.info("Automatically collected biome index after server startup");
            } catch (Exception exception) {
                LOGGER.error("Automatic biome index collection failed", exception);
            }
        });
    }
}
