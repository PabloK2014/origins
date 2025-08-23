package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для возможности есть на бегу без замедления у повара с навыком "Быстрый перекус"
 */
@Mixin(LivingEntity.class)
public class CookQuickSnackMixin {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void allowEatingWhileRunning(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность - игрок и мир серверный
        if (entity instanceof PlayerEntity player && !world.isClient) {
            // Проверяем, что игрок - повар
            if (isPlayerCook(player)) {
                // Получаем компонент навыков игрока
                PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
                if (skillComponent != null) {
                    // Получаем уровень навыка "Быстрый перекус"
                    int quickSnackLevel = skillComponent.getQuickSnackLevel();
                    
                    // Если навык изучен
                    if (quickSnackLevel > 0) {
                        // Убираем эффект замедления при еде, если игрок движется
                        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
                    }
                }
            }
        }
    }
    
    /**
     * Проверяет, является ли игрок поваром
     */
    private boolean isPlayerCook(PlayerEntity player) {
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(player);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                return origin != null && "origins:cook".equals(origin.getIdentifier().toString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке повара: " + e.getMessage());
        }
        return false;
    }
}