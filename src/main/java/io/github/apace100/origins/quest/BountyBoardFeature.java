package io.github.apace100.origins.quest;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BountyBoardFeature extends Feature<DefaultFeatureConfig> {
    public BountyBoardFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = context.getRandom();

        // Устанавливаем доску объявлений
        BlockState boardState = QuestRegistry.BOUNTY_BOARD.getDefaultState();
        
        if (world.setBlockState(pos, boardState, 3)) {
            // Создаем BlockEntity и генерируем начальные задания
            if (world.getBlockEntity(pos) instanceof BountyBoardBlockEntity blockEntity) {
                // Квесты уже генерируются в конструкторе BlockEntity
            }
            return true;
        }

        return false;
    }
} 