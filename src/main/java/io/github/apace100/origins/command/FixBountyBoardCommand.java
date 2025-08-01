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

public class FixBountyBoardCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("fix_bounty_board")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(FixBountyBoardCommand::execute));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            World world = player.getWorld();
            
            source.sendFeedback(() -> Text.literal("=== ИСПРАВЛЕНИЕ ДОСКИ ОБЪЯВЛЕНИЙ ==="), false);
            
            // Информация о загруженных квестах
            int totalQuests = QuestGenerator.getTotalQuestCount();
            source.sendFeedback(() -> Text.literal("Загружено квестов из JSON: " + totalQuests), false);
            
            if (totalQuests == 0) {
                source.sendError(Text.literal("ОШИБКА: Квесты не загружены! Перезапустите сервер."));
                return 0;
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
                source.sendFeedback(() -> Text.literal("Найдена доска в позиции: " + finalNearestPos), false);
                
                // Полное исправление доски
                source.sendFeedback(() -> Text.literal("Шаг 1: Принудительная очистка..."), false);
                nearestBoard.forceRegenerateQuests();
                
                int questCount = nearestBoard.getQuestCount();
                source.sendFeedback(() -> Text.literal("Шаг 2: Создано квестов: " + questCount), false);
                
                if (questCount == 0) {
                    source.sendError(Text.literal("ОШИБКА: Квесты не создались! Проверьте логи."));
                    return 0;
                }
                
                source.sendFeedback(() -> Text.literal("Шаг 3: Принудительная инициализация..."), false);
                nearestBoard.tryInitialPopulation();
                
                source.sendFeedback(() -> Text.literal("✅ ДОСКА ИСПРАВЛЕНА! Откройте интерфейс."), false);
                
            } else {
                source.sendFeedback(() -> Text.literal("Доска не найдена. Создаю новую..."), false);
                
                // Создаем доску рядом с игроком
                BlockPos boardPos = playerPos.add(1, 0, 0);
                world.setBlockState(boardPos, QuestRegistry.BOUNTY_BOARD.getDefaultState());
                
                // Получаем созданную доску и инициализируем
                if (world.getBlockEntity(boardPos) instanceof BountyBoardBlockEntity newBoard) {
                    newBoard.forceRegenerateQuests();
                    int questCount = newBoard.getQuestCount();
                    source.sendFeedback(() -> Text.literal("✅ СОЗДАНА НОВАЯ ДОСКА с " + questCount + " квестами в позиции: " + boardPos), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}