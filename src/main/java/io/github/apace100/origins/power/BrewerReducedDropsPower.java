package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import net.minecraft.entity.LivingEntity;

/**
 * Сила пивовара для снижения дропа с мобов
 * Снижает шанс выпадения предметов при убийстве мобов
 */
public class BrewerReducedDropsPower extends Power {
    
    private final float dropReductionMultiplier;
    
    public BrewerReducedDropsPower(PowerType<?> type, LivingEntity entity, float dropReductionMultiplier) {
        super(type, entity);
        this.dropReductionMultiplier = dropReductionMultiplier;
    }
    
    /**
     * Получает множитель снижения дропа
     */
    public float getDropReductionMultiplier() {
        return dropReductionMultiplier;
    }
    
    /**
     * Получает шанс снижения дропа (0.3 = 30% шанс отменить дроп)
     */
    public float getDropReduction() {
        return 1.0f - dropReductionMultiplier; // 0.3 для 30% шанса отмены
    }
    
    /**
     * Проверяет, должна ли сила снижать дроп
     */
    public boolean shouldReduceDrops() {
        return isActive();
    }
}