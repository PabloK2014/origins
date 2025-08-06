package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс для управления накоплением квестов в досках объявлений
 */
public class QuestAccumulation {
    private static final QuestAccumulation INSTANCE = new QuestAccumulation();
    
    // Максимальное количество запросов перед очисткой доски
    private static final int MAX_REQUESTS = 3;
    
    // Количество квестов за один запрос
    private static final int QUESTS_PER_REQUEST = 5;
    
    // Максимальное количество квестов в доске (3 запроса * 5 квестов = 15)
    private static final int MAX_QUESTS_IN_BOARD = MAX_REQUESTS * QUESTS_PER_REQUEST;
    
    // Накопленные квесты для каждого класса
    private final Map<String, List<Quest>> accumulatedQuests = new ConcurrentHashMap<>();
    
    // Счетчик запросов для каждого класса
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    
    private QuestAccumulation() {}
    
    public static QuestAccumulation getInstance() {
        return INSTANCE;
    }
    
    /**
     * Добавляет новые квесты к накопленным для указанного класса
     */
    public List<Quest> addQuestsForClass(String playerClass, List<Quest> newQuests) {
        return addQuestsForClass(playerClass, newQuests, null);
    }
    
    /**
     * Добавляет новые квесты к накопленным для указанного класса с уведомлениями
     */
    public List<Quest> addQuestsForClass(String playerClass, List<Quest> newQuests, net.minecraft.server.MinecraftServer server) {
        if (newQuests == null || newQuests.isEmpty()) {
            Origins.LOGGER.warn("🔄 [QuestAccumulation] Попытка добавить пустой список квестов для класса: " + playerClass);
            return getAccumulatedQuests(playerClass);
        }
        
        Origins.LOGGER.info("🔄 [QuestAccumulation] Добавляем " + newQuests.size() + " новых квестов для класса: " + playerClass);
        
        // Получаем текущие накопленные квесты
        List<Quest> accumulated = accumulatedQuests.computeIfAbsent(playerClass, k -> new ArrayList<>());
        
        // Увеличиваем счетчик запросов
        int currentRequestCount = requestCounts.getOrDefault(playerClass, 0) + 1;
        
        Origins.LOGGER.info("🔢 [QuestAccumulation] Запрос #" + currentRequestCount + " для класса: " + playerClass);
        
        // Проверяем, нужно ли очистить доску (после 3 запросов)
        boolean boardCleared = false;
        if (currentRequestCount > MAX_REQUESTS) {
            Origins.LOGGER.info("🗑️ [QuestAccumulation] Очищаем доску для класса " + playerClass + " после " + MAX_REQUESTS + " запросов");
            accumulated.clear();
            currentRequestCount = 1;
            boardCleared = true;
            
            // Уведомляем об очистке доски
            Origins.LOGGER.info("🔄 [QuestAccumulation] Доска очищена, начинаем новый цикл для класса: " + playerClass);
            if (server != null) {
                QuestApiChatLogger.logBoardCleared(server, playerClass);
            }
        }
        
        // Добавляем новые квесты
        accumulated.addAll(newQuests);
        
        // Обновляем счетчики
        requestCounts.put(playerClass, currentRequestCount);
        accumulatedQuests.put(playerClass, accumulated);
        
        Origins.LOGGER.info("📊 [QuestAccumulation] Итого квестов для " + playerClass + ": " + accumulated.size() + 
            " (запрос " + currentRequestCount + "/" + MAX_REQUESTS + ")");
        
        // Отправляем уведомление о накоплении квестов
        if (server != null) {
            QuestApiChatLogger.logQuestAccumulation(server, playerClass, newQuests.size(), accumulated.size(), currentRequestCount, MAX_REQUESTS);
        }
        
        // Логируем детали накопленных квестов
        for (int i = 0; i < accumulated.size(); i++) {
            Quest quest = accumulated.get(i);
            Origins.LOGGER.info("  " + (i+1) + ". " + quest.getTitle() + " (ID: " + quest.getId() + ")");
        }
        
        return new ArrayList<>(accumulated);
    }
    
    /**
     * Получает все накопленные квесты для указанного класса
     */
    public List<Quest> getAccumulatedQuests(String playerClass) {
        List<Quest> accumulated = accumulatedQuests.getOrDefault(playerClass, new ArrayList<>());
        return new ArrayList<>(accumulated);
    }
    
    /**
     * Получает количество запросов для указанного класса
     */
    public int getRequestCount(String playerClass) {
        return requestCounts.getOrDefault(playerClass, 0);
    }
    
    /**
     * Получает максимальное количество запросов перед очисткой
     */
    public int getMaxRequests() {
        return MAX_REQUESTS;
    }
    
    /**
     * Проверяет, нужно ли очистить доску при следующем запросе
     */
    public boolean shouldClearOnNextRequest(String playerClass) {
        return getRequestCount(playerClass) >= MAX_REQUESTS;
    }
    
    /**
     * Принудительно очищает накопленные квесты для указанного класса
     */
    public void clearAccumulatedQuests(String playerClass) {
        Origins.LOGGER.info("🗑️ [QuestAccumulation] Принудительная очистка квестов для класса: " + playerClass);
        accumulatedQuests.remove(playerClass);
        requestCounts.remove(playerClass);
    }
    
    /**
     * Очищает все накопленные квесты
     */
    public void clearAllAccumulatedQuests() {
        Origins.LOGGER.info("🗑️ [QuestAccumulation] Очистка всех накопленных квестов");
        accumulatedQuests.clear();
        requestCounts.clear();
    }
    
    /**
     * Удаляет конкретный квест из накопления
     */
    public boolean removeQuest(String playerClass, String questId) {
        List<Quest> accumulated = accumulatedQuests.get(playerClass);
        if (accumulated == null) {
            Origins.LOGGER.warn("🔄 [QuestAccumulation] Нет накопленных квестов для класса: " + playerClass);
            return false;
        }
        
        Origins.LOGGER.info("🔍 [QuestAccumulation] Попытка удалить квест " + questId + " из класса " + playerClass);
        Origins.LOGGER.info("📋 [QuestAccumulation] Квестов до удаления: " + accumulated.size());
        
        // Логируем все квесты перед удалением
        for (int i = 0; i < accumulated.size(); i++) {
            Quest quest = accumulated.get(i);
            Origins.LOGGER.info("  " + (i+1) + ". " + quest.getTitle() + " (ID: " + quest.getId() + ")");
        }
        
        boolean removed = accumulated.removeIf(quest -> quest.getId().equals(questId));
        
        if (removed) {
            Origins.LOGGER.info("✅ [QuestAccumulation] Удален квест " + questId + " из накопления класса " + playerClass);
            Origins.LOGGER.info("📊 [QuestAccumulation] Осталось квестов для " + playerClass + ": " + accumulated.size());
            
            // Логируем оставшиеся квесты
            for (int i = 0; i < accumulated.size(); i++) {
                Quest quest = accumulated.get(i);
                Origins.LOGGER.info("  Остался " + (i+1) + ". " + quest.getTitle() + " (ID: " + quest.getId() + ")");
            }
        } else {
            Origins.LOGGER.warn("❌ [QuestAccumulation] Квест " + questId + " не найден в накоплении класса " + playerClass);
            Origins.LOGGER.warn("📋 [QuestAccumulation] Доступные квесты:");
            for (int i = 0; i < accumulated.size(); i++) {
                Quest quest = accumulated.get(i);
                Origins.LOGGER.warn("  " + (i+1) + ". " + quest.getTitle() + " (ID: " + quest.getId() + ")");
            }
        }
        
        return removed;
    }
    
    /**
     * Получает статистику накопления квестов
     */
    public Map<String, String> getAccumulationStats() {
        Map<String, String> stats = new HashMap<>();
        
        for (String playerClass : Arrays.asList("cook", "courier", "brewer", "blacksmith", "miner", "warrior")) {
            int questCount = getAccumulatedQuests(playerClass).size();
            int requestCount = getRequestCount(playerClass);
            
            stats.put(playerClass, questCount + " квестов (запрос " + requestCount + "/" + MAX_REQUESTS + ")");
        }
        
        return stats;
    }
}