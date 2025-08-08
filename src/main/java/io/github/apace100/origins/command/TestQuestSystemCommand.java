package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.QuestAccumulation;
import io.github.apace100.origins.quest.QuestApiManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

/**
 * Команда для тестирования и отладки системы квестов
 */
public class TestQuestSystemCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("testquestsystem")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("stats")
                .executes(TestQuestSystemCommand::showStats))
            .then(CommandManager.literal("check")
                .then(CommandManager.argument("class", StringArgumentType.string())
                    .executes(TestQuestSystemCommand::checkClass)))
            .then(CommandManager.literal("clear")
                .then(CommandManager.argument("class", StringArgumentType.string())
                    .executes(TestQuestSystemCommand::clearClass)))
            .then(CommandManager.literal("force")
                .then(CommandManager.argument("class", StringArgumentType.string())
                    .executes(TestQuestSystemCommand::forceUpdate)))
        );
    }
    
    private static int showStats(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("=== СТАТИСТИКА СИСТЕМЫ КВЕСТОВ ==="), false);
        
        // Статистика накопления
        Map<String, String> stats = QuestAccumulation.getInstance().getAccumulationStats();
        source.sendFeedback(() -> Text.literal("Накопленные квесты:"), false);
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            source.sendFeedback(() -> Text.literal("  " + entry.getKey() + ": " + entry.getValue()), false);
        }
        
        // Классы, нуждающиеся в запросах
        List<String> needingRequests = QuestAccumulation.getInstance().getClassesNeedingApiRequests();
        if (!needingRequests.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Классы, нуждающиеся в новых запросах: " + needingRequests), false);
        } else {
            source.sendFeedback(() -> Text.literal("Все классы имеют достаточно квестов"), false);
        }
        
        // Статус API
        boolean apiAvailable = QuestApiManager.getInstance().isApiAvailable();
        source.sendFeedback(() -> Text.literal("API доступен: " + (apiAvailable ? "ДА" : "НЕТ")), false);
        
        return 1;
    }
    
    private static int checkClass(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerClass = StringArgumentType.getString(context, "class");
        
        QuestAccumulation accumulation = QuestAccumulation.getInstance();
        
        int questCount = accumulation.getAccumulatedQuests(playerClass).size();
        int requestCount = accumulation.getRequestCount(playerClass);
        boolean needsRequest = accumulation.needsNewApiRequest(playerClass);
        
        source.sendFeedback(() -> Text.literal("=== СТАТУС КЛАССА: " + playerClass.toUpperCase() + " ==="), false);
        source.sendFeedback(() -> Text.literal("Накопленных квестов: " + questCount), false);
        source.sendFeedback(() -> Text.literal("Выполнено запросов: " + requestCount + "/" + accumulation.getMaxRequests()), false);
        source.sendFeedback(() -> Text.literal("Нужен новый запрос: " + (needsRequest ? "ДА" : "НЕТ")), false);
        
        if (questCount > 0) {
            source.sendFeedback(() -> Text.literal("Квесты:"), false);
            accumulation.getAccumulatedQuests(playerClass).forEach(quest -> {
                source.sendFeedback(() -> Text.literal("  - " + quest.getTitle() + " (ID: " + quest.getId() + ")"), false);
            });
        }
        
        return 1;
    }
    
    private static int clearClass(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerClass = StringArgumentType.getString(context, "class");
        
        QuestAccumulation.getInstance().clearAccumulatedQuests(playerClass);
        source.sendFeedback(() -> Text.literal("Очищены накопленные квесты для класса: " + playerClass), false);
        
        return 1;
    }
    
    private static int forceUpdate(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerClass = StringArgumentType.getString(context, "class");
        
        if (source.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) source.getWorld();
            QuestApiManager.getInstance().forceUpdateClass(playerClass, serverWorld);
            source.sendFeedback(() -> Text.literal("Принудительно запрошены квесты для класса: " + playerClass), false);
        } else {
            source.sendFeedback(() -> Text.literal("Ошибка: не удалось получить серверный мир"), false);
        }
        
        return 1;
    }
}