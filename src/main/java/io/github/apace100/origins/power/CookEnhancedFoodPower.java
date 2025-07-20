package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import net.minecraft.entity.LivingEntity;

/**
 * Сила повара для улучшения еды
 * Увеличивает восстановление сытости от приготовленной еды
 */
public class CookEnhancedFoodPower extends Power {
    
    private final float nutritionMultiplier;
    private final float saturationMultiplier;
    
    public CookEnhancedFoodPower(PowerType<?> type, LivingEntity entity, float nutritionMultiplier, float saturationMultiplier) {
        super(type, entity);
        this.nutritionMultiplier = nutritionMultiplier;
        this.saturationMultiplier = saturationMultiplier;
    }
    
    /**
     * Получает множитель питательности
     */
    public float getNutritionMultiplier() {
        return nutritionMultiplier;
    }
    
    /**
     * Получает множитель насыщения
     */
    public float getSaturationMultiplier() {
        return saturationMultiplier;
    }
    
    /**
     * Проверяет, должна ли сила улучшать еду
     */
    public boolean shouldEnhanceFood() {
        return isActive();
    }
}