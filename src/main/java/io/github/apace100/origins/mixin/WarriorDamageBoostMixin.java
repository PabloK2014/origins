package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Миксин для увеличения урона воина
 */
@Mixin(LivingEntity.class)
public class WarriorDamageBoostMixin {
    
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float origins$modifyWarriorDamage(float amount, DamageSource source) {
        try {
            // Проверяем, что источник урона - игрок-воин
            if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
                
                if (!WarriorSkillHandler.isWarrior(attacker)) {
                    return amount;
                }
                
                PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(attacker);
                if (skillComponent == null) {
                    return amount;
                }
                
                float modifiedAmount = amount;
                
                // Навык "Путь Берсерка" - увеличение урона при низком здоровье
                int berserkLevel = skillComponent.getSkillLevel("berserk_way");
                if (berserkLevel > 0) {
                    float berserkBonus = WarriorSkillHandler.handleBerserkWay(attacker, berserkLevel);
                    modifiedAmount += amount * berserkBonus;
                }
                
                // Навык "Армянская сила" - увеличение урона
                int armenianStrengthLevel = skillComponent.getSkillLevel("carry");
                if (armenianStrengthLevel > 0) {
                    float strengthBonus = armenianStrengthLevel * 0.1f; // 10% за уровень
                    modifiedAmount += amount * strengthBonus;
                }
                
                return modifiedAmount;
            }
            
            return amount;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в WarriorDamageBoostMixin: " + e.getMessage(), e);
            return amount;
        }
    }
}