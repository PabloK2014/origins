package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class MinerBountyBoardBlockEntity extends BountyBoardBlockEntity {
    
    public MinerBountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.MINER_BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected String getBoardClass() {
        return "miner";
    }
}