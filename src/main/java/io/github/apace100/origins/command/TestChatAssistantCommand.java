package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.ChatAssistantApiClient;
import io.github.apace100.origins.quest.QuestAccumulation;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

/**
 * Команда для тестирования чат-помощника и системы квестов
 */
public class TestChatAssistantCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_chat_assistant")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("api_status")
                .executes(TestChatAssistantCommand::testApiStatus))
            .then(CommandManager.literal("quest_accumulation")
                .executes(TestChatAssistantCommand::testQuestAccumulation))
            .then(CommandManager.literal("integration")
                .executes(TestChatAssistantCommand::testIntegration))
        );
    }
    
    /**
     * Тестирует статус Chat API
     */
    private static int testApiStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendFeedback(() -> Text.literal("🧪 Тестируем доступность Chat API...")
                .formatted(Formatting.YELLOW), true);
            
            ChatAssistantApiClient.isChatApiAvailable()
                .thenAccept(available -> {
                    if (available) {
                        source.sendFeedback(() -> Text.literal("✅ Chat API доступен!")
                            .formatted(Formatting.GREEN), false);
                        
                        // Тестируем простой запрос
                        ChatAssistantApiClient.askQuestion("Как сделать палки?", "1.20.1")
                            .thenAccept(response -> {
                                if (response.success) {
                                    source.sendFeedback(() -> Text.literal("✅ Тестовый запрос успешен!")
                                        .formatted(Formatting.GREEN), false);
                                    source.sendFeedback(() -> Text.literal("📝 Ответ: " + response.answer.substring(0, Math.min(100, response.answer.length())) + "...")
                                        .formatted(Formatting.AQUA), false);
                                } else {
                                    source.sendFeedback(() -> Text.literal("❌ Тестовый запрос неудачен: " + response.errorMessage)
                                        .formatted(Formatting.RED), false);
                                }
                            })
                            .exceptionally(throwable -> {
                                source.sendFeedback(() -> Text.literal("❌ Ошибка при тестовом запросе: " + throwable.getMessage())
                                    .formatted(Formatting.RED), false);
                                return null;
                            });
                    } else {
                        source.sendFeedback(() -> Text.literal("❌ Chat API недоступен")
                            .formatted(Formatting.RED), false);
                    }
                })
                .exceptionally(throwable -> {
                    source.sendFeedback(() -> Text.literal("❌ Ошибка при проверке API: " + throwable.getMessage())
                        .formatted(Formatting.RED), false);
                    return null;
                });
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при тестировании Chat API", e);
            source.sendError(Text.literal("❌ Ошибка при тестировании: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Тестирует систему накопления квестов
     */
    private static int testQuestAccumulation(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendFeedback(() -> Text.literal("🧪 Тестируем систему накопления квестов...")
                .formatted(Formatting.YELLOW), true);
            
            // Получаем статистику накопления
            Map<String, String> stats = QuestAccumulation.getInstance().getAccumulationStats();
            
            source.sendFeedback(() -> Text.literal("📊 Статистика накопления квестов:")
                .formatted(Formatting.AQUA), false);
            
            for (Map.Entry<String, String> entry : stats.entrySet()) {
                String className = entry.getKey();
                String stat = entry.getValue();
                
                source.sendFeedback(() -> Text.literal("  📋 " + className + ": " + stat)
                    .formatted(Formatting.WHITE), false);
            }
            
            // Проверяем максимальные значения
            int maxRequests = QuestAccumulation.getInstance().getMaxRequests();
            source.sendFeedback(() -> Text.literal("⚙️ Максимум запросов перед очисткой: " + maxRequests)
                .formatted(Formatting.GRAY), false);
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при тестировании накопления квестов", e);
            source.sendError(Text.literal("❌ Ошибка при тестировании: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Тестирует интеграцию всех компонентов
     */
    private static int testIntegration(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendFeedback(() -> Text.literal("🧪 Тестируем интеграцию всех компонентов...")
                .formatted(Formatting.YELLOW), true);
            
            // Проверяем, что все классы загружены
            boolean allClassesLoaded = true;
            String[] testClasses = {
                "ChatAssistantApiClient",
                "ChatMessageAnimator", 
                "QuestAccumulation",
                "MinecraftChatAssistant"
            };
            
            for (String className : testClasses) {
                try {
                    Class.forName("io.github.apace100.origins.quest." + className);
                    source.sendFeedback(() -> Text.literal("✅ " + className + " загружен")
                        .formatted(Formatting.GREEN), false);
                } catch (ClassNotFoundException e) {
                    try {
                        Class.forName("io.github.apace100.origins.command." + className);
                        source.sendFeedback(() -> Text.literal("✅ " + className + " загружен")
                            .formatted(Formatting.GREEN), false);
                    } catch (ClassNotFoundException e2) {
                        source.sendFeedback(() -> Text.literal("❌ " + className + " НЕ загружен")
                            .formatted(Formatting.RED), false);
                        allClassesLoaded = false;
                    }
                }
            }
            
            if (allClassesLoaded) {
                source.sendFeedback(() -> Text.literal("🎯 Все компоненты успешно интегрированы!")
                    .formatted(Formatting.GREEN), false);
                source.sendFeedback(() -> Text.literal("💡 Попробуйте команду /ask \"как сделать палки\" для тестирования")
                    .formatted(Formatting.AQUA), false);
            } else {
                source.sendFeedback(() -> Text.literal("❌ Некоторые компоненты не загружены")
                    .formatted(Formatting.RED), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при тестировании интеграции", e);
            source.sendError(Text.literal("❌ Ошибка при тестировании: " + e.getMessage()));
            return 0;
        }
    }
}