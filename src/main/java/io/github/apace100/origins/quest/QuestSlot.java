package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Специальный слот для квестов с поддержкой drag-and-drop
 * Основан на функционале из оригинального Bountiful мода
 */
public class QuestSlot extends Slot {
    private final QuestInventory questInventory;
    private final int questIndex;
    
    public QuestSlot(QuestInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.questInventory = inventory;
        this.questIndex = index;
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        // Можно вставлять только квесты и только если слот не замаскирован
        if (!QuestItem.isQuestStack(stack) || questInventory.isSlotMasked(questIndex)) {
            return false;
        }
        
        // Проверяем, что слот пуст
        return !hasStack();
    }
    
    @Override
    public boolean canTakeItems(PlayerEntity player) {
        // Проверяем, может ли игрок взаимодействовать с этим слотом
        if (questInventory.isSlotMasked(questIndex)) {
            return false;
        }
        
        return questInventory.canPlayerAccessSlot(questIndex, player);
    }
    
    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        super.onTakeItem(player, stack);
        
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest != null) {
            // Выбираем квест при взятии
            questInventory.selectQuest(questIndex);
            
            // Применяем маскировку для похожих квестов если игрок на сервере
            if (player instanceof ServerPlayerEntity serverPlayer) {
                QuestMaskManager.maskSimilarQuests(
                    serverPlayer.getUuid(), 
                    quest, 
                    questInventory.getAvailableQuests()
                );
            }
        }
    }
    
    @Override
    public ItemStack insertStack(ItemStack stack, int count) {
        if (!canInsert(stack)) {
            return stack;
        }
        
        // Вставляем квест в слот
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest != null) {
            // Обновляем квест в инвентаре
            questInventory.refreshQuests();
        }
        
        return super.insertStack(stack, count);
    }
    
    @Override
    public ItemStack getStack() {
        // Возвращаем пустой стек если слот замаскирован
        if (questInventory.isSlotMasked(questIndex)) {
            return ItemStack.EMPTY;
        }
        
        Quest quest = questInventory.getQuest(questIndex);
        if (quest != null) {
            return QuestItem.createQuestStack(quest);
        }
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public void setStack(ItemStack stack) {
        // Квесты нельзя устанавливать напрямую в слоты
        // Это должно происходить через QuestInventory
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        questInventory.markDirty();
    }
    
    @Override
    public boolean hasStack() {
        return !questInventory.isSlotMasked(questIndex) && 
               questInventory.getQuest(questIndex) != null;
    }
    
    @Override
    public int getMaxItemCount() {
        return 1; // Только один квест на слот
    }
    
    @Override
    public int getMaxItemCount(ItemStack stack) {
        return 1;
    }
    
    /**
     * Получает квест из этого слота
     */
    public Quest getQuest() {
        return questInventory.getQuest(questIndex);
    }
    
    /**
     * Проверяет, замаскирован ли этот слот
     */
    public boolean isMasked() {
        return questInventory.isSlotMasked(questIndex);
    }
    
    /**
     * Получает индекс квеста
     */
    public int getQuestIndex() {
        return questIndex;
    }
    
    /**
     * Проверяет, выбран ли квест в этом слоте
     */
    public boolean isSelected() {
        return questInventory.getSelectedIndex() == questIndex;
    }
    
    /**
     * Проверяет, доступен ли квест для игрока
     */
    public boolean isQuestAvailable(PlayerEntity player) {
        Quest quest = getQuest();
        if (quest == null) return false;
        
        String playerClass = getPlayerClass(player);
        return quest.canPlayerTakeQuest(playerClass);
    }
    
    /**
     * Получает класс игрока для проверки доступности квестов
     */
    private String getPlayerClass(PlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                        io.github.apace100.origins.origin.OriginLayers.getLayer(
                                io.github.apace100.origins.Origins.identifier("origin")));
                if (origin != null) {
                    String originId = origin.getIdentifier().toString();
                    return switch (originId) {
                        case "origins:warrior" -> "warrior";
                        case "origins:miner" -> "miner";
                        case "origins:blacksmith" -> "blacksmith";
                        case "origins:courier" -> "courier";
                        case "origins:brewer" -> "brewer";
                        case "origins:cook" -> "cook";
                        default -> "human";
                    };
                }
            }
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при получении класса игрока: " + e.getMessage());
        }
        return "human";
    }
    
    /**
     * Проверяет, может ли квест быть перетащен в этот слот
     */
    public boolean canAcceptDraggedQuest(Quest quest, PlayerEntity player) {
        if (quest == null || hasStack() || isMasked()) {
            return false;
        }
        
        String playerClass = getPlayerClass(player);
        return quest.canPlayerTakeQuest(playerClass);
    }
    
    /**
     * Обрабатывает сброс квеста в этот слот
     */
    public boolean handleQuestDrop(Quest quest, PlayerEntity player) {
        if (!canAcceptDraggedQuest(quest, player)) {
            return false;
        }
        
        // Создаем ItemStack для квеста
        ItemStack questStack = QuestItem.createQuestStack(quest);
        
        // Вставляем в слот
        setStack(questStack);
        
        // Обновляем инвентарь
        questInventory.refreshQuests();
        
        return true;
    }
}