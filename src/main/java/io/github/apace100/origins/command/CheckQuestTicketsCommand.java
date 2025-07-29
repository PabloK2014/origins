package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.QuestInventoryManager;
import io.github.apace100.origins.quest.QuestItem;
import io.github.apace100.origins.quest.QuestTicketItem;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Команда для проверки билетов квестов в инвентаре
 */
public class CheckQuestTicketsCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("check_quest_tickets")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(CheckQuestTicketsCommand::execute));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            List<ItemStack> questTickets = inventoryManager.findQuestTickets(player);
            
            source.sendFeedback(() -> Text.literal("=== Проверка билетов квестов ==="), false);
            source.sendFeedback(() -> Text.literal("Найдено билетов: " + questTickets.size()), false);
            
            for (int i = 0; i < questTickets.size(); i++) {
                ItemStack ticket = questTickets.get(i);
                var quest = QuestItem.getQuestFromStack(ticket);
                final int index = i + 1;
                
                if (quest != null) {
                    final String title = quest.getTitle();
                    final String id = quest.getId();
                    source.sendFeedback(() -> Text.literal(String.format("%d. %s (ID: %s)", 
                        index, title, id)), false);
                } else {
                    final String ticketStr = ticket.toString();
                    source.sendFeedback(() -> Text.literal(String.format("%d. Поврежденный билет: %s", 
                        index, ticketStr)), false);
                }
            }
            
            // Проверяем все предметы в инвентаре
            source.sendFeedback(() -> Text.literal("=== Все предметы в инвентаре ==="), false);
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    boolean isQuestTicket = QuestTicketItem.isQuestTicket(stack);
                    final int slot = i;
                    final String stackStr = stack.toString();
                    final boolean isTicket = isQuestTicket;
                    source.sendFeedback(() -> Text.literal(String.format("Слот %d: %s (билет: %s)", 
                        slot, stackStr, isTicket)), false);
                }
            }
            
            return 1;
        } else {
            source.sendError(Text.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
    }
}