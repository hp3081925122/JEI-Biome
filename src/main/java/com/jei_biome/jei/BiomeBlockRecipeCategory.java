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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    public Identifier getRegistryName(BiomeBlockRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BiomeBlockRecipe recipe, IFocusGroup focuses) {
        ItemStack focusedBlockStack = getFocusedBlockStack(recipe, focuses);
        if (!focusedBlockStack.isEmpty()) {
            addSectionSlots(builder, List.of(focusedBlockStack));
        }
        addSectionSlots(builder, withoutBlockStack(recipe.terrainStacks(), focusedBlockStack));
        addSectionSlots(builder, withoutBlockStack(recipe.surfaceFeatureStacks(), focusedBlockStack));
        addSectionSlots(builder, withoutBlockStack(recipe.undergroundFeatureStacks(), focusedBlockStack));
        for (ItemStack stack : withoutBlockStack(recipe.oreStacks(), focusedBlockStack)) {
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
        addSectionSlots(builder, recipe.mobDropStacks());
        addSectionSlots(builder, recipe.mobSpawnEggStacks());
        if (!recipe.lookupStacks().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.lookupStacks());
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.lookupStacks());
        }
    }

    @Override
    public void draw(BiomeBlockRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        Identifier biomeId = Identifier.tryParse(recipe.entry().biomeId);
        Component biomeName = Component.literal(recipe.entry().biomeId);
        if (biomeId != null) {
            String translationKey = biomeId.toLanguageKey("biome");
            if (I18n.exists(translationKey)) {
                biomeName = Component.translatable(translationKey);
            }
        }
        guiGraphics.text(font, biomeName, 4, 4, 0xFF2B2B2B, false);
        drawPanel(guiGraphics, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BiomeBlockRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> contentSlots = builder.getRecipeSlots().getSlots();
        BiomeBlockScrollWidget widget = new BiomeBlockScrollWidget(recipe, getFocusedBlockStack(recipe, focuses), CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT, contentSlots);
        builder.addSlottedWidget(widget, contentSlots);
        builder.addInputHandler(widget);
    }

    static void drawScrollableContents(BiomeBlockRecipe recipe, ItemStack focusedBlockStack, GuiGraphicsExtractor guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int currentY = y + 4;
        if (!focusedBlockStack.isEmpty()) {
            currentY = drawSection(1, Component.translatable("jei_biome.label.queried_block", focusedBlockStack.getHoverName()), List.of(focusedBlockStack), guiGraphics, font, x, currentY);
        }
        List<ItemStack> terrainStacks = withoutBlockStack(recipe.terrainStacks(), focusedBlockStack);
        List<ItemStack> surfaceFeatureStacks = withoutBlockStack(recipe.surfaceFeatureStacks(), focusedBlockStack);
        List<ItemStack> undergroundFeatureStacks = withoutBlockStack(recipe.undergroundFeatureStacks(), focusedBlockStack);
        List<ItemStack> oreStacks = withoutBlockStack(recipe.oreStacks(), focusedBlockStack);
        currentY = drawSection(terrainStacks.size(), Component.translatable("jei_biome.label.terrain_blocks", terrainStacks.size()), terrainStacks, guiGraphics, font, x, currentY);
        currentY = drawSection(surfaceFeatureStacks.size(), Component.translatable("jei_biome.label.surface_feature_blocks", surfaceFeatureStacks.size()), surfaceFeatureStacks, guiGraphics, font, x, currentY);
        currentY = drawSection(undergroundFeatureStacks.size(), Component.translatable("jei_biome.label.underground_feature_blocks", undergroundFeatureStacks.size()), undergroundFeatureStacks, guiGraphics, font, x, currentY);
        currentY = drawSection(oreStacks.size(), Component.translatable("jei_biome.label.ore_blocks", oreStacks.size()), oreStacks, guiGraphics, font, x, currentY);
        currentY = drawSection(recipe.mobDropStacks().size(), Component.translatable("jei_biome.label.mob_drops", recipe.mobDropStacks().size()), recipe.mobDropStacks(), guiGraphics, font, x, currentY);
        drawSection(recipe.mobSpawnEggStacks().size(), Component.translatable("jei_biome.label.mob_spawn_eggs", recipe.mobSpawnEggStacks().size()), recipe.mobSpawnEggStacks(), guiGraphics, font, x, currentY);
    }

    static List<SlotPlacement> getSlotPlacements(BiomeBlockRecipe recipe, ItemStack focusedBlockStack) {
        Font font = Minecraft.getInstance().font;
        List<SlotPlacement> placements = new ArrayList<>();
        int currentY = 4;
        if (!focusedBlockStack.isEmpty()) {
            currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.queried_block", focusedBlockStack.getHoverName()), List.of(focusedBlockStack), font, currentY);
        }
        List<ItemStack> terrainStacks = withoutBlockStack(recipe.terrainStacks(), focusedBlockStack);
        List<ItemStack> surfaceFeatureStacks = withoutBlockStack(recipe.surfaceFeatureStacks(), focusedBlockStack);
        List<ItemStack> undergroundFeatureStacks = withoutBlockStack(recipe.undergroundFeatureStacks(), focusedBlockStack);
        List<ItemStack> oreStacks = withoutBlockStack(recipe.oreStacks(), focusedBlockStack);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.terrain_blocks", terrainStacks.size()), terrainStacks, font, currentY);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.surface_feature_blocks", surfaceFeatureStacks.size()), surfaceFeatureStacks, font, currentY);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.underground_feature_blocks", undergroundFeatureStacks.size()), undergroundFeatureStacks, font, currentY);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.ore_blocks", oreStacks.size()), oreStacks, font, currentY);
        currentY = addSlotPlacements(placements, Component.translatable("jei_biome.label.mob_drops", recipe.mobDropStacks().size()), recipe.mobDropStacks(), font, currentY);
        addSlotPlacements(placements, Component.translatable("jei_biome.label.mob_spawn_eggs", recipe.mobSpawnEggStacks().size()), recipe.mobSpawnEggStacks(), font, currentY);
        return placements;
    }

    static int getTotalContentHeight(BiomeBlockRecipe recipe, ItemStack focusedBlockStack) {
        Font font = Minecraft.getInstance().font;
        int height = 4;
        if (!focusedBlockStack.isEmpty()) {
            height += getSectionHeight(font, Component.translatable("jei_biome.label.queried_block", focusedBlockStack.getHoverName()), 1);
        }
        List<ItemStack> terrainStacks = withoutBlockStack(recipe.terrainStacks(), focusedBlockStack);
        List<ItemStack> surfaceFeatureStacks = withoutBlockStack(recipe.surfaceFeatureStacks(), focusedBlockStack);
        List<ItemStack> undergroundFeatureStacks = withoutBlockStack(recipe.undergroundFeatureStacks(), focusedBlockStack);
        List<ItemStack> oreStacks = withoutBlockStack(recipe.oreStacks(), focusedBlockStack);
        height += getSectionHeight(font, Component.translatable("jei_biome.label.terrain_blocks", terrainStacks.size()), terrainStacks.size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.surface_feature_blocks", surfaceFeatureStacks.size()), surfaceFeatureStacks.size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.underground_feature_blocks", undergroundFeatureStacks.size()), undergroundFeatureStacks.size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.ore_blocks", oreStacks.size()), oreStacks.size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.mob_drops", recipe.mobDropStacks().size()), recipe.mobDropStacks().size());
        height += getSectionHeight(font, Component.translatable("jei_biome.label.mob_spawn_eggs", recipe.mobSpawnEggStacks().size()), recipe.mobSpawnEggStacks().size());
        return height + CONTENT_PADDING_BOTTOM;
    }

    private static ItemStack getFocusedBlockStack(BiomeBlockRecipe recipe, IFocusGroup focuses) {
        return focuses.getItemStackFocuses()
                .map(focus -> focus.getTypedValue().getIngredient())
                .filter(recipe::containsBlockStack)
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    private static List<ItemStack> withoutBlockStack(List<ItemStack> stacks, ItemStack focusedBlockStack) {
        if (focusedBlockStack == null || focusedBlockStack.isEmpty()) {
            return stacks;
        }
        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.getItem() != focusedBlockStack.getItem()) {
                filtered.add(stack);
            }
        }
        return List.copyOf(filtered);
    }

    private static void addSectionSlots(IRecipeLayoutBuilder builder, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0)
                    .setStandardSlotBackground()
                    .addItemStack(stack);
        }
    }

    private static int drawSection(int count, Component title, List<ItemStack> stacks, GuiGraphicsExtractor guiGraphics, Font font, int x, int y) {
        List<FormattedCharSequence> titleLines = font.split(title, TEXT_WIDTH);
        for (int index = 0; index < titleLines.size(); index++) {
            guiGraphics.text(font, titleLines.get(index), x + 4, y + index * TITLE_LINE_HEIGHT, 0xFF555555, false);
        }
        int titleHeight = getTitleHeight(font, title);
        if (count <= 0) {
            guiGraphics.text(font, Component.translatable("jei_biome.label.empty"), x + 8, y + titleHeight, 0xFF888888, false);
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

    private static void drawPanel(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
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
