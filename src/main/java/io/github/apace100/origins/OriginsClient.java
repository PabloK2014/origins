package io.github.apace100.origins;

import io.github.apace100.origins.client.SkillKeybinds;
import io.github.apace100.origins.client.gui.OriginHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Клиентский инициализатор Origins
 */
public class OriginsClient implements ClientModInitializer {
    
    // Флаг для отслеживания, запущен ли Origins на сервере
    public static boolean isServerRunningOrigins = false;
    
    @Override
    public void onInitializeClient() {
        // Регистрируем клиентские обработчики пакетов
        io.github.apace100.origins.networking.ModPacketsS2C.register();
        
        // Инициализируем клавиши навыков
        new SkillKeybinds().onInitializeClient();
        
        // Регистрируем HUD рендер
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            OriginHudOverlay.render(drawContext, tickDelta);
        });
    }
}