package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Менеджер квестов, управляющий системой квестов Origins.
 * Обеспечивает загрузку, генерацию и управление квестами.
 */
public class QuestManager {
    private static QuestManager instance;
    
    private final Map<String, List<Quest>> questsByClass = new HashMap<>();
    private final Map<UUID, List<ActiveQuest>> activeQuests = new HashMap<>();
    private final Random random = Random.create();
    
    private QuestManager() {
        initializeQuests();
    }
    
    public static QuestManager getInstance() {
        if (instance == null) {
            instance = new QuestManager();
        }
        return instance;
    }
    
    /**
     * Инициализация квестов из JSON файлов
     */
    private void initializeQuests() {
        // Пока что используем генератор квестов
        // В будущем здесь будет загрузка из JSON файлов
        Origins.LOGGER.info("Инициализация системы квестов...");
    }
    
    /**
     * Получает случайные квесты для указанного класса
     */
    public List<Quest> getRandomQuestsForClass(String playerClass, int count) {
        if ("human".equals(playerClass)) {
            return new ArrayList<>(); // Люди не получают квесты
        }
        
        List<Quest> quests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Quest quest = generateQuestForClass(playerClass);
            if (quest != null) {
                quests.add(quest);
            }
        }
        
        return quests;
    }
    
    /**
     * Генерирует квест для указанного класса
     */
    private Quest generateQuestForClass(String playerClass) {
        // Нормализуем название класса
        String normalizedClass = playerClass.startsWith("origins:") ? 
            playerClass.substring(8) : playerClass;
        
        // Используем QuestGenerator для получения случайного квеста
        List<Quest> availableQuests = QuestGenerator.getRandomQuestsForProfession(normalizedClass, 1);
        
        return availableQuests.isEmpty() ? null : availableQuests.get(0);
    }
    
    /**
     * Проверяет, может ли игрок взять квест
     */
    public boolean canTakeQuest(PlayerEntity player, Quest quest) {
        if (quest == null || player == null) return false;
        
        UUID playerId = player.getUuid();
        
        // Проверяем, не достиг ли игрок лимита активных квестов
        List<ActiveQuest> playerQuests = activeQuests.get(playerId);
        if (playerQuests != null && playerQuests.size() >= getMaxActiveQuests()) {
            Origins.LOGGER.info("Игрок {} достиг лимита квестов: {}/{}", 
                player.getName().getString(), playerQuests.size(), getMaxActiveQuests());
            return false;
        }
        
        // Проверяем, нет ли уже этого конкретного квеста
        if (hasActiveQuest(player, quest.getId())) {
            Origins.LOGGER.info("У игрока {} уже есть квест {}", player.getName().getString(), quest.getId());
            return false;
        }
        
        // Проверяем соответствие класса
        String playerClass = QuestIntegration.getPlayerClass(player);
        if (!isClassCompatible(playerClass, quest.getPlayerClass())) {
            Origins.LOGGER.info("Класс игрока {} ({}) не подходит для квеста {} ({})", 
                player.getName().getString(), playerClass, quest.getId(), quest.getPlayerClass());
            return false;
        }
        
        return true;
    }
    
    /**
     * Начинает квест для игрока
     */
    public void startQuest(PlayerEntity player, Quest quest) {
        if (!canTakeQuest(player, quest)) {
            Origins.LOGGER.warn("Игрок {} не может взять квест {}: не прошел проверку canTakeQuest", 
                player.getName().getString(), quest.getId());
            return;
        }
        
        UUID playerId = player.getUuid();
        ActiveQuest activeQuest = new ActiveQuest(quest, System.currentTimeMillis());
        
        // Получаем или создаем список активных квестов для игрока
        List<ActiveQuest> playerQuests = activeQuests.computeIfAbsent(playerId, k -> new ArrayList<>());
        playerQuests.add(activeQuest);
        
        Origins.LOGGER.info("Игрок {} начал квест: {} (активных квестов: {})", 
            player.getName().getString(), quest.getTitle(), playerQuests.size());
    }
    
    /**
     * Завершает квест для игрока
     */
    public void completeQuest(PlayerEntity player, Quest quest) {
        UUID playerId = player.getUuid();
        List<ActiveQuest> playerQuests = activeQuests.get(playerId);
        
        if (playerQuests != null) {
            boolean removed = playerQuests.removeIf(activeQuest -> activeQuest.getQuest().equals(quest));
            if (removed) {
                Origins.LOGGER.info("Игрок {} завершил квест: {}", player.getName().getString(), quest.getTitle());
                
                // Если у игрока больше нет активных квестов, удаляем запись
                if (playerQuests.isEmpty()) {
                    activeQuests.remove(playerId);
                }
            }
        }
    }
    
    /**
     * Завершает квест для игрока по ID
     */
    public void completeQuest(PlayerEntity player, String questId) {
        UUID playerId = player.getUuid();
        List<ActiveQuest> playerQuests = activeQuests.get(playerId);
        
        if (playerQuests != null) {
            boolean removed = playerQuests.removeIf(activeQuest -> activeQuest.getQuest().getId().equals(questId));
            if (removed) {
                Origins.LOGGER.info("Игрок {} завершил квест с ID: {}", player.getName().getString(), questId);
                
                // Если у игрока больше нет активных квестов, удаляем запись
                if (playerQuests.isEmpty()) {
                    activeQuests.remove(playerId);
                }
            }
        }
    }
    
    /**
     * Отменяет все квесты для игрока
     */
    public void cancelAllQuests(PlayerEntity player) {
        UUID playerId = player.getUuid();
        List<ActiveQuest> playerQuests = activeQuests.remove(playerId);
        
        if (playerQuests != null && !playerQuests.isEmpty()) {
            Origins.LOGGER.info("Игрок {} отменил {} квестов", player.getName().getString(), playerQuests.size());
        }
    }
    
    /**
     * Отменяет конкретный квест для игрока
     */
    public void cancelQuest(PlayerEntity player, String questId) {
        UUID playerId = player.getUuid();
        List<ActiveQuest> playerQuests = activeQuests.get(playerId);
        
        if (playerQuests != null) {
            boolean removed = playerQuests.removeIf(activeQuest -> activeQuest.getQuest().getId().equals(questId));
            if (removed) {
                Origins.LOGGER.info("Игрок {} отменил квест с ID: {}", player.getName().getString(), questId);
                
                // Если у игрока больше нет активных квестов, удаляем запись
                if (playerQuests.isEmpty()) {
                    activeQuests.remove(playerId);
                }
            }
        }
    }
    
    /**
     * Получает все активные квесты игрока
     */
    public List<ActiveQuest> getActiveQuests(PlayerEntity player) {
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        return playerQuests != null ? new ArrayList<>(playerQuests) : new ArrayList<>();
    }
    
    /**
     * Получает активный квест игрока по ID
     */
    public ActiveQuest getActiveQuest(PlayerEntity player, String questId) {
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        if (playerQuests == null) return null;
        
        return playerQuests.stream()
            .filter(activeQuest -> activeQuest.getQuest().getId().equals(questId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Проверяет, есть ли у игрока активные квесты
     */
    public boolean hasActiveQuest(PlayerEntity player) {
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        return playerQuests != null && !playerQuests.isEmpty();
    }
    
    /**
     * Проверяет, есть ли у игрока конкретный активный квест
     */
    public boolean hasActiveQuest(PlayerEntity player, String questId) {
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        if (playerQuests == null) return false;
        
        return playerQuests.stream()
            .anyMatch(activeQuest -> activeQuest.getQuest().getId().equals(questId));
    }
    
    /**
     * Получает количество активных квестов у игрока
     */
    public int getActiveQuestCount(PlayerEntity player) {
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        return playerQuests != null ? playerQuests.size() : 0;
    }
    
    /**
     * Обновляет прогресс квестов (вызывается периодически)
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем истечение времени квестов
        activeQuests.entrySet().removeIf(entry -> {
            List<ActiveQuest> playerQuests = entry.getValue();
            
            // Удаляем истекшие квесты из списка игрока
            playerQuests.removeIf(activeQuest -> {
                Quest quest = activeQuest.getQuest();
                
                if (quest.getTimeLimit() > 0) {
                    long elapsedMinutes = (currentTime - activeQuest.getStartTime()) / (1000 * 60);
                    if (elapsedMinutes >= quest.getTimeLimit()) {
                        Origins.LOGGER.info("Квест {} истек по времени", quest.getTitle());
                        return true;
                    }
                }
                
                return false;
            });
            
            // Если у игрока больше нет активных квестов, удаляем запись
            return playerQuests.isEmpty();
        });
    }
    
    /**
     * Получает все активные квесты (для совместимости)
     */
    public Map<UUID, List<ActiveQuest>> getAllActiveQuests() {
        return new HashMap<>(activeQuests);
    }
    
    /**
     * Очищает все активные квесты (для отладки)
     */
    public void clearAllQuests() {
        activeQuests.clear();
        Origins.LOGGER.info("Все активные квесты очищены");
    }
    
    /**
     * Получает статистику по квестам
     */
    public QuestStats getStats() {
        return new QuestStats(
                activeQuests.size(),
                questsByClass.values().stream().mapToInt(List::size).sum()
        );
    }
    
    /**
     * Проверяет, может ли игрок принять дополнительный квест
     */
    public boolean canAcceptAdditionalQuest(PlayerEntity player) {
        if (player == null) return false;
        
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        int currentCount = playerQuests != null ? playerQuests.size() : 0;
        return currentCount < getMaxActiveQuests();
    }
    
    /**
     * Получает количество доступных слотов для квестов
     */
    public int getAvailableQuestSlots(PlayerEntity player) {
        if (player == null) return 0;
        
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        int currentCount = playerQuests != null ? playerQuests.size() : 0;
        return Math.max(0, getMaxActiveQuests() - currentCount);
    }
    
    /**
     * Проверяет, есть ли у игрока конкретный активный квест по ID
     */
    public boolean hasSpecificActiveQuest(PlayerEntity player, String questId) {
        if (player == null || questId == null) return false;
        
        List<ActiveQuest> playerQuests = activeQuests.get(player.getUuid());
        if (playerQuests == null) return false;
        
        return playerQuests.stream()
            .anyMatch(activeQuest -> activeQuest.getQuest().getId().equals(questId));
    }
    
    /**
     * Получает максимальное количество активных квестов
     */
    public int getMaxActiveQuests() {
        return 5; // Можно сделать конфигурируемым позже
    }
    
    /**
     * Проверяет совместимость классов игрока и квеста
     */
    private boolean isClassCompatible(String playerClass, String questClass) {
        if (playerClass == null || questClass == null) {
            return false;
        }
        
        // Точное совпадение
        if (playerClass.equals(questClass)) {
            return true;
        }
        
        // Квесты для "human" могут брать все
        if ("human".equals(questClass)) {
            return true;
        }
        
        // Квесты для "any" могут брать все
        if ("any".equals(questClass)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Синхронизирует состояние квестов между QuestManager и QuestInventoryManager
     */
    public void synchronizeQuestState(PlayerEntity player) {
        if (player == null) return;
        
        try {
            Origins.LOGGER.info("Синхронизация квестов для игрока: {}", player.getName().getString());
            
            UUID playerId = player.getUuid();
            List<ActiveQuest> managerQuests = activeQuests.get(playerId);
            
            // Получаем билеты из инвентаря
            io.github.apace100.origins.quest.QuestInventoryManager inventoryManager = 
                io.github.apace100.origins.quest.QuestInventoryManager.getInstance();
            java.util.List<net.minecraft.item.ItemStack> tickets = inventoryManager.findQuestTickets(player);
            
            // Создаем множества ID для сравнения
            java.util.Set<String> managerQuestIds = new java.util.HashSet<>();
            if (managerQuests != null) {
                for (ActiveQuest activeQuest : managerQuests) {
                    managerQuestIds.add(activeQuest.getQuest().getId());
                }
            }
            
            java.util.Set<String> inventoryQuestIds = new java.util.HashSet<>();
            for (net.minecraft.item.ItemStack ticket : tickets) {
                io.github.apace100.origins.quest.Quest quest = 
                    io.github.apace100.origins.quest.QuestItem.getQuestFromStack(ticket);
                if (quest != null) {
                    inventoryQuestIds.add(quest.getId());
                }
            }
            
            // Удаляем квесты из менеджера, которых нет в инвентаре
            if (managerQuests != null) {
                managerQuests.removeIf(activeQuest -> {
                    String questId = activeQuest.getQuest().getId();
                    if (!inventoryQuestIds.contains(questId)) {
                        Origins.LOGGER.info("Удаляем квест {} из менеджера (нет билета)", questId);
                        return true;
                    }
                    return false;
                });
                
                if (managerQuests.isEmpty()) {
                    activeQuests.remove(playerId);
                }
            }
            
            // Удаляем билеты из инвентаря, которых нет в менеджере
            for (net.minecraft.item.ItemStack ticket : tickets) {
                io.github.apace100.origins.quest.Quest quest = 
                    io.github.apace100.origins.quest.QuestItem.getQuestFromStack(ticket);
                if (quest != null && !managerQuestIds.contains(quest.getId())) {
                    Origins.LOGGER.info("Удаляем билет квеста {} из инвентаря (нет в менеджере)", quest.getId());
                    inventoryManager.removeQuestTicketFromInventory(player, quest.getId());
                }
            }
            
            Origins.LOGGER.info("Синхронизация завершена для игрока: {}", player.getName().getString());
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при синхронизации квестов для игрока {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Класс для хранения статистики квестов
     */
    public static class QuestStats {
        private final int activeQuests;
        private final int totalQuests;
        
        public QuestStats(int activeQuests, int totalQuests) {
            this.activeQuests = activeQuests;
            this.totalQuests = totalQuests;
        }
        
        public int getActiveQuests() { return activeQuests; }
        public int getTotalQuests() { return totalQuests; }
    }
}