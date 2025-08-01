package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.BountyBoardBlockEntity;
import io.github.apace100.origins.quest.QuestGenerator;
import io.github.apace100.origins.quest.QuestRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TestBountyBoardFixCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("test_bounty_fix")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestBountyBoardFixCommand::execute));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            World world = player.getWorld();
            
            // Информация о загруженных квестах
            int totalQuests = QuestGenerator.getTotalQuestCount();
            source.sendFeedback(() -> Text.literal("Загружено квестов из JSON: " + totalQuests), false);
            
            for (String profession : QuestGenerator.getAvailableProfessions()) {
                int count = QuestGenerator.getQuestCountForProfession(profession);
                source.sendFeedback(() -> Text.literal("- " + profession + ": " + count + " квестов"), false);
            }
            
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
                
                // Принудительно инициализируем доску
                nearestBoard.tryInitialPopulation();
                
                // Проверяем количество квестов на доске
                int questCount = nearestBoard.getQuestCount();
                source.sendFeedback(() -> Text.literal("Квестов на доске: " + questCount), false);
                
                // Принудительно пересоздаем квесты
                nearestBoard.forceRegenerateQuests();
                int newQuestCount = nearestBoard.getQuestCount();
                source.sendFeedback(() -> Text.literal("Квестов после принудительной регенерации: " + newQuestCount), false);
                
            } else {
                source.sendFeedback(() -> Text.literal("Доска объявлений не найдена поблизости"), false);
                
                // Создаем доску рядом с игроком
                BlockPos boardPos = playerPos.add(1, 0, 0);
                world.setBlockState(boardPos, QuestRegistry.BOUNTY_BOARD.getDefaultState());
                source.sendFeedback(() -> Text.literal("Создана доска объявлений в позиции: " + boardPos), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}