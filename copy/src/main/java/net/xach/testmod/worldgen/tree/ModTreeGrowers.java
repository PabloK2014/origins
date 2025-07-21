package net.xach.testmod.worldgen.tree;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel; // Исправленный импорт
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.xach.testmod.TestMod;
import net.xach.testmod.worldgen.ModConfiguredFeatures;
import org.jetbrains.annotations.Nullable;

public class ModTreeGrowers extends AbstractTreeGrower {
    @Nullable
    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource pRandom, boolean pHasFlowers) {

        if (pRandom instanceof ServerLevel serverLevel) {
            for (Player player : serverLevel.players()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Returning configured feature: MAGIC_TREE"));
            }
        }
        return ModConfiguredFeatures.MAGIC_TREE;
    }
}