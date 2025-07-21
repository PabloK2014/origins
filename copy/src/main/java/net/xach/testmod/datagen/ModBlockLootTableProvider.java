package net.xach.testmod.datagen;


import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.RegistryObject;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.block.StrawberryCrop;
import net.xach.testmod.items.TestModItems;


import java.util.Map;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    public ModBlockLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    protected ModBlockLootTableProvider(HolderLookup.Provider pRegistries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), (Map<ResourceLocation, LootTable.Builder>) pRegistries);
    }



    @Override
    protected void generate() {

        LootItemCondition.Builder lootItemConditionBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(TestModBlocks.STRAWBERRY_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(StrawberryCrop.AGE, StrawberryCrop.MAX_AGE));

        this.add(TestModBlocks.STRAWBERRY_CROP.get(), this.createCropDrops(TestModBlocks.STRAWBERRY_CROP.get(),
                TestModItems.STRAWBERRY.get(), TestModItems.STRAWBERRY_SEEDS.get(), lootItemConditionBuilder));

        this.dropSelf(TestModBlocks.MAGIC_LOG.get());
        this.dropSelf(TestModBlocks.MAGIC_WOOD.get());
        this.dropSelf(TestModBlocks.STRIPPED_MAGIC_LOG.get());
        this.dropSelf(TestModBlocks.STRIPPED_MAGIC_WOOD.get());
        this.dropSelf(TestModBlocks.MAGIC_PLANKS.get());
        this.dropSelf(TestModBlocks.MAGIC_SAPLING.get());

        this.dropSelf(TestModBlocks.MAGIC_STAIRS.get());
        this.dropSelf(TestModBlocks.MAGIC_SLAB.get());
        this.dropSelf(TestModBlocks.MAGIC_PRESSURE_PLATE.get());
        this.dropSelf(TestModBlocks.MAGIC_BUTTON.get());
        this.dropSelf(TestModBlocks.MAGIC_FENCE.get());
        this.dropSelf(TestModBlocks.MAGIC_FENCE_GATE.get());
        this.dropSelf(TestModBlocks.MAGIC_TRAPDOOR.get());

        // Для двери нужен специальный лут (дверь дропает только нижняя часть)
        this.add(TestModBlocks.MAGIC_DOOR.get(), block ->
                createDoorTable(TestModBlocks.MAGIC_DOOR.get()));

        this.add(TestModBlocks.MAGIC_LEAVES.get(), block ->
                createLeavesDrops(block, TestModBlocks.MAGIC_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));

        // Ловушка не дропает ничего при разрушении
        this.add(TestModBlocks.TRAP_BLOCK.get(), noDrop());
    }





    @Override
    protected Iterable<Block> getKnownBlocks() {
        return TestModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
