package io.github.apace100.origins.courier;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CourierKeybinds {
    public static final String CATEGORY = "key.category.courier";
    
    public static KeyBinding openOrdersScreen;
    
    public static void register() {
        openOrdersScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.courier.open_orders",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
        ));
    }
}