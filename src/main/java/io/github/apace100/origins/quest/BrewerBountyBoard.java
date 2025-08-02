package io.github.apace100.origins.quest;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BrewerBountyBoard extends BountyBoard {
    
    public BrewerBountyBoard(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BrewerBountyBoardBlockEntity(pos, state);
    }
    
    public String getBoardType() {
        return "brewer";
    }
}