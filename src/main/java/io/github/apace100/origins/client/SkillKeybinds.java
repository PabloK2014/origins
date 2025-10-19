package io.github.apace100.origins.client;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.networking.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Система клавиш для управления навыками
 */
public class SkillKeybinds implements ClientModInitializer {
    
    // Идентификаторы пакетов используются из ModPackets
    
    // Клавиши для управления навыками
    private static KeyBinding activateGlobalSkillKey;  // Кнопка активации глобального навыка класса - G
    private static KeyBinding activateActiveSkillKey;  // Кнопка активации выбранного активного навыка - K
    private static KeyBinding openSkillSelectionKey;   // Кнопка открытия GUI выбора навыка - L
    
    @Override
    public void onInitializeClient() {
        // Проверяем, что клавиши еще не зарегистрированы
        if (activateGlobalSkillKey != null || activateActiveSkillKey != null || openSkillSelectionKey != null) {
            Origins.LOGGER.warn("SkillKeybinds already initialized, skipping registration");
            return;
        }
        
                
        try {
            // Регистрируем клавишу G для глобального навыка класса
            activateGlobalSkillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.origins.activate_global_skill", // Ключ локализации
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G, // Клавиша G для глобального навыка класса
                "category.origins.skills" // Категория
            ));
            
            activateActiveSkillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.origins.activate_active_skill", // Ключ локализации
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K, // Клавиша K для активации выбранного навыка
                "category.origins.skills" // Категория
            ));
            
            openSkillSelectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.origins.open_skill_selection", // Ключ локализации
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L, // Клавиша L для открытия GUI выбора навыка
                "category.origins.skills" // Категория
            ));
            

            
                    } catch (Exception e) {
            Origins.LOGGER.error("Failed to register Origins skill keybindings: " + e.getMessage(), e);
        }
        
        // Регистрируем обработчик нажатий клавиш
        ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
            if (tickClient.player == null) return;
            
            // Проверяем нажатие клавиши активации активного навыка
            if (activateActiveSkillKey.wasPressed()) {
                                showKeybindFeedback("Активация навыка...", Formatting.YELLOW);
                sendActivateActiveSkillPacket();
            }
            
            // Проверяем нажатие клавиши открытия GUI выбора навыка
            if (openSkillSelectionKey.wasPressed()) {
                                showKeybindFeedback("Открытие меню навыков...", Formatting.BLUE);
                openSkillSelectionGUI();
            }
            
            // Проверяем нажатие клавиши активации глобального навыка класса
            if (activateGlobalSkillKey.wasPressed()) {
                                showKeybindFeedback("Активация глобального навыка...", Formatting.GREEN);
                sendActivateGlobalSkillPacket();
            }
        });
    }
    

    
    /**
     * Отправляет пакет активации выбранного активного навыка на сервер
     */
    private void sendActivateActiveSkillPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(ModPackets.ACTIVATE_ACTIVE_SKILL, buf);
    }
    
    /**
     * Открывает GUI для выбора активного навыка
     */
    private void openSkillSelectionGUI() {
        // Открываем GUI на клиенте
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            client.setScreen(new io.github.apace100.origins.client.gui.SkillSelectionScreen());
        }
    }

    /**
     * Отправляет пакет активации глобального навыка класса на сервер
     */
    private void sendActivateGlobalSkillPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(ModPackets.ACTIVATE_GLOBAL_SKILL, buf);
    }
    
    /**
     * Показывает визуальную обратную связь при нажатии клавиш
     */
    private static void showKeybindFeedback(String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Показываем сообщение в action bar (над хотбаром)
            Text feedbackText = Text.literal(message).formatted(color);
            client.player.sendMessage(feedbackText, true);
            
                    }
    }
    
    /**
     * Проверяет состояние клавиш для диагностики
     */
    public static void runKeybindingDiagnostic() {
                
        if (activateGlobalSkillKey == null) {
            Origins.LOGGER.error("Клавиша глобального навыка (G) не зарегистрирована!");
        } else {
            Origins.LOGGER.info("Клавиша глобального навыка: {} -> {}", 
                activateGlobalSkillKey.getTranslationKey(), 
                activateGlobalSkillKey.getBoundKeyLocalizedText().getString());
        }
        
        if (activateActiveSkillKey == null) {
            Origins.LOGGER.error("Клавиша активного навыка (K) не зарегистрирована!");
        } else {
            Origins.LOGGER.info("Клавиша активного навыка: {} -> {}", 
                activateActiveSkillKey.getTranslationKey(), 
                activateActiveSkillKey.getBoundKeyLocalizedText().getString());
        }
        
        if (openSkillSelectionKey == null) {
            Origins.LOGGER.error("Клавиша выбора навыка (L) не зарегистрирована!");
        } else {
            Origins.LOGGER.info("Клавиша выбора навыка: {} -> {}", 
                openSkillSelectionKey.getTranslationKey(), 
                openSkillSelectionKey.getBoundKeyLocalizedText().getString());
        }
        
            }
    
    /**
     * Получает статус клавиш для внешней диагностики
     */
    public static boolean areKeybindingsRegistered() {
        return activateGlobalSkillKey != null && 
               activateActiveSkillKey != null && 
               openSkillSelectionKey != null;
    }
    
    /**
     * Получает информацию о зарегистрированных клавишах
     */
    public static String getKeybindingInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Origins Keybindings Status:\n");
        
        if (activateGlobalSkillKey != null) {
            info.append("- Global Skill (G): ").append(activateGlobalSkillKey.getBoundKeyLocalizedText().getString()).append("\n");
        } else {
            info.append("- Global Skill (G): NOT REGISTERED\n");
        }
        
        if (activateActiveSkillKey != null) {
            info.append("- Active Skill (K): ").append(activateActiveSkillKey.getBoundKeyLocalizedText().getString()).append("\n");
        } else {
            info.append("- Active Skill (K): NOT REGISTERED\n");
        }
        
        if (openSkillSelectionKey != null) {
            info.append("- Skill Selection (L): ").append(openSkillSelectionKey.getBoundKeyLocalizedText().getString()).append("\n");
        } else {
            info.append("- Skill Selection (L): NOT REGISTERED\n");
        }
        
        return info.toString();
    }
}