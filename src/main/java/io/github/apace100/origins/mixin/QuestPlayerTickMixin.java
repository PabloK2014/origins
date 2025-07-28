package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.QuestEventHandlers;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для периодического обновления прогресса квестов
 */
@Mixin(PlayerEntity.class)
public class QuestPlayerTickMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        // Периодически обновляем прогресс квестов
        QuestEventHandlers.onPlayerTick(player);
    }
}