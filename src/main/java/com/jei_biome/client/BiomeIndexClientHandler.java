package com.jei_biome.client;

import com.jei_biome.Jei_biome;
import com.jei_biome.jei.JeiBiomePlugin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class BiomeIndexClientHandler {

    private BiomeIndexClientHandler() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        JeiBiomePlugin.refreshRuntimeRecipes();
    }
}
