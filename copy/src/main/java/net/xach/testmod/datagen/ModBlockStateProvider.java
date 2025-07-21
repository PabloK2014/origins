package net.xach.testmod.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.block.StrawberryCrop;

import java.util.Objects;
import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, TestMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        makeCrop(((CropBlock) TestModBlocks.STRAWBERRY_CROP.get()), "strawberry_crop_stage", "strawberry_crop_stage");

        logBlock(TestModBlocks.MAGIC_LOG.get());
        axisBlock(TestModBlocks.MAGIC_WOOD.get(), blockTexture(TestModBlocks.MAGIC_LOG.get()), blockTexture(TestModBlocks.MAGIC_LOG.get()));
        logBlock(TestModBlocks.STRIPPED_MAGIC_LOG.get());
        axisBlock(TestModBlocks.STRIPPED_MAGIC_WOOD.get(), blockTexture(TestModBlocks.STRIPPED_MAGIC_LOG.get()), blockTexture(TestModBlocks.STRIPPED_MAGIC_LOG.get()));

        blockItem(TestModBlocks.MAGIC_LOG);
        blockItem(TestModBlocks.MAGIC_WOOD);
        blockItem(TestModBlocks.STRIPPED_MAGIC_LOG);
        blockItem(TestModBlocks.STRIPPED_MAGIC_WOOD);

        blockWithItem(TestModBlocks.MAGIC_PLANKS);

        // Регистрируем модели для блоков
        stairsBlock(TestModBlocks.MAGIC_STAIRS.get(), blockTexture(TestModBlocks.MAGIC_PLANKS.get()));
        slabBlock(TestModBlocks.MAGIC_SLAB.get(), blockTexture(TestModBlocks.MAGIC_PLANKS.get()), blockTexture(TestModBlocks.MAGIC_PLANKS.get()));
        pressurePlateBlock(TestModBlocks.MAGIC_PRESSURE_PLATE.get(), blockTexture(TestModBlocks.MAGIC_PLANKS.get()));
        buttonBlock(TestModBlocks.MAGIC_BUTTON.get(), blockTexture(TestModBlocks.MAGIC_PLANKS.get()));
        fenceBlock(TestModBlocks.MAGIC_FENCE.get(), blockTexture(TestModBlocks.MAGIC_PLANKS.get()));
        fenceGateBlock(TestModBlocks.MAGIC_FENCE_GATE.get(), blockTexture(TestModBlocks.MAGIC_PLANKS.get()));

        // Кастомные текстуры для двери и люка
        ResourceLocation doorBottom = modLoc("block/magic_door_bottom");
        ResourceLocation doorTop = modLoc("block/magic_door_top");
        ResourceLocation trapdoorTexture = modLoc("block/magic_trapdoor");

        // Дверь с кастомными текстурами
        doorBlockWithRenderType(TestModBlocks.MAGIC_DOOR.get(), doorBottom, doorTop, "cutout");

        // Люк с кастомной текстурой
        trapdoorBlockWithRenderType(TestModBlocks.MAGIC_TRAPDOOR.get(), trapdoorTexture, true, "cutout");

        // Регистрируем айтемы для блоков
        blockItem(TestModBlocks.MAGIC_STAIRS);
        blockItem(TestModBlocks.MAGIC_SLAB);
        blockItem(TestModBlocks.MAGIC_PRESSURE_PLATE);
        blockItem(TestModBlocks.MAGIC_FENCE_GATE);

        // Кнопка, забор, дверь и люк регистрируются в ModItemModelProvider

        leavesBlock(TestModBlocks.MAGIC_LEAVES);
        saplingBlock(TestModBlocks.MAGIC_SAPLING);

        // Ловушка - используем текстуру trap_placer для отладки
        simpleBlockWithItem(TestModBlocks.TRAP_BLOCK.get(),
                models().cubeAll("trap_block", modLoc("item/trap_placer")));
    }


    private void saplingBlock(RegistryObject<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get())).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    private void leavesBlock(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(),
                models().singleTexture(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get())).getPath(), new ResourceLocation("minecraft:block/leaves"),
                        "all", blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    public void makeCrop(CropBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, block, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] states(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(
                modelName + state.getValue(((StrawberryCrop) block).getAgeProperty()),
                new ResourceLocation(TestMod.MOD_ID, "block/" + textureName + state.getValue(((StrawberryCrop) block).getAgeProperty()))).renderType("cutout"));
        return models;
    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }

    private void blockItem(RegistryObject<? extends Block> blockRegistryObject) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile(TestMod.MOD_ID + ":block/" +
                ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath()));
    }

    private void blockItem(RegistryObject<? extends Block> blockRegistryObject, String appendix) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile(TestMod.MOD_ID + ":block/" +
                ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath() + appendix));
    }
}
