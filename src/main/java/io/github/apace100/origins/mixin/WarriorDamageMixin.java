package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для обработки защитных навыков воина
 */
@Mixin(PlayerEntity.class)
public class WarriorDamageMixin {
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void origins$handleWarriorDefense(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            PlayerEntity player = (PlayerEntity) (Object) this;
            
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            
            if (!WarriorSkillHandler.isWarrior(serverPlayer)) {
                return;
            }
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(serverPlayer);
            if (skillComponent == null) {
                return;
            }
            
            // Проверяем навык "Последний Шанс"
            int lastChanceLevel = skillComponent.getSkillLevel("last_chance");
            if (lastChanceLevel > 0) {
                boolean cancelled = WarriorSkillHandler.handleLastChance(serverPlayer, amount, lastChanceLevel);
                if (cancelled) {
                    cir.setReturnValue(false); // Отменяем урон
                    return;
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
                float reducedAmount = amount * (1.0f - Math.min(damageReduction, 0.8f)); // Максимум 80% снижения
                
                // Создаем новый источник урона с измененным количеством
                if (reducedAmount < amount) {
                    // Отменяем оригинальный урон и наносим уменьшенный
                    cir.setReturnValue(false);
                    serverPlayer.damage(source, reducedAmount);
                    cir.setReturnValue(true);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в WarriorDamageMixin: " + e.getMessage(), e);
        }
    }
}