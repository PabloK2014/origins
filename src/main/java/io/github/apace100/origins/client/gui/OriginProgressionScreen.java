package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран для отображения прогрессии происхождения игрока
 */
public class OriginProgressionScreen extends Screen {
    
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 200;
    
    private int backgroundX;
    private int backgroundY;
    
    private String currentOrigin;
    private ProfessionProgress progress;
    
    public OriginProgressionScreen() {
        super(Text.translatable("screen.origins.progression"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.backgroundX = (this.width - BACKGROUND_WIDTH) / 2;
        this.backgroundY = (this.height - BACKGROUND_HEIGHT) / 2;
        
        // Получаем информацию о текущем происхождении и прогрессии
        if (this.client != null && this.client.player != null) {
            OriginComponent originComponent = ModComponents.ORIGIN.get(this.client.player);
            Origin origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
            if (origin != null) {
                this.currentOrigin = origin.getIdentifier().toString();
            }
            
            ProfessionComponent professionComponent = ProfessionComponent.KEY.get(this.client.player);
            this.progress = professionComponent.getCurrentProgress();
        }
        
        // Кнопка "Дерево навыков"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.origins.skill_tree"), button -> {
            if (this.client != null) {
                this.client.setScreen(new SkillTreeScreen());
            }
        }).dimensions(this.backgroundX + 20, this.backgroundY + BACKGROUND_HEIGHT - 60, 100, 20).build());
        
        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.close"), button -> {
            this.close();
        }).dimensions(this.backgroundX + BACKGROUND_WIDTH - 70, this.backgroundY + BACKGROUND_HEIGHT - 30, 50, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        // Рисуем фон
        Identifier backgroundTexture = new Identifier(Origins.MODID, "textures/gui/progression_background.png");
        context.drawTexture(backgroundTexture, backgroundX, backgroundY, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        
        // Заголовок
        String title = getProfessionDisplayName(currentOrigin);
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, backgroundY + 15, 0xFFFFFF);
        
        if (progress != null) {
            // Текущий уровень
            String levelText = "Уровень: " + progress.getLevel();
            context.drawTextWithShadow(this.textRenderer, levelText, backgroundX + 20, backgroundY + 40, 0xFFFF55);
            
            // Опыт
            String expText = "Опыт: " + progress.getExperience() + " / " + progress.getExperienceForNextLevel();
            context.drawTextWithShadow(this.textRenderer, expText, backgroundX + 20, backgroundY + 55, 0xAAFFAA);
            
            // Полоса опыта
            drawExperienceBar(context, backgroundX + 20, backgroundY + 70, BACKGROUND_WIDTH - 40, 10);
            
            // Статистика
            context.drawTextWithShadow(this.textRenderer, "Статистика:", backgroundX + 20, backgroundY + 90, 0xFFFFFF);
            
            // Различная статистика в зависимости от происхождения
            drawOriginSpecificStats(context, backgroundX + 20, backgroundY + 105);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawExperienceBar(DrawContext context, int x, int y, int width, int height) {
        if (progress == null) return;
        
        // Фон полосы опыта
        context.fill(x, y, x + width, y + height, 0xFF333333);
        context.drawBorder(x, y, width, height, 0xFF666666);
        
        // Заполненная часть
        double progressPercent = progress.getProgressToNextLevel();
        int filledWidth = (int) (width * progressPercent);
        
        if (filledWidth > 0) {
            // Градиент от зеленого к желтому
            int color = interpolateColor(0xFF00AA00, 0xFFFFAA00, progressPercent);
            context.fill(x + 1, y + 1, x + filledWidth - 1, y + height - 1, color);
        }
        
        // Текст прогресса
        String progressText = String.format("%.1f%%", progressPercent * 100);
        int textX = x + width / 2 - this.textRenderer.getWidth(progressText) / 2;
        int textY = y + (height - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, progressText, textX, textY, 0xFFFFFF);
    }
    
    private void drawOriginSpecificStats(DrawContext context, int x, int y) {
        if (progress == null || currentOrigin == null) return;
        
        int lineHeight = this.textRenderer.fontHeight + 2;
        int currentY = y;
        
        switch (currentOrigin) {
            case "origins:blacksmith":
                context.drawTextWithShadow(this.textRenderer, "• Предметов создано: " + progress.getStatistic("items_crafted", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Слитков переплавлено: " + progress.getStatistic("ingots_smelted", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Качественных предметов: " + progress.getStatistic("quality_items", 0), x, currentY, 0xCCCCCC);
                break;
                
            case "origins:cook":
                context.drawTextWithShadow(this.textRenderer, "• Блюд приготовлено: " + progress.getStatistic("food_cooked", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Еды съедено: " + progress.getStatistic("food_eaten", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Рецептов изучено: " + progress.getStatistic("recipes_learned", 0), x, currentY, 0xCCCCCC);
                break;
                
            case "origins:brewer":
                context.drawTextWithShadow(this.textRenderer, "• Зелий сварено: " + progress.getStatistic("potions_brewed", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Алкоголя выпито: " + progress.getStatistic("alcohol_consumed", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Уникальных рецептов: " + progress.getStatistic("unique_recipes", 0), x, currentY, 0xCCCCCC);
                break;
                
            case "origins:miner":
                context.drawTextWithShadow(this.textRenderer, "• Блоков добыто: " + progress.getStatistic("blocks_mined", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Руды найдено: " + progress.getStatistic("ores_found", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Глубина рекорд: " + progress.getStatistic("deepest_mine", 0), x, currentY, 0xCCCCCC);
                break;
                
            case "origins:courier":
                context.drawTextWithShadow(this.textRenderer, "• Расстояние пройдено: " + progress.getStatistic("distance_traveled", 0) + "м", x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Предметов доставлено: " + progress.getStatistic("items_delivered", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Торговых сделок: " + progress.getStatistic("trades_completed", 0), x, currentY, 0xCCCCCC);
                break;
                
            case "origins:warrior":
                context.drawTextWithShadow(this.textRenderer, "• Врагов убито: " + progress.getStatistic("enemies_killed", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Урона нанесено: " + progress.getStatistic("damage_dealt", 0), x, currentY, 0xCCCCCC);
                currentY += lineHeight;
                context.drawTextWithShadow(this.textRenderer, "• Боссов побеждено: " + progress.getStatistic("bosses_defeated", 0), x, currentY, 0xCCCCCC);
                break;
                
            default:
                context.drawTextWithShadow(this.textRenderer, "• Общий опыт: " + progress.getExperience(), x, currentY, 0xCCCCCC);
                break;
        }
    }
    
    private String getProfessionDisplayName(String professionId) {
        if (professionId == null) return "Неизвестно";
        
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
    
    private int interpolateColor(int color1, int color2, double factor) {
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
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}