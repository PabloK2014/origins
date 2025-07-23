package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для обработки атакующих навыков воина
 */
@Mixin(PlayerEntity.class)
public class WarriorAttackMixin {
    
    private static final ThreadLocal<Boolean> PROCESSING_ATTACK = ThreadLocal.withInitial(() -> false);
    
    @Inject(method = "attack", at = @At("HEAD"))
    private void origins$handleWarriorAttackEffects(net.minecraft.entity.Entity target, CallbackInfo ci) {
        if (PROCESSING_ATTACK.get()) {
            return;
        }
        
        try {
            PROCESSING_ATTACK.set(true);
            
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (!(player instanceof ServerPlayerEntity serverPlayer) || 
                !WarriorSkillHandler.isWarrior(serverPlayer) ||
                !(target instanceof LivingEntity livingTarget)) {
                return;
            }
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(serverPlayer);
            if (skillComponent == null) {
                return;
            }
            
            // Навык "Кровавая Рана" - шанс нанести кровотечение
            int bloodyWoundLevel = skillComponent.getSkillLevel("bloody_wound");
            if (bloodyWoundLevel > 0) {
                WarriorSkillHandler.handleBloodyWound(serverPlayer, livingTarget, bloodyWoundLevel);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в WarriorAttackMixin (effects): " + e.getMessage(), e);
        } finally {
            PROCESSING_ATTACK.set(false);
        }
    }
}