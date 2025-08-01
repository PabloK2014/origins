package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.BountifulQuestCreator;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;

/**
 * Команда для тестирования системы квестов Bountiful
 */
public class BountifulQuestCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("bountiful_quest")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("give")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("profession", StringArgumentType.string())
                        .executes(BountifulQuestCommand::giveQuest))))
            .then(CommandManager.literal("complete")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(BountifulQuestCommand::completeQuest)))
        );
    }
    
    /**
     * Выдает квест игроку
     */
    private static int giveQuest(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            String profession = StringArgumentType.getString(context, "profession");
            ServerWorld world = context.getSource().getWorld();
            
            // Создаем квест для указанной профессии
            BountifulQuestCreator creator = new BountifulQuestCreator(
                world,
                player.getBlockPos(),
                profession,
                player.experienceLevel,
                world.getTime()
            );
            
            ItemStack questItem = creator.createQuestItem();
            
            if (!questItem.isEmpty()) {
                // Даем квест игроку
                if (!player.giveItemStack(questItem)) {
                    // Если инвентарь полон, бросаем на землю
                    player.dropItem(questItem, false);
                }
                
                context.getSource().sendFeedback(
                    () -> Text.literal("Выдан квест для профессии " + profession + " игроку " + player.getName().getString()),
                    true
                );
                
                return 1;
            } else {
                context.getSource().sendError(Text.literal("Не удалось создать квест"));
                return 0;
            }
            
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Ошибка при выдаче квеста: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Завершает все квесты игрока (для тестирования)
     */
    private static int completeQuest(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            int completedQuests = 0;
            
            // Проходим по всем предметам в инвентаре
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                
                if (stack.getItem() instanceof io.github.apace100.origins.quest.BountifulQuestItem) {
                    // Пытаемся завершить квест
                    if (io.github.apace100.origins.quest.BountifulQuestEventHandler.tryCompleteQuest(player, stack)) {
                        completedQuests++;
                    }
                }
            }
            
            if (completedQuests > 0) {
                final int finalCompletedQuests = completedQuests;
                context.getSource().sendFeedback(
                    () -> Text.literal("Завершено квестов: " + finalCompletedQuests + " для игрока " + player.getName().getString()),
                    true
                );
                return completedQuests;
            } else {
                context.getSource().sendError(Text.literal("У игрока нет активных квестов для завершения"));
                return 0;
            }
            
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Ошибка при завершении квестов: " + e.getMessage()));
            return 0;
        }
    }
}