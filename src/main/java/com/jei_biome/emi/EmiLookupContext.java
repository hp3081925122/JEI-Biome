package com.jei_biome.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

public final class EmiLookupContext {

    private static EmiIngredient currentLookup = EmiStack.EMPTY;

    private EmiLookupContext() {
    }

    public static void setCurrentLookup(EmiIngredient lookup) {
        currentLookup = lookup == null ? EmiStack.EMPTY : lookup.copy();
    }

    public static EmiIngredient currentLookup() {
        return currentLookup;
    }
}
