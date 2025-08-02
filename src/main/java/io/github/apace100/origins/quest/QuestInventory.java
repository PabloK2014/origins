package io.github.apace100.origins.quest;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Расширенный инвентарь для квестов с поддержкой маскировки слотов и выбора квестов
 */
public class QuestInventory extends SimpleInventory {
    private final Set<Integer> maskedSlots = new HashSet<>();
    private int selectedIndex = -1;
    private final List<Quest> availableQuests = new ArrayList<>();
    private final BountyBoardBlockEntity blockEntity;
    
    public QuestInventory(int size, BountyBoardBlockEntity blockEntity) {
        super(size);
        this.blockEntity = blockEntity;
        refreshQuests();
    }
    
    /**
     * Маскирует слот, делая его недоступным для взаимодействия
     */
    public void maskSlot(int index) {
        if (index >= 0 && index < size()) {
            maskedSlots.add(index);
            markDirty();
        }
    }
    
    /**
     * Снимает маскировку со слота
     */
    public void unmaskSlot(int index) {
        if (maskedSlots.remove(index)) {
            markDirty();
        }
    }
    
    /**
     * Проверяет, замаскирован ли слот
     */
    public boolean isSlotMasked(int index) {
        return maskedSlots.contains(index);
    }
    
    /**
     * Получает все замаскированные слоты
     */
    public Set<Integer> getMaskedSlots() {
        return new HashSet<>(maskedSlots);
    }
    
    /**
     * Очищает все маскированные слоты
     */
    public void clearMaskedSlots() {
        maskedSlots.clear();
        markDirty();
    }
    
    /**
     * Выбирает квест по индексу
     */
    public void selectQuest(int index) {
        if (index >= 0 && index < availableQuests.size() && !isSlotMasked(index)) {
            this.selectedIndex = index;
            markDirty();
        } else if (index == -1) {
            this.selectedIndex = -1;
            markDirty();
        }
    }
    
    /**
     * Получает индекс выбранного квеста
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Получает выбранный квест
     */
    public Quest getSelectedQuest() {
        if (selectedIndex >= 0 && selectedIndex < availableQuests.size()) {
            return availableQuests.get(selectedIndex);
        }
        return null;
    }
    
    /**
     * Получает квест по индексу
     */
    public Quest getQuest(int index) {
        if (index >= 0 && index < availableQuests.size()) {
            return availableQuests.get(index);
        }
        return null;
    }
    
    /**
     * Получает список доступных квестов
     */
    public List<Quest> getAvailableQuests() {
        return new ArrayList<>(availableQuests);
    }
    
    /**
     * Получает список видимых (не замаскированных) квестов
     */
    public List<Quest> getVisibleQuests() {
        List<Quest> visibleQuests = new ArrayList<>();
        for (int i = 0; i < availableQuests.size(); i++) {
            if (!isSlotMasked(i)) {
                visibleQuests.add(availableQuests.get(i));
            }
        }
        return visibleQuests;
    }
    
    /**
     * Обновляет список квестов из блок-сущности
     */
    public void refreshQuests() {
        availableQuests.clear();
        if (blockEntity != null) {
            List<Quest> blockQuests = blockEntity.getAvailableQuests();
            availableQuests.addAll(blockQuests);
        }
        
        // Очищаем выбор если он стал невалидным
        if (selectedIndex >= availableQuests.size()) {
            selectedIndex = -1;
        }
        
        markDirty();
    }
    
    /**
     * Автоматически маскирует квесты, недоступные для игрока по классу
     */
    public void applyClassRestrictions(PlayerEntity player) {
        if (player == null || blockEntity == null) return;
        
        String playerClass = getPlayerClass(player);
        String boardClass = blockEntity.getBoardClass();
        
        // Если это обычная доска, не применяем ограничения
        if ("general".equals(boardClass)) {
            return;
        }
        
        // Если класс игрока не соответствует классу доски, маскируем все квесты
        if (!boardClass.equals(playerClass)) {
            for (int i = 0; i < availableQuests.size(); i++) {
                maskSlot(i);
            }
            return;
        }
        
        // Маскируем квесты, которые не подходят по классу
        for (int i = 0; i < availableQuests.size(); i++) {
            Quest quest = availableQuests.get(i);
            if (quest != null && quest.getPlayerClass() != null && 
                !quest.getPlayerClass().equals("any") && 
                !quest.getPlayerClass().equals(playerClass)) {
                maskSlot(i);
            }
        }
    }
    
    /**
     * Маскирует квесты аналогичные данному (для предотвращения дубликатов)
     */
    public void maskSimilarQuests(Quest quest) {
        if (quest == null) return;
        
        for (int i = 0; i < availableQuests.size(); i++) {
            Quest availableQuest = availableQuests.get(i);
            if (availableQuest != null && areSimilarQuests(quest, availableQuest)) {
                maskSlot(i);
            }
        }
    }
    
    /**
     * Снимает маскировку с квестов аналогичных данному
     */
    public void unmaskSimilarQuests(Quest quest) {
        if (quest == null) return;
        
        for (int i = 0; i < availableQuests.size(); i++) {
            Quest availableQuest = availableQuests.get(i);
            if (availableQuest != null && areSimilarQuests(quest, availableQuest)) {
                unmaskSlot(i);
            }
        }
    }
    
    /**
     * Проверяет, являются ли квесты похожими (одинаковые цели)
     */
    private boolean areSimilarQuests(Quest quest1, Quest quest2) {
        if (quest1 == null || quest2 == null) return false;
        if (quest1.getId().equals(quest2.getId())) return true;
        
        // Проверяем схожесть целей
        QuestObjective obj1 = quest1.getObjective();
        QuestObjective obj2 = quest2.getObjective();
        
        return obj1.getType() == obj2.getType() && 
               obj1.getTarget().equals(obj2.getTarget()) &&
               obj1.getAmount() == obj2.getAmount();
    }
    
    /**
     * Проверяет, может ли игрок взаимодействовать со слотом
     */
    public boolean canPlayerAccessSlot(int index, PlayerEntity player) {
        if (isSlotMasked(index)) return false;
        
        Quest quest = getQuest(index);
        if (quest == null) return false;
        
        // Проверяем класс игрока
        String playerClass = getPlayerClass(player);
        
        // Проверяем совместимость квеста с классом игрока
        if (!playerClass.equals("human") && !quest.getPlayerClass().equals(playerClass)) {
            return false;
        }
        
        return true;
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
     * Сохраняет состояние маскировки в NBT
     */
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        
        // Сохраняем замаскированные слоты
        NbtList maskedList = new NbtList();
        for (Integer slot : maskedSlots) {
            NbtCompound slotNbt = new NbtCompound();
            slotNbt.putInt("slot", slot);
            maskedList.add(slotNbt);
        }
        nbt.put("maskedSlots", maskedList);
        
        // Сохраняем выбранный индекс
        nbt.putInt("selectedIndex", selectedIndex);
        
        return nbt;
    }
    
    /**
     * Загружает состояние маскировки из NBT
     */
    public void readNbt(NbtCompound nbt) {
        maskedSlots.clear();
        
        // Загружаем замаскированные слоты
        if (nbt.contains("maskedSlots")) {
            NbtList maskedList = nbt.getList("maskedSlots", 10); // 10 = compound
            for (int i = 0; i < maskedList.size(); i++) {
                NbtCompound slotNbt = maskedList.getCompound(i);
                maskedSlots.add(slotNbt.getInt("slot"));
            }
        }
        
        // Загружаем выбранный индекс
        selectedIndex = nbt.getInt("selectedIndex");
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        if (blockEntity != null) {
            blockEntity.markDirty();
        }
    }
    
    /**
     * Проверяет валидность выбранного квеста
     */
    public boolean isSelectedQuestValid() {
        return selectedIndex >= 0 && 
               selectedIndex < availableQuests.size() && 
               !isSlotMasked(selectedIndex) &&
               availableQuests.get(selectedIndex) != null;
    }
    
    /**
     * Получает количество видимых квестов
     */
    public int getVisibleQuestCount() {
        int count = 0;
        for (int i = 0; i < availableQuests.size(); i++) {
            if (!isSlotMasked(i)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Получает индекс первого доступного квеста
     */
    public int getFirstAvailableQuestIndex() {
        for (int i = 0; i < availableQuests.size(); i++) {
            if (!isSlotMasked(i)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Устанавливает выбранный квест
     */
    public void setSelectedQuest(Quest quest) {
        if (quest == null) {
            selectedIndex = -1;
            return;
        }
        
        for (int i = 0; i < availableQuests.size(); i++) {
            if (availableQuests.get(i) != null && availableQuests.get(i).getId().equals(quest.getId())) {
                selectedIndex = i;
                markDirty();
                return;
            }
        }
    }
    
    /**
     * Очищает выбор квеста
     */
    public void clearSelection() {
        selectedIndex = -1;
        markDirty();
    }
}