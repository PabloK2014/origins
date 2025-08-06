package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.gui.SpriteHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Экран для классовых досок объявлений с отображением времени до обновления
 */
public class ClassBountyBoardScreen extends BountyBoardScreen {
    
    public ClassBountyBoardScreen(BountyBoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Вызываем базовую отрисовку
        super.drawForeground(context, mouseX, mouseY);
        
        // Отрисовываем время до обновления вместо прогресса уровня
        drawUpdateTimer(context);
    }
    
    /**
     * Отрисовывает таймер до следующего обновления квестов
     */
    private void drawUpdateTimer(DrawContext context) {
        if (!(handler.getBlockEntity() instanceof ClassBountyBoardBlockEntity classBoard)) {
            return;
        }
        
        // Получаем время до обновления
        int minutesUntilUpdate = classBoard.getMinutesUntilUpdate();
        int secondsUntilUpdate = classBoard.getSecondsUntilUpdate();
        
        // Позиция еще ниже - под всем интерфейсом
        int timerX = 8; // Левая часть экрана
        int timerY = backgroundHeight + 5; // Под интерфейсом доски
        
        // Формируем текст таймера с более подробной информацией
        String timerText;
        if (minutesUntilUpdate > 0 || secondsUntilUpdate > 0) {
            timerText = String.format("Обновление через: %d:%02d", minutesUntilUpdate, secondsUntilUpdate);
        } else {
            timerText = "Обновление...";
        }
        
        // Отрисовываем текст
        context.drawText(textRenderer, timerText, timerX, timerY, 0xEADAB5, false);
        
        // Отрисовываем индикатор API статуса рядом с таймером
        int statusX = timerX + textRenderer.getWidth(timerText) + 10;
        drawApiStatus(context, statusX, timerY);
        
        // Отрисовываем прогресс-бар под таймером
        drawUpdateProgressBar(context, timerX, timerY + 12, minutesUntilUpdate, secondsUntilUpdate);
    }
    
    /**
     * Отрисовывает статус API
     */
    private void drawApiStatus(DrawContext context, int x, int y) {
        QuestApiManager manager = QuestApiManager.getInstance();
        boolean apiAvailable = manager.isApiAvailable();
        
        String statusText;
        int statusColor;
        
        if (apiAvailable) {
            statusText = "✓ API подключен";
            statusColor = 0x00FF00; // Зеленый
        } else {
            statusText = "✗ API недоступен";
            statusColor = 0xFF0000; // Красный
        }
        
        context.drawText(textRenderer, statusText, x, y, statusColor, false);
    }
    
    /**
     * Отрисовывает прогресс-бар до следующего обновления
     */
    private void drawUpdateProgressBar(DrawContext context, int x, int y, int minutesUntilUpdate, int secondsUntilUpdate) {
        int barWidth = 100;
        int barHeight = 4;
        
        // Общее время обновления - 30 минут (1800 секунд)
        int totalSeconds = 30 * 60;
        int remainingSeconds = minutesUntilUpdate * 60 + secondsUntilUpdate;
        
        // Вычисляем прогресс (от 0.0 до 1.0)
        float progress = 1.0f - ((float) remainingSeconds / totalSeconds);
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        
        // Отрисовываем фон прогресс-бара
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);
        
        // Отрисовываем заполненную часть
        int filledWidth = (int) (barWidth * progress);
        int barColor = progress > 0.8f ? 0xFF00FF00 : 0xFF0088FF; // Зеленый если почти готово, иначе синий
        context.fill(x, y, x + filledWidth, y + barHeight, barColor);
        
        // Отрисовываем рамку
        context.fill(x - 1, y - 1, x + barWidth + 1, y, 0xFFFFFFFF); // Верх
        context.fill(x - 1, y + barHeight, x + barWidth + 1, y + barHeight + 1, 0xFFFFFFFF); // Низ
        context.fill(x - 1, y, x, y + barHeight, 0xFFFFFFFF); // Лево
        context.fill(x + barWidth, y, x + barWidth + 1, y + barHeight, 0xFFFFFFFF); // Право
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Отрисовываем дополнительные подсказки для классовых досок
        renderClassBoardTooltips(context, mouseX, mouseY);
    }
    
    /**
     * Отрисовывает подсказки специфичные для классовых досок
     */
    private void renderClassBoardTooltips(DrawContext context, int mouseX, int mouseY) {
        if (!(handler.getBlockEntity() instanceof ClassBountyBoardBlockEntity classBoard)) {
            return;
        }
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        // Область таймера (соответствует новой позиции под интерфейсом)
        int timerX = x + 8; // Левая часть экрана
        int timerY = y + backgroundHeight + 5; // Под интерфейсом доски
        int timerWidth = 250; // Ширина области для таймера и статуса API
        int timerHeight = 25; // Высота области включая прогресс-бар
        
        if (mouseX >= timerX && mouseX < timerX + timerWidth && 
            mouseY >= timerY && mouseY < timerY + timerHeight) {
            
            // Показываем подробную информацию о системе обновлений
            String boardClass = classBoard.getBoardClass();
            QuestApiManager manager = QuestApiManager.getInstance();
            
            Text tooltip = Text.literal("Система автообновления квестов")
                .formatted(Formatting.GOLD)
                .append(Text.literal("\n\nКласс доски: " + getClassDisplayName(boardClass))
                    .formatted(Formatting.WHITE))
                .append(Text.literal("\nИнтервал обновления: 30 минут")
                    .formatted(Formatting.GRAY))
                .append(Text.literal("\nИсточник: FastAPI сервер")
                    .formatted(Formatting.GRAY))
                .append(Text.literal("\nСтатус API: " + (manager.isApiAvailable() ? "Подключен" : "Недоступен"))
                    .formatted(manager.isApiAvailable() ? Formatting.GREEN : Formatting.RED));
            
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }
    
    /**
     * Получает отображаемое название класса
     */
    private String getClassDisplayName(String className) {
        switch (className.toLowerCase()) {
            case "cook": return "Повар";
            case "warrior": return "Воин";
            case "blacksmith": return "Кузнец";
            case "brewer": return "Алхимик";
            case "courier": return "Курьер";
            case "miner": return "Шахтер";
            default: return className;
        }
    }
}