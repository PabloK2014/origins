package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.random.Random;

/**
 * Сила кузнеца для создания предметов с различным качеством
 */
public class BlacksmithQualityCraftingPower extends Power {
    
    public enum ItemQuality {
        POOR,
        NORMAL,
        GOOD,
        LEGENDARY
    }
    
    private final float poorChance;
    private final float normalChance;
    private final float goodChance;
    private final float legendaryChance;
    
    public BlacksmithQualityCraftingPower(PowerType<?> type, LivingEntity entity, float poorChance, float normalChance, float goodChance, float legendaryChance) {
        super(type, entity);
        this.poorChance = poorChance;
        this.normalChance = normalChance;
        this.goodChance = goodChance;
        this.legendaryChance = legendaryChance;
    }
    
    /**
     * Определяет качество предмета на основе случайности
     */
    public ItemQuality determineQuality(Random random) {
        float roll = random.nextFloat();
        
        // Проверяем шансы в правильном порядке: плохое -> обычное -> хорошее -> легендарное
        if (roll < poorChance) {
            return ItemQuality.POOR;
        } else if (roll < poorChance + normalChance) {
            return ItemQuality.NORMAL;
        } else if (roll < poorChance + normalChance + goodChance) {
            return ItemQuality.GOOD;
        } else {
            return ItemQuality.LEGENDARY;
        }
    }
    
    /**
     * Проверяет, должна ли сила применяться к крафту
     */
    public boolean shouldApplyQuality() {
        return isActive();
    }
}