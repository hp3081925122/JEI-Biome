package com.jei_biome;

import com.jei_biome.data.BiomeBlockIndexCacheLoader;
import com.jei_biome.export.BiomeBlockIndexExporter;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Jei_biome.MODID)
public class Jei_biome {

    public static final String MODID = "jei_biome";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Jei_biome() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            BiomeBlockIndexExporter.export(event.getServer());
            BiomeBlockIndexCacheLoader.load();
            LOGGER.info("Automatically collected biome index after server startup");
        } catch (Exception exception) {
            LOGGER.error("Automatic biome index collection failed", exception);
        }
    }
}
