package com.jei_biome.client;

import com.jei_biome.Jei_biome;
import com.jei_biome.jei.JeiBiomePlugin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Jei_biome.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BiomeIndexClientHandler {

    private BiomeIndexClientHandler() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        JeiBiomePlugin.refreshRuntimeRecipes();
        reloadEmiRecipes();
    }

    private static void reloadEmiRecipes() {
        try {
            Class<?> reloadManager = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            reloadManager.getMethod("reloadRecipes").invoke(null);
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException exception) {
            Jei_biome.LOGGER.error("Automatic EMI recipe refresh failed", exception);
        }
    }
}
