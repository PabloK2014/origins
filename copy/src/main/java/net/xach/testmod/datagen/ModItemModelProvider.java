package net.xach.testmod.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.items.TestModItems;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, TestMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(TestModItems.STRAWBERRY.get());
        basicItem(TestModItems.STRAWBERRY_SEEDS.get());
        saplingItem(TestModBlocks.MAGIC_SAPLING);

        // Предметы
        basicItem(TestModItems.PEPPER_SPRAY.get());
        basicItem(TestModItems.TRAP_PLACER.get());
        basicItem(TestModItems.FOOD_BAG.get());

        // Специальные модели для блоков
        buttonItem(TestModBlocks.MAGIC_BUTTON, TestModBlocks.MAGIC_PLANKS);
        fenceItem(TestModBlocks.MAGIC_FENCE, TestModBlocks.MAGIC_PLANKS);

        // Простые модели предметов для двери и люка
        doorItem(TestModBlocks.MAGIC_DOOR);
        simpleBlockItemTexture(TestModBlocks.MAGIC_TRAPDOOR, "magic_trapdoor");


            basicItem(TestModItems.PEPPER_SPRAY.get());
            basicItem(TestModItems.TRAP_PLACER.get());
            basicItem(TestModItems.FOOD_BAG.get());
    }

    private ItemModelBuilder saplingItem(RegistryObject<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.tryParse("item/generated")).texture("layer0",
                new ResourceLocation(TestMod.MOD_ID,"block/" + item.getId().getPath()));
    }

    private ItemModelBuilder buttonItem(RegistryObject<? extends Block> block, RegistryObject<Block> baseBlock) {
        return withExistingParent(block.getId().getPath(), mcLoc("block/button_inventory"))
                .texture("texture", new ResourceLocation(TestMod.MOD_ID, "block/" + baseBlock.getId().getPath()));
    }

    private ItemModelBuilder fenceItem(RegistryObject<? extends Block> block, RegistryObject<Block> baseBlock) {
        return withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture", new ResourceLocation(TestMod.MOD_ID, "block/" + baseBlock.getId().getPath()));
    }

    private ItemModelBuilder doorItem(RegistryObject<? extends Block> block) {
        return withExistingParent(block.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", new ResourceLocation(TestMod.MOD_ID, "item/" + block.getId().getPath()));
    }

    private ItemModelBuilder simpleBlockItemTexture(RegistryObject<? extends Block> block, String textureName) {
        return withExistingParent(block.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", new ResourceLocation(TestMod.MOD_ID, "block/" + textureName));
    }
}
