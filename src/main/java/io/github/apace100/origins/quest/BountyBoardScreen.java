package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.gui.SpriteHelper;
import io.github.apace100.origins.quest.gui.QuestButton;
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

        // Список квестов
        if (toggledOut) {
            drawQuestList(context, mouseX, mouseY);
            
            if (getValidQuests().isEmpty()) {
                String emptyText = "Нет доступных квестов!";
                int textX = 85 - textRenderer.getWidth(emptyText) / 2;
                context.drawText(textRenderer, emptyText, textX, 78, 0xEADAB5, false);
            }
        }

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
    


    @Override
    protected void init() {
        super.init();
        titleY = 6;
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


    

    
    /**
     * Получает текст цели квеста
     */
    private String getObjectiveText(QuestObjective objective) {
        switch (objective.getType()) {
            case COLLECT:
                return "Собрать: " + getItemName(objective.getTarget()) + " x" + objective.getAmount();
            case KILL:
                return "Убить: " + getEntityName(objective.getTarget()) + " x" + objective.getAmount();
            case CRAFT:
                return "Создать: " + getItemName(objective.getTarget()) + " x" + objective.getAmount();
            default:
                return objective.getTarget() + " x" + objective.getAmount();
        }
    }
    
    /**
     * Получает сокращенный текст цели квеста для билета
     */
    private String getShortObjectiveText(QuestObjective objective) {
        String itemName = getItemName(objective.getTarget());
        if (itemName.length() > 12) itemName = itemName.substring(0, 9) + "...";
        
        switch (objective.getType()) {
            case COLLECT:
                return itemName + " x" + objective.getAmount();
            case KILL:
                return "Убить x" + objective.getAmount();
            case CRAFT:
                return "Создать x" + objective.getAmount();
            default:
                return itemName + " x" + objective.getAmount();
        }
    }
    
    /**
     * Получает читаемое название предмета
     */
    private String getItemName(String itemId) {
        String[] parts = itemId.replace("minecraft:", "").split("_");
        StringBuilder name = new StringBuilder();
        
        for (String part : parts) {
            if (name.length() > 0) name.append(" ");
            name.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        
        return name.toString();
    }
    
    /**
     * Получает читаемое название сущности
     */
    private String getEntityName(String entityId) {
        return getItemName(entityId);
    }
    
    public BountyBoardScreenHandler getHandler() {
        return handler;
    }

}