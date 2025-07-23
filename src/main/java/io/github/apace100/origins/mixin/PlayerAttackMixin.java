package io.github.apace100.origins.mixin;

import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для перехвата атак игрока и применения эффектов навыков
 */
@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {
    
    @Inject(method = "attack", at = @At("HEAD"))
    private void origins$onPlayerAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        // Проверяем только для серверной стороны
        if (player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        try {
            // Проверяем и применяем раскаленный удар кузнеца
            if (BlacksmithSkillHandler.isBlacksmith(serverPlayer)) {
                BlacksmithSkillHandler.applyHotStrike(serverPlayer, target);
            }
            
            // Здесь можно добавить другие эффекты навыков при атаке
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при применении эффектов навыков при атаке: " + e.getMessage(), e);
        }
    }
}