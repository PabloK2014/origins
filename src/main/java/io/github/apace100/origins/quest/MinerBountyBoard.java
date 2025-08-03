package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class MinerBountyBoard extends ClassBountyBoard {
    
    public MinerBountyBoard(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MinerBountyBoardBlockEntity(pos, state);
    }
    
    @Override
    public String getBoardType() {
        return "miner";
    }
    
    @Override
    protected BlockEntityType<?> getExpectedBlockEntityType() {
        return QuestRegistry.MINER_BOUNTY_BOARD_BLOCK_ENTITY;
    }
}