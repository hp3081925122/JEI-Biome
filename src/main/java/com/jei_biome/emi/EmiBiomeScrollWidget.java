package com.jei_biome.emi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class EmiBiomeScrollWidget extends Widget implements EmiScrollableWidget {

    private final EmiRecipe recipe;
    private final List<Entry> entries;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int contentHeight;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public EmiBiomeScrollWidget(EmiRecipe recipe, List<Entry> entries, int x, int y, int width, int height) {
        this.recipe = recipe;
        this.entries = List.copyOf(entries);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        int maxY = 0;
        for (Entry entry : entries) {
            maxY = Math.max(maxY, entry.y + entry.height);
        }
        this.contentHeight = Math.max(height, maxY + 4);
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        drawPanel(guiGraphics);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        int screenX = Math.round(matrix.m30());
        int screenY = Math.round(matrix.m31());
        guiGraphics.enableScissor(screenX + x + 1, screenY + y + 1, screenX + x + width - 14, screenY + y + height - 1);
        for (Entry entry : entries) {
            int drawY = y + entry.y - scrollOffset;
            if (drawY + entry.height < y || drawY > y + height) {
                continue;
            }
            if (entry.text != null) {
                guiGraphics.drawString(EmiBiomeText.font(), entry.text, x + entry.x, drawY, entry.color, false);
            } else if (entry.stack != null && !entry.stack.isEmpty()) {
                SlotWidget slot = new SlotWidget(entry.stack, x + entry.x, drawY)
                        .drawBack(true)
                        .recipeContext(recipe);
                for (Component tooltipLine : entry.tooltip) {
                    slot.appendTooltip(tooltipLine);
                }
                slot.render(guiGraphics, mouseX, mouseY, delta);
            }
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        Entry entry = hoveredEntry(mouseX, mouseY);
        if (entry == null || entry.stack == null || entry.stack.isEmpty()) {
            return List.of();
        }
        SlotWidget slot = new SlotWidget(entry.stack, x + entry.x, y + entry.y - scrollOffset)
                .drawBack(true)
                .recipeContext(recipe);
        for (Component tooltipLine : entry.tooltip) {
            slot.appendTooltip(tooltipLine);
        }
        return slot.getTooltip(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && contentHeight > height && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollbarGrabOffset = mouseY >= scrollbarThumbY() && mouseY < scrollbarThumbY() + scrollbarThumbHeight()
                    ? mouseY - scrollbarThumbY()
                    : scrollbarThumbHeight() / 2;
            updateScrollFromMouse(mouseY);
            return true;
        }
        Entry entry = hoveredEntry(mouseX, mouseY);
        if (entry == null || entry.stack == null || entry.stack.isEmpty()) {
            return false;
        }
        SlotWidget slot = new SlotWidget(entry.stack, x + entry.x, y + entry.y - scrollOffset)
                .drawBack(true)
                .recipeContext(recipe);
        return slot.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean jeiBiome$mouseScrolled(int mouseX, int mouseY, double delta) {
        if (!getBounds().contains(mouseX, mouseY) || contentHeight <= height) {
            return false;
        }
        scrollOffset = clamp(scrollOffset - (int) Math.signum(delta) * 18, 0, contentHeight - height);
        return true;
    }

    @Override
    public boolean jeiBiome$mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar || button != 0) {
            return false;
        }
        updateScrollFromMouse(mouseY);
        return true;
    }

    @Override
    public boolean jeiBiome$mouseReleased(int mouseX, int mouseY, int button) {
        if (!draggingScrollbar || button != 0) {
            return false;
        }
        draggingScrollbar = false;
        return true;
    }

    private Entry hoveredEntry(int mouseX, int mouseY) {
        if (!getBounds().contains(mouseX, mouseY)) {
            return null;
        }
        int contentX = mouseX - x;
        int contentY = mouseY - y + scrollOffset;
        for (Entry entry : entries) {
            if (entry.stack != null && contentX >= entry.x && contentX < entry.x + 18 && contentY >= entry.y && contentY < entry.y + 18) {
                return entry;
            }
        }
        return null;
    }

    private void drawPanel(GuiGraphics guiGraphics) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    private void drawScrollbar(GuiGraphics guiGraphics) {
        int barX = x + width - 12;
        guiGraphics.fill(barX, y + 4, barX + 8, y + height - 4, 0xFFB5B5B5);
        if (contentHeight <= height) {
            guiGraphics.fill(barX + 1, y + 5, barX + 7, y + height - 5, 0xFFE0E0E0);
            return;
        }
        guiGraphics.fill(barX + 1, scrollbarThumbY(), barX + 7, scrollbarThumbY() + scrollbarThumbHeight(), 0xFF8A8A8A);
    }

    private boolean isOverScrollbar(int mouseX, int mouseY) {
        int barX = x + width - 12;
        return mouseX >= barX && mouseX < barX + 8 && mouseY >= y + 4 && mouseY < y + height - 4;
    }

    private int scrollbarTrackHeight() {
        return height - 10;
    }

    private int scrollbarThumbHeight() {
        return Math.max(16, height * scrollbarTrackHeight() / contentHeight);
    }

    private int scrollbarThumbY() {
        return y + 5 + scrollOffset * (scrollbarTrackHeight() - scrollbarThumbHeight()) / Math.max(1, contentHeight - height);
    }

    private void updateScrollFromMouse(int mouseY) {
        int trackY = y + 5;
        int availableTrack = Math.max(1, scrollbarTrackHeight() - scrollbarThumbHeight());
        int thumbTop = clamp(mouseY - scrollbarGrabOffset, trackY, trackY + availableTrack);
        scrollOffset = clamp((thumbTop - trackY) * (contentHeight - height) / availableTrack, 0, contentHeight - height);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<Entry> entries = new ArrayList<>();

        public int addWrappedText(Component text, int x, int y, int color) {
            int currentY = y;
            for (FormattedCharSequence line : EmiBiomeText.split(text)) {
                entries.add(new Entry(x, currentY, EmiBiomeText.TEXT_WIDTH, EmiBiomeText.LINE_HEIGHT, line, color, null, List.of()));
                currentY += EmiBiomeText.LINE_HEIGHT;
            }
            return y + EmiBiomeText.wrappedHeight(text);
        }

        public void addStack(EmiIngredient stack, int x, int y, List<Component> tooltip) {
            entries.add(new Entry(x, y, 18, 18, null, 0, stack, List.copyOf(tooltip)));
        }

        public EmiBiomeScrollWidget build(EmiRecipe recipe, int x, int y, int width, int height) {
            return new EmiBiomeScrollWidget(recipe, entries, x, y, width, height);
        }
    }

    private record Entry(int x, int y, int width, int height, FormattedCharSequence text, int color, EmiIngredient stack, List<Component> tooltip) {
    }
}
