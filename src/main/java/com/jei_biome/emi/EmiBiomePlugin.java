package com.jei_biome.emi;

import com.jei_biome.Jei_biome;
import com.jei_biome.config.JeiBiomeConfig;
import com.jei_biome.jei.JeiBiomePlugin;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

@EmiEntrypoint
public final class EmiBiomePlugin implements EmiPlugin {

    public static final EmiRecipeCategory BIOME_BLOCKS = new EmiRecipeCategory(
            new ResourceLocation(Jei_biome.MODID, "biome_blocks"),
            EmiStack.of(Items.GRASS_BLOCK)
    );
    public static final EmiRecipeCategory BIOME_MOBS = new EmiRecipeCategory(
            new ResourceLocation(Jei_biome.MODID, "biome_mobs"),
            EmiStack.of(Items.CREEPER_SPAWN_EGG)
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(BIOME_BLOCKS);
        // 生物生成关闭时不注册 EMI 的群系生物分类。
        if (JeiBiomeConfig.showMobSpawns()) {
            registry.addCategory(BIOME_MOBS);
        }
        for (var recipe : JeiBiomePlugin.getSharedRecipes()) {
            registry.addRecipe(new EmiBiomeBlockRecipe(recipe));
        }
        if (JeiBiomeConfig.showMobSpawns()) {
            for (var recipe : JeiBiomePlugin.getSharedMobRecipes()) {
                registry.addRecipe(new EmiBiomeMobRecipe(recipe));
            }
        }
    }
}
