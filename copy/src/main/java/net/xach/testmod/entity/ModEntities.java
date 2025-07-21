package net.xach.testmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xach.testmod.TestMod;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TestMod.MOD_ID);

    public static final RegistryObject<EntityType<DagestanskiBrother>> DAGESTANSKI_BROTHER =
            ENTITY_TYPES.register("dagestanski_brother", () -> EntityType.Builder.of(DagestanskiBrother::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F)
                    .build("dagestanski_brother"));

    public static final RegistryObject<EntityType<DagestanskiRogue>> DAGESTANSKI_ROGUE =
            ENTITY_TYPES.register("dagestanski_rogue", () -> EntityType.Builder.of(DagestanskiRogue::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build("dagestanski_rogue"));

    public static void register(IEventBus eventBus) {
        System.out.println("Registering ModEntities...");
        ENTITY_TYPES.register(eventBus);
        System.out.println("ModEntities registered successfully");
    }
}
