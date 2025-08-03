package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class CourierBountyBoardBlockEntity extends ClassBountyBoardBlockEntity {
    
    public CourierBountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.COURIER_BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected String getBoardClass() {
        return "courier";
    }
}