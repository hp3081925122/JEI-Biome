package com.jei_biome.jei;

import mezz.jei.common.Internal;
import mezz.jei.common.config.IClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

final class JeiBiomeLayout {
    private static final int SCREEN_MARGIN = 58;
    private static final int CENTER_SEARCH_BAR_SCREEN_MARGIN = 76;
    private static final int RECIPE_AREA_HEADER = 40;
    private static final int RECIPE_BORDER = 8;

    private JeiBiomeLayout() {
    }

    static int fitHeight(int preferredHeight, int minHeight) {
        return Mth.clamp(maxRecipeHeight(), minHeight, preferredHeight);
    }

    private static int maxRecipeHeight() {
        IClientConfig config = Internal.getClientConfigs().getClientConfig();
        int screenMargin = config.isCenterSearchBarEnabled() ? CENTER_SEARCH_BAR_SCREEN_MARGIN : SCREEN_MARGIN;
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight() - screenMargin;
        guiHeight = Math.min(Math.max(guiHeight, IClientConfig.minRecipeGuiHeight), config.getMaxRecipeGuiHeight());
        return guiHeight - RECIPE_AREA_HEADER - RECIPE_BORDER;
    }
}
