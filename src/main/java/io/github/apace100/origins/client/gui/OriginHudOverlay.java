package io.github.apace100.origins.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;

/**
 * Класс для отображения иконки происхождения в интерфейсе
 */
@Environment(EnvType.CLIENT)
public class OriginHudOverlay {
    
    private static final Identifier INVENTORY_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory_overlay.png");
    private static final Identifier MINER_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/miner.png");
    private static final Identifier BREWER_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/brewer.png");
    private static final Identifier COOK_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/chef.png");
    private static final Identifier BLACKSMITH_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/customhp.png");
    private static final Identifier WARRIOR_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/war.png");
    private static final Identifier COURIER_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/yandex.png");
    private static final Identifier MISSING_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/missing.png");
    
    /**
     * Отображает иконку происхождения в инвентаре
     */
    public static void renderOriginIcon(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Проверяем, что открыт инвентарь
        if (client.currentScreen instanceof InventoryScreen) {
            // Получаем компонент происхождения игрока
            OriginComponent component = ModComponents.ORIGIN.get(client.player);
            Origin origin = component.getOrigin(OriginLayers.getLayer(new Identifier(Origins.MODID, "origin")));
            
            // Если у игрока есть происхождение, отображаем его иконку
            if (origin != null && origin.getIdentifier() != null) {
                String originId = origin.getIdentifier().getPath();
                
                // Координаты для отображения иконки (центр верхней части инвентаря)
                int width = client.getWindow().getScaledWidth();
                int height = client.getWindow().getScaledHeight();
                
                // Размещаем иконку в центре верхней части инвентаря
                int x = width / 2 - 8; // 8 - половина ширины иконки (16/2)
                int y = height / 2 - 100; // Поднимаем выше над текстом "Создание"
                
                // Отображаем фон для иконки
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShaderTexture(0, INVENTORY_TEXTURE);
                context.drawTexture(INVENTORY_TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
                
                // Выбираем текстуру иконки в зависимости от происхождения
                Identifier iconTexture;
                switch (originId) {
                    case "miner":
                        iconTexture = MINER_TEXTURE;
                        break;
                    case "brewer":
                        iconTexture = BREWER_TEXTURE;
                        break;
                    case "cook":
                        iconTexture = COOK_TEXTURE;
                        break;
                    case "blacksmith":
                        iconTexture = BLACKSMITH_TEXTURE;
                        break;
                    case "warrior":
                        iconTexture = WARRIOR_TEXTURE;
                        break;
                    case "courier":
                        iconTexture = COURIER_TEXTURE;
                        break;
                    default:
                        iconTexture = MISSING_TEXTURE;
                        break;
                }
                
                // Отображаем иконку происхождения
                RenderSystem.setShaderTexture(0, iconTexture);
                context.drawTexture(iconTexture, x + 4, y + 4, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}