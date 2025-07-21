package io.github.apace100.origins.client.gui.inventorytab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public abstract class InventoryTab {
    protected final Text title;
    protected final Identifier texture;
    protected final int preferredPos;
    protected final Class<?>[] screenClasses;

    public InventoryTab(Text title, Identifier texture, int preferredPos, Class<?>... screenClasses) {
        this.title = title;
        this.texture = texture;
        this.preferredPos = preferredPos;
        this.screenClasses = screenClasses;
    }

    public boolean canClick(Class<?> screenClass, MinecraftClient client) {
        for (Class<?> clazz : screenClasses) {
            if (clazz.equals(screenClass)) {
                return true;
            }
        }
        return false;
    }

    public abstract void onClick(MinecraftClient client);

    public Text getTitle() {
        return title;
    }

    public Identifier getTexture() {
        return texture;
    }

    public int getPreferredPos() {
        return preferredPos;
    }
} 