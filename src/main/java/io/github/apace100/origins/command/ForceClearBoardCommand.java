package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.BountyBoardBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ForceClearBoardCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("force_clear_board")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(ForceClearBoardCommand::execute));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            World world = player.getWorld();
            
            // Ищем ближайшую доску объявлений
            BlockPos playerPos = player.getBlockPos();
            BountyBoardBlockEntity nearestBoard = null;
            BlockPos nearestPos = null;
            
            for (int x = -10; x <= 10; x++) {
                for (int y = -5; y <= 5; y++) {
                    for (int z = -10; z <= 10; z++) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        if (world.getBlockEntity(checkPos) instanceof BountyBoardBlockEntity board) {
                            nearestBoard = board;
                            nearestPos = checkPos;
                            break;
                        }
                    }
                    if (nearestBoard != null) break;
                }
                if (nearestBoard != null) break;
            }
            
            if (nearestBoard != null) {
                final BlockPos finalNearestPos = nearestPos;
                source.sendFeedback(() -> Text.literal("Найдена доска объявлений в позиции: " + finalNearestPos), false);
                
                // Принудительно очищаем и регенерируем
                nearestBoard.forceRegenerateQuests();
                
                int questCount = nearestBoard.getQuestCount();
                source.sendFeedback(() -> Text.literal("Доска очищена и регенерирована! Квестов: " + questCount), false);
                
            } else {
                source.sendFeedback(() -> Text.literal("Доска объявлений не найдена поблизости"), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}