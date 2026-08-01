package com.jei_biome.jei;

import com.jei_biome.Jei_biome;
import com.jei_biome.data.BiomeBlockIndexCache;
import com.jei_biome.data.BiomeBlockIndexCacheLoader;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JeiPlugin
public final class JeiBiomePlugin implements IModPlugin {

    private static volatile CachedRecipes cachedRecipes;
    private final Identifier pluginId = Identifier.fromNamespaceAndPath(Jei_biome.MODID, "plugin");

    @Override
    public Identifier getPluginUid() {
        return pluginId;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new BiomeBlockRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BiomeMobRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(BiomeBlockRecipeCategory.TYPE, getSharedRecipes());
        registration.addRecipes(BiomeMobRecipeCategory.TYPE, getSharedMobRecipes());
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(BiomeBlockRecipeCategory.TYPE, new BiomeBlockRecipeLookupPlugin());
        registration.addTypedRecipeManagerPlugin(BiomeMobRecipeCategory.TYPE, new BiomeMobRecipeLookupPlugin());
    }

    public static List<BiomeBlockRecipe> getSharedRecipes() {
        BiomeBlockIndexCache cache = BiomeBlockIndexCacheLoader.load();
        CachedRecipes snapshot = cachedRecipes;
        if (snapshot != null && snapshot.sourceCache == cache) {
            return snapshot.recipes;
        }
        synchronized (JeiBiomePlugin.class) {
            snapshot = cachedRecipes;
            if (snapshot == null || snapshot.sourceCache != cache) {
                List<BiomeBlockRecipe> recipes = new ArrayList<>();
                for (BiomeBlockIndexCache.BiomeEntry entry : cache.biomes) {
                    recipes.add(new BiomeBlockRecipe(entry));
                }
                cachedRecipes = new CachedRecipes(cache, List.copyOf(recipes));
            }
            return cachedRecipes.recipes;
        }
    }

    private record CachedRecipes(BiomeBlockIndexCache sourceCache, List<BiomeBlockRecipe> recipes) {
    }

    public static List<BiomeMobRecipe> getSharedMobRecipes() {
        BiomeBlockIndexCache cache = BiomeBlockIndexCacheLoader.load();
        CachedMobRecipes snapshot = cachedMobRecipes;
        if (snapshot != null && snapshot.sourceCache == cache) {
            return snapshot.recipes;
        }
        synchronized (JeiBiomePlugin.class) {
            snapshot = cachedMobRecipes;
            if (snapshot == null || snapshot.sourceCache != cache) {
                List<BiomeMobRecipe> recipes = new ArrayList<>();
                for (BiomeBlockIndexCache.BiomeEntry entry : cache.biomes) {
                    recipes.add(new BiomeMobRecipe(entry));
                }
                cachedMobRecipes = new CachedMobRecipes(cache, List.copyOf(recipes));
            }
            return cachedMobRecipes.recipes;
        }
    }

    private static volatile CachedMobRecipes cachedMobRecipes;

    private record CachedMobRecipes(BiomeBlockIndexCache sourceCache, List<BiomeMobRecipe> recipes) {
    }

    private static final class BiomeBlockRecipeLookupPlugin implements ISimpleRecipeManagerPlugin<BiomeBlockRecipe> {

        private volatile CachedLookup cachedLookup;

        @Override
        public boolean isHandledInput(ITypedIngredient<?> ingredient) {
            return getMatchedRecipes(ingredient) != null;
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> ingredient) {
            return getMatchedRecipes(ingredient) != null;
        }

        @Override
        public List<BiomeBlockRecipe> getRecipesForInput(ITypedIngredient<?> ingredient) {
            List<BiomeBlockRecipe> recipes = getMatchedRecipes(ingredient);
            return recipes != null ? recipes : List.of();
        }

        @Override
        public List<BiomeBlockRecipe> getRecipesForOutput(ITypedIngredient<?> ingredient) {
            List<BiomeBlockRecipe> recipes = getMatchedRecipes(ingredient);
            return recipes != null ? recipes : List.of();
        }

        @Override
        public List<BiomeBlockRecipe> getAllRecipes() {
            return getLookup().recipes;
        }

        private List<BiomeBlockRecipe> getMatchedRecipes(ITypedIngredient<?> ingredient) {
            CachedLookup lookup = getLookup();
            return ingredient.getItemStack()
                    .map(ItemStack::getItem)
                    .map(lookup.recipesByItem::get)
                    .orElse(null);
        }

        private CachedLookup getLookup() {
            List<BiomeBlockRecipe> recipes = getSharedRecipes();
            CachedLookup snapshot = cachedLookup;
            if (snapshot != null && snapshot.recipes == recipes) {
                return snapshot;
            }
            synchronized (this) {
                snapshot = cachedLookup;
                if (snapshot == null || snapshot.recipes != recipes) {
                    Map<Item, Set<BiomeBlockRecipe>> deduplicatedIndex = new IdentityHashMap<>();
                    for (BiomeBlockRecipe recipe : recipes) {
                        for (ItemStack stack : recipe.lookupStacks()) {
                            deduplicatedIndex.computeIfAbsent(stack.getItem(), item -> new LinkedHashSet<>()).add(recipe);
                        }
                    }
                    Map<Item, List<BiomeBlockRecipe>> index = new IdentityHashMap<>();
                    for (Map.Entry<Item, Set<BiomeBlockRecipe>> entry : deduplicatedIndex.entrySet()) {
                        index.put(entry.getKey(), List.copyOf(entry.getValue()));
                    }
                    cachedLookup = new CachedLookup(recipes, Collections.unmodifiableMap(index));
                }
                return cachedLookup;
            }
        }

        private record CachedLookup(List<BiomeBlockRecipe> recipes, Map<Item, List<BiomeBlockRecipe>> recipesByItem) {
        }
    }

    private static final class BiomeMobRecipeLookupPlugin implements ISimpleRecipeManagerPlugin<BiomeMobRecipe> {

        private volatile CachedMobLookup cachedLookup;

        @Override
        public boolean isHandledInput(ITypedIngredient<?> ingredient) {
            return getMatchedRecipes(ingredient) != null;
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> ingredient) {
            return getMatchedRecipes(ingredient) != null;
        }

        @Override
        public List<BiomeMobRecipe> getRecipesForInput(ITypedIngredient<?> ingredient) {
            List<BiomeMobRecipe> recipes = getMatchedRecipes(ingredient);
            return recipes != null ? recipes : List.of();
        }

        @Override
        public List<BiomeMobRecipe> getRecipesForOutput(ITypedIngredient<?> ingredient) {
            List<BiomeMobRecipe> recipes = getMatchedRecipes(ingredient);
            return recipes != null ? recipes : List.of();
        }

        @Override
        public List<BiomeMobRecipe> getAllRecipes() {
            return getLookup().recipes;
        }

        private List<BiomeMobRecipe> getMatchedRecipes(ITypedIngredient<?> ingredient) {
            CachedMobLookup lookup = getLookup();
            return ingredient.getItemStack()
                    .map(ItemStack::getItem)
                    .map(lookup.recipesByItem::get)
                    .orElse(null);
        }

        private CachedMobLookup getLookup() {
            List<BiomeMobRecipe> recipes = getSharedMobRecipes();
            CachedMobLookup snapshot = cachedLookup;
            if (snapshot != null && snapshot.recipes == recipes) {
                return snapshot;
            }
            synchronized (this) {
                snapshot = cachedLookup;
                if (snapshot == null || snapshot.recipes != recipes) {
                    Map<Item, Set<BiomeMobRecipe>> deduplicatedIndex = new IdentityHashMap<>();
                    for (BiomeMobRecipe recipe : recipes) {
                        for (ItemStack stack : recipe.lookupStacks()) {
                            deduplicatedIndex.computeIfAbsent(stack.getItem(), item -> new LinkedHashSet<>()).add(recipe);
                        }
                    }
                    Map<Item, List<BiomeMobRecipe>> index = new IdentityHashMap<>();
                    for (Map.Entry<Item, Set<BiomeMobRecipe>> entry : deduplicatedIndex.entrySet()) {
                        index.put(entry.getKey(), List.copyOf(entry.getValue()));
                    }
                    cachedLookup = new CachedMobLookup(recipes, Collections.unmodifiableMap(index));
                }
                return cachedLookup;
            }
        }

        private record CachedMobLookup(List<BiomeMobRecipe> recipes, Map<Item, List<BiomeMobRecipe>> recipesByItem) {
        }
    }
}
