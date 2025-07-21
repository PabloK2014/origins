package net.xach.testmod;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TestMod.MOD_ID);

    public static final RegistryObject<MenuType<ClassSelectionMenu>> CLASS_SELECTION = MENUS.register("class_selection",
            () -> IForgeMenuType.create((id, inv, data) -> new ClassSelectionMenu(id, inv)));

    public static final RegistryObject<MenuType<SkillTreeMenu>> SKILL_TREE = MENUS.register("skill_tree",
            () -> IForgeMenuType.create((id, inv, data) -> new SkillTreeMenu(id, inv)));

    public static final RegistryObject<MenuType<ActiveSkillSelectionMenu>> ACTIVE_SKILL_SELECTION = MENUS.register("active_skill_selection",
            () -> IForgeMenuType.create((id, inv, data) -> new ActiveSkillSelectionMenu(id, inv)));

    public static final RegistryObject<MenuType<FoodBagMenu>> FOOD_BAG = MENUS.register("food_bag",
            () -> IForgeMenuType.create(FoodBagMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}