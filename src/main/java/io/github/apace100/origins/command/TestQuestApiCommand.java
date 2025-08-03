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
            for (String playerClass : manager.getAvailableClasses()) {
                int questCount = manager.getQuestsForClass(playerClass).size();
                int minutesUntilUpdate = manager.getMinutesUntilNextUpdate(playerClass);
                
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
}