package com.jei_biome.mixin.emi;

import com.jei_biome.emi.EmiLookupContext;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = EmiApi.class, remap = false)
public abstract class EmiApiMixin {

    @Inject(method = "setPages", at = @At("HEAD"))
    private static void jeiBiome$captureLookup(Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient lookup, CallbackInfo ci) {
        EmiLookupContext.setCurrentLookup(lookup);
    }
}
