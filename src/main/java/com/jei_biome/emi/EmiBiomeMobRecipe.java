package com.jei_biome.emi;

import com.jei_biome.data.BiomeBlockIndexCache;
import com.jei_biome.jei.BiomeMobRecipe;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class EmiBiomeMobRecipe implements EmiRecipe {

    private final BiomeMobRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public EmiBiomeMobRecipe(BiomeMobRecipe recipe) {
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
        return EmiBiomePlugin.BIOME_MOBS;
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
        if (recipe.mobEntries().isEmpty()) {
            builder.addWrappedText(Component.translatable("jei_biome.label.empty"), 8, y + 4, EmiBiomeText.EMPTY_COLOR);
            widgets.add(builder.build(this, 0, EmiBiomeText.CONTENT_Y, getDisplayWidth(), EmiBiomeText.CONTENT_HEIGHT));
            return;
        }
        for (var entry : groupedEntries().entrySet()) {
            Component title = categoryName(entry.getKey());
            int slotY = builder.addWrappedText(title, 4, y, EmiBiomeText.SECTION_COLOR);
            List<BiomeMobRecipe.MobDisplayEntry> entries = entry.getValue();
            for (int index = 0; index < entries.size(); index++) {
                BiomeMobRecipe.MobDisplayEntry mobEntry = entries.get(index);
                if (mobEntry.stack().isEmpty()) {
                    continue;
                }
                int x = 4 + index % EmiBiomeText.GRID_COLUMNS * EmiBiomeText.SLOT_SIZE;
                int slotRowY = slotY + index / EmiBiomeText.GRID_COLUMNS * EmiBiomeText.SLOT_SIZE;
                builder.addStack(EmiStack.of(mobEntry.stack()), x, slotRowY, mobTooltip(mobEntry));
            }
            int rows = Math.max(1, (entries.size() + EmiBiomeText.GRID_COLUMNS - 1) / EmiBiomeText.GRID_COLUMNS);
            y = slotY + rows * EmiBiomeText.SLOT_SIZE + EmiBiomeText.SECTION_GAP;
        }
        widgets.add(builder.build(this, 0, EmiBiomeText.CONTENT_Y, getDisplayWidth(), EmiBiomeText.CONTENT_HEIGHT));
    }

    private LinkedHashMap<String, List<BiomeMobRecipe.MobDisplayEntry>> groupedEntries() {
        LinkedHashMap<String, List<BiomeMobRecipe.MobDisplayEntry>> grouped = new LinkedHashMap<>();
        for (BiomeMobRecipe.MobDisplayEntry entry : recipe.mobEntries()) {
            if (!entry.stack().isEmpty()) {
                grouped.computeIfAbsent(entry.data().category, ignored -> new ArrayList<>()).add(entry);
            }
        }
        return grouped;
    }

    private Component categoryName(String category) {
        return EmiBiomeText.translateOrLiteral("jei_biome.mob_category." + category, category);
    }

    private List<Component> mobTooltip(BiomeMobRecipe.MobDisplayEntry entry) {
        List<Component> tooltip = new ArrayList<>();
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
        return tooltip;
    }

    private void addMobSpawnTooltip(List<Component> tooltip, BiomeBlockIndexCache.MobSpawnEntry entry) {
        tooltip.add(Component.translatable("jei_biome.tooltip.mob_category", categoryName(entry.category)).withStyle(ChatFormatting.AQUA));
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
                tooltip.add(Component.translatable("jei_biome.tooltip.mob_placement", EmiBiomeText.translateOrLiteral(placementKey, entry.placementType)).withStyle(ChatFormatting.GRAY));
            }
            if (entry.heightmapType != null && !entry.heightmapType.isBlank()) {
                String heightmapKey = "jei_biome.mob_heightmap." + entry.heightmapType.toLowerCase(Locale.ROOT);
                tooltip.add(Component.translatable("jei_biome.tooltip.mob_heightmap", EmiBiomeText.translateOrLiteral(heightmapKey, entry.heightmapType)).withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("jei_biome.tooltip.mob_spawn_rules_note").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private boolean hasDropItems(BiomeMobRecipe.MobDisplayEntry entry) {
        for (BiomeBlockIndexCache.MobSpawnEntry spawnEntry : entry.spawns()) {
            if (spawnEntry.dropItems != null && !spawnEntry.dropItems.isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
