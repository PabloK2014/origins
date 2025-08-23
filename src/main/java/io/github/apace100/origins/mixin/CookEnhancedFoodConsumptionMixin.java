package io.github.apace100.origins.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import io.github.apace100.origins.Origins;   
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для применения улучшений еды при её употреблении
 */
@Mixin(LivingEntity.class)
public class CookEnhancedFoodConsumptionMixin {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void applyEnhancedFoodEffects(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity instanceof PlayerEntity player && !world.isClient) {
            if (stack.hasNbt()) {
                NbtCompound nbt = stack.getNbt();
                
                if (nbt != null && nbt.getBoolean("CookEnhanced")) {
                    // Получаем множители
                    float nutritionMultiplier = nbt.getFloat("NutritionMultiplier");
                    float saturationMultiplier = nbt.getFloat("SaturationMultiplier");
                    
                    // Применяем дополнительное питание
                    if (stack.getItem().isFood()) {
                        var foodComponent = stack.getItem().getFoodComponent();
                        if (foodComponent != null) {
                            int bonusNutrition = Math.round(foodComponent.getHunger() * (nutritionMultiplier - 1.0f));
                            float bonusSaturation = foodComponent.getSaturationModifier() * (saturationMultiplier - 1.0f);
                            
                            // Добавляем бонусное питание
                            player.getHungerManager().add(bonusNutrition, bonusSaturation);
                        }
                    }
                }
            }
        }
    }
}