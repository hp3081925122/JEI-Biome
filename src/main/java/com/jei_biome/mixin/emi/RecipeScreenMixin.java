package com.jei_biome.mixin.emi;

import com.jei_biome.emi.EmiScrollableWidget;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin {

    @Shadow
    private List<WidgetGroup> currentPage;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void jeiBiome$clickRecipeWidgets(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        if (currentPage == null) {
            return;
        }
        for (WidgetGroup group : currentPage) {
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            if (!new Bounds(group.x(), group.y(), group.getWidth(), group.getHeight()).contains(screenX, screenY)) {
                continue;
            }
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget && widget.getBounds().contains(localX, localY) && widget.mouseClicked(localX, localY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void jeiBiome$scrollRecipeWidgets(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        if (currentPage == null) {
            return;
        }
        for (WidgetGroup group : currentPage) {
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            if (!new Bounds(group.x(), group.y(), group.getWidth(), group.getHeight()).contains(screenX, screenY)) {
                continue;
            }
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget scrollable && scrollable.jeiBiome$mouseScrolled(localX, localY, amount)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void jeiBiome$dragRecipeWidgets(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        if (currentPage == null) {
            return;
        }
        for (WidgetGroup group : currentPage) {
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget scrollable && scrollable.jeiBiome$mouseDragged(localX, localY, button, dragX, dragY)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void jeiBiome$releaseRecipeWidgets(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        if (currentPage == null) {
            return;
        }
        for (WidgetGroup group : currentPage) {
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget scrollable && scrollable.jeiBiome$mouseReleased(localX, localY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
