package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для иммунитета к огню и лаве у повара с навыком "Огнестойкость"
 */
@Mixin(LivingEntity.class)
public class CookFireImmunityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void makeFireImmune(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность - игрок
        if (entity instanceof PlayerEntity player) {
            // Проверяем, что игрок - повар
            if (isPlayerCook(player)) {
                // Получаем компонент навыков игрока
                PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
                if (skillComponent != null) {
                    // Получаем уровень навыка "Огнестойкость"
                    int fireImmunityLevel = skillComponent.getFireImmunityLevel();
                    
                    // Если навык изучен
                    if (fireImmunityLevel > 0) {
                        // Проверяем, является ли источник урона огнем или лавой
                        if (damageSource.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE) || 
                            damageSource.isOf(net.minecraft.entity.damage.DamageTypes.LAVA)) {
                            // Делаем игрока невосприимчивым к огню и лаве
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
    }
    
    @Inject(method = "canWalkOnFluid", at = @At("HEAD"), cancellable = true)
    private void makeLavaWalkable(FluidState state, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность - игрок
        if (entity instanceof PlayerEntity player) {
            // Проверяем, что игрок - повар
            if (isPlayerCook(player)) {
                // Получаем компонент навыков игрока
                PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
                if (skillComponent != null) {
                    // Получаем уровень навыка "Огнестойкость"
                    int fireImmunityLevel = skillComponent.getFireImmunityLevel();
                    
                    // Если навык изучен
                    if (fireImmunityLevel > 0) {
                        // Проверяем, является ли жидкость лавой
                        if (state.isIn(FluidTags.LAVA)) {
                            // Разрешаем ходить по лаве
                            cir.setReturnValue(true);
                            return;
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