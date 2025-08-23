package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для добавления баффа "Вдохновение" при потреблении еды у повара с навыком "Готово!"
 */
@Mixin(LivingEntity.class)
public class CookReadyMixin {

    @Inject(method = "eatFood", at = @At("TAIL"))
    private void addInspirationBuff(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
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
                            // Получаем уровень навыка "Готово!"
                            int readyLevel = skillComponent.getReadyLevel();
                            
                            // Если навык изучен
                            if (readyLevel > 0) {
                                // Рассчитываем длительность баффа: 30 секунд + 10 секунд за уровень
                                int duration = 600 + (readyLevel * 200); // 600 тиков = 30 секунд + 200 тиков за уровень
                                
                                // Создаем эффект "Вдохновение" (+10% урона)
                                StatusEffectInstance inspirationEffect = new StatusEffectInstance(
                                    StatusEffects.STRENGTH, // Вместо ModStatusEffects.INSPIRATION используем STRENGTH
                                    duration, // Длительность
                                    0, // Уровень эффекта
                                    false, // Не ambient
                                    true, // Показывать частицы
                                    true  // Показывать иконку
                                );
                                
                                // Применяем эффект
                                player.addStatusEffect(inspirationEffect);
                                
                                Origins.LOGGER.info("Повар {} получил бафф 'Вдохновение' от навыка 'Готово!' на {} тиков", 
                                    player.getName().getString(), duration);
                                
                                player.sendMessage(
                                    net.minecraft.text.Text.literal("Вы получили бафф 'Вдохновение'!")
                                        .formatted(net.minecraft.util.Formatting.GOLD), 
                                    false
                                );
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