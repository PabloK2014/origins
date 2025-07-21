package net.xach.testmod.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xach.testmod.TestMod;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.food.ModFoodProperties;
import net.xach.testmod.items.custom.FoodBagItem;
import net.xach.testmod.items.custom.PepperSprayItem;
import net.xach.testmod.items.custom.TrapPlacerItem;

public class TestModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TestMod.MOD_ID);

    public static final RegistryObject<Item> STRAWBERRY = ITEMS.register("strawberry",
            () -> new Item(new Item.Properties().food(ModFoodProperties.STRAWBERRY)));

    public static final RegistryObject<Item> PEPPER_SPRAY = ITEMS.register("pepper_spray",
            () -> new PepperSprayItem(new Item.Properties()));
    public static final RegistryObject<Item> TRAP_PLACER = ITEMS.register("trap_placer",
            () -> new TrapPlacerItem(new Item.Properties()));
    public static final RegistryObject<Item> FOOD_BAG = ITEMS.register("food_bag",
            () -> new FoodBagItem(new Item.Properties()));

    public static final RegistryObject<Item> MAGIC_SAPLING = ITEMS.register("magic_sapling",
            () -> new ItemNameBlockItem(TestModBlocks.MAGIC_SAPLING.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRAWBERRY_SEEDS = ITEMS.register("strawberry_seeds",
            () -> new ItemNameBlockItem(TestModBlocks.STRAWBERRY_CROP.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
