package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.QuestInventoryManager;
import io.github.apace100.origins.quest.QuestManager;
import io.github.apace100.origins.quest.QuestTicketAcceptanceHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/**
 * Команда для очистки квестов игрока и диагностики проблем с квестами
 */
public class ClearQuestsCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("origins")
            .then(CommandManager.literal("quest")
                .then(CommandManager.literal("clear")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> clearOwnQuests(context))
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> clearPlayerQuests(context))
                    )
                )
                .then(CommandManager.literal("status")
                    .executes(context -> showQuestStatus(context))
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> showPlayerQuestStatus(context))
                    )
                )
                .then(CommandManager.literal("debug")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> debugQuestSystem(context))
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> debugPlayerQuests(context))
                    )
                )
            )
        );
    }
    
    /**
     * Очищает квесты текущего игрока
     */
    private static int clearOwnQuests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
        
        return clearQuests(source, player);
    }
    
    /**
     * Очищает квесты указанного игрока
     */
    private static int clearPlayerQuests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "player");
            
            for (ServerPlayerEntity player : players) {
                clearQuests(source, player);
            }
            
            return players.size();
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка при получении игрока: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Очищает все квесты игрока
     */
    private static int clearQuests(ServerCommandSource source, ServerPlayerEntity player) {
        try {
            Origins.LOGGER.info("Очистка квестов для игрока: {}", player.getName().getString());
            
            // Получаем информацию о квестах до очистки
            QuestManager questManager = QuestManager.getInstance();
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            
            int activeQuestsCount = questManager.getActiveQuestCount(player);
            int ticketsCount = inventoryManager.getActiveQuestCount(player);
            
            // Очищаем активные квесты в QuestManager
            questManager.cancelAllQuests(player);
            
            // Очищаем билеты квестов из инвентаря
            inventoryManager.clearAllQuestTickets(player);
            
            // Отправляем сообщения
            source.sendFeedback(() -> Text.literal("✓ Квесты игрока " + player.getName().getString() + " очищены")
                .formatted(Formatting.GREEN), true);
            
            source.sendFeedback(() -> Text.literal("  Удалено активных квестов: " + activeQuestsCount)
                .formatted(Formatting.YELLOW), false);
            
            source.sendFeedback(() -> Text.literal("  Удалено билетов из инвентаря: " + ticketsCount)
                .formatted(Formatting.YELLOW), false);
            
            // Уведомляем игрока
            player.sendMessage(
                Text.literal("Ваши квесты были очищены администратором")
                    .formatted(Formatting.YELLOW), 
                false
            );
            
            Origins.LOGGER.info("Квесты игрока {} успешно очищены", player.getName().getString());
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при очистке квестов игрока {}: {}", player.getName().getString(), e.getMessage());
            source.sendError(Text.literal("Ошибка при очистке квестов: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Показывает статус квестов текущего игрока
     */
    private static int showQuestStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
        
        QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
        handler.showQuestStatus(player);
        
        return 1;
    }
    
    /**
     * Показывает статус квестов указанного игрока
     */
    private static int showPlayerQuestStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "player");
            
            for (ServerPlayerEntity player : players) {
                source.sendFeedback(() -> Text.literal("=== Статус квестов игрока " + player.getName().getString() + " ===")
                    .formatted(Formatting.GOLD), false);
                
                QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
                handler.showQuestStatus(player);
            }
            
            return players.size();
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка при получении игрока: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Отладочная информация о системе квестов
     */
    private static int debugQuestSystem(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            QuestManager questManager = QuestManager.getInstance();
            QuestManager.QuestStats stats = questManager.getStats();
            
            source.sendFeedback(() -> Text.literal("=== Отладка системы квестов ===")
                .formatted(Formatting.GOLD), false);
            
            source.sendFeedback(() -> Text.literal("Всего активных игроков с квестами: " + stats.getActiveQuests())
                .formatted(Formatting.AQUA), false);
            
            source.sendFeedback(() -> Text.literal("Всего квестов в системе: " + stats.getTotalQuests())
                .formatted(Formatting.AQUA), false);
            
            source.sendFeedback(() -> Text.literal("Максимум квестов на игрока: " + questManager.getMaxActiveQuests())
                .formatted(Formatting.AQUA), false);
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении отладочной информации: {}", e.getMessage());
            source.sendError(Text.literal("Ошибка при получении отладочной информации: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Отладочная информация о квестах конкретного игрока
     */
    private static int debugPlayerQuests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "player");
            
            for (ServerPlayerEntity player : players) {
                source.sendFeedback(() -> Text.literal("=== Отладка квестов игрока " + player.getName().getString() + " ===")
                    .formatted(Formatting.GOLD), false);
                
                QuestManager questManager = QuestManager.getInstance();
                QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
                
                // Информация из QuestManager
                int activeQuestsManager = questManager.getActiveQuestCount(player);
                int availableSlots = questManager.getAvailableQuestSlots(player);
                boolean canAcceptMore = questManager.canAcceptAdditionalQuest(player);
                
                source.sendFeedback(() -> Text.literal("QuestManager - активных квестов: " + activeQuestsManager)
                    .formatted(Formatting.YELLOW), false);
                
                source.sendFeedback(() -> Text.literal("QuestManager - доступно слотов: " + availableSlots)
                    .formatted(Formatting.YELLOW), false);
                
                source.sendFeedback(() -> Text.literal("QuestManager - может принять еще: " + canAcceptMore)
                    .formatted(Formatting.YELLOW), false);
                
                // Информация из QuestInventoryManager
                int ticketsInInventory = inventoryManager.getActiveQuestCount(player);
                boolean hasReachedLimit = inventoryManager.hasReachedQuestLimit(player);
                
                source.sendFeedback(() -> Text.literal("QuestInventoryManager - билетов в инвентаре: " + ticketsInInventory)
                    .formatted(Formatting.AQUA), false);
                
                source.sendFeedback(() -> Text.literal("QuestInventoryManager - достиг лимита: " + hasReachedLimit)
                    .formatted(Formatting.AQUA), false);
                
                // Проверяем рассинхронизацию
                if (activeQuestsManager != ticketsInInventory) {
                    source.sendFeedback(() -> Text.literal("⚠ РАССИНХРОНИЗАЦИЯ: QuestManager и QuestInventoryManager показывают разное количество квестов!")
                        .formatted(Formatting.RED), false);
                }
            }
            
            return players.size();
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при отладке квестов игрока: {}", e.getMessage());
            source.sendError(Text.literal("Ошибка при отладке квестов игрока: " + e.getMessage()));
            return 0;
        }
    }
}