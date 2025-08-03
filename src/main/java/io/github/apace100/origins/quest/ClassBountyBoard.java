package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Базовый класс для классовых досок объявлений
 */
public abstract class ClassBountyBoard extends BountyBoard {
    
    public ClassBountyBoard(Settings settings) {
        super(settings);
    }
    
    /**
     * Возвращает тип доски для этого класса
     */
    public abstract String getBoardType();
    
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Используем специальный ticker для классовых досок
        // Проверяем каждый тип классовой доски отдельно
        if (type == QuestRegistry.COOK_BOUNTY_BOARD_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<CookBountyBoardBlockEntity>) ClassBountyBoardBlockEntity::tick;
        } else if (type == QuestRegistry.WARRIOR_BOUNTY_BOARD_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<WarriorBountyBoardBlockEntity>) ClassBountyBoardBlockEntity::tick;
        } else if (type == QuestRegistry.BLACKSMITH_BOUNTY_BOARD_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<BlacksmithBountyBoardBlockEntity>) ClassBountyBoardBlockEntity::tick;
        } else if (type == QuestRegistry.BREWER_BOUNTY_BOARD_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<BrewerBountyBoardBlockEntity>) ClassBountyBoardBlockEntity::tick;
        } else if (type == QuestRegistry.COURIER_BOUNTY_BOARD_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<CourierBountyBoardBlockEntity>) ClassBountyBoardBlockEntity::tick;
        } else if (type == QuestRegistry.MINER_BOUNTY_BOARD_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<MinerBountyBoardBlockEntity>) ClassBountyBoardBlockEntity::tick;
        }
        return null;
    }
    
    /**
     * Возвращает ожидаемый тип BlockEntity для этой доски
     */
    protected abstract BlockEntityType<?> getExpectedBlockEntityType();
}