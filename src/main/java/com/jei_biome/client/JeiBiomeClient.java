package com.jei_biome.client;

import com.jei_biome.Jei_biome;
import com.jei_biome.jei.JeiBiomePlugin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class JeiBiomeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            JeiBiomePlugin.refreshRuntimeRecipes();
            reloadEmiRecipes();
        }));
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
