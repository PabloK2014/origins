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
 * Команда для тестирования автоматической инициализации времени в билетах квестов
 */
public class TestAutoTimeCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_auto_time")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestAutoTimeCommand::testAutoTime)
        );
    }
    
    private static int testAutoTime(CommandContext<ServerCommandSource> context) {
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
            
            // Сбрасываем билет в исходное состояние
            net.minecraft.nbt.NbtCompound nbt = heldItem.getOrCreateNbt();
            nbt.putString("quest_state", QuestTicketState.AVAILABLE.getName());
            nbt.putLong("accept_time", 0L);
            nbt.remove("client_tooltip_time");
            nbt.remove("last_tooltip_update");
            nbt.remove("client_time_cache");
            nbt.remove("tooltip_render_time");
            
            source.sendFeedback(() -> Text.literal("Билет квеста сброшен в состояние 'Доступен'").formatted(net.minecraft.util.Formatting.GREEN), false);
            source.sendFeedback(() -> Text.literal("Теперь наведите курсор на билет - время должно автоматически начать отсчет!").formatted(net.minecraft.util.Formatting.GOLD), false);
            source.sendFeedback(() -> Text.literal("Лимит времени квеста: " + quest.getTimeLimit() + " минут").formatted(net.minecraft.util.Formatting.YELLOW), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Ошибка: " + e.getMessage()).formatted(net.minecraft.util.Formatting.RED), false);
            return 0;
        }
    }
}