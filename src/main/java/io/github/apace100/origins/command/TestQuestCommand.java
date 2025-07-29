package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Команда для тестирования системы квестов
 */
public class TestQuestCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("testquest")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("give")
                .executes(TestQuestCommand::giveTestQuest))
            .then(CommandManager.literal("progress")
                .executes(TestQuestCommand::testProgress))
        );
    }
    
    private static int giveTestQuest(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Создаем тестовый квест
            QuestObjective objective = new QuestObjective(
                QuestObjective.ObjectiveType.CRAFT,
                "minecraft:bread",
                5
            );
            
            QuestReward reward = new QuestReward(
                QuestReward.RewardType.SKILL_POINT_TOKEN,
                1,
                100
            );
            
            Quest testQuest = new Quest(
                "test_bread_craft",
                "any",
                1,
                "Тестовый хлеб",
                "Создайте 5 хлеба для тестирования системы квестов",
                objective,
                60,
                reward
            );
            
            // Создаем билет квеста
            ItemStack questTicket = QuestTicketItem.createQuestTicket(testQuest);
            
            // Даем билет игроку
            if (!player.getInventory().insertStack(questTicket)) {
                player.dropItem(questTicket, false);
            }
            
            source.sendFeedback(() -> Text.literal("Выдан тестовый квест на создание хлеба"), false);
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка при выдаче тестового квеста: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int testProgress(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Тестируем отслеживание прогресса
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, "craft", "minecraft:bread", 1);
            
            source.sendFeedback(() -> Text.literal("Протестирован прогресс крафта хлеба"), false);
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка при тестировании прогресса: " + e.getMessage()));
            return 0;
        }
    }
}