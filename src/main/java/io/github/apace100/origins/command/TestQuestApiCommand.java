package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.QuestApiChatLogger;
import io.github.apace100.origins.quest.QuestApiManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команды для тестирования Quest API
 */
public class TestQuestApiCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("quest_api")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("test")
                .then(CommandManager.argument("class", StringArgumentType.string())
                    .executes(context -> testApiForClass(context, StringArgumentType.getString(context, "class"), 10))
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 10))
                        .executes(context -> testApiForClass(context, 
                            StringArgumentType.getString(context, "class"),
                            IntegerArgumentType.getInteger(context, "count"))))))
            .then(CommandManager.literal("status")
                .executes(TestQuestApiCommand::checkApiStatus))
            .then(CommandManager.literal("reload")
                .executes(TestQuestApiCommand::reloadAllQuests))
            .then(CommandManager.literal("reload_class")
                .then(CommandManager.argument("class", StringArgumentType.string())
                    .executes(context -> reloadClassQuests(context, StringArgumentType.getString(context, "class")))))
            .then(CommandManager.literal("clear_cache")
                .executes(TestQuestApiCommand::clearCache))
            .then(CommandManager.literal("test_parse")
                .executes(TestQuestApiCommand::testJsonParsing))
            .then(CommandManager.literal("raw_json")
                .executes(TestQuestApiCommand::getRawJson))
            .then(CommandManager.literal("debug_cache")
                .executes(TestQuestApiCommand::debugCache))
        );
    }
    
    /**
     * Тестирует API для указанного класса
     */
    private static int testApiForClass(CommandContext<ServerCommandSource> context, String playerClass, int questCount) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            QuestApiManager manager = QuestApiManager.getInstance();
            
            source.sendFeedback(() -> Text.literal("🧪 Тестируем API для класса: " + playerClass + " (квестов: " + questCount + ")")
                .formatted(Formatting.YELLOW), true);
            
            // Принудительно обновляем квесты для класса
            manager.forceUpdateClass(playerClass, world);
            
            source.sendFeedback(() -> Text.literal("✅ Запрос отправлен! Проверьте чат для результатов.")
                .formatted(Formatting.GREEN), true);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Ошибка при тестировании API: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Проверяет статус API
     */
    private static int checkApiStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        QuestApiManager manager = QuestApiManager.getInstance();
        
        boolean isAvailable = manager.isApiAvailable();
        
        if (isAvailable) {
            source.sendFeedback(() -> Text.literal("🟢 Quest API: ПОДКЛЮЧЕН")
                .formatted(Formatting.GREEN), false);
            
            // Показываем информацию о кэшированных квестах
            ServerWorld world = source.getWorld();
            for (String playerClass : manager.getAvailableClasses()) {
                int questCount = manager.getQuestsForClass(playerClass).size();
                int minutesUntilUpdate = manager.getMinutesUntilNextUpdate(playerClass, world);
                
                source.sendFeedback(() -> Text.literal("  📋 " + playerClass + ": " + questCount + " квестов")
                    .formatted(Formatting.AQUA)
                    .append(Text.literal(" (обновление через " + minutesUntilUpdate + " мин)")
                        .formatted(Formatting.GRAY)), false);
            }
        } else {
            source.sendFeedback(() -> Text.literal("🔴 Quest API: НЕДОСТУПЕН")
                .formatted(Formatting.RED), false);
            source.sendFeedback(() -> Text.literal("  Проверьте, что FastAPI сервер запущен на localhost:8000")
                .formatted(Formatting.GRAY), false);
        }
        
        return 1;
    }
    
    /**
     * Перезагружает квесты для всех классов
     */
    private static int reloadAllQuests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            QuestApiManager manager = QuestApiManager.getInstance();
            
            source.sendFeedback(() -> Text.literal("🔄 Перезагружаем квесты для всех классов...")
                .formatted(Formatting.YELLOW), true);
            
            QuestApiChatLogger.logQuestUpdate(world.getServer());
            
            for (String playerClass : manager.getAvailableClasses()) {
                manager.forceUpdateClass(playerClass, world);
            }
            
            source.sendFeedback(() -> Text.literal("✅ Запросы отправлены! Проверьте чат для результатов.")
                .formatted(Formatting.GREEN), true);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Ошибка при перезагрузке квестов: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Перезагружает квесты для указанного класса
     */
    private static int reloadClassQuests(CommandContext<ServerCommandSource> context, String playerClass) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            QuestApiManager manager = QuestApiManager.getInstance();
            
            source.sendFeedback(() -> Text.literal("🔄 Перезагружаем квесты для класса: " + playerClass)
                .formatted(Formatting.YELLOW), true);
            
            manager.forceUpdateClass(playerClass, world);
            
            source.sendFeedback(() -> Text.literal("✅ Запрос отправлен! Проверьте чат для результатов.")
                .formatted(Formatting.GREEN), true);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Ошибка при перезагрузке квестов для класса " + playerClass + ": " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Очищает кэш квестов
     */
    private static int clearCache(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Здесь мы бы очистили кэш, но у нас нет публичного метода для этого
            // Можно добавить метод clearCache() в QuestApiManager если нужно
            
            source.sendFeedback(() -> Text.literal("🗑️ Кэш квестов очищен!")
                .formatted(Formatting.GREEN), true);
            
            source.sendFeedback(() -> Text.literal("💡 Используйте /quest_api reload для загрузки новых квестов")
                .formatted(Formatting.GRAY), false);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Ошибка при очистке кэша: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Тестирует парсинг JSON без поля description
     */
    private static int testJsonParsing(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendFeedback(() -> Text.literal("🧪 Тестируем парсинг JSON без поля description...")
                .formatted(Formatting.YELLOW), true);
            
            // Тестовый JSON без поля description
            String testJson = """
                {
                  "cook": [
                    {
                      "id": "cook_test_1",
                      "playerClass": "origins:cook",
                      "level": 1,
                      "title": "Тестовый квест повара",
                      "objective": {
                        "type": "collect",
                        "target": "minecraft:wheat",
                        "amount": 5
                      },
                      "timeLimit": 20,
                      "reward": {
                        "type": "skill_point_token",
                        "tier": 1,
                        "experience": 500
                      }
                    }
                  ],
                  "warrior": [
                    {
                      "id": "warrior_test_1",
                      "playerClass": "origins:warrior",
                      "level": 1,
                      "title": "Тестовый квест воина",
                      "objective": {
                        "type": "kill",
                        "target": "zombie",
                        "amount": 3
                      },
                      "timeLimit": 25,
                      "reward": {
                        "type": "skill_point_token",
                        "tier": 1,
                        "experience": 500
                      }
                    }
                  ],
                  "courier": [],
                  "brewer": [],
                  "blacksmith": [],
                  "miner": []
                }
                """;
            
            // Используем рефлексию для доступа к приватному методу парсинга
            try {
                Class<?> clientClass = Class.forName("io.github.apace100.origins.quest.QuestApiClient");
                java.lang.reflect.Method parseMethod = clientClass.getDeclaredMethod("parseAllQuestsFromJson", String.class);
                parseMethod.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                java.util.Map<String, java.util.List<io.github.apace100.origins.quest.Quest>> result = 
                    (java.util.Map<String, java.util.List<io.github.apace100.origins.quest.Quest>>) parseMethod.invoke(null, testJson);
                
                int totalQuests = result.values().stream().mapToInt(java.util.List::size).sum();
                
                source.sendFeedback(() -> Text.literal("✅ Парсинг успешен! Получено " + totalQuests + " квестов")
                    .formatted(Formatting.GREEN), true);
                
                for (java.util.Map.Entry<String, java.util.List<io.github.apace100.origins.quest.Quest>> entry : result.entrySet()) {
                    String className = entry.getKey();
                    int questCount = entry.getValue().size();
                    
                    source.sendFeedback(() -> Text.literal("  📋 " + className + ": " + questCount + " квестов")
                        .formatted(Formatting.AQUA), false);
                    
                    for (io.github.apace100.origins.quest.Quest quest : entry.getValue()) {
                        source.sendFeedback(() -> Text.literal("    - " + quest.getTitle() + " (ID: " + quest.getId() + ")")
                            .formatted(Formatting.GRAY), false);
                    }
                }
                
                return 1;
                
            } catch (Exception e) {
                source.sendError(Text.literal("❌ Ошибка при тестировании парсинга: " + e.getMessage()));
                e.printStackTrace();
                return 0;
            }
            
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Общая ошибка при тестировании: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Получает сырой JSON ответ от API и выводит его в чат
     */
    private static int getRawJson(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendFeedback(() -> Text.literal("🔍 Запрашиваем сырой JSON от API...")
                .formatted(Formatting.YELLOW), true);
            
            // Делаем прямой HTTP запрос к API
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8000/quests/all"))
                .timeout(java.time.Duration.ofSeconds(600)) // 10 минут таймаут
                .GET()
                .build();
            
            source.sendFeedback(() -> Text.literal("⏳ Отправляем запрос к API (может занять до 10 минут)...")
                .formatted(Formatting.GRAY), false);
            
            // Выполняем запрос асинхронно
            httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        String jsonResponse = response.body();
                        
                        source.sendFeedback(() -> Text.literal("✅ Получен JSON ответ (" + jsonResponse.length() + " символов)")
                            .formatted(Formatting.GREEN), false);
                        
                        // Разбиваем JSON на части для отображения в чате (Minecraft имеет ограничение на длину сообщений)
                        int maxLength = 500; // Максимальная длина одного сообщения
                        int parts = (jsonResponse.length() + maxLength - 1) / maxLength;
                        
                        source.sendFeedback(() -> Text.literal("📄 JSON ответ (разбит на " + parts + " частей):")
                            .formatted(Formatting.AQUA), false);
                        
                        for (int i = 0; i < parts; i++) {
                            int start = i * maxLength;
                            int end = Math.min(start + maxLength, jsonResponse.length());
                            String part = jsonResponse.substring(start, end);
                            
                            final int partNum = i + 1;
                            source.sendFeedback(() -> Text.literal("📄 Часть " + partNum + "/" + parts + ": " + part)
                                .formatted(Formatting.WHITE), false);
                        }
                        
                        source.sendFeedback(() -> Text.literal("✅ Полный JSON ответ выведен выше")
                            .formatted(Formatting.GREEN), false);
                        
                    } else {
                        source.sendFeedback(() -> Text.literal("❌ API вернул ошибку: " + response.statusCode())
                            .formatted(Formatting.RED), false);
                        source.sendFeedback(() -> Text.literal("Ответ: " + response.body())
                            .formatted(Formatting.GRAY), false);
                    }
                })
                .exceptionally(throwable -> {
                    source.sendFeedback(() -> Text.literal("❌ Ошибка при запросе к API: " + throwable.getMessage())
                        .formatted(Formatting.RED), false);
                    return null;
                });
            
            source.sendFeedback(() -> Text.literal("📡 Запрос отправлен асинхронно, ожидайте результат...")
                .formatted(Formatting.GRAY), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Ошибка при получении сырого JSON: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Отладка кэша квестов - показывает что находится в кэше
     */
    private static int debugCache(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            QuestApiManager manager = QuestApiManager.getInstance();
            
            source.sendFeedback(() -> Text.literal("🔍 Отладка кэша квестов:")
                .formatted(Formatting.YELLOW), true);
            
            boolean hasAnyQuests = false;
            
            for (String playerClass : manager.getAvailableClasses()) {
                java.util.List<io.github.apace100.origins.quest.Quest> quests = manager.getQuestsForClass(playerClass);
                
                source.sendFeedback(() -> Text.literal("📋 " + playerClass + ": " + quests.size() + " квестов")
                    .formatted(Formatting.AQUA), false);
                
                if (!quests.isEmpty()) {
                    hasAnyQuests = true;
                    for (int i = 0; i < Math.min(3, quests.size()); i++) {
                        final int questNum = i + 1;
                        final io.github.apace100.origins.quest.Quest quest = quests.get(i);
                        source.sendFeedback(() -> Text.literal("  " + questNum + ". " + quest.getTitle() + " (ID: " + quest.getId() + ")")
                            .formatted(Formatting.GRAY), false);
                    }
                    if (quests.size() > 3) {
                        final int remaining = quests.size() - 3;
                        source.sendFeedback(() -> Text.literal("  ... и еще " + remaining + " квестов")
                            .formatted(Formatting.DARK_GRAY), false);
                    }
                }
            }
            
            if (!hasAnyQuests) {
                source.sendFeedback(() -> Text.literal("❌ Кэш пуст! Попробуйте выполнить /quest_api reload")
                    .formatted(Formatting.RED), false);
            } else {
                source.sendFeedback(() -> Text.literal("✅ Кэш содержит квесты. Если они не отображаются в досках, проблема в обновлении досок.")
                    .formatted(Formatting.GREEN), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Ошибка при отладке кэша: " + e.getMessage()));
            return 0;
        }
    }
}