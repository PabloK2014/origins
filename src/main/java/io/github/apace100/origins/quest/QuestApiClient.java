package io.github.apace100.origins.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.apace100.origins.Origins;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Клиент для работы с FastAPI сервером генерации квестов
 */
public class QuestApiClient {
    private static final String API_BASE_URL = "http://localhost:8000";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(600)) // Увеличен таймаут до 10 минут
            .build();
    private static final Gson gson = new Gson();
    
    // Кэш для всех квестов
    private static Map<String, List<Quest>> allQuestsCache = new HashMap<>();
    private static long lastAllQuestsFetch = 0;
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30 минут в миллисекундах
    
    // Флаг для предотвращения множественных запросов
    private static volatile boolean isLoadingAllQuests = false;

    /**
     * Получает все квесты для всех классов одним запросом (ОПТИМИЗИРОВАННАЯ ВЕРСИЯ)
     */
    public static CompletableFuture<Map<String, List<Quest>>> getAllQuests() {
        return CompletableFuture.supplyAsync(() -> {
            if (isLoadingAllQuests) {
                Origins.LOGGER.info("⏳ Already loading all quests, waiting...");
                while (isLoadingAllQuests) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return new HashMap<>();
                    }
                }
                return allQuestsCache;
            }
            
            isLoadingAllQuests = true;
            
            try {
                String url = API_BASE_URL + "/quests/all";
                Origins.LOGGER.info("🚀 OPTIMIZED API REQUEST: " + url);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(600)) // Таймаут 10 минут
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                Origins.LOGGER.info("📡 API RESPONSE: Status " + response.statusCode());
                
                if (response.statusCode() == 200) {
                    Map<String, List<Quest>> allQuests = parseAllQuestsFromJson(response.body());
                    Origins.LOGGER.info("✅ API SUCCESS: Получено квестов для всех классов!");
                    
                    allQuestsCache = allQuests;
                    lastAllQuestsFetch = System.currentTimeMillis();
                    
                    return allQuests;
                } else {
                    Origins.LOGGER.error("❌ API ERROR: Status " + response.statusCode());
                    Origins.LOGGER.error("Response body: " + response.body());
                    return new HashMap<>();
                }
                
            } catch (IOException | InterruptedException e) {
                Origins.LOGGER.error("🔥 API EXCEPTION: Failed to fetch all quests: " + e.getMessage());
                return new HashMap<>();
            } finally {
                isLoadingAllQuests = false;
            }
        });
    }

    /**
     * Получает квесты для указанного класса (использует кэш или новый API)
     */
    public static CompletableFuture<List<Quest>> getQuestsForClass(String playerClass, int questCount) {
        return CompletableFuture.supplyAsync(() -> {
            if (allQuestsCache.containsKey(playerClass) && 
                System.currentTimeMillis() - lastAllQuestsFetch < CACHE_DURATION) {
                Origins.LOGGER.info("📦 CACHE HIT: Используем кэшированные квесты для " + playerClass);
                return allQuestsCache.get(playerClass);
            }
            
            Origins.LOGGER.info("🔄 CACHE MISS: Загружаем все квесты через оптимизированный API");
            try {
                Map<String, List<Quest>> allQuests = getAllQuests().get();
                return allQuests.getOrDefault(playerClass, new ArrayList<>());
            } catch (Exception e) {
                Origins.LOGGER.error("❌ Failed to get quests for class " + playerClass + ": " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Парсит JSON ответ с квестами для всех классов
     */
    private static Map<String, List<Quest>> parseAllQuestsFromJson(String jsonResponse) {
        Map<String, List<Quest>> allQuests = new HashMap<>();
        String[] classes = {"cook", "courier", "brewer", "blacksmith", "miner", "warrior"};
        
        Origins.LOGGER.info("🔍 [QuestApiClient] Начинаем парсинг JSON ответа длиной: " + jsonResponse.length() + " символов");
        
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            Origins.LOGGER.info("✅ [QuestApiClient] JSON успешно распарсен в объект");
            
            for (String className : classes) {
                List<Quest> classQuests = new ArrayList<>();
                Origins.LOGGER.info("🔍 [QuestApiClient] Обрабатываем класс: " + className);
                
                if (responseObj.has(className)) {
                    JsonArray questsArray = responseObj.getAsJsonArray(className);
                    Origins.LOGGER.info("📋 [QuestApiClient] Найден массив квестов для " + className + " размером: " + questsArray.size());
                    
                    for (int i = 0; i < questsArray.size(); i++) {
                        JsonObject questObj = questsArray.get(i).getAsJsonObject();
                        Origins.LOGGER.info("🔍 [QuestApiClient] Парсим квест " + (i+1) + " для " + className);
                        
                        Quest quest = parseQuestFromJsonObject(questObj);
                        if (quest != null) {
                            classQuests.add(quest);
                            Origins.LOGGER.info("✅ [QuestApiClient] Квест успешно создан: " + quest.getTitle());
                        } else {
                            Origins.LOGGER.warn("❌ [QuestApiClient] Не удалось создать квест " + (i+1) + " для " + className);
                        }
                    }
                } else {
                    Origins.LOGGER.warn("❌ [QuestApiClient] Класс " + className + " не найден в JSON ответе");
                }
                
                allQuests.put(className, classQuests);
                Origins.LOGGER.info("📊 [QuestApiClient] Итого для " + className + ": " + classQuests.size() + " квестов");
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("🔥 [QuestApiClient] Критическая ошибка при парсинге JSON", e);
            Origins.LOGGER.error("🔥 [QuestApiClient] JSON содержимое: " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
        }
        
        int totalQuests = allQuests.values().stream().mapToInt(List::size).sum();
        Origins.LOGGER.info("🎯 [QuestApiClient] ИТОГО РАСПАРСЕНО: " + totalQuests + " квестов");
        
        return allQuests;
    }

    /**
     * Парсит JSON ответ от API в список квестов (старый метод для совместимости)
     */
    private static List<Quest> parseQuestsFromJson(String jsonResponse) {
        List<Quest> quests = new ArrayList<>();
        
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray questsArray = responseObj.getAsJsonArray("quests");
            
            for (int i = 0; i < questsArray.size(); i++) {
                JsonObject questObj = questsArray.get(i).getAsJsonObject();
                Quest quest = parseQuestFromJsonObject(questObj);
                if (quest != null) {
                    quests.add(quest);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to parse quests from JSON response", e);
        }
        
        return quests;
    }
    
    /**
     * Парсит отдельный квест из JSON объекта
     */
    private static Quest parseQuestFromJsonObject(JsonObject questObj) {
        try {
            String id = questObj.get("id").getAsString();
            String playerClass = questObj.get("playerClass").getAsString();
            int level = questObj.get("level").getAsInt();
            String title = questObj.get("title").getAsString();
            String description = ""; // Убираем парсинг description из API
            int timeLimit = questObj.get("timeLimit").getAsInt();
            
            JsonObject objectiveObj = questObj.getAsJsonObject("objective");
            String objectiveType = objectiveObj.get("type").getAsString();
            String target = objectiveObj.get("target").getAsString();
            int amount = objectiveObj.get("amount").getAsInt();
            
            QuestObjective.ObjectiveType objType = parseObjectiveType(objectiveType);
            QuestObjective objective = new QuestObjective(objType, target, amount);
            
            JsonObject rewardObj = questObj.getAsJsonObject("reward");
            String rewardType = rewardObj.get("type").getAsString();
            int tier = rewardObj.get("tier").getAsInt();
            int experience = rewardObj.get("experience").getAsInt();
            
            QuestReward.RewardType rewType = parseRewardType(rewardType);
            QuestReward reward = new QuestReward(rewType, tier, experience);
            
            Quest quest = new Quest(id, playerClass, level, title, description, objective, timeLimit, reward);
            Origins.LOGGER.info("✅ [QuestApiClient] Quest создан: " + quest.getTitle() + " (ID: " + quest.getId() + ")");
            
            return quest;
            
        } catch (Exception e) {
            Origins.LOGGER.error("🔥 [QuestApiClient] Ошибка при парсинге квеста из JSON", e);
            Origins.LOGGER.error("🔥 [QuestApiClient] Проблемный JSON: " + questObj.toString());
            return null;
        }
    }
    
    /**
     * Конвертирует строку типа цели в enum
     */
    private static QuestObjective.ObjectiveType parseObjectiveType(String type) {
        switch (type.toLowerCase()) {
            case "collect":
                return QuestObjective.ObjectiveType.COLLECT;
            case "craft":
                return QuestObjective.ObjectiveType.CRAFT;
            case "kill":
                return QuestObjective.ObjectiveType.KILL;
            case "mine":
                return QuestObjective.ObjectiveType.COLLECT;
            default:
                return QuestObjective.ObjectiveType.COLLECT;
        }
    }
    
    /**
     * Конвертирует строку типа награды в enum
     */
    private static QuestReward.RewardType parseRewardType(String type) {
        switch (type.toLowerCase()) {
            case "skill_point_token":
                return QuestReward.RewardType.SKILL_POINT_TOKEN;
            case "experience":
                return QuestReward.RewardType.EXPERIENCE;
            case "item":
                return QuestReward.RewardType.ITEM;
            default:
                return QuestReward.RewardType.SKILL_POINT_TOKEN;
        }
    }
    
    /**
     * Проверяет доступность API
     */
    public static CompletableFuture<Boolean> isApiAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                boolean isAvailable = response.statusCode() == 200;
                Origins.LOGGER.info("🔍 API Health Check: " + (isAvailable ? "✅ AVAILABLE" : "❌ UNAVAILABLE") + " (Status: " + response.statusCode() + ")");
                
                return isAvailable;
                
            } catch (Exception e) {
                Origins.LOGGER.warn("🔍 API Health Check: ❌ EXCEPTION - " + e.getMessage());
                return false;
            }
        });
    }
}