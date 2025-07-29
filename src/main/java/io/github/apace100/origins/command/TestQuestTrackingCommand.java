package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.QuestEventHandlers;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Команда для тестирования отслеживания квестов
 */
public class TestQuestTrackingCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_quest_tracking")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestQuestTrackingCommand::execute));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            // Симулируем крафт хлеба
            ItemStack breadStack = new ItemStack(Items.BREAD, 1);
            QuestEventHandlers.onItemCraft(player, breadStack);
            
            source.sendFeedback(() -> Text.literal("Симулирован крафт хлеба для тестирования квестов"), false);
            return 1;
        } else {
            source.sendError(Text.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
    }
}