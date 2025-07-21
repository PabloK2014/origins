package io.github.apace100.origins.client.gui.inventorytab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import io.github.apace100.origins.client.gui.LevelZSkillScreen;

public class LevelzTab extends InventoryTab {
    public LevelzTab(Text title, Identifier texture, int preferredPos, Class<?>... screenClasses) {
        super(title, texture, preferredPos, screenClasses);
    }

    @Override
    public boolean canClick(Class<?> screenClass, MinecraftClient client) {
        // Можно добавить дополнительные условия, если нужно
        return super.canClick(screenClass, client);
    }

    @Override
    public void onClick(MinecraftClient client) {
        client.setScreen(new LevelZSkillScreen());
    }
} 