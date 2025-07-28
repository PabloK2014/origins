package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Специальный слот для выбранного/принятого квеста
 */
public class SelectedQuestSlot extends Slot {
    private final QuestInventory questInventory;
    
    public SelectedQuestSlot(QuestInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.questInventory = inventory;
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        // Можно вставлять только квесты
        if (!QuestItem.isQuestStack(stack)) {
            return false;
        }
        
        // Проверяем, что слот пуст
        return !hasStack();
    }
    
    @Override
    public boolean canTakeItems(PlayerEntity player) {
        // Можно брать квест из слота выбора
        return hasStack();
    }
    
    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        super.onTakeItem(player, stack);
        
        // Снимаем выбор квеста
        questInventory.selectQuest(-1);
        
        // Снимаем маскировку с похожих квестов
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest != null) {
            questInventory.unmaskSimilarQuests(quest);
        }
    }
    
    @Override
    public ItemStack getStack() {
        Quest selectedQuest = questInventory.getSelectedQuest();
        if (selectedQuest != null) {
            return QuestItem.createQuestStack(selectedQuest);
        }
        return ItemStack.EMPTY;
    }
    
    @Override
    public void setStack(ItemStack stack) {
        if (stack.isEmpty()) {
            // Очищаем выбор и снимаем маскировку
            Quest previousQuest = questInventory.getSelectedQuest();
            questInventory.selectQuest(-1);
            
            if (previousQuest != null) {
                questInventory.unmaskSimilarQuests(previousQuest);
            }
        } else if (QuestItem.isQuestStack(stack)) {
            // Устанавливаем новый выбранный квест
            Quest quest = QuestItem.getQuestFromStack(stack);
            if (quest != null) {
                // Снимаем маскировку с предыдущего квеста
                Quest previousQuest = questInventory.getSelectedQuest();
                if (previousQuest != null) {
                    questInventory.unmaskSimilarQuests(previousQuest);
                }
                
                // Находим индекс квеста в списке доступных
                int questIndex = findQuestIndex(quest);
                if (questIndex >= 0) {
                    questInventory.selectQuest(questIndex);
                    // Маскируем похожие квесты
                    questInventory.maskSimilarQuests(quest);
                }
            }
        }
        markDirty();
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        questInventory.markDirty();
    }
    
    @Override
    public boolean hasStack() {
        return questInventory.getSelectedQuest() != null;
    }
    
    @Override
    public int getMaxItemCount() {
        return 1; // Только один квест
    }
    
    @Override
    public int getMaxItemCount(ItemStack stack) {
        return QuestItem.isQuestStack(stack) ? 1 : 0;
    }
    
    /**
     * Получает выбранный квест
     */
    public Quest getSelectedQuest() {
        return questInventory.getSelectedQuest();
    }
    
    /**
     * Проверяет, можно ли принять выбранный квест
     */
    public boolean canAcceptQuest(PlayerEntity player) {
        Quest quest = getSelectedQuest();
        if (quest == null) return false;
        
        // Проверяем класс игрока
        String playerClass = getPlayerClass(player);
        return quest.canPlayerTakeQuest(playerClass);
    }
    
    /**
     * Проверяет, можно ли сдать выбранный квест
     */
    public boolean canCompleteQuest(PlayerEntity player) {
        Quest quest = getSelectedQuest();
        if (quest == null) return false;
        
        // Проверяем выполнение целей квеста
        return isQuestCompleted(player, quest);
    }
    
    /**
     * Находит индекс квеста в списке доступных
     */
    private int findQuestIndex(Quest quest) {
        if (quest == null) return -1;
        
        var availableQuests = questInventory.getAvailableQuests();
        for (int i = 0; i < availableQuests.size(); i++) {
            Quest availableQuest = availableQuests.get(i);
            if (availableQuest != null && availableQuest.getId().equals(quest.getId())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Получает класс игрока
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
     * Проверяет, выполнен ли квест
     */
    private boolean isQuestCompleted(PlayerEntity player, Quest quest) {
        QuestObjective objective = quest.getObjective();
        if (objective == null) return false;
        
        switch (objective.getType()) {
            case COLLECT:
                return hasItemInInventory(player, objective.getTarget(), objective.getAmount());
            case KILL:
                // Для убийства мобов нужна отдельная система отслеживания
                return objective.isCompleted();
            case CRAFT:
                // Для крафта также нужна отдельная система
                return objective.isCompleted();
            default:
                return false;
        }
    }
    
    /**
     * Обрабатывает принятие квеста
     */
    public boolean acceptQuest(PlayerEntity player) {
        Quest quest = getSelectedQuest();
        if (quest == null || !canAcceptQuest(player)) {
            return false;
        }
        
        // Здесь будет логика добавления квеста в активные квесты игрока
        // Пока что просто очищаем слот
        setStack(ItemStack.EMPTY);
        
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(
                net.minecraft.text.Text.translatable("quest.origins.accepted", quest.getTitle()),
                false
            );
        }
        
        return true;
    }
    
    /**
     * Обрабатывает сдачу квеста
     */
    public boolean completeQuest(PlayerEntity player) {
        Quest quest = getSelectedQuest();
        if (quest == null || !canCompleteQuest(player)) {
            return false;
        }
        
        // Забираем необходимые предметы
        if (quest.getObjective().getType() == QuestObjective.ObjectiveType.COLLECT) {
            removeItemFromInventory(player, quest.getObjective().getTarget(), quest.getObjective().getAmount());
        }
        
        // Выдаем награду
        if (quest.getReward() != null && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            quest.getReward().giveReward(serverPlayer);
        }
        
        // Очищаем слот
        setStack(ItemStack.EMPTY);
        
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(
                net.minecraft.text.Text.translatable("quest.origins.completed", quest.getTitle()),
                false
            );
        }
        
        return true;
    }
    
    /**
     * Проверяет, истек ли квест
     */
    public boolean isQuestExpired(long startTime) {
        Quest quest = getSelectedQuest();
        return quest != null && quest.isExpired(startTime);
    }
    
    /**
     * Получает прогресс выполнения квеста в процентах
     */
    public float getQuestProgress(PlayerEntity player) {
        Quest quest = getSelectedQuest();
        if (quest == null) return 0f;
        
        QuestObjective objective = quest.getObjective();
        if (objective == null) return 0f;
        
        switch (objective.getType()) {
            case COLLECT:
                int currentAmount = getItemAmountInInventory(player, objective.getTarget());
                return Math.min(1f, (float) currentAmount / objective.getAmount());
            case KILL:
            case CRAFT:
                return objective.isCompleted() ? 1f : 0f;
            default:
                return 0f;
        }
    }
    
    /**
     * Получает количество предмета в инвентаре
     */
    private int getItemAmountInInventory(PlayerEntity player, String itemId) {
        int foundAmount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().toString().equals(itemId)) {
                foundAmount += stack.getCount();
            }
        }
        return foundAmount;
    }
    
    /**
     * Удаляет предметы из инвентаря игрока
     */
    private void removeItemFromInventory(PlayerEntity player, String itemId, int amount) {
        int remainingToRemove = amount;
        for (int i = 0; i < player.getInventory().size() && remainingToRemove > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().toString().equals(itemId)) {
                int toRemove = Math.min(remainingToRemove, stack.getCount());
                stack.decrement(toRemove);
                remainingToRemove -= toRemove;
            }
        }
    }
    
    /**
     * Проверяет наличие предмета в инвентаре
     */
    private boolean hasItemInInventory(PlayerEntity player, String itemId, int amount) {
        int foundAmount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().toString().equals(itemId)) {
                foundAmount += stack.getCount();
                if (foundAmount >= amount) return true;
            }
        }
        return false;
    }
}