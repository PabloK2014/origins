package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.QuestEventHandlers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для отслеживания убийства мобов в квестах
 */
@Mixin(LivingEntity.class)
public class QuestKillMixin {
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность была убита игроком
        if (entity.getWorld().isClient || entity.getAttacker() == null || !(entity.getAttacker() instanceof PlayerEntity player)) {
            return;
        }
        
        // Отслеживаем убийство для квестов
        QuestEventHandlers.onEntityKill(player, entity);
    }
}