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
    private final Map<UUID, ActiveQuest> activeQuests = new HashMap<>();
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
        
        // Проверяем, есть ли уже активный квест
        if (activeQuests.containsKey(playerId)) {
            return false;
        }
        
        // Проверяем соответствие класса
        String playerClass = QuestIntegration.getPlayerClass(player);
        if (!quest.getPlayerClass().equals(playerClass)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Начинает квест для игрока
     */
    public void startQuest(PlayerEntity player, Quest quest) {
        if (!canTakeQuest(player, quest)) {
            return;
        }
        
        UUID playerId = player.getUuid();
        ActiveQuest activeQuest = new ActiveQuest(quest, System.currentTimeMillis());
        activeQuests.put(playerId, activeQuest);
        
        Origins.LOGGER.info("Игрок {} начал квест: {}", player.getName().getString(), quest.getTitle());
    }
    
    /**
     * Завершает квест для игрока
     */
    public void completeQuest(PlayerEntity player, Quest quest) {
        UUID playerId = player.getUuid();
        ActiveQuest activeQuest = activeQuests.get(playerId);
        
        if (activeQuest != null && activeQuest.getQuest().equals(quest)) {
            activeQuests.remove(playerId);
            Origins.LOGGER.info("Игрок {} завершил квест: {}", player.getName().getString(), quest.getTitle());
        }
    }
    
    /**
     * Отменяет квест для игрока
     */
    public void cancelQuest(PlayerEntity player) {
        UUID playerId = player.getUuid();
        ActiveQuest activeQuest = activeQuests.remove(playerId);
        
        if (activeQuest != null) {
            Origins.LOGGER.info("Игрок {} отменил квест: {}", player.getName().getString(), 
                    activeQuest.getQuest().getTitle());
        }
    }
    
    /**
     * Получает активный квест игрока
     */
    public ActiveQuest getActiveQuest(PlayerEntity player) {
        return activeQuests.get(player.getUuid());
    }
    
    /**
     * Проверяет, есть ли у игрока активный квест
     */
    public boolean hasActiveQuest(PlayerEntity player) {
        return activeQuests.containsKey(player.getUuid());
    }
    
    /**
     * Обновляет прогресс квестов (вызывается периодически)
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем истечение времени квестов
        activeQuests.entrySet().removeIf(entry -> {
            ActiveQuest activeQuest = entry.getValue();
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
    }
    
    /**
     * Получает все активные квесты
     */
    public Map<UUID, ActiveQuest> getActiveQuests() {
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