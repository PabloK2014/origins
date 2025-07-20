package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import net.minecraft.entity.LivingEntity;

/**
 * Сила пивовара для улучшения зелий
 * Увеличивает длительность эффектов зелий в 2 раза
 */
public class BrewerEnhancedPotionsPower extends Power {
    
    private final float durationMultiplier;
    
    public BrewerEnhancedPotionsPower(PowerType<?> type, LivingEntity entity, float durationMultiplier) {
        super(type, entity);
        this.durationMultiplier = durationMultiplier;
    }
    
    /**
     * Получает множитель длительности зелий
     */
    public float getDurationMultiplier() {
        return durationMultiplier;
    }
    
    /**
     * Проверяет, должна ли сила улучшать зелья
     */
    public boolean shouldEnhancePotion() {
        return isActive();
    }
}