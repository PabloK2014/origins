package io.github.apace100.origins.quest;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Управляет маскировкой квестов для игроков
 * Основано на системе репутации из оригинального Bountiful
 */
public class QuestMaskManager {
    private static final Map<UUID, PlayerQuestMask> playerMasks = new HashMap<>();
    
    /**
     * Получает маску квестов для игрока
     */
    public static PlayerQuestMask getMask(UUID playerId) {
        return playerMasks.computeIfAbsent(playerId, k -> new PlayerQuestMask());
    }
    
    /**
     * Маскирует квесты аналогичные данному для игрока
     */
    public static void maskSimilarQuests(UUID playerId, Quest quest, List<Quest> availableQuests) {
        if (quest == null || availableQuests == null) return;
        
        PlayerQuestMask mask = getMask(playerId);
        
        for (int i = 0; i < availableQuests.size(); i++) {
            Quest availableQuest = availableQuests.get(i);
            if (availableQuest != null && areSimilarQuests(quest, availableQuest)) {
                mask.maskSlot(i);
            }
        }
    }
    
    /**
     * Снимает маскировку с квестов аналогичных данному для игрока
     */
    public static void unmaskSimilarQuests(UUID playerId, Quest quest, List<Quest> availableQuests) {
        if (quest == null || availableQuests == null) return;
        
        PlayerQuestMask mask = getMask(playerId);
        
        for (int i = 0; i < availableQuests.size(); i++) {
            Quest availableQuest = availableQuests.get(i);
            if (availableQuest != null && areSimilarQuests(quest, availableQuest)) {
                mask.unmaskSlot(i);
            }
        }
    }
    
    /**
     * Проверяет, замаскирован ли слот для игрока
     */
    public static boolean isSlotMasked(UUID playerId, int slotIndex) {
        return getMask(playerId).isSlotMasked(slotIndex);
    }
    
    /**
     * Получает все замаскированные слоты для игрока
     */
    public static Set<Integer> getMaskedSlots(UUID playerId) {
        return getMask(playerId).getMaskedSlots();
    }
    
    /**
     * Очищает все маски для игрока
     */
    public static void clearMasks(UUID playerId) {
        PlayerQuestMask mask = getMask(playerId);
        mask.clearMasks();
    }
    
    /**
     * Проверяет, являются ли квесты похожими
     */
    private static boolean areSimilarQuests(Quest quest1, Quest quest2) {
        if (quest1 == null || quest2 == null) return false;
        if (quest1.getId().equals(quest2.getId())) return true;
        
        // Проверяем схожесть целей
        QuestObjective obj1 = quest1.getObjective();
        QuestObjective obj2 = quest2.getObjective();
        
        if (obj1 == null || obj2 == null) return false;
        
        return obj1.getType() == obj2.getType() && 
               obj1.getTarget().equals(obj2.getTarget()) &&
               obj1.getAmount() == obj2.getAmount();
    }
    
    /**
     * Сохраняет маски игрока в NBT
     */
    public static NbtCompound savePlayerMask(UUID playerId) {
        PlayerQuestMask mask = getMask(playerId);
        return mask.writeNbt();
    }
    
    /**
     * Загружает маски игрока из NBT
     */
    public static void loadPlayerMask(UUID playerId, NbtCompound nbt) {
        PlayerQuestMask mask = getMask(playerId);
        mask.readNbt(nbt);
    }
    
    /**
     * Удаляет данные игрока (при выходе с сервера)
     */
    public static void removePlayer(UUID playerId) {
        playerMasks.remove(playerId);
    }
    
    /**
     * Класс для хранения маски квестов конкретного игрока
     */
    public static class PlayerQuestMask {
        private final Set<Integer> maskedSlots = new HashSet<>();
        private final Map<String, Long> questCooldowns = new HashMap<>();
        
        /**
         * Маскирует слот
         */
        public void maskSlot(int slotIndex) {
            maskedSlots.add(slotIndex);
        }
        
        /**
         * Снимает маскировку со слота
         */
        public void unmaskSlot(int slotIndex) {
            maskedSlots.remove(slotIndex);
        }
        
        /**
         * Проверяет, замаскирован ли слот
         */
        public boolean isSlotMasked(int slotIndex) {
            return maskedSlots.contains(slotIndex);
        }
        
        /**
         * Получает все замаскированные слоты
         */
        public Set<Integer> getMaskedSlots() {
            return new HashSet<>(maskedSlots);
        }
        
        /**
         * Очищает все маски
         */
        public void clearMasks() {
            maskedSlots.clear();
        }
        
        /**
         * Добавляет кулдаун для квеста
         */
        public void addQuestCooldown(String questId, long cooldownTime) {
            questCooldowns.put(questId, System.currentTimeMillis() + cooldownTime);
        }
        
        /**
         * Проверяет, есть ли кулдаун у квеста
         */
        public boolean hasQuestCooldown(String questId) {
            Long cooldownEnd = questCooldowns.get(questId);
            if (cooldownEnd == null) return false;
            
            if (System.currentTimeMillis() >= cooldownEnd) {
                questCooldowns.remove(questId);
                return false;
            }
            return true;
        }
        
        /**
         * Получает оставшееся время кулдауна квеста в миллисекундах
         */
        public long getRemainingCooldown(String questId) {
            Long cooldownEnd = questCooldowns.get(questId);
            if (cooldownEnd == null) return 0;
            
            long remaining = cooldownEnd - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
        
        /**
         * Сохраняет маску в NBT
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
            
            // Сохраняем кулдауны квестов
            NbtCompound cooldownsNbt = new NbtCompound();
            for (Map.Entry<String, Long> entry : questCooldowns.entrySet()) {
                cooldownsNbt.putLong(entry.getKey(), entry.getValue());
            }
            nbt.put("questCooldowns", cooldownsNbt);
            
            return nbt;
        }
        
        /**
         * Загружает маску из NBT
         */
        public void readNbt(NbtCompound nbt) {
            maskedSlots.clear();
            questCooldowns.clear();
            
            // Загружаем замаскированные слоты
            if (nbt.contains("maskedSlots")) {
                NbtList maskedList = nbt.getList("maskedSlots", 10); // 10 = compound
                for (int i = 0; i < maskedList.size(); i++) {
                    NbtCompound slotNbt = maskedList.getCompound(i);
                    maskedSlots.add(slotNbt.getInt("slot"));
                }
            }
            
            // Загружаем кулдауны квестов
            if (nbt.contains("questCooldowns")) {
                NbtCompound cooldownsNbt = nbt.getCompound("questCooldowns");
                for (String key : cooldownsNbt.getKeys()) {
                    questCooldowns.put(key, cooldownsNbt.getLong(key));
                }
            }
        }
        
        /**
         * Очищает устаревшие кулдауны
         */
        public void cleanupExpiredCooldowns() {
            long currentTime = System.currentTimeMillis();
            questCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        }
    }
}