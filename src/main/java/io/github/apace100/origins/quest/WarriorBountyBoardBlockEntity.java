package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class WarriorBountyBoardBlockEntity extends ClassBountyBoardBlockEntity {
    
    public WarriorBountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.WARRIOR_BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected String getBoardClass() {
        return "warrior";
    }
}