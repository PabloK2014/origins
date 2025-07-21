package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mixin для начисления опыта курьеру за перемещение на большие расстояния
 */
@Mixin(PlayerEntity.class)
public class CourierExperienceMixin {

    // Хранение последних позиций игроков
    private static final Map<UUID, Vec3d> lastPositions = new HashMap<>();
    // Хранение накопленного расстояния
    private static final Map<UUID, Double> accumulatedDistances = new HashMap<>();
    // Минимальное расстояние для начисления опыта (в блоках)
    private static final double DISTANCE_THRESHOLD = 100.0;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void trackDistanceAndGiveExperience(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        if (player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Проверяем, что игрок имеет происхождение курьера
        if (!isPlayerCourier(serverPlayer)) {
            return;
        }
        
        UUID playerId = player.getUuid();
        Vec3d currentPos = player.getPos();
        
        // Если это первый тик, сохраняем текущую позицию
        if (!lastPositions.containsKey(playerId)) {
            lastPositions.put(playerId, currentPos);
            accumulatedDistances.put(playerId, 0.0);
            return;
        }
        
        // Получаем последнюю позицию и рассчитываем пройденное расстояние
        Vec3d lastPos = lastPositions.get(playerId);
        double distance = currentPos.distanceTo(lastPos);
        
        // Игнорируем телепортацию (слишком большое расстояние за один тик)
        if (distance > 20) {
            lastPositions.put(playerId, currentPos);
            return;
        }
        
        // Накапливаем пройденное расстояние
        double accumulated = accumulatedDistances.get(playerId) + distance;
        accumulatedDistances.put(playerId, accumulated);
        
        // Если накопленное расстояние превысило порог, начисляем опыт
        if (accumulated >= DISTANCE_THRESHOLD) {
            // Начисляем опыт за каждые DISTANCE_THRESHOLD блоков
            int expAmount = (int) (accumulated / DISTANCE_THRESHOLD);
            ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
            component.addExperience(expAmount * 5); // 5 опыта за каждые 100 блоков
            
            // Вычитаем начисленное расстояние
            accumulatedDistances.put(playerId, accumulated % DISTANCE_THRESHOLD);
        }
        
        // Обновляем последнюю позицию
        lastPositions.put(playerId, currentPos);
    }
    
    /**
     * Проверяет, является ли игрок курьером
     */
    private boolean isPlayerCourier(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                io.github.apace100.origins.Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:courier");
        } catch (Exception e) {
            return false;
        }
    }
}