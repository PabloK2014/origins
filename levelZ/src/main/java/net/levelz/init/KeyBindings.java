package net.levelz.init;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.levelz.screen.EnergyScreen;

public class KeyBindings {
    public static KeyMapping OPEN_ENERGY_SCREEN;
    
    public static void register() {
        OPEN_ENERGY_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.levelz.open_energy_screen",
            GLFW.GLFW_KEY_K,
            "category.levelz.general"
        ));
        
        // Register the key press handler
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_ENERGY_SCREEN.consumeClick()) {
                if (client.player != null) {
                    Minecraft.getInstance().setScreen(new EnergyScreen(
                        ((net.levelz.mixin.PlayerEntityMixin) client.player).getLevelZEnergy()
                    ));
                }
            }
        });
    }
} 