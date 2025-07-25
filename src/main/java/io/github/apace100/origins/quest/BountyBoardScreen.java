package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.gui.SpriteHelper;
import io.github.apace100.origins.quest.gui.QuestButton;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class BountyBoardScreen extends HandledScreen<BountyBoardScreenHandler> {
    private static final int BACKGROUND_WIDTH = 348;
    private static final int BACKGROUND_HEIGHT = 165;
    
    private final boolean toggledOut = true;
    private final int bgOffset = toggledOut ? 204 : 4;
    
    private final List<QuestButton> questButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private int selectedQuestIndex = -1;

    public BountyBoardScreen(BountyBoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BACKGROUND_WIDTH;
        this.backgroundHeight = BACKGROUND_HEIGHT;
        this.playerInventoryTitleY = this.backgroundHeight - 94;

        // Инициализация кнопок квестов
        for (int i = 0; i < 21; i++) {
            questButtons.add(new QuestButton(this, i));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // Отрисовка фона доски объявлений
        SpriteHelper.drawBoardBackground(context, x, y, toggledOut);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Заголовок
        Text titleText = Text.translatable("gui.origins.bounty_board.title");
        int titleX = (backgroundWidth - textRenderer.getWidth(titleText)) / 2 - 53;
        context.drawText(textRenderer, titleText, titleX, 6, 0xEADAB5, false);

        // Полоска прогресса игрока
        drawPlayerProgressBar(context);

        // Список квестов
        if (toggledOut) {
            drawQuestList(context, mouseX, mouseY);
            
            if (getValidQuests().isEmpty()) {
                String emptyText = "Нет доступных квестов!";
                int textX = 85 - textRenderer.getWidth(emptyText) / 2;
                context.drawText(textRenderer, emptyText, textX, 78, 0xEADAB5, false);
            }
        }

        // Инвентарь игрока
        context.drawText(textRenderer, playerInventoryTitle, 8, playerInventoryTitleY, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        
        // Отрисовка подсказок для квестов
        if (toggledOut) {
            renderQuestTooltips(context, mouseX, mouseY);
        }
        
        // Отрисовка подсказки для полосы прогресса
        renderProgressBarTooltip(context, mouseX, mouseY);
    }
    
    /**
     * Отрисовка полосы прогресса игрока
     */
    private void drawPlayerProgressBar(DrawContext context) {
        if (client == null || client.player == null) return;
        
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
        int playerLevel = skillComponent.getPlayerLevel();
        float progress = 0.5f; // Заглушка для прогресса
        
        int barX = bgOffset;
        int barY = 75;
        
        // Отрисовка полосы прогресса
        SpriteHelper.drawProgressBar(context, barX, barY, progress);
        
        // Отрисовка текста уровня
        String levelText = String.valueOf(playerLevel);
        int textX = barX + 51 - textRenderer.getWidth(levelText) / 2;
        context.drawText(textRenderer, levelText, textX, barY - 10, getRarityColor(playerLevel), false);
    }
    
    /**
     * Отрисовка подсказки для полосы прогресса
     */
    private void renderProgressBarTooltip(DrawContext context, int mouseX, int mouseY) {
        if (client == null || client.player == null) return;
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        int barX = x + bgOffset;
        int barY = y + 75;
        
        // Проверяем, наведена ли мышь на полосу прогресса
        if (mouseX >= barX - 28 && mouseX < barX + 102 + 28 && 
            mouseY >= barY - 10 && mouseY < barY + 15) {
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
            int playerLevel = skillComponent.getPlayerLevel();
            float progress = 0.5f; // Заглушка для прогресса
            int currentExp = 0; // Заглушка для опыта
            int expForNextLevel = 1000; // Заглушка для опыта до следующего уровня
            
            List<Text> tooltip = new ArrayList<>();
            
            // Заголовок
            tooltip.add(Text.literal("Прогресс персонажа").formatted(Formatting.YELLOW));
            
            // Текущий уровень
            tooltip.add(Text.literal("Уровень: " + playerLevel)
                    .formatted(getFormattingForLevel(playerLevel)));
            
            // Прогресс до следующего уровня
            int progressPercent = (int)(progress * 100);
            tooltip.add(Text.literal("Прогресс: " + progressPercent + "%")
                    .formatted(Formatting.GREEN));
            
            // Опыт
            tooltip.add(Text.literal("Опыт: " + currentExp + " / " + expForNextLevel)
                    .formatted(Formatting.AQUA));
            
            // Класс игрока
            String playerClass = QuestIntegration.getPlayerClass(client.player);
            String localizedClass = QuestIntegration.getLocalizedClassName(playerClass);
            tooltip.add(Text.literal("Класс: " + localizedClass)
                    .formatted(Formatting.GOLD));
            
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }
    
    /**
     * Получает форматирование для уровня
     */
    private Formatting getFormattingForLevel(int level) {
        if (level <= 10) return Formatting.WHITE;
        else if (level <= 20) return Formatting.GREEN;
        else if (level <= 30) return Formatting.YELLOW;
        else if (level <= 40) return Formatting.GOLD;
        else return Formatting.RED;
    }
    
    /**
     * Отрисовка списка квестов
     */
    private void drawQuestList(DrawContext context, int mouseX, int mouseY) {
        List<QuestButton> validQuests = getValidQuests();
        int startY = 18;
        int maxVisible = 7;
        
        for (int i = 0; i < Math.min(maxVisible, validQuests.size()); i++) {
            int index = i + scrollOffset;
            if (index >= validQuests.size()) break;
            
            QuestButton button = validQuests.get(index);
            int buttonX = 5;
            int buttonY = startY + i * 20;
            
            boolean hovered = isPointInButton(mouseX, mouseY, buttonX, buttonY);
            button.render(context, buttonX, buttonY, mouseX, mouseY, hovered);
        }
        
        // Отрисовка полосы прокрутки если нужно
        if (validQuests.size() > maxVisible) {
            float scrollProgress = (float) scrollOffset / (validQuests.size() - maxVisible);
            SpriteHelper.drawScrollbar(context, 166, startY, maxVisible * 20, scrollProgress);
        }
    }
    
    /**
     * Отрисовка подсказок для квестов
     */
    private void renderQuestTooltips(DrawContext context, int mouseX, int mouseY) {
        List<QuestButton> validQuests = getValidQuests();
        int startY = 18;
        int maxVisible = 7;
        
        for (int i = 0; i < Math.min(maxVisible, validQuests.size()); i++) {
            int index = i + scrollOffset;
            if (index >= validQuests.size()) break;
            
            QuestButton button = validQuests.get(index);
            int buttonX = 5;
            int buttonY = startY + i * 20;
            
            if (isPointInButton(mouseX, mouseY, buttonX, buttonY)) {
                button.renderTooltip(context, mouseX, mouseY);
                break;
            }
        }
    }
    
    /**
     * Проверяет, находится ли точка в пределах кнопки
     */
    private boolean isPointInButton(int mouseX, int mouseY, int buttonX, int buttonY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        int absoluteX = x + buttonX;
        int absoluteY = y + buttonY;
        
        return mouseX >= absoluteX && mouseX < absoluteX + QuestButton.WIDTH &&
               mouseY >= absoluteY && mouseY < absoluteY + QuestButton.HEIGHT;
    }
    
    /**
     * Получает список валидных квестов
     */
    private List<QuestButton> getValidQuests() {
        List<QuestButton> validQuests = new ArrayList<>();
        for (QuestButton button : questButtons) {
            if (button.getQuestData() != null) {
                validQuests.add(button);
            }
        }
        return validQuests;
    }
    
    /**
     * Получает цвет для отображения уровня
     */
    private int getRarityColor(int level) {
        if (level <= 10) return 0xFFFFFF;      // Белый
        else if (level <= 20) return 0x00FF00; // Зеленый
        else if (level <= 30) return 0xFFD700; // Золотой
        else if (level <= 40) return 0xFF8C00; // Оранжевый
        else return 0xFF0000;                   // Красный
    }

    @Override
    protected void init() {
        super.init();
        titleY = 6;

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // Кнопка "Взять квест"
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.origins.bounty_board.take"),
                button -> takeQuest()
        ).dimensions(x + 8, y + 130, 70, 20).build());

        // Кнопка "Сдать квест"
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.origins.bounty_board.complete"),
                button -> completeQuest()
        ).dimensions(x + 88, y + 130, 80, 20).build());
        
        // Кнопка "Обновить список"
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.origins.bounty_board.refresh"),
                button -> refreshQuests()
        ).dimensions(x + 178, y + 130, 80, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggledOut) {
            // Проверяем клик по кнопкам квестов
            List<QuestButton> validQuests = getValidQuests();
            int startY = 18;
            int maxVisible = 7;
            
            for (int i = 0; i < Math.min(maxVisible, validQuests.size()); i++) {
                int index = i + scrollOffset;
                if (index >= validQuests.size()) break;
                
                QuestButton questButton = validQuests.get(index);
                int buttonX = 5;
                int buttonY = startY + i * 20;
                
                if (isPointInButton((int)mouseX, (int)mouseY, buttonX, buttonY)) {
                    questButton.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (toggledOut) {
            List<QuestButton> validQuests = getValidQuests();
            int maxVisible = 7;
            
            if (validQuests.size() > maxVisible) {
                scrollOffset = Math.max(0, Math.min(validQuests.size() - maxVisible, 
                    scrollOffset - (int)delta));
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void takeQuest() {
        Quest selectedQuest = handler.getSelectedQuest();
        if (selectedQuest != null && client != null && client.player != null) {
            handler.acceptQuest(selectedQuest);
            client.player.sendMessage(
                    Text.translatable("gui.origins.bounty_board.quest_accepted"),
                    true
            );
        }
    }

    private void completeQuest() {
        Quest selectedQuest = handler.getSelectedQuest();
        if (selectedQuest != null && client != null && client.player != null) {
            handler.completeQuest(selectedQuest, client.player);
        }
    }
    
    private void refreshQuests() {
        if (handler != null) {
            handler.refreshQuests();
        }
    }
    
    public BountyBoardScreenHandler getHandler() {
        return handler;
    }

}