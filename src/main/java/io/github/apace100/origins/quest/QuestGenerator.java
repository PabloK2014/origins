package io.github.apace100.origins.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.apace100.origins.Origins;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Загрузчик квестов из JSON файлов для системы досок объявлений.
 * Читает квесты из папки data/origins/quests/
 */
public class QuestGenerator {
    private static final Gson GSON = new Gson();
    private static final Map<String, List<Quest>> loadedQuests = new HashMap<>();
    private static final Random random = new Random();
    
    /**
     * Загружает все квесты из JSON файлов
     */
    public static void loadQuestsFromResources(ResourceManager resourceManager) {
        loadedQuests.clear();
        
        // Список файлов квестов для загрузки
        String[] questFiles = {
            "cook_quests.json",
            "warrior_quests.json", 
            "courier_quests.json",
            "brewer_quests.json",
            "blacksmith_quests.json",
            "miner_quests.json"
        };
        
        for (String fileName : questFiles) {
            loadQuestFile(resourceManager, fileName);
        }
        
        Origins.LOGGER.info("Загружено квестов: " + getTotalQuestCount());
    }
    
    /**
     * Загружает квесты из конкретного JSON файла
     */
    private static void loadQuestFile(ResourceManager resourceManager, String fileName) {
        Identifier questFileId = new Identifier("origins", "quests/" + fileName);
        
        try {
            Optional<Resource> resource = resourceManager.getResource(questFileId);
            if (resource.isEmpty()) {
                Origins.LOGGER.warn("Файл квестов не найден: " + fileName);
                return;
            }
            
            Origins.LOGGER.info("Загружаем файл квестов: " + fileName);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
                
                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                if (jsonObject == null || !jsonObject.has("quests")) {
                    Origins.LOGGER.warn("Неверная структура файла квестов: " + fileName);
                    return;
                }
                
                JsonArray questsArray = jsonObject.getAsJsonArray("quests");
                List<Quest> quests = new ArrayList<>();
                
                for (JsonElement questElement : questsArray) {
                    try {
                        Quest quest = parseQuestFromJson(questElement.getAsJsonObject());
                        if (quest != null && quest.isValid()) {
                            quests.add(quest);
                        }
                    } catch (Exception e) {
                        Origins.LOGGER.error("Ошибка при парсинге квеста из " + fileName + ": " + e.getMessage());
                    }
                }
                
                // Группируем квесты по классам
                for (Quest quest : quests) {
                    String playerClass = normalizePlayerClass(quest.getPlayerClass());
                    loadedQuests.computeIfAbsent(playerClass, k -> new ArrayList<>()).add(quest);
                }
                
                Origins.LOGGER.info("Загружено " + quests.size() + " квестов из " + fileName);
                
            }
        } catch (IOException e) {
            Origins.LOGGER.error("Ошибка при чтении файла квестов " + fileName + ": " + e.getMessage());
        }
    }
    
    /**
     * Парсит квест из JSON объекта
     */
    private static Quest parseQuestFromJson(JsonObject json) {
        try {
            String id = json.get("id").getAsString();
            
            // Поддерживаем как playerClass, так и profession для совместимости
            String playerClass = "human";
            if (json.has("playerClass")) {
                playerClass = json.get("playerClass").getAsString();
            } else if (json.has("profession")) {
                playerClass = "origins:" + json.get("profession").getAsString();
            }
            
            int level = json.has("level") ? json.get("level").getAsInt() : 1;
            String title = json.has("title") ? json.get("title").getAsString() : "Безымянный квест";
            String description = json.has("description") ? json.get("description").getAsString() : "Описание отсутствует";
            int timeLimit = json.has("timeLimit") ? json.get("timeLimit").getAsInt() : 60;
            
            // Парсим цель квеста
            QuestObjective objective = null;
            if (json.has("objective")) {
                JsonObject objJson = json.getAsJsonObject("objective");
                String type = objJson.get("type").getAsString();
                
                // Поддерживаем как target, так и item для совместимости
                String target = "";
                if (objJson.has("target")) {
                    target = objJson.get("target").getAsString();
                } else if (objJson.has("item")) {
                    target = objJson.get("item").getAsString();
                }
                
                int amount = objJson.get("amount").getAsInt();
                
                QuestObjective.ObjectiveType objectiveType = switch (type.toLowerCase()) {
                    case "collect" -> QuestObjective.ObjectiveType.COLLECT;
                    case "kill" -> QuestObjective.ObjectiveType.KILL;
                    case "craft" -> QuestObjective.ObjectiveType.CRAFT;
                    default -> QuestObjective.ObjectiveType.COLLECT;
                };
                
                objective = new QuestObjective(objectiveType, target, amount);
            }
            
            // Парсим награду квеста
            QuestReward reward = null;
            if (json.has("reward")) {
                JsonObject rewardJson = json.getAsJsonObject("reward");
                String type = rewardJson.get("type").getAsString();
                int tier = rewardJson.has("tier") ? rewardJson.get("tier").getAsInt() : 1;
                int experience = rewardJson.has("experience") ? rewardJson.get("experience").getAsInt() : 500;
                
                QuestReward.RewardType rewardType = switch (type.toLowerCase()) {
                    case "skill_point_token" -> QuestReward.RewardType.SKILL_POINT_TOKEN;
                    case "experience" -> QuestReward.RewardType.EXPERIENCE;
                    case "item" -> QuestReward.RewardType.ITEM;
                    default -> QuestReward.RewardType.SKILL_POINT_TOKEN;
                };
                
                reward = new QuestReward(rewardType, tier, experience);
            }
            
            if (objective == null || reward == null) {
                Origins.LOGGER.warn("Квест " + id + " имеет неполные данные");
                return null;
            }
            
            return new Quest(id, playerClass, level, title, description, objective, timeLimit, reward);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при парсинге квеста: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Нормализует название класса игрока
     */
    private static String normalizePlayerClass(String playerClass) {
        if (playerClass.startsWith("origins:")) {
            return playerClass.substring(8); // Убираем префикс "origins:"
        }
        return playerClass;
    }
    
    /**
     * Получает случайные квесты для указанной профессии
     */
    public static List<Quest> getRandomQuestsForProfession(String profession, int count) {
        List<Quest> availableQuests = loadedQuests.get(profession);
        if (availableQuests == null || availableQuests.isEmpty()) {
            Origins.LOGGER.warn("Нет квестов для профессии: " + profession);
            return new ArrayList<>();
        }
        
        List<Quest> result = new ArrayList<>();
        List<Quest> questsCopy = new ArrayList<>(availableQuests);
        Collections.shuffle(questsCopy, random);
        
        for (int i = 0; i < Math.min(count, questsCopy.size()); i++) {
            // Создаем свежую копию квеста с сброшенным прогрессом
            result.add(questsCopy.get(i).createFreshCopy());
        }
        
        return result;
    }
    
    /**
     * Получает квесты определенного уровня для профессии
     */
    public static List<Quest> getQuestsByLevel(String profession, int level, int count) {
        List<Quest> availableQuests = loadedQuests.get(profession);
        if (availableQuests == null || availableQuests.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Quest> levelQuests = availableQuests.stream()
                .filter(quest -> quest.getLevel() == level)
                .toList();
        
        if (levelQuests.isEmpty()) {
            // Если нет квестов точного уровня, берем ближайшие
            levelQuests = availableQuests.stream()
                    .filter(quest -> Math.abs(quest.getLevel() - level) <= 1)
                    .toList();
        }
        
        List<Quest> result = new ArrayList<>(levelQuests);
        Collections.shuffle(result, random);
        
        return result.stream()
                .limit(count)
                .map(Quest::createFreshCopy)
                .toList();
    }
    
    /**
     * Получает все доступные профессии с квестами
     */
    public static Set<String> getAvailableProfessions() {
        return loadedQuests.keySet();
    }
    
    /**
     * Проверяет, есть ли квесты для указанной профессии
     */
    public static boolean hasProfessionQuests(String profession) {
        return loadedQuests.containsKey(profession) && !loadedQuests.get(profession).isEmpty();
    }
    
    /**
     * Получает общее количество загруженных квестов
     */
    public static int getTotalQuestCount() {
        return loadedQuests.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Получает количество квестов для конкретной профессии
     */
    public static int getQuestCountForProfession(String profession) {
        List<Quest> quests = loadedQuests.get(profession);
        return quests != null ? quests.size() : 0;
    }
    
    /**
     * Очищает загруженные квесты (для перезагрузки)
     */
    public static void clearLoadedQuests() {
        loadedQuests.clear();
    }
    
    /**
     * Получает квест по ID
     */
    public static Quest getQuestById(String questId) {
        for (List<Quest> quests : loadedQuests.values()) {
            for (Quest quest : quests) {
                if (quest.getId().equals(questId)) {
                    return quest.createFreshCopy();
                }
            }
        }
        return null;
    }
    
    /**
     * Получает все квесты для профессии
     */
    public static List<Quest> getAllQuestsForProfession(String profession) {
        List<Quest> quests = loadedQuests.get(profession);
        if (quests == null) {
            return new ArrayList<>();
        }
        
        return quests.stream()
                .map(Quest::createFreshCopy)
                .toList();
    }
    
    /**
     * Генерирует случайный квест для указанной профессии и уровня (для совместимости)
     */
    public static Quest generateRandomQuest(String profession, int level) {
        List<Quest> levelQuests = getQuestsByLevel(profession, level, 1);
        return levelQuests.isEmpty() ? null : levelQuests.get(0);
    }
}