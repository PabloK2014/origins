package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.BountifulQuestEventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для отслеживания убийства мобов в квестах Bountiful
 */
@Mixin(LivingEntity.class)
public class BountifulQuestKillMixin {
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность была убита игроком
        if (damageSource.getAttacker() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) damageSource.getAttacker();
            
            // Обновляем прогресс квестов на убийство
            BountifulQuestEventHandler.updateKillProgress(player, entity);
        }
    }
}