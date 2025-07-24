package io.github.apace100.origins;

import io.github.apace100.origins.client.SkillKeybinds;
import io.github.apace100.origins.client.gui.OriginHudOverlay;
import io.github.apace100.origins.quest.BountyBoardScreen;
import io.github.apace100.origins.quest.QuestRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

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
        
        // Регистрируем экран доски объявлений
        HandledScreens.register(QuestRegistry.BOUNTY_BOARD_SCREEN_HANDLER, BountyBoardScreen::new);
        
        // Регистрируем HUD рендер
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            OriginHudOverlay.render(drawContext, tickDelta);
        });
    }
}