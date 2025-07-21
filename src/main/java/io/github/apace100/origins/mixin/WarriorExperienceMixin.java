package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для начисления опыта воину за убийство мобов
 */
@Mixin(LivingEntity.class)
public class WarriorExperienceMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void giveExperienceForKill(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Проверяем, что сущность была убита игроком
        if (entity.getWorld().isClient || entity.getAttacker() == null || !(entity.getAttacker() instanceof PlayerEntity player)) {
            return;
        }
        
        // Проверяем, что игрок имеет происхождение воина
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !isPlayerWarrior(serverPlayer)) {
            return;
        }
        
        // Начисляем опыт в зависимости от типа моба
        int expAmount = calculateKillExperience(entity);
        if (expAmount > 0) {
            ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
            component.addExperience(expAmount);
        }
    }
    
    /**
     * Проверяет, является ли игрок воином
     */
    private boolean isPlayerWarrior(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                io.github.apace100.origins.Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:warrior");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Рассчитывает количество опыта за убийство моба
     */
    private int calculateKillExperience(LivingEntity entity) {
        // Боссы дают больше всего опыта
        if (entity instanceof EnderDragonEntity || entity.getType() == EntityType.WITHER) {
            return 100; // Боссы
        }
        
        // Враждебные мобы дают средний опыт
        if (entity instanceof HostileEntity) {
            // Более сильные враждебные мобы дают больше опыта
            if (entity.getMaxHealth() > 20) {
                return 10; // Сильные враждебные мобы
            }
            return 5; // Обычные враждебные мобы
        }
        
        // Мирные мобы дают мало опыта
        if (isAnimalEntity(entity)) {
            return 2; // Животные
        }
        
        return 1; // Другие сущности
    }
    
    /**
     * Проверяет, является ли сущность животным
     */
    private boolean isAnimalEntity(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        return type == EntityType.COW || type == EntityType.PIG || type == EntityType.SHEEP ||
               type == EntityType.CHICKEN || type == EntityType.RABBIT || type == EntityType.HORSE ||
               type == EntityType.DONKEY || type == EntityType.MULE || type == EntityType.LLAMA ||
               type == EntityType.CAT || type == EntityType.WOLF || type == EntityType.PARROT ||
               type == EntityType.OCELOT || type == EntityType.FOX || type == EntityType.BEE ||
               type == EntityType.TURTLE || type == EntityType.PANDA || type == EntityType.POLAR_BEAR ||
               type == EntityType.DOLPHIN || type == EntityType.SQUID || type == EntityType.COD ||
               type == EntityType.SALMON || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH;
    }
}