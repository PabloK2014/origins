package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для обработки убийств воином
 */
@Mixin(LivingEntity.class)
public class WarriorKillMixin {
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void origins$handleWarriorKill(DamageSource damageSource, CallbackInfo ci) {
        try {
            LivingEntity killedEntity = (LivingEntity) (Object) this;
            
            // Проверяем, что убийца - игрок
            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                
                if (!WarriorSkillHandler.isWarrior(killer)) {
                    return;
                }
                
                PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(killer);
                if (skillComponent == null) {
                    return;
                }
                
                // Навык "Жажда битвы" - восстановление здоровья при убийстве
                int thirstBattleLevel = skillComponent.getSkillLevel("thirst_battle");
                if (thirstBattleLevel > 0) {
                    WarriorSkillHandler.handleThirstBattle(killer, killedEntity, thirstBattleLevel);
                }
                
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в WarriorKillMixin: " + e.getMessage(), e);
        }
    }
}