package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.QuestRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CreateBountyBoardCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("create_bounty_board")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(CreateBountyBoardCommand::createBoard)
        );
    }
    
    private static int createBoard(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        
        // Ищем подходящее место для размещения доски
        BlockPos targetPos = null;
        
        // Проверяем позиции вокруг игрока
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction direction : horizontalDirections) {
            BlockPos checkPos = playerPos.offset(direction, 2);
            if (world.getBlockState(checkPos).isAir() && 
                !world.getBlockState(checkPos.down()).isAir()) {
                targetPos = checkPos;
                break;
            }
        }
        
        // Если не нашли подходящее место, ставим прямо перед игроком
        if (targetPos == null) {
            targetPos = playerPos.offset(Direction.NORTH, 2);
        }
        
        // Размещаем доску объявлений
        world.setBlockState(targetPos, QuestRegistry.BOUNTY_BOARD.getDefaultState());
        
        final BlockPos finalPos = targetPos;
        source.sendFeedback(() -> Text.literal("Доска объявлений создана в позиции: " + 
            finalPos.getX() + ", " + finalPos.getY() + ", " + finalPos.getZ()), true);
        
        return 1;
    }
}