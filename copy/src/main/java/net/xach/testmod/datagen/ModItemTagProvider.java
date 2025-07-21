package net.xach.testmod.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture,
                              CompletableFuture<TagLookup<Block>> lookupCompletableFuture, @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, completableFuture, lookupCompletableFuture, TestMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(ItemTags.LOGS_THAT_BURN)
                .add(TestModBlocks.MAGIC_LOG.get().asItem())
                .add(TestModBlocks.MAGIC_WOOD.get().asItem())
                .add(TestModBlocks.STRIPPED_MAGIC_LOG.get().asItem())
                .add(TestModBlocks.STRIPPED_MAGIC_WOOD.get().asItem());

        tag(ItemTags.PLANKS)
                .add(TestModBlocks.MAGIC_PLANKS.get().asItem());
    }
}