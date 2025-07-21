package net.xach.testmod.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xach.testmod.TestMod;
import net.xach.testmod.worldgen.ModConfiguredFeatures;
import net.xach.testmod.worldgen.ModPlacedFeatures;
import net.xach.testmod.worldgen.ModBiomeModifiers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();


        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK))));


        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);


        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));

        // Добавляем провайдер рецептов
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput));

        // Добавляем провайдер тегов предметов
        generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider,
                blockTagsProvider.contentsGetter(), existingFileHelper));

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                packOutput,
                lookupProvider,
                new RegistrySetBuilder()
                        .add(Registries.CONFIGURED_FEATURE, context -> {
                            ModConfiguredFeatures.bootstrap(context);
                        })
                        .add(Registries.PLACED_FEATURE, context -> {
                            ModPlacedFeatures.bootstrap(context);
                        })
                        .add(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                            ModBiomeModifiers.bootstrap(context);}),
                Set.of(TestMod.MOD_ID)
        ));
    }
}
