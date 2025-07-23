package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для раскалённого удара кузнеца
 */
@Mixin(PlayerEntity.class)
public class BlacksmithHotStrikeMixin {
    
    @Inject(method = "attack", at = @At("TAIL"))
    private void origins$handleHotStrike(Entity target, CallbackInfo ci) {
        try {
            PlayerEntity player = (PlayerEntity) (Object) this;
            
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            
            if (!BlacksmithSkillHandler.isBlacksmith(serverPlayer)) {
                return;
            }
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(serverPlayer);
            if (skillComponent == null) {
                return;
            }
            
            // Проверяем навык раскалённого удара
            int hotStrikeLevel = skillComponent.getSkillLevel("hot_strike");
            if (hotStrikeLevel > 0) {
                BlacksmithSkillHandler.applyHotStrike(serverPlayer, target);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в BlacksmithHotStrikeMixin: " + e.getMessage(), e);
        }
    }
}