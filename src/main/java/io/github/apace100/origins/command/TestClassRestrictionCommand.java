package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команда для тестирования системы ограничений по классам
 */
public class TestClassRestrictionCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_class_restriction")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestClassRestrictionCommand::testClassRestriction)
        );
    }
    
    private static int testClassRestriction(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
        
        try {
            // Получаем класс игрока
            String playerClass = QuestIntegration.getPlayerClass(player);
            
            source.sendFeedback(() -> Text.literal("=== Тест системы ограничений по классам ===")
                .formatted(Formatting.GOLD), false);
            
            source.sendFeedback(() -> Text.literal("Ваш класс: " + playerClass)
                .formatted(Formatting.AQUA), false);
            
            // Создаем тестовые квесты для разных классов
            String[] testClasses = {"warrior", "cook", "miner", "blacksmith", "courier", "brewer", "human"};
            
            QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
            
            for (String testClass : testClasses) {
                // Создаем тестовый квест
                Quest testQuest = createTestQuest(testClass);
                
                // Проверяем, может ли игрок взять этот квест
                boolean canAccept = handler.canAcceptQuest(player, testQuest);
                
                Formatting color = canAccept ? Formatting.GREEN : Formatting.RED;
                String status = canAccept ? "✓ МОЖНО" : "✗ НЕЛЬЗЯ";
                
                source.sendFeedback(() -> Text.literal("Квест для " + getLocalizedClassName(testClass) + ": " + status)
                    .formatted(color), false);
            }
            
            source.sendFeedback(() -> Text.literal("=== Тест завершен ===")
                .formatted(Formatting.GOLD), false);
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при тестировании ограничений по классам: {}", e.getMessage());
            source.sendError(Text.literal("Произошла ошибка: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Создает тестовый квест для указанного класса
     */
    private static Quest createTestQuest(String playerClass) {
        String questId = "test_" + playerClass + "_" + System.currentTimeMillis();
        String title = "Тестовый квест для " + getLocalizedClassName(playerClass);
        String description = "Тестовый квест для проверки ограничений по классам";
        
        QuestObjective objective = new QuestObjective(
            QuestObjective.ObjectiveType.COLLECT,
            "minecraft:dirt",
            1
        );
        
        QuestReward reward = new QuestReward(
            QuestReward.RewardType.EXPERIENCE,
            1,
            100
        );
        
        return new Quest(questId, playerClass, 1, title, description, objective, 30, reward);
    }
    
    /**
     * Получает локализованное название класса
     */
    private static String getLocalizedClassName(String playerClass) {
        return switch (playerClass) {
            case "warrior" -> "Воин";
            case "cook" -> "Повар";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "courier" -> "Курьер";
            case "brewer" -> "Пивовар";
            case "human" -> "Человек";
            default -> "Неизвестный (" + playerClass + ")";
        };
    }
}