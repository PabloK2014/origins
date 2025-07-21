package net.xach.testmod;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyBindings {
    public static final KeyMapping ACTIVATE_SKILL = new KeyMapping(
            "key.testmod.activate_skill",
            75, // K
            "key.categories.testmod"
    );

    public static final KeyMapping OPEN_ACTIVE_SKILL_MENU = new KeyMapping(
            "key.testmod.open_active_skill_menu",
            76, // L
            "key.categories.testmod"
    );

    public static final KeyMapping TOGGLE_AREA_MINING = new KeyMapping(
            "key.testmod.toggle_area_mining",
            77, // M
            "key.categories.testmod"
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_SKILL);
        event.register(OPEN_ACTIVE_SKILL_MENU);
        event.register(TOGGLE_AREA_MINING);
    }
}
