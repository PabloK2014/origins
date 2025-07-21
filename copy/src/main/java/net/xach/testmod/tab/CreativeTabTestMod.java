package net.xach.testmod.tab;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.items.TestModItems;

public class CreativeTabTestMod {
    // Создаем DeferredRegister для регистрации креативных вкладок
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TestMod.MOD_ID);

    // Регистрируем вкладку
    public static final RegistryObject<CreativeModeTab> MAGIC_TAB = CREATIVE_MODE_TABS.register("magic_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(TestModItems.STRAWBERRY.get())) // Иконка вкладки
                    .title(Component.translatable("itemGroup.testmod")) // Название вкладки
                    .displayItems((pParameters, output) -> {
                        // Добавляем элементы и блоки в вкладку
                        output.accept(TestModItems.STRAWBERRY.get());
                        output.accept(TestModItems.STRAWBERRY_SEEDS.get());
                        output.accept(TestModItems.FOOD_BAG.get());
                        output.accept(TestModBlocks.MAGIC_LOG.get());
                        output.accept(TestModBlocks.MAGIC_LEAVES.get());
                        output.accept(TestModBlocks.MAGIC_PLANKS.get());
                        output.accept(TestModBlocks.MAGIC_WOOD.get());
                        output.accept(TestModBlocks.MAGIC_SAPLING.get());
                        output.accept(TestModBlocks.MAGIC_STAIRS.get());
                        output.accept(TestModBlocks.MAGIC_SLAB.get());
                        output.accept(TestModBlocks.MAGIC_PRESSURE_PLATE.get());
                        output.accept(TestModBlocks.MAGIC_BUTTON.get());
                        output.accept(TestModBlocks.MAGIC_FENCE.get());
                        output.accept(TestModBlocks.MAGIC_FENCE_GATE.get());
                        output.accept(TestModBlocks.MAGIC_TRAPDOOR.get());
                        output.accept(TestModBlocks.MAGIC_DOOR.get());
                        output.accept(TestModBlocks.TRAP_BLOCK.get());
                        output.accept(TestModItems.PEPPER_SPRAY.get());
                    })
                    .build());
}