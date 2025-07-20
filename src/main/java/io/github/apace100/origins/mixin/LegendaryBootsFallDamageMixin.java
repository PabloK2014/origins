package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Миксин для иммунитета к урону от падения у легендарных ботинок
 */
@Mixin(value = PlayerEntity.class, priority = 900)
public class LegendaryBootsFallDamageMixin {
    
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float reduceFallDamage(float amount, DamageSource source) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        // Проверяем, является ли урон от падения
        if (isFallDamage(source)) {
            ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
            
            if (!boots.isEmpty() && boots.getItem() instanceof ArmorItem && 
                BlacksmithQualityCraftingPower.isLegendary(boots)) {
                
                // Иммунитет к урону от падения до 10 блоков
                // Примерно 10 блоков = 20 единиц урона от падения
                if (amount <= 20.0f) {
                    return 0.0f; // Полный иммунитет
                } else {
                    return amount - 20.0f; // Уменьшаем урон на 20 единиц
                }
            }
        }
        
        return amount;
    }
    
    private boolean isFallDamage(DamageSource source) {
        try {
            return source.getType().msgId().equals("fall");
        } catch (Exception e) {
            // Fallback: проверяем через toString или другие методы
            String sourceString = source.toString().toLowerCase();
            return sourceString.contains("fall");
        }
    }
}