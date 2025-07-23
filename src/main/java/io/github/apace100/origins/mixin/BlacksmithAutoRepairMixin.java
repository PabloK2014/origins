package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для автоматического ремонта предметов кузнеца
 */
@Mixin(ServerPlayerEntity.class)
public class BlacksmithAutoRepairMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void origins$handleAutoRepair(CallbackInfo ci) {
        try {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            
            if (!BlacksmithSkillHandler.isBlacksmith(player)) {
                return;
            }
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
            if (skillComponent == null) {
                return;
            }
            
            // Обрабатываем авторемонт
            int autoRepairLevel = skillComponent.getSkillLevel("auto_repair");
            if (autoRepairLevel > 0) {
                BlacksmithSkillHandler.handleAutoRepair(player, autoRepairLevel);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в BlacksmithAutoRepairMixin: " + e.getMessage(), e);
        }
    }
}