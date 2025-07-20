package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.progression.OriginProgression;
import io.github.apace100.origins.progression.OriginProgressionComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * GUI экран для отображения прогрессии происхождения
 */
public class OriginProgressionScreen extends Screen {
    
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("textures/gui/demo_background.png");
    private static final int GUI_WIDTH = 248;
    private static final int GUI_HEIGHT = 166;
    
    private OriginProgression progression;
    private int backgroundX;
    private int backgroundY;
    
    public OriginProgressionScreen() {
        super(Text.literal("Прогрессия происхождения"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.backgroundX = (this.width - GUI_WIDTH) / 2;
        this.backgroundY = (this.height - GUI_HEIGHT) / 2;
        
        // Получаем прогрессию игрока
        if (this.client != null && this.client.player != null) {
            OriginProgressionComponent component = OriginProgressionComponent.KEY.get(this.client.player);
            this.progression = component.getCurrentProgression();
        }
        
        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> this.close())
            .dimensions(this.backgroundX + GUI_WIDTH - 80, this.backgroundY + GUI_HEIGHT - 30, 70, 20)
            .build());
        
        // Кнопка дерева навыков (пока заглушка)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Навыки"), button -> {
            // TODO: Открыть дерево навыков
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Дерево навыков будет добавлено позже!")
                    .formatted(Formatting.YELLOW), false);
            }
        }).dimensions(this.backgroundX + 10, this.backgroundY + GUI_HEIGHT - 30, 70, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим затемненный фон
        this.renderBackground(context);
        
        // Рендерим фон GUI
        context.drawTexture(BACKGROUND_TEXTURE, backgroundX, backgroundY, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        // Рендерим содержимое
        renderProgressionInfo(context);
        
        // Рендерим виджеты (кнопки)
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderProgressionInfo(DrawContext context) {
        if (progression == null) {
            // Если нет прогрессии, показываем сообщение
            Text noProgressionText = Text.literal("Нет активного происхождения")
                .formatted(Formatting.GRAY);
            
            int textWidth = this.textRenderer.getWidth(noProgressionText);
            context.drawText(this.textRenderer, noProgressionText, 
                backgroundX + (GUI_WIDTH - textWidth) / 2, 
                backgroundY + GUI_HEIGHT / 2, 
                0xFFFFFF, false);
            return;
        }
        
        int startY = backgroundY + 20;
        int centerX = backgroundX + GUI_WIDTH / 2;
        
        // Заголовок
        Text titleText = Text.literal("Прогрессия происхождения")
            .formatted(Formatting.BOLD, Formatting.DARK_BLUE);
        int titleWidth = this.textRenderer.getWidth(titleText);
        context.drawText(this.textRenderer, titleText, 
            centerX - titleWidth / 2, startY, 0xFFFFFF, false);
        
        startY += 25;
        
        // Название происхождения
        String originName = getOriginDisplayName(progression.getOriginId());
        Text originText = Text.literal(originName)
            .formatted(Formatting.GOLD);
        int originWidth = this.textRenderer.getWidth(originText);
        context.drawText(this.textRenderer, originText, 
            centerX - originWidth / 2, startY, 0xFFFFFF, false);
        
        startY += 20;
        
        // Уровень
        Text levelText = Text.literal("Уровень: " + progression.getLevel())
            .formatted(Formatting.GREEN);
        int levelWidth = this.textRenderer.getWidth(levelText);
        context.drawText(this.textRenderer, levelText, 
            centerX - levelWidth / 2, startY, 0xFFFFFF, false);
        
        startY += 20;
        
        // Прогресс-бар
        renderProgressBar(context, centerX - 100, startY, 200, 10);
        
        startY += 20;
        
        // Опыт
        Text expText = Text.literal(String.format("Опыт: %d / %d (%d%%)", 
            progression.getExperience(), 
            progression.getExperienceForNextLevel(),
            progression.getProgressPercent()))
            .formatted(Formatting.AQUA);
        int expWidth = this.textRenderer.getWidth(expText);
        context.drawText(this.textRenderer, expText, 
            centerX - expWidth / 2, startY, 0xFFFFFF, false);
        
        startY += 20;
        
        // Общий опыт
        Text totalExpText = Text.literal("Общий опыт: " + progression.getTotalExperience())
            .formatted(Formatting.GRAY);
        int totalExpWidth = this.textRenderer.getWidth(totalExpText);
        context.drawText(this.textRenderer, totalExpText, 
            centerX - totalExpWidth / 2, startY, 0xFFFFFF, false);
    }
    
    private void renderProgressBar(DrawContext context, int x, int y, int width, int height) {
        if (progression == null) return;
        
        // Фон прогресс-бара
        context.fill(x, y, x + width, y + height, 0xFF333333);
        
        // Заполнение прогресс-бара
        float progress = progression.getProgressToNextLevel();
        int fillWidth = (int) (width * progress);
        
        // Градиент от зеленого к желтому
        int color = progress < 0.5f ? 
            0xFF00FF00 : // Зеленый
            0xFFFFFF00;  // Желтый
        
        if (fillWidth > 0) {
            context.fill(x, y, x + fillWidth, y + height, color);
        }
        
        // Рамка прогресс-бара
        context.drawBorder(x, y, width, height, 0xFFFFFFFF);
        
        // Текст процентов в центре
        String percentText = progression.getProgressPercent() + "%";
        int textWidth = this.textRenderer.getWidth(percentText);
        context.drawText(this.textRenderer, Text.literal(percentText), 
            x + (width - textWidth) / 2, y + 1, 0xFFFFFF, false);
    }
    
    private String getOriginDisplayName(String originId) {
        // Преобразуем ID происхождения в читаемое название
        return switch (originId) {
            case "origins:blacksmith" -> "🔨 Кузнец";
            case "origins:brewer" -> "🧪 Алхимик";
            case "origins:cook" -> "👨‍🍳 Повар";
            case "origins:courier" -> "📦 Курьер";
            case "origins:warrior" -> "⚔️ Воин";
            case "origins:miner" -> "⛏️ Шахтер";
            case "origins:human" -> "👤 Человек";
            default -> originId.replace("origins:", "").replace("_", " ");
        };
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Не ставим игру на паузу
    }
}