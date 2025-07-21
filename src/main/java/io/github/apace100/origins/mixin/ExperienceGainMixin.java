package io.github.apace100.origins.mixin;

import io.github.apace100.origins.progression.OriginProgressionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для обработки общего получения опыта
 */
@Mixin(PlayerEntity.class)
public class ExperienceGainMixin {

    private static final int PASSIVE_EXP_INTERVAL = 1200; // 1 минута (20 тиков * 60 секунд)
    private int tickCounter = 0;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void givePassiveExperience(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        if (player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Увеличиваем счетчик тиков
        tickCounter++;
        
        // Каждую минуту даем небольшое количество опыта
        if (tickCounter >= PASSIVE_EXP_INTERVAL) {
            tickCounter = 0;
            
            // Получаем компонент прогрессии
            OriginProgressionComponent component = OriginProgressionComponent.KEY.get(serverPlayer);
            
            // Проверяем, есть ли у игрока активное происхождение
            if (component.getCurrentProgression() != null) {
                // Даем 1 опыт каждую минуту просто за игру
                component.addExperience(1);
            }
        }
    }
}