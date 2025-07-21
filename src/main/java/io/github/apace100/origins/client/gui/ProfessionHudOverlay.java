package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * HUD-элемент для отображения прогресса профессии
 */
@Environment(EnvType.CLIENT)
public class ProfessionHudOverlay {
    
    private static final Identifier PROGRESS_BAR_TEXTURE = new Identifier(Origins.MODID, "textures/gui/progress_bar.png");
    private static final int PROGRESS_BAR_WIDTH = 182;
    private static final int PROGRESS_BAR_HEIGHT = 5;
    
    /**
     * Отображает прогресс профессии в HUD
     */
    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Проверяем, что игрок существует и не открыт другой экран
        if (client.player == null || client.currentScreen != null) return;
        
        // Получаем компонент профессии игрока
        ProfessionComponent component = ProfessionComponent.KEY.get(client.player);
        ProfessionProgress progress = component.getCurrentProgress();
        
        // Если нет прогресса, не отображаем ничего
        if (progress == null) return;
        
        // Координаты для отображения (правый верхний угол)
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        int x = width - PROGRESS_BAR_WIDTH - 10;
        int y = 10;
        
        // Отображаем уровень
        Text levelText = Text.translatable("gui.origins.level_short", progress.getLevel())
            .formatted(Formatting.GOLD);
        context.drawText(client.textRenderer, levelText, x, y, 0xFFFFFF, true);
        
        y += 12;
        
        // Фон полосы прогресса
        context.drawTexture(PROGRESS_BAR_TEXTURE, 
            x, y, 
            0, 0, 
            PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        
        // Заполнение полосы прогресса
        int fillWidth = (int)(PROGRESS_BAR_WIDTH * progress.getLevelProgress());
        context.drawTexture(PROGRESS_BAR_TEXTURE, 
            x, y, 
            0, PROGRESS_BAR_HEIGHT, 
            fillWidth, PROGRESS_BAR_HEIGHT);
    }
}