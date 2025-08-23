package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Миксин для применения бонуса урона от скилла "Фламбе"
 */
@Mixin(LivingEntity.class)
public class PlayerFlambeDamageMixin {

    /**
     * Модифицирует урон, наносимый игроком-поваром с активным скиллом "Фламбе"
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamageFromCook(float amount, DamageSource source) {
        Entity attacker = source.getAttacker();
        
        // Проверяем, что атакующий - игрок на сервере
        if (attacker instanceof ServerPlayerEntity serverPlayer) {
            try {
                // Проверяем, что игрок - повар
                if (isPlayerCook(serverPlayer)) {
                    LivingEntity target = (LivingEntity) (Object) this;
                    PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(serverPlayer);
                    if (skillComponent != null) {
                        float multiplier = skillComponent.applyFlambeBoost(target);
                        if (multiplier != 1.0f) {
                            float newAmount = amount * multiplier;
                            Origins.LOGGER.debug("Фламбе: урон изменен с {} на {} для цели {}", amount, newAmount, target.getName().getString());
                            return newAmount;
                        }
                    }
                }
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при применении бонуса 'Фламбе': " + e.getMessage(), e);
            }
        }
        
        return amount;
    }
    
    /**
     * Проверяет, является ли игрок поваром
     */
    private boolean isPlayerCook(ServerPlayerEntity player) {
        try {
            io.github.apace100.origins.component.OriginComponent originComponent = 
                io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                    io.github.apace100.origins.origin.OriginLayers.getLayer(
                        Origins.identifier("origin")
                    )
                );
                return origin != null && "origins:cook".equals(origin.getIdentifier().toString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке повара: " + e.getMessage());
        }
        return false;
    }
}