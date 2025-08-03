package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BlacksmithBountyBoard extends ClassBountyBoard {
    
    public BlacksmithBountyBoard(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BlacksmithBountyBoardBlockEntity(pos, state);
    }
    
    @Override
    public String getBoardType() {
        return "blacksmith";
    }
    
    @Override
    protected BlockEntityType<?> getExpectedBlockEntityType() {
        return QuestRegistry.BLACKSMITH_BOUNTY_BOARD_BLOCK_ENTITY;
    }
}