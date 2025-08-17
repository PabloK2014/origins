package io.github.apace100.origins.skill;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Обработчик навыков курьера
 */
public class CourierSkillHandler {
    
    private static final Random RANDOM = new Random();
    
    /**
     * Проверяет, является ли игрок курьером
     */
    public static boolean isCourier(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(serverPlayer);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                return origin != null && "origins:courier".equals(origin.getIdentifier().toString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке курьера: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Обрабатывает навык "Базовая скорость"
     */
    public static float handleSpeedBoost(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 0.0f;
        
        return skillLevel * 0.05f; // 5% за уровень
    }
    
    /**
     * Обрабатывает навык "Снижение голода"
     */
    public static float handleHungerReduction(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 1.0f;
        
        return 1.0f - (skillLevel * 0.1f); // 10% снижение за уровень
    }
    
    /**
     * Обрабатывает навык "Рывок"
     */
    public static void handleSprintBoost(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        long lastUse = nbt.getLong("SprintBoostLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 600) { // 30 секунд
            nbt.putLong("SprintBoostLastUse", currentTime);
            player.readNbt(nbt);
            
            // Применяем эффект скорости на 10 секунд
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED, 
                200, // 10 секунд
                skillLevel - 1 // Уровень эффекта
            ));
            
            player.sendMessage(
                Text.literal("Рывок активирован!")
                    .formatted(Formatting.YELLOW), 
                true // action bar
            );
            
                    } else {
            long cooldownLeft = 600 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Рывок перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает навык "Всплеск скорости"
     */
    public static void handleSpeedSurge(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        long lastUse = nbt.getLong("SpeedSurgeLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 1200) { // 60 секунд
            nbt.putLong("SpeedSurgeLastUse", currentTime);
            player.readNbt(nbt);
            
            // Применяем мощный эффект скорости на 30 секунд
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED, 
                600, // 30 секунд
                2 // Уровень III
            ));
            
            // Восстанавливаем голод
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(20.0f);
            
            player.sendMessage(
                Text.literal("Всплеск скорости! Голод восстановлен!")
                    .formatted(Formatting.GOLD), 
                false
            );
            
                    } else {
            long cooldownLeft = 1200 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Всплеск скорости перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает навык "Базовые слоты"
     */
    public static int handleExtraSlots(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 0;
        
        return skillLevel; // 1 слот за уровень
    }
    
    /**
     * Обрабатывает навык "Магнитные карманы"
     */
    public static void handleMagneticPockets(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Увеличиваем радиус подбора предметов
        double radius = 1.5 + skillLevel; // Базовый радиус + 1 блок за уровень
        
        // Ищем предметы в радиусе
        var items = player.getWorld().getEntitiesByClass(ItemEntity.class, 
            player.getBoundingBox().expand(radius), 
            item -> !item.cannotPickup());
        
        for (ItemEntity item : items) {
            // Притягиваем предмет к игроку
            Vec3d direction = player.getPos().subtract(item.getPos()).normalize();
            item.setVelocity(direction.multiply(0.1));
        }
    }
    
    /**
     * Обрабатывает навык "Улыбка Курьера" (скидки у торговцев)
     */
    public static float handleTradeDiscount(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 1.0f;
        
        return 1.0f - (skillLevel * 0.15f); // 15% скидка за уровень
    }
    
    /**
     * Обрабатывает навык "Ловушка"
     */
    public static void handleTrap(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        long lastUse = nbt.getLong("TrapLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 600) { // 30 секунд
            nbt.putLong("TrapLastUse", currentTime);
            
            // Устанавливаем невидимую ловушку на текущей позиции
            var pos = player.getBlockPos();
            nbt.putLong("TrapX", pos.getX());
            nbt.putLong("TrapY", pos.getY());
            nbt.putLong("TrapZ", pos.getZ());
            nbt.putLong("TrapTime", currentTime);
            nbt.putInt("TrapLevel", skillLevel);
            
            player.sendMessage(
                Text.literal("Ловушка установлена!")
                    .formatted(Formatting.RED), 
                true // action bar
            );
            
                    } else {
            long cooldownLeft = 600 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Ловушка перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
        }
    }
    
    /**
     * Проверяет активацию ловушки
     */
    public static void checkTrapActivation(ServerPlayerEntity player, net.minecraft.entity.Entity target) {
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        if (!nbt.contains("TrapTime")) return;
        
        long trapTime = nbt.getLong("TrapTime");
        long currentTime = player.getWorld().getTime();
        
        // Ловушка активна 60 секунд
        if (currentTime - trapTime > 1200) {
            nbt.remove("TrapTime");
            return;
        }
        
        var trapPos = new net.minecraft.util.math.BlockPos(
            (int) nbt.getLong("TrapX"),
            (int) nbt.getLong("TrapY"),
            (int) nbt.getLong("TrapZ")
        );
        
        // Проверяем, находится ли цель рядом с ловушкой
        if (target.getBlockPos().isWithinDistance(trapPos, 2.0)) {
            int trapLevel = nbt.getInt("TrapLevel");
            
            // Оглушаем цель
            if (target instanceof net.minecraft.entity.LivingEntity living) {
                living.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.SLOWNESS,
                    100 + (trapLevel * 20), // 5 + 1 сек за уровень
                    3 // Уровень IV
                ));
                
                living.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.WEAKNESS,
                    100 + (trapLevel * 20),
                    1 // Уровень II
                ));
            }
            
            player.sendMessage(
                Text.literal("Ловушка сработала!")
                    .formatted(Formatting.GREEN), 
                true // action bar
            );
            
            // Удаляем ловушку после срабатывания
            nbt.remove("TrapTime");
            
            Origins.LOGGER.info("Ловушка курьера {} сработала на {}", 
                player.getName().getString(), target.getName().getString());
        }
    }
}