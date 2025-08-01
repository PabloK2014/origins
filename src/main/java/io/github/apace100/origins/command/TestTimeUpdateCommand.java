package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

/**
 * Команда для тестирования обновления времени в билетах квестов
 */
public class TestTimeUpdateCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_time_update")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 120))
                .executes(TestTimeUpdateCommand::testTimeUpdate))
            .executes(context -> testTimeUpdate(context, 5)) // По умолчанию 5 минут
        );
    }
    
    private static int testTimeUpdate(CommandContext<ServerCommandSource> context) {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        return testTimeUpdate(context, minutes);
    }
    
    private static int testTimeUpdate(CommandContext<ServerCommandSource> context, int minutes) {
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
            
            // Устанавливаем время принятия квеста назад на указанное количество минут
            long currentTime = System.currentTimeMillis();
            long testAcceptTime = currentTime - (minutes * 60 * 1000L); // Отнимаем минуты в миллисекундах
            
            // Принимаем квест с тестовым временем
            QuestTicketItem.markAsAccepted(heldItem, testAcceptTime);
            
            // Принудительно обновляем клиентские данные
            net.minecraft.nbt.NbtCompound nbt = heldItem.getOrCreateNbt();
            nbt.putLong("force_client_update", currentTime);
            nbt.putLong("client_sync_time", currentTime);
            
            source.sendFeedback(() -> Text.literal("Билет квеста помечен как принятый " + minutes + " минут назад").formatted(net.minecraft.util.Formatting.GREEN), false);
            source.sendFeedback(() -> Text.literal("Лимит времени квеста: " + quest.getTimeLimit() + " минут").formatted(net.minecraft.util.Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("Теперь наведите курсор на билет - время должно обновляться в реальном времени!").formatted(net.minecraft.util.Formatting.GOLD), false);
            
            // Вычисляем оставшееся время
            long elapsedSeconds = (currentTime - testAcceptTime) / 1000;
            long totalLimitSeconds = quest.getTimeLimit() * 60;
            long remainingSeconds = Math.max(0, totalLimitSeconds - elapsedSeconds);
            
            if (remainingSeconds > 0) {
                long remainingMinutes = remainingSeconds / 60;
                long remainingSecondsOnly = remainingSeconds % 60;
                source.sendFeedback(() -> Text.literal("Текущее оставшееся время: " + remainingMinutes + "м " + remainingSecondsOnly + "с").formatted(net.minecraft.util.Formatting.AQUA), false);
            } else {
                source.sendFeedback(() -> Text.literal("Время истекло! Квест должен быть помечен как проваленный.").formatted(net.minecraft.util.Formatting.RED), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Ошибка: " + e.getMessage()).formatted(net.minecraft.util.Formatting.RED), false);
            return 0;
        }
    }
}