package io.github.apace100.origins.quest;

import net.minecraft.inventory.SimpleInventory;
import java.util.List;
import java.util.ArrayList;

/**
 * Инвентарь для квестов на доске объявлений
 */
public class QuestInventory extends SimpleInventory {
    private final BountyBoardBlockEntity blockEntity;
    private int selectedIndex = -1;
    
    public QuestInventory(int size, BountyBoardBlockEntity blockEntity) {
        super(size);
        this.blockEntity = blockEntity;
    }
    
    /**
     * Получает список доступных квестов
     */
    public List<Quest> getAvailableQuests() {
        if (blockEntity != null) {
            return blockEntity.getAvailableQuests();
        }
        return new ArrayList<>();
    }
    
    /**
     * Получает индекс выбранного квеста
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Выбирает квест по индексу
     */
    public void selectQuest(int index) {
        this.selectedIndex = index;
    }
    
    /**
     * Получает выбранный квест
     */
    public Quest getSelectedQuest() {
        if (selectedIndex >= 0 && selectedIndex < getAvailableQuests().size()) {
            return getAvailableQuests().get(selectedIndex);
        }
        return null;
    }
    
    /**
     * Получает квест по индексу
     */
    public Quest getQuest(int index) {
        List<Quest> quests = getAvailableQuests();
        if (index >= 0 && index < quests.size()) {
            return quests.get(index);
        }
        return null;
    }
    
    /**
     * Обновляет список квестов
     */
    public void refreshQuests() {
        // Сбрасываем выбранный индекс при обновлении
        selectedIndex = -1;
    }
}