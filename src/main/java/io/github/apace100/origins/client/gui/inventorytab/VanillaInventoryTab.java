package io.github.apace100.origins.client.gui.inventorytab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VanillaInventoryTab extends InventoryTab {
    public VanillaInventoryTab(Text title, Identifier texture, int preferredPos, Class<?>... screenClasses) {
        super(title, texture, preferredPos, screenClasses);
    }

    @Override
    public void onClick(MinecraftClient client) {
        client.setScreen(new InventoryScreen(client.player));
    }
} 