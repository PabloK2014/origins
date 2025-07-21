package net.xach.testmod.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> MAGIC_TREE = registerKey("magic_tree");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        register(context, MAGIC_TREE, Feature.TREE, createOakLikeTree().build());
    }

    private static TreeConfiguration.TreeConfigurationBuilder createOakLikeTree() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(TestModBlocks.MAGIC_LOG.get()), // Ваш кастомный ствол
                new StraightTrunkPlacer(6, 2, 0), // Высота ствола: 4 базовых + до 2 дополнительных
                BlockStateProvider.simple(TestModBlocks.MAGIC_LEAVES.get()), // Ваша кастомная листва
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2), // Крона как у дуба
                new TwoLayersFeatureSize(3, 0, 1) // Размер как у дуба
        );
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(TestMod.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}