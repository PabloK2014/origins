package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.QuestProgressTracker;
import io.github.apace100.origins.quest.QuestTicketItem;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команда для тестирования обновления прогресса квестов
 */
public class TestProgressCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("testprogress")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("action", StringArgumentType.string())
                .then(CommandManager.argument("target", StringArgumentType.string())
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .executes(TestProgressCommand::execute)))));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            String action = StringArgumentType.getString(context, "action");
            String target = StringArgumentType.getString(context, "target");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            
            Origins.LOGGER.info("Тестируем прогресс: action={}, target={}, amount={}", action, target, amount);
            
            // Ищем билеты квестов в инвентаре игрока
            boolean foundTicket = false;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (QuestTicketItem.isQuestTicket(stack) && QuestTicketItem.isAccepted(stack)) {
                    Origins.LOGGER.info("Найден активный билет квеста: {}", stack);
                    
                    // Тестируем обновление прогресса
                    boolean updated = QuestTicketItem.updateQuestProgress(stack, action, target, amount);
                    
                    if (updated) {
                        source.sendFeedback(() -> Text.literal("✅ Прогресс обновлен для билета квеста!")
                            .formatted(Formatting.GREEN), false);
                        foundTicket = true;
                    } else {
                        source.sendFeedback(() -> Text.literal("❌ Прогресс не обновлен - цель не найдена или не подходит")
                            .formatted(Formatting.RED), false);
                    }
                }
            }
            
            if (!foundTicket) {
                source.sendFeedback(() -> Text.literal("❌ Активные билеты квестов не найдены в инвентаре")
                    .formatted(Formatting.RED), false);
            }
            
            // Также тестируем через QuestProgressTracker
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, action, target, amount);
            
            source.sendFeedback(() -> Text.literal("🔄 Также отправлено через QuestProgressTracker")
                .formatted(Formatting.YELLOW), false);
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в команде testprogress: {}", e.getMessage());
            context.getSource().sendError(Text.literal("Ошибка: " + e.getMessage()));
            return 0;
        }
    }
}