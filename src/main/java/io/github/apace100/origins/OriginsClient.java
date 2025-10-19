package io.github.apace100.origins;

import io.github.apace100.origins.client.SkillKeybinds;
import io.github.apace100.origins.client.gui.OriginHudOverlay;
import io.github.apace100.origins.quest.BountyBoardScreen;
import io.github.apace100.origins.quest.BountyBoardScreenHandler;
import io.github.apace100.origins.quest.ClassBountyBoardScreen;
import io.github.apace100.origins.quest.QuestRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        
        // Регистрируем кейбинды курьера
        io.github.apace100.origins.courier.CourierKeybinds.register();
        
        // Регистрируем клиентские обработчики пакетов курьера
        io.github.apace100.origins.courier.client.CourierClientPacketHandler.registerClientHandlers();
        
        // Кейбинд убран по запросу
        
        // Регистрируем экран доски объявлений с проверкой типа доски
        HandledScreens.register(QuestRegistry.BOUNTY_BOARD_SCREEN_HANDLER, 
            (BountyBoardScreenHandler handler, PlayerInventory inventory, Text title) -> {
                // Проверяем, является ли это классовой доской
                if (handler.isClassBoard()) {
                    return new ClassBountyBoardScreen(handler, inventory, title);
                } else {
                    return new BountyBoardScreen(handler, inventory, title);
                }
            });
        
        // Регистрируем HUD рендер
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            OriginHudOverlay.render(drawContext, tickDelta);
        });
        
        // Регистрируем предикаты модели для квестов Bountiful
        io.github.apace100.origins.quest.BountifulQuestItem.registerModelPredicates();
        
        // Регистрируем клиентский обновлятель времени билетов квестов
        io.github.apace100.origins.quest.QuestTicketClientUpdater.register();
    }
}