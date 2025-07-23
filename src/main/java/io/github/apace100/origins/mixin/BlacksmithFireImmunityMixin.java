package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для огненного иммунитета кузнеца
 */
@Mixin(PlayerEntity.class)
public class BlacksmithFireImmunityMixin {
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void origins$handleFireImmunity(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
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
            
            // Проверяем навык огненного иммунитета
            int fireImmunityLevel = skillComponent.getSkillLevel("fire_immunity");
            if (fireImmunityLevel > 0) {
                // Проверяем, является ли урон огненным
                if (source.isOf(net.minecraft.entity.damage.DamageTypes.IN_FIRE) ||
                    source.isOf(net.minecraft.entity.damage.DamageTypes.ON_FIRE) ||
                    source.isOf(net.minecraft.entity.damage.DamageTypes.LAVA) ||
                    source.isOf(net.minecraft.entity.damage.DamageTypes.HOT_FLOOR)) {
                    
                    if (BlacksmithSkillHandler.handleFireImmunity(serverPlayer, fireImmunityLevel)) {
                        cir.setReturnValue(false); // Отменяем урон
                        return;
                    }
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в BlacksmithFireImmunityMixin: " + e.getMessage(), e);
        }
    }
}