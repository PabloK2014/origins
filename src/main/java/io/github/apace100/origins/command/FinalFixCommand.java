package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.BountyBoardBlockEntity;
import io.github.apace100.origins.quest.QuestRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FinalFixCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("final_fix")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(FinalFixCommand::execute));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            World world = player.getWorld();
            BlockPos playerPos = player.getBlockPos();
            
            source.sendFeedback(() -> Text.literal("=== ФИНАЛЬНОЕ ИСПРАВЛЕНИЕ ДОСКИ ==="), false);
            
            // Ищем или создаем доску
            BountyBoardBlockEntity board = null;
            BlockPos boardPos = null;
            
            // Поиск существующей доски
            for (int x = -5; x <= 5; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        if (world.getBlockEntity(checkPos) instanceof BountyBoardBlockEntity foundBoard) {
                            board = foundBoard;
                            boardPos = checkPos;
                            break;
                        }
                    }
                    if (board != null) break;
                }
                if (board != null) break;
            }
            
            // Создаем доску если не найдена
            if (board == null) {
                boardPos = playerPos.add(1, 0, 0);
                world.setBlockState(boardPos, QuestRegistry.BOUNTY_BOARD.getDefaultState());
                board = (BountyBoardBlockEntity) world.getBlockEntity(boardPos);
                final BlockPos newBoardPos = boardPos;
                source.sendFeedback(() -> Text.literal("Создана новая доска в позиции: " + newBoardPos), false);
            } else {
                final BlockPos finalBoardPos = boardPos;
                source.sendFeedback(() -> Text.literal("Найдена доска в позиции: " + finalBoardPos), false);
            }
            
            if (board != null) {
                // Полная очистка и регенерация
                source.sendFeedback(() -> Text.literal("Очищаем и регенерируем квесты..."), false);
                board.forceRegenerateQuests();
                
                // Проверяем результат
                int questCount = board.getQuestCount();
                if (questCount > 0) {
                    source.sendFeedback(() -> Text.literal("✅ УСПЕХ! Создано " + questCount + " квестов"), false);
                    source.sendFeedback(() -> Text.literal("Откройте доску объявлений - квесты должны отображаться!"), false);
                } else {
                    source.sendError(Text.literal("❌ ОШИБКА: Квесты не создались"));
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка: " + e.getMessage()));
            return 0;
        }
    }
}