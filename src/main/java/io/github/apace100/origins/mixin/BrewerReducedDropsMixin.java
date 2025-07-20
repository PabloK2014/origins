package io.github.apace100.origins.mixin;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.origins.power.BrewerReducedDropsPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для снижения дропа с мобов для пивовара
 * Применяется к методу dropLoot в LivingEntity для Minecraft 1.20.1
 */
@Mixin(LivingEntity.class)
public class BrewerReducedDropsMixin {

    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void reduceDropsForBrewer(DamageSource damageSource, boolean causedByPlayer, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем только на серверной стороне и если есть атакующий игрок
        if (!entity.getWorld().isClient && damageSource.getAttacker() instanceof PlayerEntity player) {
            // Проверяем, есть ли у игрока сила пивовара
            PowerHolderComponent.getPowers(player, BrewerReducedDropsPower.class).forEach(power -> {
                if (power.shouldReduceDrops()) {
                    // Случайно отменяем дроп с шансом, равным силе снижения
                    if (entity.getRandom().nextFloat() < power.getDropReduction()) {
                        ci.cancel(); // Отменяем весь дроп предметов
                    }
                }
            });
        }
    }
}