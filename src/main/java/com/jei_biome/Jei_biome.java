package com.jei_biome;

import com.jei_biome.command.BiomeExportCommands;
import com.jei_biome.data.BiomeBlockIndexCacheLoader;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class Jei_biome implements ModInitializer {

    public static final String MODID = "jei_biome";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BiomeExportCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (BiomeBlockIndexCacheLoader.load().biomes.isEmpty()) {
                handler.player.sendSystemMessage(Component.translatable("jei_biome.message.missing_biome_index"));
            }
        });
    }
}
