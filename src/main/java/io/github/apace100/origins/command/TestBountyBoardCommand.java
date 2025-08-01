package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.BountyBoardBlockEntity;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TestBountyBoardCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_bounty_board")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestBountyBoardCommand::testBoard)
        );
    }
    
    private static int testBoard(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        
        // Ищем доску объявлений поблизости
        BountyBoardBlockEntity foundBoard = null;
        BlockPos foundPos = null;
        
        for (int x = playerPos.getX() - 10; x <= playerPos.getX() + 10; x++) {
            for (int y = playerPos.getY() - 5; y <= playerPos.getY() + 5; y++) {
                for (int z = playerPos.getZ() - 10; z <= playerPos.getZ() + 10; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    
                    if (world.getBlockState(pos).getBlock() == QuestRegistry.BOUNTY_BOARD) {
                        if (world.getBlockEntity(pos) instanceof BountyBoardBlockEntity board) {
                            foundBoard = board;
                            foundPos = pos;
                            break;
                        }
                    }
                }
                if (foundBoard != null) break;
            }
            if (foundBoard != null) break;
        }
        
        if (foundBoard == null) {
            source.sendError(Text.literal("Доска объявлений не найдена поблизости"));
            return 0;
        }
        
        final BlockPos finalFoundPos = foundPos;
        source.sendFeedback(() -> Text.literal("Найдена доска объявлений в позиции: " + finalFoundPos), false);
        
        // Тестируем количество квестов
        int questCount = foundBoard.getQuestCount();
        source.sendFeedback(() -> Text.literal("Количество предметов в инвентаре: " + questCount), false);
        
        // Тестируем список доступных квестов
        List<Quest> availableQuests = foundBoard.getAvailableQuests();
        source.sendFeedback(() -> Text.literal("Количество доступных квестов: " + availableQuests.size()), false);
        
        // Показываем первые 5 квестов
        for (int i = 0; i < Math.min(5, availableQuests.size()); i++) {
            Quest quest = availableQuests.get(i);
            final int index = i + 1;
            source.sendFeedback(() -> Text.literal(index + ". " + quest.getTitle() + " (" + quest.getPlayerClass() + ")"), false);
        }
        
        return availableQuests.size();
    }
}