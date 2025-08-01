package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import io.github.apace100.origins.quest.QuestTicketItem;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestItem;
import io.github.apace100.origins.quest.QuestTicketState;

/**
 * Команда для проверки статуса времени в билете квеста
 */
public class CheckTimeStatusCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("check_time_status")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(CheckTimeStatusCommand::checkTimeStatus)
        );
    }
    
    private static int checkTimeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ItemStack heldItem = player.getMainHandStack();
            
            if (!QuestTicketItem.isQuestTicket(heldItem)) {
                source.sendFeedback(() -> Text.literal("Держите билет квеста в руке!").formatted(net.minecraft.util.Formatting.RED), false);
                return 0;
            }
            
            Quest quest = QuestItem.getQuestFromStack(heldItem);
            if (quest == null) {
                source.sendFeedback(() -> Text.literal("Билет поврежден!").formatted(net.minecraft.util.Formatting.RED), false);
                return 0;
            }
            
            QuestTicketState state = QuestTicketItem.getTicketState(heldItem);
            long acceptTime = QuestTicketItem.getAcceptTime(heldItem);
            
            source.sendFeedback(() -> Text.literal("=== Статус билета квеста ===").formatted(net.minecraft.util.Formatting.GOLD), false);
            source.sendFeedback(() -> Text.literal("Название: " + quest.getTitle()).formatted(net.minecraft.util.Formatting.WHITE), false);
            source.sendFeedback(() -> Text.literal("Состояние: " + state.getDisplayName()).formatted(net.minecraft.util.Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("Лимит времени: " + quest.getTimeLimit() + " минут").formatted(net.minecraft.util.Formatting.AQUA), false);
            
            if (acceptTime > 0) {
                long currentTime = System.currentTimeMillis();
                long elapsedSeconds = (currentTime - acceptTime) / 1000;
                long totalLimitSeconds = quest.getTimeLimit() * 60;
                long remainingSeconds = Math.max(0, totalLimitSeconds - elapsedSeconds);
                
                source.sendFeedback(() -> Text.literal("Время принятия: " + new java.util.Date(acceptTime)).formatted(net.minecraft.util.Formatting.GRAY), false);
                source.sendFeedback(() -> Text.literal("Прошло времени: " + (elapsedSeconds / 60) + "м " + (elapsedSeconds % 60) + "с").formatted(net.minecraft.util.Formatting.WHITE), false);
                
                if (remainingSeconds > 0) {
                    long remainingMinutes = remainingSeconds / 60;
                    long remainingSecondsOnly = remainingSeconds % 60;
                    source.sendFeedback(() -> Text.literal("Осталось времени: " + remainingMinutes + "м " + remainingSecondsOnly + "с").formatted(net.minecraft.util.Formatting.GREEN), false);
                } else {
                    source.sendFeedback(() -> Text.literal("Время истекло!").formatted(net.minecraft.util.Formatting.RED), false);
                }
            } else {
                source.sendFeedback(() -> Text.literal("Время не инициализировано").formatted(net.minecraft.util.Formatting.RED), false);
                source.sendFeedback(() -> Text.literal("Наведите курсор на билет для автоматической инициализации").formatted(net.minecraft.util.Formatting.YELLOW), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Ошибка: " + e.getMessage()).formatted(net.minecraft.util.Formatting.RED), false);
            return 0;
        }
    }
}