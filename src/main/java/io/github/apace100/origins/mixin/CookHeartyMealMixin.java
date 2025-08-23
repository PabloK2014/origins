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
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для увеличения продолжительности эффекта сытости у повара с навыком "Сытный обед"
 */
@Mixin(LivingEntity.class)
public class CookHeartyMealMixin {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void extendSaturationDuration(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность - игрок и мир серверный
        if (entity instanceof PlayerEntity player && !world.isClient) {
            // Проверяем, что игрок - повар
            if (isPlayerCook(player)) {
                // Проверяем, что предмет - еда
                if (stack.isFood()) {
                    var foodComponent = stack.getItem().getFoodComponent();
                    if (foodComponent != null) {
                        // Получаем компонент навыков игрока
                        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
                        if (skillComponent != null) {
                            // Получаем уровень навыка "Сытный обед"
                            int heartyMealLevel = skillComponent.getHeartyMealLevel();
                            
                            // Если навык изучен (любой уровень)
                            if (heartyMealLevel > 0) {
                                // Получаем менеджер голода игрока
                                var hungerManager = player.getHungerManager();
                                
                                // Получаем текущую насыщенность
                                float currentSaturation = hungerManager.getSaturationLevel();
                                
                                // Увеличиваем насыщенность в 2 раза
                                float extendedSaturation = currentSaturation * 2.0f;
                                float bonusSaturation = extendedSaturation - currentSaturation;
                                
                                Origins.LOGGER.debug("Повар {} получил бонус от 'Сытного обеда': +{} насыщения", 
                                    player.getName().getString(), bonusSaturation);
                                
                                // Добавляем бонусную насыщенность
                                hungerManager.setSaturationLevel(extendedSaturation);
                            }
                        }
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