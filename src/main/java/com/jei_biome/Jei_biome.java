package com.jei_biome;

import com.jei_biome.client.BiomeIndexClientHandler;
import com.jei_biome.data.BiomeBlockIndexCacheLoader;
import com.jei_biome.export.BiomeBlockIndexExporter;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(Jei_biome.MODID)
public class Jei_biome {

    public static final String MODID = "jei_biome";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Jei_biome() {
        NeoForge.EVENT_BUS.register(this);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(BiomeIndexClientHandler.class);
        }
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
