package io.github.apace100.origins.quest;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Обработчик drag-and-drop операций для квестов
 * Основан на функционале из оригинального Bountiful мода
 */
public class QuestDragHandler {
    private Quest draggedQuest;
    private ItemStack draggedStack;
    private int sourceSlot = -1;
    private boolean isDragging = false;
    private int mouseX, mouseY;
    private int dragStartX, dragStartY;
    private long dragStartTime;
    
    // Константы для анимации
    private static final int DRAG_THRESHOLD = 5; // Минимальное расстояние для начала перетаскивания
    private static final float DRAG_SCALE = 1.2f; // Масштаб при перетаскивании
    private static final int DRAG_ALPHA = 200; // Прозрачность при перетаскивании
    
    /**
     * Начинает перетаскивание квеста
     */
    public boolean startDrag(Quest quest, int slot, int mouseX, int mouseY) {
        if (quest == null) return false;
        
        this.draggedQuest = quest;
        this.draggedStack = QuestItem.createQuestStack(quest);
        this.sourceSlot = slot;
        this.dragStartX = mouseX;
        this.dragStartY = mouseY;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.dragStartTime = System.currentTimeMillis();
        this.isDragging = false; // Начинаем с false, активируем при движении
        
        return true;
    }
    
    /**
     * Обновляет состояние перетаскивания
     */
    public void updateDrag(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        
        // Активируем перетаскивание только после превышения порога
        if (!isDragging) {
            int deltaX = Math.abs(mouseX - dragStartX);
            int deltaY = Math.abs(mouseY - dragStartY);
            if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
                isDragging = true;
            }
        }
    }
    
    /**
     * Завершает перетаскивание и возвращает результат
     */
    public DragResult completeDrag(int mouseX, int mouseY, QuestInventory questInventory) {
        if (!isDragging || draggedQuest == null) {
            cancelDrag();
            return DragResult.CANCELLED;
        }
        
        DragResult result = DragResult.CANCELLED;
        
        // Здесь будет логика определения целевого слота
        // Пока что просто отменяем
        
        cancelDrag();
        return result;
    }
    
    /**
     * Отменяет перетаскивание
     */
    public void cancelDrag() {
        this.draggedQuest = null;
        this.draggedStack = ItemStack.EMPTY;
        this.sourceSlot = -1;
        this.isDragging = false;
        this.mouseX = 0;
        this.mouseY = 0;
        this.dragStartX = 0;
        this.dragStartY = 0;
        this.dragStartTime = 0;
    }
    
    /**
     * Проверяет, происходит ли перетаскивание
     */
    public boolean isDragging() {
        return isDragging && draggedQuest != null;
    }
    
    /**
     * Получает перетаскиваемый квест
     */
    public Quest getDraggedQuest() {
        return draggedQuest;
    }
    
    /**
     * Получает исходный слот
     */
    public int getSourceSlot() {
        return sourceSlot;
    }
    
    /**
     * Отрисовывает перетаскиваемый квест
     */
    public void renderDraggedQuest(DrawContext context) {
        if (!isDragging() || draggedStack.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Вычисляем анимацию
        long currentTime = System.currentTimeMillis();
        float animationProgress = Math.min((currentTime - dragStartTime) / 200f, 1f); // 200ms анимация
        
        // Применяем масштаб и прозрачность
        float scale = MathHelper.lerp(animationProgress, 1f, DRAG_SCALE);
        int alpha = (int) MathHelper.lerp(animationProgress, 255f, DRAG_ALPHA);
        
        // Позиция с учетом масштаба
        int renderX = mouseX - 8;
        int renderY = mouseY - 8;
        
        // Сохраняем матрицу
        context.getMatrices().push();
        
        // Применяем трансформации
        context.getMatrices().translate(renderX + 8, renderY + 8, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-8, -8, 0);
        
        // Отрисовываем предмет с прозрачностью
        context.drawItem(draggedStack, 0, 0);
        
        // Восстанавливаем матрицу
        context.getMatrices().pop();
        
        // Отрисовываем количество если нужно
        if (draggedStack.getCount() > 1) {
            String countText = String.valueOf(draggedStack.getCount());
            int textX = renderX + 16 - client.textRenderer.getWidth(countText);
            int textY = renderY + 8;
            context.drawText(client.textRenderer, countText, textX, textY, 0xFFFFFF, true);
        }
        
        // Отрисовываем дополнительную информацию о квесте
        QuestDragVisualizer.renderDragInfo(context, draggedQuest, mouseX, mouseY);
        
        // Отрисовываем траекторию перетаскивания
        if (hasPassedDragThreshold()) {
            QuestDragVisualizer.renderDragTrail(context, dragStartX, dragStartY, mouseX, mouseY, 
                0.5f, draggedQuest.getRarity().getColor().getColorValue() != null ? 
                draggedQuest.getRarity().getColor().getColorValue() : 0xFFFFFF);
        }
    }
    
    /**
     * Проверяет, находится ли точка в области слота
     */
    public boolean isPointInSlot(int pointX, int pointY, int slotX, int slotY, int slotWidth, int slotHeight) {
        return pointX >= slotX && pointX < slotX + slotWidth &&
               pointY >= slotY && pointY < slotY + slotHeight;
    }
    
    /**
     * Получает текущую позицию мыши X
     */
    public int getMouseX() {
        return mouseX;
    }
    
    /**
     * Получает текущую позицию мыши Y
     */
    public int getMouseY() {
        return mouseY;
    }
    
    /**
     * Проверяет, прошло ли достаточно времени для начала перетаскивания
     */
    public boolean hasPassedDragThreshold() {
        return System.currentTimeMillis() - dragStartTime > 100; // 100ms задержка
    }
    
    /**
     * Отрисовывает подсветку слота при перетаскивании
     */
    public void renderSlotHighlight(DrawContext context, int slotX, int slotY, int slotWidth, int slotHeight, 
                                   boolean isValidTarget) {
        if (!isDragging()) return;
        
        QuestDragVisualizer.HighlightType type = isValidTarget ? 
            QuestDragVisualizer.HighlightType.VALID_DROP : 
            QuestDragVisualizer.HighlightType.INVALID_DROP;
            
        QuestDragVisualizer.renderAnimatedSlotHighlight(context, slotX, slotY, slotWidth, slotHeight, 
            type, System.currentTimeMillis());
    }
    
    /**
     * Отрисовывает индикатор возможности сброса
     */
    public void renderDropIndicator(DrawContext context, boolean canDrop) {
        if (!isDragging()) return;
        
        if (canDrop) {
            QuestDragVisualizer.renderDropValidIndicator(context, mouseX, mouseY);
        } else {
            QuestDragVisualizer.renderDropProhibitedIndicator(context, mouseX, mouseY);
        }
    }
    
    /**
     * Результат операции перетаскивания
     */
    public enum DragResult {
        SUCCESS,        // Успешно перемещен
        CANCELLED,      // Отменено
        INVALID_TARGET, // Неверная цель
        SLOT_OCCUPIED,  // Слот занят
        QUEST_MASKED    // Квест замаскирован
    }
}