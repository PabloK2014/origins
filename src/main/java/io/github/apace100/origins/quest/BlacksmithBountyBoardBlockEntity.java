package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlacksmithBountyBoardBlockEntity extends BountyBoardBlockEntity {
    
    public BlacksmithBountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.BLACKSMITH_BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected String getBoardClass() {
        return "blacksmith";
    }
}