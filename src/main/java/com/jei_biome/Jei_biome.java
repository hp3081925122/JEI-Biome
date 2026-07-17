package com.jei_biome;

import com.jei_biome.command.BiomeExportCommands;
import com.jei_biome.data.BiomeBlockIndexCacheLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@Mod(Jei_biome.MODID)
public class Jei_biome {

    public static final String MODID = "jei_biome";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Jei_biome() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BiomeExportCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!BiomeBlockIndexCacheLoader.load().biomes.isEmpty()) {
            return;
        }
        event.getEntity().sendSystemMessage(Component.translatable("jei_biome.message.missing_biome_index"));
    }
}
