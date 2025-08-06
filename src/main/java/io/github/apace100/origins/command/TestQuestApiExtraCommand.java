package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.QuestApiClient;
import io.github.apace100.origins.quest.QuestApiManager;
import io.github.apace100.origins.quest.QuestAccumulation;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команда для тестирования дополнительных запросов к Quest API
 */
public class TestQuestApiExtraCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_quest_api_extra")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestQuestApiExtraCommand::executeAll)
            .then(CommandManager.argument("class", StringArgumentType.string())
                .suggests((context, builder) -> {
                    builder.suggest("cook");
                    builder.suggest("courier");
                    builder.suggest("brewer");
                    builder.suggest("blacksmith");
                    builder.suggest("miner");
                    builder.suggest("warrior");
                    return builder.buildFuture();
                })
                .executes(TestQuestApiExtraCommand::executeClass)
            )
        );
    }
    
    /**
     * Выполняет дополнительный запрос для всех классов
     */
    private static int executeAll(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        source.sendMessage(Text.literal("🧪 [TEST] Запускаем дополнительный запрос для всех классов...")
            .formatted(Formatting.YELLOW));
        
        // Показываем текущую статистику накопления
        showAccumulationStats(source);
        
        // Принудительно запускаем дополнительный запрос
        QuestApiManager manager = QuestApiManager.getInstance();
        manager.forceUpdateClass("all", world);
        
        source.sendMessage(Text.literal("✅ [TEST] Дополнительный запрос отправлен!")
            .formatted(Formatting.GREEN));
        
        return 1;
    }
    
    /**
     * Выполняет дополнительный запрос для конкретного класса
     */
    private static int executeClass(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        String playerClass = StringArgumentType.getString(context, "class");
        
        // Проверяем валидность класса
        String[] validClasses = {"cook", "courier", "brewer", "blacksmith", "miner", "warrior"};
        boolean isValidClass = false;
        for (String validClass : validClasses) {
            if (validClass.equals(playerClass)) {
                isValidClass = true;
                break;
            }
        }
        
        if (!isValidClass) {
            source.sendError(Text.literal("❌ Неверный класс! Доступные: cook, courier, brewer, blacksmith, miner, warrior"));
            return 0;
        }
        
        source.sendMessage(Text.literal("🧪 [TEST] Запускаем дополнительный запрос для класса: " + playerClass)
            .formatted(Formatting.YELLOW));
        
        // Показываем текущую статистику для класса
        showClassStats(source, playerClass);
        
        // Принудительно запускаем дополнительный запрос для класса
        QuestApiManager manager = QuestApiManager.getInstance();
        manager.forceUpdateClass(playerClass, world);
        
        source.sendMessage(Text.literal("✅ [TEST] Дополнительный запрос для " + playerClass + " отправлен!")
            .formatted(Formatting.GREEN));
        
        return 1;
    }
    
    /**
     * Показывает статистику накопления квестов для всех классов
     */
    private static void showAccumulationStats(ServerCommandSource source) {
        source.sendMessage(Text.literal("📊 [STATS] Текущая статистика накопления квестов:")
            .formatted(Formatting.AQUA));
        
        QuestAccumulation accumulation = QuestAccumulation.getInstance();
        String[] classes = {"cook", "courier", "brewer", "blacksmith", "miner", "warrior"};
        
        for (String playerClass : classes) {
            int questCount = accumulation.getAccumulatedQuests(playerClass).size();
            int requestCount = accumulation.getRequestCount(playerClass);
            int maxRequests = accumulation.getMaxRequests();
            
            String className = getClassDisplayName(playerClass);
            
            source.sendMessage(Text.literal("  • " + className + ": " + questCount + " квестов (запрос " + requestCount + "/" + maxRequests + ")")
                .formatted(questCount > 0 ? Formatting.GREEN : Formatting.GRAY));
        }
    }
    
    /**
     * Показывает статистику для конкретного класса
     */
    private static void showClassStats(ServerCommandSource source, String playerClass) {
        QuestAccumulation accumulation = QuestAccumulation.getInstance();
        
        int questCount = accumulation.getAccumulatedQuests(playerClass).size();
        int requestCount = accumulation.getRequestCount(playerClass);
        int maxRequests = accumulation.getMaxRequests();
        boolean shouldClear = accumulation.shouldClearOnNextRequest(playerClass);
        
        String className = getClassDisplayName(playerClass);
        
        source.sendMessage(Text.literal("📊 [STATS] Статистика для " + className + ":")
            .formatted(Formatting.AQUA));
        
        source.sendMessage(Text.literal("  Накопленных квестов: " + questCount)
            .formatted(Formatting.WHITE));
        
        source.sendMessage(Text.literal("  Запросов выполнено: " + requestCount + "/" + maxRequests)
            .formatted(Formatting.WHITE));
        
        if (shouldClear) {
            source.sendMessage(Text.literal("  ⚠️ Доска будет очищена при следующем запросе!")
                .formatted(Formatting.YELLOW));
        } else {
            source.sendMessage(Text.literal("  ✅ Квесты будут добавлены к существующим")
                .formatted(Formatting.GREEN));
        }
        
        // Показываем детали накопленных квестов
        if (questCount > 0) {
            source.sendMessage(Text.literal("  Накопленные квесты:")
                .formatted(Formatting.GRAY));
            
            accumulation.getAccumulatedQuests(playerClass).forEach(quest -> {
                source.sendMessage(Text.literal("    - " + quest.getTitle() + " (ID: " + quest.getId() + ")")
                    .formatted(Formatting.DARK_GRAY));
            });
        }
    }
    
    /**
     * Получает отображаемое название класса
     */
    private static String getClassDisplayName(String className) {
        switch (className.toLowerCase()) {
            case "cook": return "Повар";
            case "warrior": return "Воин";
            case "blacksmith": return "Кузнец";
            case "brewer": return "Алхимик";
            case "courier": return "Курьер";
            case "miner": return "Шахтер";
            default: return className;
        }
    }
}