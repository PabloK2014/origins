package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BrewerBountyBoardBlockEntity extends BountyBoardBlockEntity {
    
    public BrewerBountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.BREWER_BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected String getBoardClass() {
        return "brewer";
    }
}