package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class CookBountyBoardBlockEntity extends ClassBountyBoardBlockEntity {
    
    public CookBountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.COOK_BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected String getBoardClass() {
        return "cook";
    }
}