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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin для добавления случайных позитивных эффектов от еды у повара с навыком "Шеф-повар"
 */
@Mixin(LivingEntity.class)
public class CookChefMasterMixin {

    @Inject(method = "eatFood", at = @At("TAIL"))
    private void addRandomPositiveEffects(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
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
                            // Получаем уровень навыка "Шеф-повар"
                            int chefMasterLevel = skillComponent.getChefMasterLevel();
                            
                            // Если навык изучен
                            if (chefMasterLevel > 0) {
                                // Рассчитываем шанс: 10% за уровень
                                float chance = chefMasterLevel * 0.10f;
                                float randomValue = player.getRandom().nextFloat();
                                
                                Origins.LOGGER.debug("Повар {} проверяет шанс эффекта от 'Шеф-повара': {} < {}", 
                                    player.getName().getString(), randomValue, chance);
                                
                                if (randomValue < chance) {
                                    // Выбираем случайный позитивный эффект
                                    StatusEffectInstance effect = getRandomPositiveEffect(player.getRandom());
                                    
                                    if (effect != null) {
                                        player.addStatusEffect(effect);
                                        
                                        player.sendMessage(
                                            net.minecraft.text.Text.literal("Вы получили случайный бафф от шеф-повара!")
                                                .formatted(net.minecraft.util.Formatting.GOLD), 
                                            false
                                        );
                                        
                                        Origins.LOGGER.info("Повар {} получил случайный эффект от 'Шеф-повара': {}", 
                                            player.getName().getString(), effect.getEffectType());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Возвращает случайный позитивный эффект
     */
    private StatusEffectInstance getRandomPositiveEffect(Random random) {
        List<StatusEffectInstance> positiveEffects = new ArrayList<>();
        
        // Добавляем различные позитивные эффекты
        positiveEffects.add(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0)); // Регенерация 10 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.SPEED, 600, 0)); // Скорость 30 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 0)); // Сила 30 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 0)); // Сопротивление 30 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 600, 0)); // Прыгучесть 30 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 0)); // Поглощение 60 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.LUCK, 1200, 0)); // Удача 60 сек
        positiveEffects.add(new StatusEffectInstance(StatusEffects.HASTE, 600, 0)); // Спешка 30 сек
        
        // Выбираем случайный эффект
        if (!positiveEffects.isEmpty()) {
            return positiveEffects.get(random.nextInt(positiveEffects.size()));
        }
        
        return null;
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