package com.jei_biome.emi;

import com.jei_biome.data.BiomeBlockIndexCache;
import com.jei_biome.jei.BiomeBlockRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class EmiBiomeBlockRecipe implements EmiRecipe {

    private final BiomeBlockRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public EmiBiomeBlockRecipe(BiomeBlockRecipe recipe) {
        this.recipe = recipe;
        List<EmiIngredient> lookup = new ArrayList<>();
        for (ItemStack stack : recipe.lookupStacks()) {
            lookup.add(EmiStack.of(stack));
        }
        this.inputs = List.copyOf(lookup);
        this.outputs = lookup.stream()
                .filter(EmiStack.class::isInstance)
                .map(EmiStack.class::cast)
                .toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EmiBiomePlugin.BIOME_BLOCKS;
    }

    @Override
    public ResourceLocation getId() {
        ResourceLocation id = recipe.id();
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "/" + id.getPath());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return EmiBiomeText.RECIPE_WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return EmiBiomeText.RECIPE_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addText(EmiBiomeText.biomeName(recipe.entry().biomeId), 2, 2, EmiBiomeText.TITLE_COLOR, false);
        EmiBiomeScrollWidget.Builder builder = EmiBiomeScrollWidget.builder();
        int y = 4;
        ItemStack focusedBlockStack = focusedBlockStack();
        if (!focusedBlockStack.isEmpty()) {
            y = addSection(builder, y, Component.translatable("jei_biome.label.queried_block", focusedBlockStack.getHoverName()), List.of(focusedBlockStack), false);
        }
        List<ItemStack> terrainStacks = withoutBlockStack(recipe.terrainStacks(), focusedBlockStack);
        List<ItemStack> surfaceFeatureStacks = withoutBlockStack(recipe.surfaceFeatureStacks(), focusedBlockStack);
        List<ItemStack> undergroundFeatureStacks = withoutBlockStack(recipe.undergroundFeatureStacks(), focusedBlockStack);
        List<ItemStack> oreStacks = withoutBlockStack(recipe.oreStacks(), focusedBlockStack);
        y = addSection(builder, y, Component.translatable("jei_biome.label.terrain_blocks", terrainStacks.size()), terrainStacks, false);
        y = addSection(builder, y, Component.translatable("jei_biome.label.surface_feature_blocks", surfaceFeatureStacks.size()), surfaceFeatureStacks, false);
        y = addSection(builder, y, Component.translatable("jei_biome.label.underground_feature_blocks", undergroundFeatureStacks.size()), undergroundFeatureStacks, false);
        y = addSection(builder, y, Component.translatable("jei_biome.label.ore_blocks", oreStacks.size()), oreStacks, true);
        y = addSection(builder, y, Component.translatable("jei_biome.label.mob_drops", recipe.mobDropStacks().size()), recipe.mobDropStacks(), false);
        addSection(builder, y, Component.translatable("jei_biome.label.mob_spawn_eggs", recipe.mobSpawnEggStacks().size()), recipe.mobSpawnEggStacks(), false);
        widgets.add(builder.build(this, 0, EmiBiomeText.CONTENT_Y, getDisplayWidth(), EmiBiomeText.CONTENT_HEIGHT));
    }

    private ItemStack focusedBlockStack() {
        EmiIngredient focused = EmiLookupContext.currentLookup();
        if (focused == null || focused.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (EmiStack stack : focused.getEmiStacks()) {
            ItemStack itemStack = stack.getItemStack();
            if (!itemStack.isEmpty() && recipe.containsBlockStack(itemStack)) {
                return itemStack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private List<ItemStack> withoutBlockStack(List<ItemStack> stacks, ItemStack focusedBlockStack) {
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

    private int addSection(EmiBiomeScrollWidget.Builder builder, int y, Component title, List<ItemStack> stacks, boolean oreTooltip) {
        int slotY = builder.addWrappedText(title, 4, y, EmiBiomeText.SECTION_COLOR);
        if (stacks.isEmpty()) {
            builder.addWrappedText(Component.translatable("jei_biome.label.empty"), 8, slotY + 4, EmiBiomeText.EMPTY_COLOR);
            return slotY + EmiBiomeText.SLOT_SIZE + EmiBiomeText.SECTION_GAP;
        }
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            int x = 4 + index % EmiBiomeText.GRID_COLUMNS * EmiBiomeText.SLOT_SIZE;
            int slotRowY = slotY + index / EmiBiomeText.GRID_COLUMNS * EmiBiomeText.SLOT_SIZE;
            List<Component> tooltip = List.of();
            if (oreTooltip) {
                tooltip = oreTooltip(stack);
            }
            builder.addStack(EmiStack.of(stack), x, slotRowY, tooltip);
        }
        int rows = Math.max(1, (stacks.size() + EmiBiomeText.GRID_COLUMNS - 1) / EmiBiomeText.GRID_COLUMNS);
        return slotY + rows * EmiBiomeText.SLOT_SIZE + EmiBiomeText.SECTION_GAP;
    }

    private List<Component> oreTooltip(ItemStack stack) {
        List<BiomeBlockIndexCache.OreDistributionLine> lines = recipe.getOreDistributionLines(stack);
        if (lines.isEmpty()) {
            return List.of();
        }
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("jei_biome.tooltip.ore_distribution").withStyle(ChatFormatting.GOLD));
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                tooltip.add(Component.empty());
            }
            BiomeBlockIndexCache.OreDistributionLine line = lines.get(index);
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
        return tooltip;
    }

}
