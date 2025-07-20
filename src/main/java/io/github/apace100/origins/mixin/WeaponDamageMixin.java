package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import io.github.apace100.origins.util.ItemQualityHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Миксин для модификации урона оружия в зависимости от качества
 */
@Mixin(LivingEntity.class)
public class WeaponDamageMixin {
    
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamage(float amount, DamageSource source) {
        if (source.getAttacker() instanceof PlayerEntity player) {
            ItemStack weapon = player.getMainHandStack();
            
            if (!weapon.isEmpty() && (weapon.getItem() instanceof SwordItem)) {
                float damageModifier = ItemQualityHelper.getDamageModifier(weapon);
                
                if (damageModifier != 0.0f) {
                    float modifiedDamage = amount * (1.0f + damageModifier);
                    
                    // Применяем легендарные эффекты для мечей
                    if (BlacksmithQualityCraftingPower.isLegendary(weapon)) {
                        applyLegendarySwordEffects(player, (LivingEntity) (Object) this, modifiedDamage);
                    }
                    
                    return modifiedDamage;
                }
            }
        }
        
        return amount;
    }
    
    private void applyLegendarySwordEffects(PlayerEntity attacker, LivingEntity target, float damage) {
        // Вампиризм: восстанавливает 20% от нанесённого урона
        float healing = damage * 0.20f;
        attacker.heal(healing);
        
        // 25% шанс поджечь врага на 3 секунды
        if (attacker.getRandom().nextFloat() < 0.25f) {
            target.setOnFireFor(3);
        }
    }
}