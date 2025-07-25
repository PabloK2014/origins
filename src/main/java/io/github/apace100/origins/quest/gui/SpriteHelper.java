package io.github.apace100.origins.quest.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Вспомогательный класс для работы со спрайтами интерфейса доски объявлений.
 * Заменяет функциональность KSpriteGrid из оригинального мода Bountiful.
 */
public class SpriteHelper {
    
    // Основные текстуры
    private static final Identifier BOARD_TEXTURE = new Identifier("origins", "textures/gui/container/new_new_board.png");
    private static final Identifier VILLAGER_TEXTURE = new Identifier("textures/gui/container/villager2.png");
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
    
    // Размеры текстур
    private static final int BOARD_TEX_WIDTH = 512;
    private static final int BOARD_TEX_HEIGHT = 512;
    private static final int VILLAGER_TEX_WIDTH = 512;
    private static final int VILLAGER_TEX_HEIGHT = 256;
    private static final int WIDGETS_TEX_WIDTH = 256;
    private static final int WIDGETS_TEX_HEIGHT = 256;
    
    // Координаты спрайтов доски объявлений
    public static final SpriteInfo BOARD_BG_BIG = new SpriteInfo(0, 0, 348, 165);
    public static final SpriteInfo BOARD_BG_SMALL = new SpriteInfo(0, 166, 176, 165);
    public static final SpriteInfo BOARD_HIGHLIGHT = new SpriteInfo(349, 0, 20, 20);
    
    // Координаты спрайтов полосы прогресса (из villager2.png)
    public static final SpriteInfo BAR_BG = new SpriteInfo(0, 186, 102, 5);
    public static final SpriteInfo BAR_FG = new SpriteInfo(0, 191, 102, 5);
    public static final SpriteInfo SLIDER = new SpriteInfo(0, 199, 6, 27);
    
    // Координаты спрайтов кнопок (из widgets.png)
    public static final SpriteInfo BUTTON_DEFAULT = new SpriteInfo(0, 66, 200, 20);
    public static final SpriteInfo BUTTON_CAP = new SpriteInfo(198, 66, 2, 20);
    
    /**
     * Информация о спрайте
     */
    public static class SpriteInfo {
        public final int u, v, width, height;
        
        public SpriteInfo(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Отрисовка спрайта из текстуры доски объявлений
     */
    public static void drawBoardSprite(DrawContext context, int x, int y, SpriteInfo sprite) {
        context.drawTexture(BOARD_TEXTURE, x, y, sprite.u, sprite.v, sprite.width, sprite.height, 
                           BOARD_TEX_WIDTH, BOARD_TEX_HEIGHT);
    }
    
    /**
     * Отрисовка спрайта из текстуры доски объявлений с кастомной шириной
     */
    public static void drawBoardSprite(DrawContext context, int x, int y, SpriteInfo sprite, int customWidth) {
        context.drawTexture(BOARD_TEXTURE, x, y, sprite.u, sprite.v, customWidth, sprite.height, 
                           BOARD_TEX_WIDTH, BOARD_TEX_HEIGHT);
    }
    
    /**
     * Отрисовка спрайта из текстуры villager2.png
     */
    public static void drawVillagerSprite(DrawContext context, int x, int y, SpriteInfo sprite) {
        context.drawTexture(VILLAGER_TEXTURE, x, y, sprite.u, sprite.v, sprite.width, sprite.height, 
                           VILLAGER_TEX_WIDTH, VILLAGER_TEX_HEIGHT);
    }
    
    /**
     * Отрисовка спрайта из текстуры villager2.png с кастомной шириной
     */
    public static void drawVillagerSprite(DrawContext context, int x, int y, SpriteInfo sprite, int customWidth) {
        context.drawTexture(VILLAGER_TEXTURE, x, y, sprite.u, sprite.v, customWidth, sprite.height, 
                           VILLAGER_TEX_WIDTH, VILLAGER_TEX_HEIGHT);
    }
    
    /**
     * Отрисовка спрайта из текстуры widgets.png
     */
    public static void drawWidgetSprite(DrawContext context, int x, int y, SpriteInfo sprite) {
        context.drawTexture(WIDGETS_TEXTURE, x, y, sprite.u, sprite.v, sprite.width, sprite.height, 
                           WIDGETS_TEX_WIDTH, WIDGETS_TEX_HEIGHT);
    }
    
    /**
     * Отрисовка спрайта из текстуры widgets.png с кастомной шириной
     */
    public static void drawWidgetSprite(DrawContext context, int x, int y, SpriteInfo sprite, int customWidth) {
        context.drawTexture(WIDGETS_TEXTURE, x, y, sprite.u, sprite.v, customWidth, sprite.height, 
                           WIDGETS_TEX_WIDTH, WIDGETS_TEX_HEIGHT);
    }
    
    /**
     * Отрисовка фона доски объявлений
     */
    public static void drawBoardBackground(DrawContext context, int x, int y, boolean expanded) {
        if (expanded) {
            drawBoardSprite(context, x, y, BOARD_BG_BIG);
        } else {
            drawBoardSprite(context, x, y, BOARD_BG_SMALL);
        }
    }
    
    /**
     * Отрисовка подсветки выделенного элемента
     */
    public static void drawHighlight(DrawContext context, int x, int y) {
        drawBoardSprite(context, x, y, BOARD_HIGHLIGHT);
        // Добавляем полупрозрачный оверлей
        context.fill(x + 2, y + 2, x + 18, y + 18, 0x88000000);
    }
    
    /**
     * Отрисовка полосы прогресса
     */
    public static void drawProgressBar(DrawContext context, int x, int y, float progress) {
        // Фон полосы
        drawVillagerSprite(context, x, y, BAR_BG);
        
        // Заполнение полосы
        int fillWidth = Math.max(1, (int)(progress * BAR_FG.width));
        drawVillagerSprite(context, x, y, BAR_FG, fillWidth);
    }
    
    /**
     * Отрисовка кнопки квеста
     */
    public static void drawQuestButton(DrawContext context, int x, int y, int width, boolean selected, boolean hovered) {
        // Основная часть кнопки
        int mainWidth = width - BUTTON_CAP.width;
        drawWidgetSprite(context, x, y, BUTTON_DEFAULT, mainWidth);
        
        // Закрывающая часть кнопки
        drawWidgetSprite(context, x + mainWidth, y, BUTTON_CAP);
        
        // Цветовые оверлеи
        int color;
        int alpha;
        
        if (selected) {
            color = 0x41261b;
            alpha = 0x96;
        } else {
            color = 0xb86f50;
            alpha = 0x48;
        }
        
        context.fill(x, y, x + width, y + 20, (alpha << 24) | color);
        
        // Подсветка при наведении
        if (hovered && !selected) {
            context.fill(x, y, x + width, y + 20, 0x33FFFFFF);
        }
    }
    
    /**
     * Отрисовка полосы прокрутки
     */
    public static void drawScrollbar(DrawContext context, int x, int y, int height, float scrollProgress) {
        // Фон полосы прокрутки (можно добавить позже если нужно)
        
        // Ползунок
        int sliderY = y + (int)((height - SLIDER.height) * scrollProgress);
        drawVillagerSprite(context, x, sliderY, SLIDER);
    }
}