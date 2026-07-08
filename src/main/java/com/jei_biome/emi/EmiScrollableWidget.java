package com.jei_biome.emi;

public interface EmiScrollableWidget {

    boolean jeiBiome$mouseScrolled(int mouseX, int mouseY, double delta);

    boolean jeiBiome$mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY);

    boolean jeiBiome$mouseReleased(int mouseX, int mouseY, int button);
}
