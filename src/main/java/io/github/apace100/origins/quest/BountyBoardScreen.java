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
    private final QuestSelectionState selectionState = new QuestSelectionState();
    private boolean isDragging = false;
    private Quest draggedQuest = null;

    public BountyBoardScreen(BountyBoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BACKGROUND_WIDTH;
        this.backgroundHeight = BACKGROUND_HEIGHT;
        this.playerInventoryTitleY = this.backgroundHeight - 94;

        // Инициализация кнопок квестов
        for (int i = 0; i < 21; i++) {
            questButtons.add(new QuestButton(this, i));
        }
        
        // Принудительно обновляем квесты при открытии экрана
        handler.refreshAvailableQuests();
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
        
        // Отрисовка drag-and-drop визуализации
        renderDragAndDropEffects(context, mouseX, mouseY);
    }
    
    /**
     * Отрисовывает эффекты drag-and-drop
     */
    private void renderDragAndDropEffects(DrawContext context, int mouseX, int mouseY) {
        QuestDragHandler dragHandler = handler.getDragHandler();
        if (dragHandler.isDragging()) {
            // Отрисовываем перетаскиваемый квест
            dragHandler.renderDraggedQuest(context);
            
            // Отрисовываем подсветку слотов
            renderSlotHighlights(context, mouseX, mouseY, dragHandler);
        }
    }
    
    /**
     * Отрисовывает подсветку слотов при перетаскивании
     */
    private void renderSlotHighlights(DrawContext context, int mouseX, int mouseY, QuestDragHandler dragHandler) {
        Quest draggedQuest = dragHandler.getDraggedQuest();
        if (draggedQuest == null) return;
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        // Подсветка selected quest slot
        int selectedSlotX = x + 50;
        int selectedSlotY = y + 50;
        boolean canDropInSelected = handler.canPlaceQuestInSlot(draggedQuest, 21);
        
        if (isPointInArea(mouseX, mouseY, selectedSlotX, selectedSlotY, 16, 16)) {
            dragHandler.renderSlotHighlight(context, selectedSlotX, selectedSlotY, 16, 16, canDropInSelected);
        }
        
        // Подсветка quest slots
        for (int i = 0; i < 21; i++) {
            int slotX = x + 173 + (i % 7) * 18;
            int slotY = y + 18 + (i / 7) * 18;
            
            if (isPointInArea(mouseX, mouseY, slotX, slotY, 16, 16)) {
                boolean canDrop = !handler.isQuestSlotMasked(i);
                dragHandler.renderSlotHighlight(context, slotX, slotY, 16, 16, canDrop);
            }
        }
    }
    

    
    /**
     * Отрисовка списка квестов
     */
    private void drawQuestList(DrawContext context, int mouseX, int mouseY) {
        List<QuestButton> allQuests = getAllQuests();
        int startY = 18;
        int maxVisible = 7;
        int visibleIndex = 0;
        int renderedCount = 0;
        
        for (int i = 0; i < allQuests.size() && renderedCount < maxVisible; i++) {
            QuestButton button = allQuests.get(i);
            boolean isMasked = handler.isQuestSlotMasked(i);
            
            // Для замаскированных квестов показываем заглушку, но не считаем их в видимых
            if (isMasked) {
                continue; // Полностью пропускаем замаскированные квесты
            }
            
            // Проверяем скроллинг для видимых квестов
            if (visibleIndex < scrollOffset) {
                visibleIndex++;
                continue;
            }
            
            int buttonX = 5;
            int buttonY = startY + renderedCount * 20;
            
            boolean hovered = isPointInButton(mouseX, mouseY, buttonX, buttonY);
            boolean isSelected = selectionState.isSelected(i, button.getQuest());
            
            // Отрисовываем подсветку выбранного квеста
            if (isSelected) {
                renderQuestSelection(context, buttonX, buttonY);
            }
            
            // Добавляем визуальные эффекты для drag-and-drop
            if (isDragging && draggedQuest != null && button.getQuest() != null && 
                button.getQuest().getId().equals(draggedQuest.getId())) {
                // Отрисовываем полупрозрачную версию исходного квеста
                renderDragSourceQuest(context, button, buttonX, buttonY, false);
            } else {
                button.render(context, buttonX, buttonY, mouseX, mouseY, hovered, isSelected);
            }
            
            // Отрисовываем индикатор возможности сброса при drag-and-drop
            if (isDragging && hovered) {
                renderDropIndicator(context, buttonX, buttonY, true);
            }
            
            visibleIndex++;
            renderedCount++;
        }
        
        // Отрисовка полосы прокрутки если нужно
        int visibleQuestCount = getVisibleQuestCount();
        if (visibleQuestCount > maxVisible) {
            float scrollProgress = (float) scrollOffset / (visibleQuestCount - maxVisible);
            SpriteHelper.drawScrollbar(context, 166, startY, maxVisible * 20, scrollProgress);
        }
        
        // Отрисовка индикатора пустых слотов при drag-and-drop
        if (isDragging) {
            renderEmptySlotIndicators(context, renderedCount, maxVisible, startY);
        }
    }
    
    /**
     * Отрисовывает подсветку выбранного квеста
     */
    private void renderQuestSelection(DrawContext context, int x, int y) {
        // Отрисовываем золотую рамку вокруг выбранного квеста
        context.fill(x - 2, y - 2, x + QuestButton.WIDTH + 2, y - 1, 0xFFFFD700); // Верх
        context.fill(x - 2, y + QuestButton.HEIGHT + 1, x + QuestButton.WIDTH + 2, y + QuestButton.HEIGHT + 2, 0xFFFFD700); // Низ
        context.fill(x - 2, y - 1, x - 1, y + QuestButton.HEIGHT + 1, 0xFFFFD700); // Лево
        context.fill(x + QuestButton.WIDTH + 1, y - 1, x + QuestButton.WIDTH + 2, y + QuestButton.HEIGHT + 1, 0xFFFFD700); // Право
        
        // Добавляем легкую подсветку фона
        context.fill(x, y, x + QuestButton.WIDTH, y + QuestButton.HEIGHT, 0x20FFFF00);
    }
    
    /**
     * Отрисовывает замаскированный слот квеста
     */
    private void renderMaskedQuestSlot(DrawContext context, int x, int y) {
        // Отрисовываем затемненный фон
        context.fill(x, y, x + QuestButton.WIDTH, y + QuestButton.HEIGHT, 0x80000000);
        
        // Отрисовываем иконку маски или текст
        String maskedText = "Скрыто";
        int textX = x + (QuestButton.WIDTH - textRenderer.getWidth(maskedText)) / 2;
        int textY = y + (QuestButton.HEIGHT - 8) / 2;
        context.drawText(textRenderer, maskedText, textX, textY, 0x808080, false);
    }
    
    /**
     * Отрисовывает исходный квест при drag-and-drop (полупрозрачно)
     */
    private void renderDragSourceQuest(DrawContext context, QuestButton button, int x, int y, boolean hovered) {
        // Сохраняем текущее состояние матрицы
        context.getMatrices().push();
        
        // Применяем полупрозрачность
        // В Minecraft 1.20.1 используем другой подход для прозрачности
        button.render(context, x, y, -1, -1, false); // Отрисовываем без hover эффекта
        
        // Накладываем полупрозрачный слой
        context.fill(x, y, x + QuestButton.WIDTH, y + QuestButton.HEIGHT, 0x80FFFFFF);
        
        context.getMatrices().pop();
    }
    
    /**
     * Отрисовывает индикатор возможности сброса
     */
    private void renderDropIndicator(DrawContext context, int x, int y, boolean canDrop) {
        int color = canDrop ? 0x8000FF00 : 0x80FF0000; // Зеленый или красный
        context.fill(x - 2, y - 2, x + QuestButton.WIDTH + 2, y + QuestButton.HEIGHT + 2, color);
    }
    
    /**
     * Отрисовывает индикаторы пустых слотов при drag-and-drop
     */
    private void renderEmptySlotIndicators(DrawContext context, int questCount, int maxVisible, int startY) {
        // Отрисовываем пунктирные рамки для пустых слотов
        for (int i = questCount; i < maxVisible; i++) {
            int buttonX = 5;
            int buttonY = startY + i * 20;
            
            // Пунктирная рамка
            drawDashedRect(context, buttonX, buttonY, QuestButton.WIDTH, QuestButton.HEIGHT, 0x80808080);
        }
    }
    
    /**
     * Отрисовывает пунктирную рамку
     */
    private void drawDashedRect(DrawContext context, int x, int y, int width, int height, int color) {
        // Верхняя линия
        for (int i = 0; i < width; i += 4) {
            context.fill(x + i, y, x + i + 2, y + 1, color);
        }
        // Нижняя линия
        for (int i = 0; i < width; i += 4) {
            context.fill(x + i, y + height - 1, x + i + 2, y + height, color);
        }
        // Левая линия
        for (int i = 0; i < height; i += 4) {
            context.fill(x, y + i, x + 1, y + i + 2, color);
        }
        // Правая линия
        for (int i = 0; i < height; i += 4) {
            context.fill(x + width - 1, y + i, x + width, y + i + 2, color);
        }
    }
    
    /**
     * Отрисовка подсказок для квестов
     */
    private void renderQuestTooltips(DrawContext context, int mouseX, int mouseY) {
        List<QuestButton> allQuests = getAllQuests();
        int startY = 18;
        int maxVisible = 7;
        int visibleIndex = 0;
        
        for (int i = 0; i < allQuests.size() && visibleIndex < maxVisible; i++) {
            // Пропускаем замаскированные квесты
            if (handler.isQuestSlotMasked(i)) {
                continue;
            }
            
            // Проверяем скроллинг
            if (visibleIndex < scrollOffset) {
                visibleIndex++;
                continue;
            }
            
            QuestButton button = allQuests.get(i);
            int buttonX = 5;
            int buttonY = startY + (visibleIndex - scrollOffset) * 20;
            
            if (isPointInButton(mouseX, mouseY, buttonX, buttonY)) {
                button.renderTooltip(context, mouseX, mouseY);
                break;
            }
            
            visibleIndex++;
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
     * Проверяет, находится ли точка в указанной области
     */
    private boolean isPointInArea(int mouseX, int mouseY, int areaX, int areaY, int width, int height) {
        return mouseX >= areaX && mouseX < areaX + width &&
               mouseY >= areaY && mouseY < areaY + height;
    }
    
    /**
     * Получает список валидных квестов
     */
    private List<QuestButton> getValidQuests() {
        List<QuestButton> validQuests = new ArrayList<>();
        List<Quest> availableQuests = handler.getAvailableQuests();
        
        for (int i = 0; i < Math.min(availableQuests.size(), questButtons.size()); i++) {
            // Проверяем, не замаскирован ли этот слот
            if (handler.isQuestSlotMasked(i)) {
                continue; // Пропускаем замаскированные квесты
            }
            
            QuestButton button = questButtons.get(i);
            Quest quest = availableQuests.get(i);
            button.setQuestData(quest);
            validQuests.add(button);
        }
        return validQuests;
    }
    
    /**
     * Получает список всех квестов включая замаскированные (для внутреннего использования)
     */
    private List<QuestButton> getAllQuests() {
        List<QuestButton> allQuests = new ArrayList<>();
        List<Quest> availableQuests = handler.getAvailableQuests();
        
        for (int i = 0; i < Math.min(availableQuests.size(), questButtons.size()); i++) {
            QuestButton button = questButtons.get(i);
            Quest quest = availableQuests.get(i);
            button.setQuestData(quest);
            allQuests.add(button);
        }
        
        return allQuests;
    }
    


    @Override
    protected void init() {
        super.init();
        titleY = 6;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        
        QuestDragHandler dragHandler = handler.getDragHandler();
        
        // Обработка завершения drag-and-drop
        if (dragHandler.isDragging()) {

            if (handleDragDrop(mouseX, mouseY)) {
                return true;
            }
        }
        
        if (toggledOut) {

            // Проверяем клик по кнопкам квестов
            List<QuestButton> allQuests = getAllQuests();
            int startY = 18;
            int maxVisible = 7;
            int visibleIndex = 0;
            

            
            for (int i = 0; i < allQuests.size() && visibleIndex < maxVisible; i++) {
                // Пропускаем замаскированные квесты
                if (handler.isQuestSlotMasked(i)) {

                    continue;
                }
                
                // Проверяем скроллинг
                if (visibleIndex < scrollOffset) {
                    visibleIndex++;
                    continue;
                }
                
                QuestButton questButton = allQuests.get(i);
                int buttonX = 5;
                int buttonY = startY + (visibleIndex - scrollOffset) * 20;
                

                
                if (isPointInButton((int)mouseX, (int)mouseY, buttonX, buttonY)) {

                    
                    // Обновляем состояние выбора
                    Quest clickedQuest = questButton.getQuest();
                    if (clickedQuest != null) {
                        selectionState.selectQuest(i, clickedQuest);
                        handler.setSelectedQuestIndex(i);

                    }
                    
                    // Обработка принятия квеста через ЛКМ
                    if (button == 0 && clickedQuest != null) { // Left click

                        if (acceptQuestDirectly(clickedQuest)) {

                            return true;
                        }
                        Origins.LOGGER.warn("Прямое принятие квеста не удалось, пробуем drag-and-drop");
                        // Fallback к drag-and-drop если прямое принятие не удалось
                        if (startDragFromButton(questButton, i, mouseX, mouseY)) {
                            return true;
                        }
                    }
                    questButton.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
                
                visibleIndex++;
            }

        } else {

        }


        // Если клик не был обработан выше, передаем его в стандартную обработку
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Принимает квест через создание билета в инвентаре игрока
     */
    private boolean acceptQuestDirectly(Quest quest) {
        if (quest == null || client == null || client.player == null) {
            Origins.LOGGER.warn("acceptQuestDirectly: quest={}, client={}, player={}", 
                quest, client, client != null ? client.player : null);
            return false;
        }
        
        try {

            
            // Получаем BlockEntity доски объявлений
            BountyBoardBlockEntity boardEntity = handler.getBoardEntity();
            if (boardEntity == null) {
                Origins.LOGGER.warn("BoardEntity is null, cannot accept quest");
                return false;
            }
            

            
            // Используем QuestTicketAcceptanceHandler для принятия квеста
            QuestTicketAcceptanceHandler acceptanceHandler = QuestTicketAcceptanceHandler.getInstance();
            boolean result = acceptanceHandler.acceptQuestFromBoard(client.player, quest, boardEntity);
            
            if (result) {

                // Обновляем экран локально
                refreshQuestList();
                return true;
            } else {
                Origins.LOGGER.warn("Не удалось принять квест {} через билет", quest.getId());
                return false;
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принятии квеста через билет {}: {}", quest.getId(), e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Обновляет список квестов на экране
     */
    private void refreshQuestList() {
        // Обновляем доступные квесты
        if (handler != null) {
            handler.refreshAvailableQuests();
        }
        
        // Пересоздаем кнопки квестов
        questButtons.clear();
        for (int i = 0; i < 21; i++) {
            questButtons.add(new QuestButton(this, i));
        }
    }
    
    /**
     * Начинает перетаскивание квеста из кнопки
     */
    private boolean startDragFromButton(QuestButton button, int questIndex, double mouseX, double mouseY) {
        Quest quest = button.getQuest();
        if (quest == null) return false;
        
        QuestDragHandler dragHandler = handler.getDragHandler();
        if (dragHandler.startDrag(quest, questIndex, (int)mouseX, (int)mouseY)) {
            isDragging = true;
            draggedQuest = quest;
            return true;
        }
        return false;
    }
    
    /**
     * Обрабатывает завершение drag-and-drop операции
     */
    private boolean handleDragDrop(double mouseX, double mouseY) {
        QuestDragHandler dragHandler = handler.getDragHandler();
        Quest draggedQuest = dragHandler.getDraggedQuest();
        if (draggedQuest == null) return false;
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        // Проверяем сброс в selected quest slot
        int selectedSlotX = x + 50;
        int selectedSlotY = y + 50;
        if (isPointInArea((int)mouseX, (int)mouseY, selectedSlotX, selectedSlotY, 16, 16)) {
            if (handler.canPlaceQuestInSlot(draggedQuest, 21)) {
                handler.acceptQuestViaDragDrop(draggedQuest);
                finishDragging();
                return true;
            }
        }
        
        // Проверяем сброс в quest slots (для отмены)
        for (int i = 0; i < 21; i++) {
            int slotX = x + 173 + (i % 7) * 18;
            int slotY = y + 18 + (i / 7) * 18;
            
            if (isPointInArea((int)mouseX, (int)mouseY, slotX, slotY, 16, 16)) {
                // Возвращаем квест обратно в список
                finishDragging();
                return true;
            }
        }
        
        // Отмена перетаскивания
        cancelDragging();
        return true;
    }
    
    /**
     * Завершает перетаскивание
     */
    private void finishDragging() {
        handler.finishDragging(-1);
        isDragging = false;
        draggedQuest = null;
    }
    
    /**
     * Отменяет перетаскивание
     */
    private void cancelDragging() {
        handler.cancelDragging();
        isDragging = false;
        draggedQuest = null;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (toggledOut) {
            List<QuestButton> validQuests = getValidQuests();
            int maxVisible = 7;
            
            // Учитываем только видимые (не замаскированные) квесты для скроллинга
            int visibleQuestCount = getVisibleQuestCount();
            
            if (visibleQuestCount > maxVisible) {
                scrollOffset = Math.max(0, Math.min(visibleQuestCount - maxVisible, 
                    scrollOffset - (int)delta));
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * Получает количество видимых (не замаскированных) квестов
     */
    private int getVisibleQuestCount() {
        int count = 0;
        List<Quest> availableQuests = handler.getAvailableQuests();
        
        for (int i = 0; i < availableQuests.size(); i++) {
            if (!handler.isQuestSlotMasked(i)) {
                count++;
            }
        }
        
        return count;
    }
    

    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Обновляем состояние drag-and-drop при перетаскивании
        QuestDragHandler dragHandler = handler.getDragHandler();
        if (dragHandler.isDragging()) {
            dragHandler.updateDrag((int)mouseX, (int)mouseY);
            handler.updateDragState(mouseX, mouseY);
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
    
    /**
     * Получает текущие координаты мыши для использования в кнопках
     */
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    
    public int getLastMouseX() {
        return lastMouseX;
    }
    
    public int getLastMouseY() {
        return lastMouseY;
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        this.lastMouseX = (int) mouseX;
        this.lastMouseY = (int) mouseY;
        
        // Обновляем состояние drag-and-drop
        QuestDragHandler dragHandler = handler.getDragHandler();
        if (dragHandler.isDragging()) {
            dragHandler.updateDrag((int)mouseX, (int)mouseY);
            handler.updateDragState(mouseX, mouseY);
        }
    }
    
    /**
     * Получает текстовый рендерер для использования в кнопках
     */
    public net.minecraft.client.font.TextRenderer getTextRenderer() {
        return this.textRenderer;
    }

}