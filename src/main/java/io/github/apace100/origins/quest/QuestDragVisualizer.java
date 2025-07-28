package io.github.apace100.origins.quest;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Визуализатор для drag-and-drop операций с квестами
 * Основан на визуальных эффектах из оригинального Bountiful мода
 */
public class QuestDragVisualizer {
    private static final Identifier SLOT_HIGHLIGHT_TEXTURE = new Identifier("textures/gui/container/generic_54.png");
    
    // Цвета для различных состояний
    private static final int VALID_DROP_COLOR = 0x80FFFFFF;      // Белый с прозрачностью
    private static final int INVALID_DROP_COLOR = 0x80FF0000;    // Красный с прозрачностью
    private static final int HOVER_COLOR = 0x80FFFF00;           // Желтый с прозрачностью
    private static final int SELECTED_COLOR = 0x8000FF00;        // Зеленый с прозрачностью
    
    /**
     * Отрисовывает подсветку слота
     */
    public static void renderSlotHighlight(DrawContext context, int x, int y, int width, int height, HighlightType type) {
        int color = switch (type) {
            case VALID_DROP -> VALID_DROP_COLOR;
            case INVALID_DROP -> INVALID_DROP_COLOR;
            case HOVER -> HOVER_COLOR;
            case SELECTED -> SELECTED_COLOR;
        };
        
        // Отрисовываем подсветку как полупрозрачный прямоугольник
        context.fill(x, y, x + width, y + height, color);
        
        // Добавляем рамку для лучшей видимости
        int borderColor = (color & 0x00FFFFFF) | 0xFF000000; // Убираем прозрачность для рамки
        drawBorder(context, x, y, width, height, borderColor);
    }
    
    /**
     * Отрисовывает анимированную подсветку слота
     */
    public static void renderAnimatedSlotHighlight(DrawContext context, int x, int y, int width, int height, 
                                                  HighlightType type, long animationTime) {
        // Создаем пульсирующий эффект
        float pulse = (float) (Math.sin(animationTime * 0.01) * 0.3 + 0.7);
        
        int baseColor = switch (type) {
            case VALID_DROP -> VALID_DROP_COLOR;
            case INVALID_DROP -> INVALID_DROP_COLOR;
            case HOVER -> HOVER_COLOR;
            case SELECTED -> SELECTED_COLOR;
        };
        
        // Применяем пульсацию к альфа-каналу
        int alpha = (int) ((baseColor >>> 24) * pulse);
        int color = (baseColor & 0x00FFFFFF) | (alpha << 24);
        
        context.fill(x, y, x + width, y + height, color);
        
        // Рамка тоже пульсирует
        int borderAlpha = (int) (255 * pulse);
        int borderColor = (baseColor & 0x00FFFFFF) | (borderAlpha << 24);
        drawBorder(context, x, y, width, height, borderColor);
    }
    
    /**
     * Отрисовывает индикатор запрета перетаскивания
     */
    public static void renderDropProhibitedIndicator(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Отрисовываем красный крестик
        int size = 16;
        int x = mouseX - size / 2;
        int y = mouseY - size / 2;
        
        // Фон крестика
        context.fill(x, y, x + size, y + size, 0x80000000);
        
        // Линии крестика
        int lineColor = 0xFFFF0000;
        
        // Диагональ слева направо
        for (int i = 0; i < size; i++) {
            context.fill(x + i, y + i, x + i + 1, y + i + 1, lineColor);
            context.fill(x + i, y + i + 1, x + i + 1, y + i + 2, lineColor);
        }
        
        // Диагональ справа налево
        for (int i = 0; i < size; i++) {
            context.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, lineColor);
            context.fill(x + size - 1 - i, y + i + 1, x + size - i, y + i + 2, lineColor);
        }
    }
    
    /**
     * Отрисовывает индикатор валидного места для сброса
     */
    public static void renderDropValidIndicator(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Отрисовываем зеленую галочку
        int size = 16;
        int x = mouseX - size / 2;
        int y = mouseY - size / 2;
        
        // Фон галочки
        context.fill(x, y, x + size, y + size, 0x80000000);
        
        // Линии галочки
        int lineColor = 0xFF00FF00;
        
        // Короткая линия галочки
        for (int i = 0; i < 6; i++) {
            context.fill(x + 4 + i, y + 8 + i, x + 5 + i, y + 9 + i, lineColor);
            context.fill(x + 4 + i, y + 9 + i, x + 5 + i, y + 10 + i, lineColor);
        }
        
        // Длинная линия галочки
        for (int i = 0; i < 8; i++) {
            context.fill(x + 10 - i, y + 4 + i, x + 11 - i, y + 5 + i, lineColor);
            context.fill(x + 10 - i, y + 5 + i, x + 11 - i, y + 6 + i, lineColor);
        }
    }
    
    /**
     * Отрисовывает траекторию перетаскивания
     */
    public static void renderDragTrail(DrawContext context, int startX, int startY, int endX, int endY, 
                                      float alpha, int color) {
        if (alpha <= 0) return;
        
        // Создаем градиентную линию от начальной до конечной точки
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            float progress = (float) i / steps;
            int x = (int) MathHelper.lerp(progress, startX, endX);
            int y = (int) MathHelper.lerp(progress, startY, endY);
            
            // Уменьшаем альфу по мере удаления от курсора
            float stepAlpha = alpha * (1f - progress * 0.7f);
            int stepColor = (color & 0x00FFFFFF) | ((int) (stepAlpha * 255) << 24);
            
            // Отрисовываем точку
            context.fill(x - 1, y - 1, x + 1, y + 1, stepColor);
        }
    }
    
    /**
     * Отрисовывает информацию о перетаскиваемом квесте
     */
    public static void renderDragInfo(DrawContext context, Quest quest, int mouseX, int mouseY) {
        if (quest == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Отрисовываем название квеста рядом с курсором
        String questTitle = quest.getTitle();
        int textWidth = client.textRenderer.getWidth(questTitle);
        int textHeight = client.textRenderer.fontHeight;
        
        int textX = mouseX + 16; // Смещение от курсора
        int textY = mouseY - textHeight - 4;
        
        // Фон для текста
        context.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + textHeight + 2, 0x80000000);
        
        // Текст с цветом редкости
        int textColor = quest.getRarity().getColor().getColorValue() != null ? 
            quest.getRarity().getColor().getColorValue() : 0xFFFFFF;
        
        context.drawText(client.textRenderer, questTitle, textX, textY, textColor, false);
        
        // Дополнительная информация
        String classInfo = "Класс: " + getClassDisplayName(quest.getPlayerClass());
        int classTextY = textY + textHeight + 2;
        context.drawText(client.textRenderer, classInfo, textX, classTextY, 0xAAAAAA, false);
    }
    
    /**
     * Отрисовывает рамку вокруг области
     */
    private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        // Верхняя линия
        context.fill(x, y, x + width, y + 1, color);
        // Нижняя линия
        context.fill(x, y + height - 1, x + width, y + height, color);
        // Левая линия
        context.fill(x, y, x + 1, y + height, color);
        // Правая линия
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
    
    /**
     * Получает отображаемое название класса
     */
    private static String getClassDisplayName(String playerClass) {
        return switch (playerClass) {
            case "warrior" -> "Воин";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "courier" -> "Курьер";
            case "brewer" -> "Пивовар";
            case "cook" -> "Повар";
            case "any" -> "Любой";
            default -> playerClass;
        };
    }
    
    /**
     * Типы подсветки слотов
     */
    public enum HighlightType {
        VALID_DROP,    // Валидное место для сброса
        INVALID_DROP,  // Невалидное место для сброса
        HOVER,         // Наведение мыши
        SELECTED       // Выбранный слот
    }
}