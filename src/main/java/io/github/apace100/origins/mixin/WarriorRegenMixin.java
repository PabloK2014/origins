package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для обработки регенерации воина
 */
@Mixin(PlayerEntity.class)
public class WarriorRegenMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void origins$handleWarriorRegen(CallbackInfo ci) {
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
            
            // Навык "Монобровь" - регенерация здоровья
            int monobrowLevel = skillComponent.getSkillLevel("tadjic");
            if (monobrowLevel > 0 && serverPlayer.age % 100 == 0) { // Каждые 5 секунд
                float healAmount = monobrowLevel * 0.5f; // 0.5 HP за уровень каждые 5 секунд
                if (serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.heal(healAmount);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в WarriorRegenMixin: " + e.getMessage(), e);
        }
    }
}