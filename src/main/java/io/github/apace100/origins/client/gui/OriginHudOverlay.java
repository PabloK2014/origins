package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * HUD оверлей для отображения информации о прогрессии происхождения
 */
public class OriginHudOverlay {
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final int HUD_WIDTH = 150;
    private static final int HUD_HEIGHT = 45;
    
    public static void render(DrawContext context, float tickDelta) {
        if (client.player == null || client.options.debugEnabled) {
            return;
        }
        // Проверка на скрытие HUD через конфиг
        if (!io.github.apace100.origins.Origins.config.showHudOverlay) {
            return;
        }
        // Получаем информацию о текущем происхождении
        OriginComponent originComponent = ModComponents.ORIGIN.get(client.player);
        Origin origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
        if (origin == null) {
            return;
        }
        // Получаем прогрессию
        ProfessionComponent professionComponent = ProfessionComponent.KEY.get(client.player);
        ProfessionProgress progress = professionComponent.getCurrentProgress();
        if (progress == null) {
            return;
        }
        // Получаем навыки
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
        // Позиция HUD (левый верхний угол)
        int hudX = 10;
        int hudY = 10;
        renderHud(context, hudX, hudY, origin, progress, skillComponent);
    }
    
    private static void renderHud(DrawContext context, int x, int y, Origin origin, 
                                 ProfessionProgress progress, PlayerSkillComponent skillComponent) {
        
        // Фон HUD
        context.fill(x, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0x80000000);
        context.drawBorder(x, y, HUD_WIDTH, HUD_HEIGHT, 0xFF555555);
        
        // Иконка происхождения
        Identifier iconTexture = getOriginIcon(origin.getIdentifier().toString());
        context.drawTexture(iconTexture, x + 5, y + 5, 0, 0, 16, 16, 16, 16);
        
        // Название происхождения
        String originName = getProfessionDisplayName(origin.getIdentifier().toString());
        context.drawTextWithShadow(client.textRenderer, originName, x + 25, y + 5, 0xFFFFFF);
        
        // Уровень
        String levelText = "Ур. " + progress.getLevel();
        context.drawTextWithShadow(client.textRenderer, levelText, x + 25, y + 17, 0xFFFF55);
        
        // Полоса опыта
        int barX = x + 5;
        int barY = y + 30;
        int barWidth = HUD_WIDTH - 10;
        int barHeight = 8;
        
        drawExperienceBar(context, barX, barY, barWidth, barHeight, progress);
        
        // Доступные очки навыков
        int availablePoints = skillComponent.getAvailableSkillPoints();
        if (availablePoints > 0) {
            String pointsText = "+" + availablePoints + " очков навыков";
            context.drawTextWithShadow(client.textRenderer, pointsText, x + 5, y + 42, 0xFF55FF55);
        }
    }
    
    private static void drawExperienceBar(DrawContext context, int x, int y, int width, int height, 
                                        ProfessionProgress progress) {
        // Фон полосы
        context.fill(x, y, x + width, y + height, 0xFF333333);
        context.drawBorder(x, y, width, height, 0xFF666666);
        
        // Заполненная часть
        double progressPercent = progress.getProgressToNextLevel();
        int filledWidth = (int) (width * progressPercent);
        
        if (filledWidth > 0) {
            // Градиент от синего к зеленому
            int color = interpolateColor(0xFF0066CC, 0xFF00CC66, progressPercent);
            context.fill(x + 1, y + 1, x + filledWidth - 1, y + height - 1, color);
        }
        
        // Текст опыта
        String expText = progress.getExperience() + "/" + progress.getExperienceForNextLevel();
        int textWidth = client.textRenderer.getWidth(expText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - client.textRenderer.fontHeight) / 2;
        
        // Тень для лучшей читаемости
        context.drawText(client.textRenderer, expText, textX + 1, textY + 1, 0xFF000000, false);
        context.drawText(client.textRenderer, expText, textX, textY, 0xFFFFFFFF, false);
    }
    
    private static Identifier getOriginIcon(String originId) {
        String iconPath = switch (originId) {
            case "origins:blacksmith" -> "textures/gui/icons/blacksmith.png";
            case "origins:brewer" -> "textures/gui/icons/brewer.png";
            case "origins:cook" -> "textures/gui/icons/cook.png";
            case "origins:courier" -> "textures/gui/icons/courier.png";
            case "origins:warrior" -> "textures/gui/icons/warrior.png";
            case "origins:miner" -> "textures/gui/icons/miner.png";
            case "origins:human" -> "textures/gui/icons/human.png";
            default -> "textures/gui/icons/default.png";
        };
        
        return new Identifier(Origins.MODID, iconPath);
    }
    
    private static String getProfessionDisplayName(String professionId) {
        return switch (professionId) {
            case "origins:blacksmith" -> "🔨 Кузнец";
            case "origins:brewer" -> "🍺 Пивовар";
            case "origins:cook" -> "👨‍🍳 Повар";
            case "origins:courier" -> "📦 Курьер";
            case "origins:warrior" -> "⚔️ Воин";
            case "origins:miner" -> "⛏️ Шахтер";
            case "origins:human" -> "👤 Человек";
            default -> professionId.replace("origins:", "").replace("_", " ");
        };
    }
    
    private static int interpolateColor(int color1, int color2, double factor) {
        if (factor < 0) factor = 0;
        if (factor > 1) factor = 1;
        
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}