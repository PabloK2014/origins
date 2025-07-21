package net.xach.testmod.food;

import com.mojang.blaze3d.shaders.Effect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProperties {
    public static final FoodProperties STRAWBERRY = new FoodProperties.Builder().nutrition(2).saturationMod(1.0F)
            .effect(new MobEffectInstance(MobEffects.LUCK,400),0.20f).fast().build();

}
