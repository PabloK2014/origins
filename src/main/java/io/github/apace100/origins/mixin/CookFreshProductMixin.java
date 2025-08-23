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
 * Mixin для увеличения восстановления сытости от еды у повара с навыком "Свежий продукт"
 */
@Mixin(LivingEntity.class)
public class CookFreshProductMixin {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void enhanceFoodNutrition(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
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
                            // Получаем уровень навыка "Свежий продукт"
                            int freshProductLevel = skillComponent.getFreshProductLevel();
                            
                            // Если навык изучен
                            if (freshProductLevel > 0) {
                                // Рассчитываем бонус: 2% за уровень
                                float bonusPercentage = freshProductLevel * 0.02f;
                                
                                // Применяем бонус к восстановлению сытости
                                // Получаем текущие значения
                                int baseHunger = foodComponent.getHunger();
                                float baseSaturation = foodComponent.getSaturationModifier();
                                
                                // Рассчитываем бонусные значения
                                int bonusHunger = Math.round(baseHunger * bonusPercentage);
                                float bonusSaturation = baseSaturation * bonusPercentage;
                                
                                Origins.LOGGER.debug("Повар {} получил бонус от 'Свежего продукта': +{} сытости, +{} насыщения", 
                                    player.getName().getString(), bonusHunger, bonusSaturation);
                                
                                // Добавляем бонусную сытость
                                player.getHungerManager().add(bonusHunger, bonusSaturation);
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