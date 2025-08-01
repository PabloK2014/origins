package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.BountyBoardBlockEntity;
import io.github.apace100.origins.quest.QuestRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class RefreshBountyBoardCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("refresh_bounty_boards")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 100))
                .executes(RefreshBountyBoardCommand::refreshBoardsInRadius))
            .executes(RefreshBountyBoardCommand::refreshNearbyBoard)
        );
    }
    
    private static int refreshNearbyBoard(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        int refreshedCount = 0;
        
        // Ищем доску объявлений в радиусе 10 блоков от игрока
        for (int x = playerPos.getX() - 10; x <= playerPos.getX() + 10; x++) {
            for (int y = playerPos.getY() - 5; y <= playerPos.getY() + 5; y++) {
                for (int z = playerPos.getZ() - 10; z <= playerPos.getZ() + 10; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    
                    if (world.getBlockState(pos).getBlock() == QuestRegistry.BOUNTY_BOARD) {
                        if (world.getBlockEntity(pos) instanceof BountyBoardBlockEntity bountyBoard) {
                            bountyBoard.refreshQuests();
                            refreshedCount++;
                        }
                    }
                }
            }
        }
        
        final int finalCount = refreshedCount;
        if (refreshedCount > 0) {
            source.sendFeedback(() -> Text.literal("Обновлено досок объявлений: " + finalCount), true);
        } else {
            source.sendFeedback(() -> Text.literal("Доски объявлений не найдены поблизости"), false);
        }
        
        return refreshedCount;
    }
    
    private static int refreshBoardsInRadius(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos centerPos = BlockPos.ofFloored(source.getPosition());
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int refreshedCount = 0;
        
        // Проходим по блокам в радиусе
        for (int x = centerPos.getX() - radius; x <= centerPos.getX() + radius; x++) {
            for (int y = Math.max(world.getBottomY(), centerPos.getY() - radius); 
                 y <= Math.min(world.getTopY(), centerPos.getY() + radius); y++) {
                for (int z = centerPos.getZ() - radius; z <= centerPos.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    
                    if (world.getBlockState(pos).getBlock() == QuestRegistry.BOUNTY_BOARD) {
                        if (world.getBlockEntity(pos) instanceof BountyBoardBlockEntity bountyBoard) {
                            bountyBoard.refreshQuests();
                            refreshedCount++;
                        }
                    }
                }
            }
        }
        
        final int finalCount2 = refreshedCount;
        source.sendFeedback(() -> Text.literal("Обновлено досок объявлений в радиусе " + radius + ": " + finalCount2), true);
        return refreshedCount;
    }
}