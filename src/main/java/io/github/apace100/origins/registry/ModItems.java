package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.content.OrbOfOriginItem;
import io.github.apace100.origins.item.FoodBagItem;
import io.github.apace100.origins.item.TrapPlacerItem;
import io.github.apace100.origins.item.PepperSprayItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModItems {

    public static final Item ORB_OF_ORIGIN = new OrbOfOriginItem();
    public static final Item FOOD_BAG = new FoodBagItem(new Item.Settings().maxCount(1));
    public static final Item TRAP_PLACER = new TrapPlacerItem(new Item.Settings().maxCount(16));
    public static final Item PEPPER_SPRAY = new PepperSprayItem(new Item.Settings().maxCount(1).maxDamage(64));

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "orb_of_origin"), ORB_OF_ORIGIN);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "food_bag"), FOOD_BAG);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "trap_placer"), TRAP_PLACER);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "pepper_spray"), PEPPER_SPRAY);
    }
}
