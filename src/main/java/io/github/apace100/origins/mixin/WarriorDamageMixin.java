package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для обработки защитных навыков воина
 */
@Mixin(PlayerEntity.class)
public class WarriorDamageMixin {
    
    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float origins$modifyDamageAmount(float amount, DamageSource source) {
        try {
            PlayerEntity player = (PlayerEntity) (Object) this;
            
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return amount;
            }
            
            if (!WarriorSkillHandler.isWarrior(serverPlayer)) {
                return amount;
            }
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(serverPlayer);
            if (skillComponent == null) {
                return amount;
            }
            
            // Проверяем навык "Последний Шанс"
            int lastChanceLevel = skillComponent.getSkillLevel("last_chance");
            if (lastChanceLevel > 0) {
                if (WarriorSkillHandler.handleLastChance(serverPlayer, amount, lastChanceLevel)) {
                    return 0.0f;
                }
            }
            
            // Применяем снижение урона от навыков защиты
            float damageReduction = 0.0f;
            
            // Навык "Железная Стена"
            int ironWallLevel = skillComponent.getSkillLevel("iron");
            if (ironWallLevel > 0) {
                damageReduction += WarriorSkillHandler.handleIronWall(serverPlayer, ironWallLevel);
            }
            
            // Навык "Крепость"
            int fortressLevel = skillComponent.getSkillLevel("fortress");
            if (fortressLevel > 0) {
                damageReduction += WarriorSkillHandler.handleFortress(serverPlayer, fortressLevel);
            }
            
            // Применяем снижение урона
            if (damageReduction > 0.0f) {
                return amount * (1.0f - Math.min(damageReduction, 0.8f)); // Максимум 80% снижения
            }
            
            return amount;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в WarriorDamageMixin: " + e.getMessage(), e);
            return amount;
        }
    }
}