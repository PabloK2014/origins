package net.xach.testmod.datagen;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.items.TestModItems;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // Рецепт досок из бревен
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, TestModBlocks.MAGIC_PLANKS.get(), 4)
                .requires(TestModBlocks.MAGIC_LOG.get())
                .unlockedBy("has_magic_log", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_LOG.get()).build()))
                .save(pWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, TestModBlocks.MAGIC_PLANKS.get(), 4)
                .requires(TestModBlocks.MAGIC_WOOD.get())
                .unlockedBy("has_magic_wood", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_WOOD.get()).build()))
                .save(pWriter, TestMod.MOD_ID + ":magic_planks_from_magic_wood");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, TestModBlocks.MAGIC_PLANKS.get(), 4)
                .requires(TestModBlocks.STRIPPED_MAGIC_LOG.get())
                .unlockedBy("has_stripped_magic_log", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.STRIPPED_MAGIC_LOG.get()).build()))
                .save(pWriter, TestMod.MOD_ID + ":magic_planks_from_stripped_magic_log");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, TestModBlocks.MAGIC_PLANKS.get(), 4)
                .requires(TestModBlocks.STRIPPED_MAGIC_WOOD.get())
                .unlockedBy("has_stripped_magic_wood", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.STRIPPED_MAGIC_WOOD.get()).build()))
                .save(pWriter, TestMod.MOD_ID + ":magic_planks_from_stripped_magic_wood");

        // Рецепт ступенек
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TestModBlocks.MAGIC_STAIRS.get(), 4)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт полублоков
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, TestModBlocks.MAGIC_SLAB.get(), 6)
                .pattern("###")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт нажимной плиты
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TestModBlocks.MAGIC_PRESSURE_PLATE.get())
                .pattern("##")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт кнопки
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, TestModBlocks.MAGIC_BUTTON.get())
                .requires(TestModBlocks.MAGIC_PLANKS.get())
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт забора
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TestModBlocks.MAGIC_FENCE.get(), 3)
                .pattern("#S#")
                .pattern("#S#")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .define('S', Items.STICK)
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт калитки
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TestModBlocks.MAGIC_FENCE_GATE.get())
                .pattern("S#S")
                .pattern("S#S")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .define('S', Items.STICK)
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт двери
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TestModBlocks.MAGIC_DOOR.get(), 3)
                .pattern("##")
                .pattern("##")
                .pattern("##")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт люка
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, TestModBlocks.MAGIC_TRAPDOOR.get(), 2)
                .pattern("###")
                .pattern("###")
                .define('#', TestModBlocks.MAGIC_PLANKS.get())
                .unlockedBy("has_magic_planks", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(TestModBlocks.MAGIC_PLANKS.get()).build()))
                .save(pWriter);

        // Рецепт семян клубники из семян пшеницы
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TestModItems.STRAWBERRY_SEEDS.get())
                .requires(Items.WHEAT_SEEDS)
                .unlockedBy("has_wheat_seeds", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(Items.WHEAT_SEEDS).build()))
                .save(pWriter);

        // Рецепт магического саженца из дубового саженца
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, TestModBlocks.MAGIC_SAPLING.get())
                .requires(Items.OAK_SAPLING)
                .unlockedBy("has_oak_sapling", inventoryTrigger(ItemPredicate.Builder.item()
                        .of(Items.OAK_SAPLING).build()))
                .save(pWriter);
    }
}
