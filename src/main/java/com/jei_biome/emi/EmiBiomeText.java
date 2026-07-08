package com.jei_biome.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

final class EmiBiomeText {

    static final int RECIPE_WIDTH = 172;
    static final int RECIPE_HEIGHT = 170;
    static final int CONTENT_Y = 16;
    static final int CONTENT_HEIGHT = 150;
    static final int TEXT_WIDTH = 164;
    static final int SLOT_SIZE = 18;
    static final int GRID_COLUMNS = 8;
    static final int LINE_HEIGHT = 10;
    static final int SECTION_GAP = 8;
    static final int TITLE_COLOR = 0xFF333333;
    static final int SECTION_COLOR = 0xFF555555;
    static final int EMPTY_COLOR = 0xFF888888;

    private EmiBiomeText() {
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    static List<FormattedCharSequence> split(Component text) {
        return font().split(text, TEXT_WIDTH);
    }

    static int wrappedHeight(Component text) {
        return Math.max(12, split(text).size() * LINE_HEIGHT + 2);
    }

    static Component biomeName(String rawBiomeId) {
        ResourceLocation biomeId = ResourceLocation.tryParse(rawBiomeId);
        if (biomeId != null) {
            String translationKey = biomeId.toLanguageKey("biome");
            if (I18n.exists(translationKey)) {
                return Component.translatable(translationKey);
            }
        }
        return Component.literal(rawBiomeId);
    }

    static Component translateOrLiteral(String translationKey, String fallback) {
        if (I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }
        if (fallback != null && I18n.exists(fallback)) {
            return Component.translatable(fallback);
        }
        return Component.literal(cleanFallbackText(fallback));
    }

    static String cleanFallbackText(String fallback) {
        if (fallback == null || fallback.isBlank()) {
            return "";
        }
        String value = fallback;
        if (value.startsWith("category.")) {
            value = value.substring("category.".length());
        }
        int colonIndex = value.indexOf(':');
        if (colonIndex >= 0 && colonIndex < value.length() - 1) {
            value = value.substring(colonIndex + 1);
        }
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < value.length() - 1) {
            value = value.substring(dotIndex + 1);
        }
        return value.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
