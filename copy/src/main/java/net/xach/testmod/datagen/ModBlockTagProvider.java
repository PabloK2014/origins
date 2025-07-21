package net.xach.testmod.datagen;


import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, TestMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(BlockTags.LOGS_THAT_BURN)
                .add(TestModBlocks.MAGIC_LOG.get())
                .add(TestModBlocks.MAGIC_WOOD.get())
                .add(TestModBlocks.STRIPPED_MAGIC_LOG.get())
                .add(TestModBlocks.STRIPPED_MAGIC_WOOD.get());

        // Добавляем доски в соответствующие теги
        this.tag(BlockTags.PLANKS)
                .add(TestModBlocks.MAGIC_PLANKS.get());

        // Добавляем деревянные блоки в теги
        this.tag(BlockTags.WOODEN_STAIRS)
                .add(TestModBlocks.MAGIC_STAIRS.get());

        this.tag(BlockTags.WOODEN_SLABS)
                .add(TestModBlocks.MAGIC_SLAB.get());

        this.tag(BlockTags.WOODEN_PRESSURE_PLATES)
                .add(TestModBlocks.MAGIC_PRESSURE_PLATE.get());

        this.tag(BlockTags.WOODEN_BUTTONS)
                .add(TestModBlocks.MAGIC_BUTTON.get());

        this.tag(BlockTags.WOODEN_FENCES)
                .add(TestModBlocks.MAGIC_FENCE.get());

        this.tag(BlockTags.FENCE_GATES)
                .add(TestModBlocks.MAGIC_FENCE_GATE.get());

        this.tag(BlockTags.WOODEN_DOORS)
                .add(TestModBlocks.MAGIC_DOOR.get());

        this.tag(BlockTags.WOODEN_TRAPDOORS)
                .add(TestModBlocks.MAGIC_TRAPDOOR.get());

        // Добавляем в теги для инструментов
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(TestModBlocks.MAGIC_LOG.get())
                .add(TestModBlocks.MAGIC_WOOD.get())
                .add(TestModBlocks.STRIPPED_MAGIC_LOG.get())
                .add(TestModBlocks.STRIPPED_MAGIC_WOOD.get())
                .add(TestModBlocks.MAGIC_PLANKS.get())
                .add(TestModBlocks.MAGIC_STAIRS.get())
                .add(TestModBlocks.MAGIC_SLAB.get())
                .add(TestModBlocks.MAGIC_PRESSURE_PLATE.get())
                .add(TestModBlocks.MAGIC_BUTTON.get())
                .add(TestModBlocks.MAGIC_FENCE.get())
                .add(TestModBlocks.MAGIC_FENCE_GATE.get())
                .add(TestModBlocks.MAGIC_DOOR.get())
                .add(TestModBlocks.MAGIC_TRAPDOOR.get());
    }
}
