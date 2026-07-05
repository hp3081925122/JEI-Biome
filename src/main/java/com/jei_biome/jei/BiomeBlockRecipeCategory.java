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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class BiomeBlockRecipeCategory implements IRecipeCategory<BiomeBlockRecipe> {

    public static final RecipeType<BiomeBlockRecipe> TYPE = RecipeType.create(Jei_biome.MODID, "biome_blocks", BiomeBlockRecipe.class);

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

    public BiomeBlockRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.GRASS_BLOCK));
    }

    @Override
    public RecipeType<BiomeBlockRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei_biome.category.biome_blocks");
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
    public ResourceLocation getRegistryName(BiomeBlockRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BiomeBlockRecipe recipe, IFocusGroup focuses) {
        addSectionSlots(builder, recipe.terrainStacks());
        addSectionSlots(builder, recipe.surfaceFeatureStacks());
        addSectionSlots(builder, recipe.undergroundFeatureStacks());
        for (ItemStack stack : recipe.oreStacks()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0)
                    .setStandardSlotBackground()
                    .addItemStack(stack)
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                        List<BiomeBlockIndexCache.OreDistributionLine> lines = recipe.getOreDistributionLines(stack);
                        if (!lines.isEmpty()) {
                            tooltip.add(Component.translatable("jei_biome.tooltip.ore_distribution").withStyle(ChatFormatting.GOLD));
                            for (int index = 0; index < lines.size(); index++) {
                                if (index > 0) {
                                    tooltip.add(Component.empty());
                                }
                                addOreDistributionTooltip(tooltip, lines.get(index));
                            }
                        }
                    });
        }
        if (!recipe.lookupStacks().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.lookupStacks());
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.lookupStacks());
        }
    }

    @Override
    public void draw(BiomeBlockRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        ResourceLocation biomeId = ResourceLocation.tryParse(recipe.entry().biomeId);
        Component biomeName = Component.literal(recipe.entry().biomeId);
        if (biomeId != null) {
            String translationKey = biomeId.toLanguageKey("biome");
            if (I18n.exists(translationKey)) {
                biomeName = Component.translatable(translationKey);
            }
        }
        guiGraphics.drawString(font, biomeName, 4, 4, 0xFF2B2B2B, false);
        drawPanel(guiGraphics, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BiomeBlockRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> contentSlots = builder.getRecipeSlots().getSlots();
        BiomeBlockScrollWidget widget = new BiomeBlockScrollWidget(recipe, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT, contentSlots);
        builder.addSlottedWidget(widget, contentSlots);
        builder.addInputHandler(widget);
    }

    static void drawScrollableContents(BiomeBlockRecipe recipe, GuiGraphics guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int currentY = y + 4;
        currentY = drawSection(recipe.entry().terrainBlocks.size(), Component.translatable("jei_biome.label.terrain_blocks", recipe.entry().terrainBlocks.size()), recipe.terrainStacks(), guiGraphics, font, x, currentY);
        currentY = drawSection(recipe.entry().surfaceFeatureBlocks.size(), Component.translatable("jei_biome.label.surface_feature_blocks", recipe.entry().surfaceFeatureBlocks.size()), recipe.surfaceFeatureStacks(), guiGraphics, font, x, currentY);
        currentY = drawSection(recipe.entry().undergroundFeatureBlocks.size(), Component.translatable("jei_biome.label.underground_feature_blocks", recipe.entry().undergroundFeatureBlocks.size()), recipe.undergroundFeatureStacks(), guiGraphics, font, x, currentY);
        drawSection(recipe.entry().oreBlocks.size(), Component.translatable("jei_biome.label.ore_blocks", recipe.entry().oreBlocks.size()), recipe.oreStacks(), guiGraphics, font, x, currentY);
    }

    static List<SlotPlacement> getSlotPlacements(BiomeBlockRecipe recipe) {
        Font font = Minecraft.getInstance().font;
        List<SlotPlacement> placements = new ArrayList<>();
        int currentY = 4;
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.terrain_blocks", recipe.entry().terrainBlocks.size()), recipe.terrainStacks(), font, currentY);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.surface_feature_blocks", recipe.entry().surfaceFeatureBlocks.size()), recipe.surfaceFeatureStacks(), font, currentY);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.underground_feature_blocks", recipe.entry().undergroundFeatureBlocks.size()), recipe.undergroundFeatureStacks(), font, currentY);
        addSlotPlacements(placements, Component.translatable("jei_biome.label.ore_blocks", recipe.entry().oreBlocks.size()), recipe.oreStacks(), font, currentY);
        return placements;
    }

    static int getTotalContentHeight(BiomeBlockRecipe recipe) {
        Font font = Minecraft.getInstance().font;
        int height = 4;
        height += getSectionHeight(font, Component.translatable("jei_biome.label.terrain_blocks", recipe.entry().terrainBlocks.size()), recipe.terrainStacks().size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.surface_feature_blocks", recipe.entry().surfaceFeatureBlocks.size()), recipe.surfaceFeatureStacks().size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.underground_feature_blocks", recipe.entry().undergroundFeatureBlocks.size()), recipe.undergroundFeatureStacks().size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.ore_blocks", recipe.entry().oreBlocks.size()), recipe.oreStacks().size());
        return height + CONTENT_PADDING_BOTTOM;
    }

    private static void addSectionSlots(IRecipeLayoutBuilder builder, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0)
                    .setStandardSlotBackground()
                    .addItemStack(stack);
        }
    }

    private static int drawSection(int count, Component title, List<ItemStack> stacks, GuiGraphics guiGraphics, Font font, int x, int y) {
        List<FormattedCharSequence> titleLines = font.split(title, TEXT_WIDTH);
        for (int index = 0; index < titleLines.size(); index++) {
            guiGraphics.drawString(font, titleLines.get(index), x + 4, y + index * TITLE_LINE_HEIGHT, 0xFF555555, false);
        }
        int titleHeight = getTitleHeight(font, title);
        if (count <= 0) {
            guiGraphics.drawString(font, Component.translatable("jei_biome.label.empty"), x + 8, y + titleHeight, 0xFF888888, false);
        }
        return y + getSectionHeight(font, title, stacks.size());
    }

    private static void addOreDistributionTooltip(ITooltipBuilder tooltip, BiomeBlockIndexCache.OreDistributionLine line) {
        String sourceKind = line.sourceKind == null || line.sourceKind.isBlank() ? "common" : line.sourceKind;
        tooltip.add(Component.translatable("jei_biome.tooltip.ore_source", Component.translatable("jei_biome.tooltip.ore_source." + sourceKind)).withStyle(ChatFormatting.AQUA));
        if (line.veinSize > 0) {
            tooltip.add(Component.translatable("jei_biome.tooltip.ore_size", line.veinSize).withStyle(ChatFormatting.GRAY));
        }
        if (line.attemptsPerChunk != null && !line.attemptsPerChunk.isBlank()) {
            tooltip.add(Component.translatable("jei_biome.tooltip.ore_count", line.attemptsPerChunk).withStyle(ChatFormatting.GRAY));
        }
        if (line.averageEveryChunks != null && !line.averageEveryChunks.isBlank()) {
            tooltip.add(Component.translatable("jei_biome.tooltip.ore_rarity", line.averageEveryChunks).withStyle(ChatFormatting.GRAY));
        }
        if (line.minHeight != null && !line.minHeight.isBlank() && line.maxHeight != null && !line.maxHeight.isBlank()) {
            tooltip.add(Component.translatable("jei_biome.tooltip.ore_height", line.minHeight, line.maxHeight).withStyle(ChatFormatting.DARK_GREEN));
        }
        String heightMode = line.heightMode == null || line.heightMode.isBlank() ? "custom" : line.heightMode;
        tooltip.add(Component.translatable("jei_biome.tooltip.ore_height." + heightMode).withStyle(ChatFormatting.GREEN));
    }

    private static int addSlotPlacements(List<SlotPlacement> placements, Component title, List<ItemStack> stacks, Font font, int currentY) {
        int slotStartY = currentY + getTitleHeight(font, title);
        for (int index = 0; index < stacks.size(); index++) {
            int column = index % GRID_COLUMNS;
            int row = index / GRID_COLUMNS;
            placements.add(new SlotPlacement(4 + column * SLOT_SIZE, slotStartY + row * SLOT_SIZE));
        }
        return currentY + getSectionHeight(font, title, stacks.size());
    }

    private static int getSectionHeight(Font font, Component title, int stackCount) {
        int rows = Math.max(1, (stackCount + GRID_COLUMNS - 1) / GRID_COLUMNS);
        return getTitleHeight(font, title) + rows * SLOT_SIZE + SECTION_GAP;
    }

    private static int getTitleHeight(Font font, Component title) {
        int lineCount = Math.max(1, font.split(title, TEXT_WIDTH).size());
        return Math.max(SECTION_TITLE_HEIGHT, lineCount * TITLE_LINE_HEIGHT + 2);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    static int getScrollbarWidth() {
        return SCROLLBAR_WIDTH;
    }

    record SlotPlacement(int x, int y) {
    }
}
