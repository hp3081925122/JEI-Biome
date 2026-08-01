package com.jei_biome.jei;

import com.jei_biome.Jei_biome;
import com.jei_biome.data.BiomeBlockIndexCache;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BiomeMobRecipeCategory implements IRecipeCategory<BiomeMobRecipe> {

    public static final RecipeType<BiomeMobRecipe> TYPE = RecipeType.create(Jei_biome.MODID, "biome_mobs", BiomeMobRecipe.class);

    private static final int WIDTH = 188;
    private static final int HEIGHT = 170;
    private static final int CONTENT_X = 4;
    private static final int CONTENT_Y = 16;
    private static final int CONTENT_WIDTH = 180;
    private static final int CONTENT_HEIGHT = 150;
    private static final int SCROLLBAR_WIDTH = 16;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_COLUMNS = 8;
    private static final int SECTION_TITLE_HEIGHT = 12;
    private static final int TITLE_LINE_HEIGHT = 10;
    private static final int SECTION_GAP = 8;
    private static final int CONTENT_PADDING_BOTTOM = 6;
    private static final int TEXT_WIDTH = CONTENT_WIDTH - SCROLLBAR_WIDTH - 8;
    private final IDrawable icon;

    public BiomeMobRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.CREEPER_SPAWN_EGG));
    }

    @Override
    public RecipeType<BiomeMobRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei_biome.category.biome_mobs");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Identifier getRegistryName(BiomeMobRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BiomeMobRecipe recipe, IFocusGroup focuses) {
        ItemStack focusedDropStack = getFocusedDropStack(recipe, focuses);
        for (BiomeMobRecipe.MobDisplayEntry entry : getVisibleDropSourceEntries(recipe, focusedDropStack)) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0)
                    .setStandardSlotBackground()
                    .addItemStack(entry.stack())
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> addMobEntryTooltip(tooltip, entry));
        }
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            if (!entry.stack().isEmpty()) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0)
                        .setStandardSlotBackground()
                        .addItemStack(entry.stack())
                        .addRichTooltipCallback((recipeSlotView, tooltip) -> addMobEntryTooltip(tooltip, entry));
            }
        }
        if (!recipe.lookupStacks().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.lookupStacks());
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.lookupStacks());
        }
    }

    @Override
    public void draw(BiomeMobRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.text(font, getBiomeName(recipe.entry().biomeId), 4, 4, 0xFF2B2B2B, false);
        drawPanel(guiGraphics, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BiomeMobRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> contentSlots = builder.getRecipeSlots().getSlots();
        BiomeMobScrollWidget widget = new BiomeMobScrollWidget(recipe, getFocusedDropStack(recipe, focuses), CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT, contentSlots);
        builder.addSlottedWidget(widget, contentSlots);
        builder.addInputHandler(widget);
    }

    static void drawScrollableContents(BiomeMobRecipe recipe, ItemStack focusedDropStack, GuiGraphicsExtractor guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int currentY = y + 4;
        List<BiomeMobRecipe.MobDisplayEntry> dropSourceEntries = getVisibleDropSourceEntries(recipe, focusedDropStack);
        if (!dropSourceEntries.isEmpty()) {
            Component title = getDropSourceTitle(focusedDropStack);
            currentY += drawWrappedText(guiGraphics, font, title, x + 4, currentY, 0xFF555555);
            int rows = Math.max(1, (dropSourceEntries.size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
            currentY += rows * SLOT_SIZE + SECTION_GAP;
        }
        String currentCategory = "";
        if (recipe.mobEntries().isEmpty()) {
            guiGraphics.text(font, Component.translatable("jei_biome.label.empty"), x + 8, currentY, 0xFF888888, false);
            return;
        }
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            if (!entry.data().category.equals(currentCategory)) {
                currentCategory = entry.data().category;
                currentY += drawWrappedText(guiGraphics, font, translateOrLiteral("jei_biome.mob_category." + currentCategory, currentCategory), x + 4, currentY, 0xFF555555);
            }
            if (isLastEntryInCategory(recipe, entry)) {
                currentY += getCategoryGridHeight(recipe, currentCategory) + SECTION_GAP;
            }
        }
    }

    static List<SlotPlacement> getSlotPlacements(BiomeMobRecipe recipe, ItemStack focusedDropStack) {
        Font font = Minecraft.getInstance().font;
        List<SlotPlacement> placements = new ArrayList<>();
        int currentY = 4;
        List<BiomeMobRecipe.MobDisplayEntry> dropSourceEntries = getVisibleDropSourceEntries(recipe, focusedDropStack);
        if (!dropSourceEntries.isEmpty()) {
            currentY += getTitleHeight(font, getDropSourceTitle(focusedDropStack));
            for (int index = 0; index < dropSourceEntries.size(); index++) {
                int column = index % GRID_COLUMNS;
                int row = index / GRID_COLUMNS;
                placements.add(new SlotPlacement(4 + column * SLOT_SIZE, currentY + row * SLOT_SIZE));
            }
            int rows = Math.max(1, (dropSourceEntries.size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
            currentY += rows * SLOT_SIZE + SECTION_GAP;
        }
        String currentCategory = "";
        int categoryIndex = 0;
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            if (!entry.data().category.equals(currentCategory)) {
                currentCategory = entry.data().category;
                currentY += getTitleHeight(font, translateOrLiteral("jei_biome.mob_category." + currentCategory, currentCategory));
                categoryIndex = 0;
            }
            if (!entry.stack().isEmpty()) {
                int column = categoryIndex % GRID_COLUMNS;
                int row = categoryIndex / GRID_COLUMNS;
                placements.add(new SlotPlacement(4 + column * SLOT_SIZE, currentY + row * SLOT_SIZE));
                categoryIndex++;
            }
            if (isLastEntryInCategory(recipe, entry)) {
                currentY += getCategoryGridHeight(recipe, currentCategory) + SECTION_GAP;
            }
        }
        return placements;
    }

    static int getTotalContentHeight(BiomeMobRecipe recipe, ItemStack focusedDropStack) {
        if (recipe.mobEntries().isEmpty()) {
            return 24;
        }
        Font font = Minecraft.getInstance().font;
        int height = 4;
        List<BiomeMobRecipe.MobDisplayEntry> dropSourceEntries = getVisibleDropSourceEntries(recipe, focusedDropStack);
        if (!dropSourceEntries.isEmpty()) {
            int rows = Math.max(1, (dropSourceEntries.size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
            height += getTitleHeight(font, getDropSourceTitle(focusedDropStack)) + rows * SLOT_SIZE + SECTION_GAP;
        }
        String currentCategory = "";
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            if (!entry.data().category.equals(currentCategory)) {
                currentCategory = entry.data().category;
                height += getTitleHeight(font, translateOrLiteral("jei_biome.mob_category." + currentCategory, currentCategory));
            }
            if (isLastEntryInCategory(recipe, entry)) {
                height += getCategoryGridHeight(recipe, currentCategory) + SECTION_GAP;
            }
        }
        return height + CONTENT_PADDING_BOTTOM;
    }

    static int getScrollbarWidth() {
        return SCROLLBAR_WIDTH;
    }

    private static Component getBiomeName(String rawBiomeId) {
        Identifier biomeId = Identifier.tryParse(rawBiomeId);
        if (biomeId != null) {
            String translationKey = biomeId.toLanguageKey("biome");
            if (I18n.exists(translationKey)) {
                return Component.translatable(translationKey);
            }
        }
        return Component.literal(rawBiomeId);
    }

    private static void addMobEntryTooltip(ITooltipBuilder tooltip, BiomeMobRecipe.MobDisplayEntry entry) {
        tooltip.add(Component.translatable("jei_biome.tooltip.mob_spawn").withStyle(ChatFormatting.GOLD));
        if (hasDropItems(entry)) {
            tooltip.add(Component.translatable("jei_biome.tooltip.mob_drop_lookup").withStyle(ChatFormatting.DARK_GREEN));
        }
        for (int index = 0; index < entry.spawns().size(); index++) {
            if (index > 0) {
                tooltip.add(Component.empty());
            }
            addMobSpawnTooltip(tooltip, entry.spawns().get(index));
        }
    }

    private static void addMobSpawnTooltip(ITooltipBuilder tooltip, BiomeBlockIndexCache.MobSpawnEntry entry) {
        tooltip.add(Component.translatable("jei_biome.tooltip.mob_category", translateOrLiteral("jei_biome.mob_category." + entry.category, entry.category)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("jei_biome.tooltip.mob_weight", entry.weight).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei_biome.tooltip.mob_group", entry.minCount, entry.maxCount).withStyle(ChatFormatting.GRAY));
        if (entry.spawnCharge != null && !entry.spawnCharge.isBlank() && entry.spawnEnergyBudget != null && !entry.spawnEnergyBudget.isBlank()) {
            tooltip.add(Component.translatable("jei_biome.tooltip.mob_spawn_cost", entry.spawnCharge, entry.spawnEnergyBudget).withStyle(ChatFormatting.GRAY));
        }
        if (!entry.hasPlacement) {
            tooltip.add(Component.translatable("jei_biome.tooltip.mob_no_placement").withStyle(ChatFormatting.DARK_GRAY));
        } else if ((entry.placementType != null && !entry.placementType.isBlank()) || (entry.heightmapType != null && !entry.heightmapType.isBlank())) {
            tooltip.add(Component.translatable("jei_biome.tooltip.mob_spawn_conditions").withStyle(ChatFormatting.DARK_GREEN));
            if (entry.placementType != null && !entry.placementType.isBlank()) {
                String placementKey = "jei_biome.mob_placement." + entry.placementType.toLowerCase(Locale.ROOT);
                tooltip.add(Component.translatable("jei_biome.tooltip.mob_placement", translateOrLiteral(placementKey, entry.placementType)).withStyle(ChatFormatting.GRAY));
            }
            if (entry.heightmapType != null && !entry.heightmapType.isBlank()) {
                String heightmapKey = "jei_biome.mob_heightmap." + entry.heightmapType.toLowerCase(Locale.ROOT);
                tooltip.add(Component.translatable("jei_biome.tooltip.mob_heightmap", translateOrLiteral(heightmapKey, entry.heightmapType)).withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("jei_biome.tooltip.mob_spawn_rules_note").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component translateOrLiteral(String translationKey, String fallback) {
        if (I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }
        if (fallback != null && I18n.exists(fallback)) {
            return Component.translatable(fallback);
        }
        return Component.literal(cleanFallbackText(fallback));
    }

    private static boolean hasDropItems(BiomeMobRecipe.MobDisplayEntry entry) {
        for (BiomeBlockIndexCache.MobSpawnEntry spawnEntry : entry.spawns()) {
            if (spawnEntry.dropItems != null && !spawnEntry.dropItems.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLastEntryInCategory(BiomeMobRecipe recipe, BiomeMobRecipe.MobDisplayEntry currentEntry) {
        List<BiomeMobRecipe.MobDisplayEntry> entries = recipe.mobEntries();
        int index = entries.indexOf(currentEntry);
        return index == entries.size() - 1 || !entries.get(index + 1).data().category.equals(currentEntry.data().category);
    }

    private static int getCategoryGridHeight(BiomeMobRecipe recipe, String category) {
        int count = 0;
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            if (entry.data().category.equals(category) && !entry.stack().isEmpty()) {
                count++;
            }
        }
        int rows = Math.max(1, (count + GRID_COLUMNS - 1) / GRID_COLUMNS);
        return rows * SLOT_SIZE;
    }

    private static ItemStack getFocusedDropStack(BiomeMobRecipe recipe, IFocusGroup focuses) {
        return focuses.getItemStackFocuses()
                .map(focus -> focus.getTypedValue().getIngredient())
                .filter(stack -> !getDropSourceEntries(recipe, stack).isEmpty())
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    private static List<BiomeMobRecipe.MobDisplayEntry> getDropSourceEntries(BiomeMobRecipe recipe, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Identifier focusedItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (focusedItemId == null) {
            return List.of();
        }
        List<BiomeMobRecipe.MobDisplayEntry> entries = new ArrayList<>();
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            for (BiomeBlockIndexCache.MobSpawnEntry spawnEntry : entry.spawns()) {
                if (spawnEntry.dropItems != null && spawnEntry.dropItems.contains(focusedItemId.toString())) {
                    entries.add(entry);
                    break;
                }
            }
        }
        return List.copyOf(entries);
    }

    private static List<BiomeMobRecipe.MobDisplayEntry> getVisibleDropSourceEntries(BiomeMobRecipe recipe, ItemStack stack) {
        List<BiomeMobRecipe.MobDisplayEntry> entries = new ArrayList<>();
        for (BiomeMobRecipe.MobDisplayEntry entry : getDropSourceEntries(recipe, stack)) {
            if (!entry.stack().isEmpty()) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    private static Component getDropSourceTitle(ItemStack focusedDropStack) {
        Component focusedName = focusedDropStack.getHoverName().copy().withStyle(ChatFormatting.BLUE);
        return Component.translatable("jei_biome.label.mob_drop_sources", focusedName);
    }

    private static int drawWrappedText(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int color) {
        List<FormattedCharSequence> lines = font.split(text, TEXT_WIDTH);
        for (int index = 0; index < lines.size(); index++) {
            guiGraphics.text(font, lines.get(index), x, y + index * TITLE_LINE_HEIGHT, color, false);
        }
        return getTitleHeight(font, text);
    }

    private static int getTitleHeight(Font font, Component text) {
        int lineCount = Math.max(1, font.split(text, TEXT_WIDTH).size());
        return Math.max(SECTION_TITLE_HEIGHT, lineCount * TITLE_LINE_HEIGHT + 2);
    }

    private static String cleanFallbackText(String fallback) {
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
        return value.replace('_', ' ');
    }

    private static void drawPanel(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    record SlotPlacement(int x, int y) {
    }
}
