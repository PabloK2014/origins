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
import io.github.apace100.origins.quest.QuestTicketState;

/**
 * Команда для сброса времени всех билетов квестов в инвентаре игрока
 */
public class ResetAllTicketTimesCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("reset_all_ticket_times")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(ResetAllTicketTimesCommand::resetAllTicketTimes)
        );
    }
    
    private static int resetAllTicketTimes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            int resetCount = 0;
            
            // Проходим по всему инвентарю игрока
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                
                if (QuestTicketItem.isQuestTicket(stack)) {
                    // Сбрасываем билет в исходное состояние
                    net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
                    nbt.putString("quest_state", QuestTicketState.AVAILABLE.getName());
                    nbt.putLong("accept_time", 0L);
                    nbt.remove("client_tooltip_time");
                    nbt.remove("last_tooltip_update");
                    nbt.remove("client_time_cache");
                    nbt.remove("tooltip_render_time");
                    nbt.remove("client_current_time");
                    nbt.putBoolean("completion_ready", false);
                    
                    resetCount++;
                }
            }
            
            final int finalResetCount = resetCount; // Делаем переменную final для лямбды
            
            if (finalResetCount > 0) {
                source.sendFeedback(() -> Text.literal("Сброшено время для " + finalResetCount + " билетов квестов").formatted(net.minecraft.util.Formatting.GREEN), false);
                source.sendFeedback(() -> Text.literal("Теперь время будет автоматически инициализироваться при наведении курсора на билеты").formatted(net.minecraft.util.Formatting.YELLOW), false);
            } else {
                source.sendFeedback(() -> Text.literal("В инвентаре не найдено билетов квестов").formatted(net.minecraft.util.Formatting.RED), false);
            }
            
            return finalResetCount;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Ошибка: " + e.getMessage()).formatted(net.minecraft.util.Formatting.RED), false);
            return 0;
        }
    }
}