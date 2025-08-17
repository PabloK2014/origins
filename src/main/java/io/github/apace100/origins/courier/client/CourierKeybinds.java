package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Кейбинды для системы заказов курьера
 */
public class CourierKeybinds {
    
    private static KeyBinding openOrdersKey;
    private static KeyBinding createOrderKey;
    
    /**
     * Регистрирует кейбинды
     */
    public static void register() {
        // Кейбинд для открытия списка заказов
        openOrdersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.origins.open_orders",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.origins.courier"
        ));
        
        // Кейбинд для создания заказа
        createOrderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.origins.create_order",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.origins.courier"
        ));
        
        // Регистрируем обработчик нажатий клавиш
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openOrdersKey.wasPressed()) {
                ClientOrderManager.getInstance().openOrdersScreen();
            }
            
            while (createOrderKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new CreateOrderScreen());
            }
        });
        
            }
}