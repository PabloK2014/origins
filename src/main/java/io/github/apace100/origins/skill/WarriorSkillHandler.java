package io.github.apace100.origins.skill;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.Random;

/**
 * Обработчик навыков воина
 */
public class WarriorSkillHandler {
    
    private static final Random RANDOM = new Random();
    
    /**
     * Проверяет, является ли игрок воином
     */
    public static boolean isWarrior(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(serverPlayer);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                return origin != null && "origins:warrior".equals(origin.getIdentifier().toString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке воина: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Обрабатывает навык "Путь Берсерка"
     */
    public static float handleBerserkWay(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 0.0f;
        
        float healthPercent = player.getHealth() / player.getMaxHealth();
        
        // Чем меньше здоровья, тем больше урон
        if (healthPercent <= 0.3f) { // Ниже 30% здоровья
            float damageBonus = skillLevel * 0.15f; // 15% за уровень
            return damageBonus * (1.0f - healthPercent); // Максимальный бонус при 0% здоровья
        }
        
        return 0.0f;
    }
    
    /**
     * Обрабатывает навык "Кровавая Рана"
     */
    public static void handleBloodyWound(ServerPlayerEntity attacker, LivingEntity target, int skillLevel) {
        if (skillLevel <= 0) return;
        
        int chance = skillLevel * 10; // 10% за уровень
        if (RANDOM.nextInt(100) < chance) {
            // Применяем эффект кровотечения (урон со временем)
            target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WITHER,
                100 + (skillLevel * 20), // 5 + 1 сек за уровень
                0 // Уровень I
            ));
            
            attacker.sendMessage(
                Text.literal("Кровавая рана нанесена!")
                    .formatted(Formatting.DARK_RED), 
                true // action bar
            );
            
            Origins.LOGGER.info("Воин {} нанес кровавую рану {}", 
                attacker.getName().getString(), target.getName().getString());
        }
    }
    
    /**
     * Обрабатывает навык "Безумный Рывок"
     */
    public static void handleMadBoost(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        long lastUse = nbt.getLong("MadBoostLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 1200) { // 60 секунд
            nbt.putLong("MadBoostLastUse", currentTime);
            player.readNbt(nbt);
            
            // Телепортируемся к ближайшему врагу и наносим урон по области
            var nearbyEnemies = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(10.0),
                entity -> entity != player && entity.isAlive() && !entity.isTeammate(player)
            );
            
            if (!nearbyEnemies.isEmpty()) {
                LivingEntity target = nearbyEnemies.get(RANDOM.nextInt(nearbyEnemies.size()));
                
                // Телепортируемся к цели
                player.teleport(target.getX(), target.getY(), target.getZ());
                
                // Наносим урон по области
                var areaTargets = player.getWorld().getEntitiesByClass(
                    LivingEntity.class,
                    player.getBoundingBox().expand(3.0),
                    entity -> entity != player && entity.isAlive()
                );
                
                float damage = 4.0f + (skillLevel * 2.0f);
                for (LivingEntity areaTarget : areaTargets) {
                    areaTarget.damage(player.getDamageSources().playerAttack(player), damage);
                }
                
                player.sendMessage(
                    Text.literal("Безумный рывок! Урон по области: " + damage)
                        .formatted(Formatting.RED), 
                    false
                );
                
                Origins.LOGGER.info("Воин {} использовал безумный рывок", player.getName().getString());
            }
        } else {
            long cooldownLeft = 1200 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Безумный рывок перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает навык "Жажда Битвы"
     */
    public static void handleThirstBattle(ServerPlayerEntity player, LivingEntity killedEntity, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Восстанавливаем здоровье при убийстве
        float healAmount = skillLevel * 2.0f; // 2 HP за уровень
        player.heal(healAmount);
        
        // Даем временный бафф силы
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.STRENGTH,
            200, // 10 секунд
            0 // Уровень I
        ));
        
        player.sendMessage(
            Text.literal("Жажда битвы! Здоровье восстановлено: +" + healAmount + " HP")
                .formatted(Formatting.DARK_RED), 
            true // action bar
        );
        
        Origins.LOGGER.info("Воин {} восстановил {} HP от жажды битвы", 
            player.getName().getString(), healAmount);
    }
    
    /**
     * Обрабатывает навык "Последний Шанс"
     */
    public static boolean handleLastChance(ServerPlayerEntity player, float damage, int skillLevel) {
        if (skillLevel <= 0) return false;
        
        // Проверяем, будет ли урон смертельным
        if (player.getHealth() - damage <= 0) {
            var nbt = new net.minecraft.nbt.NbtCompound();
            player.writeNbt(nbt);
            long lastUse = nbt.getLong("LastChanceLastUse");
            long currentTime = player.getWorld().getTime();
            
            // Кулдаун 5 минут
            if (currentTime - lastUse >= 6000) {
                nbt.putLong("LastChanceLastUse", currentTime);
                player.readNbt(nbt);
                
                // Даем неуязвимость на 3 секунды
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE,
                    60, // 3 секунды
                    4 // Уровень V (почти полная неуязвимость)
                ));
                
                // Восстанавливаем немного здоровья
                player.setHealth(4.0f); // 2 сердца
                
                player.sendMessage(
                    Text.literal("ПОСЛЕДНИЙ ШАНС! Неуязвимость на 3 секунды!")
                        .formatted(Formatting.GOLD), 
                    false
                );
                
                Origins.LOGGER.info("Воин {} использовал последний шанс", player.getName().getString());
                return true; // Отменяем урон
            }
        }
        
        return false;
    }
    
    /**
     * Обрабатывает навык "Железная Стена"
     */
    public static float handleIronWall(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 0.0f;
        
        return skillLevel * 0.15f; // 15% снижение урона за уровень
    }
    
    /**
     * Обрабатывает навык "Несокрушимость"
     */
    public static void handleIndestructibility(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        long lastUse = nbt.getLong("IndestructibilityLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 1800) { // 90 секунд
            nbt.putLong("IndestructibilityLastUse", currentTime);
            player.readNbt(nbt);
            
            // Даем сопротивление урону на 10 секунд
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                200, // 10 секунд
                2 // Уровень III
            ));
            
            player.sendMessage(
                Text.literal("Несокрушимость активирована!")
                    .formatted(Formatting.BLUE), 
                false
            );
            
            Origins.LOGGER.info("Воин {} активировал несокрушимость", player.getName().getString());
        } else {
            long cooldownLeft = 1800 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Несокрушимость перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает навык "Крепость"
     */
    public static float handleFortress(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 0.0f;
        
        // Проверяем, стоит ли игрок на месте
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        var lastPos = new net.minecraft.util.math.Vec3d(
            nbt.getDouble("FortressLastX"),
            nbt.getDouble("FortressLastY"),
            nbt.getDouble("FortressLastZ")
        );
        
        var currentPos = player.getPos();
        double distance = lastPos.distanceTo(currentPos);
        
        // Обновляем позицию
        nbt.putDouble("FortressLastX", currentPos.x);
        nbt.putDouble("FortressLastY", currentPos.y);
        nbt.putDouble("FortressLastZ", currentPos.z);
        player.readNbt(nbt);
        
        // Если игрок не двигался (расстояние меньше 0.1 блока)
        if (distance < 0.1) {
            int stationaryTime = nbt.getInt("FortressStationaryTime") + 1;
            nbt.putInt("FortressStationaryTime", stationaryTime);
            
            // Максимальный бонус через 5 секунд (100 тиков)
            if (stationaryTime >= 100) {
                return skillLevel * 0.25f; // 25% снижение урона за уровень
            }
        } else {
            nbt.putInt("FortressStationaryTime", 0);
        }
        
        return 0.0f;
    }
    
    /**
     * Обрабатывает навыки ветки "Таджик"
     */
    public static void handleTadjicSkills(ServerPlayerEntity player, int monobrowLevel, int armenianStrengthLevel, int dagestanskayaBratvaLevel) {
        // Монобровь - регенерация здоровья
        if (monobrowLevel > 0 && player.age % 100 == 0) { // Каждые 5 секунд
            player.heal(monobrowLevel * 0.5f);
        }
        
        // Армянская сила - увеличение урона (обрабатывается в миксине атаки)
        
        // Дагестанская братва - призыв союзников (активная способность)
        if (dagestanskayaBratvaLevel > 0) {
            handleDagestanskayaBratva(player, dagestanskayaBratvaLevel);
        }
    }
    
    /**
     * Обрабатывает навык "Дагестанская братва"
     */
    public static void handleDagestanskayaBratva(ServerPlayerEntity player, int skillLevel) {
        var nbt = new net.minecraft.nbt.NbtCompound();
        player.writeNbt(nbt);
        long lastUse = nbt.getLong("DagestanskayaBratvaLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 2400) { // 120 секунд
            nbt.putLong("DagestanskayaBratvaLastUse", currentTime);
            player.readNbt(nbt);
            
            // Призываем железных големов как союзников
            for (int i = 0; i < skillLevel; i++) {
                var golem = new net.minecraft.entity.passive.IronGolemEntity(
                    net.minecraft.entity.EntityType.IRON_GOLEM, 
                    player.getWorld()
                );
                
                // Размещаем рядом с игроком
                double angle = (2 * Math.PI * i) / skillLevel;
                double x = player.getX() + Math.cos(angle) * 2;
                double z = player.getZ() + Math.sin(angle) * 2;
                
                golem.setPosition(x, player.getY(), z);
                player.getWorld().spawnEntity(golem);
            }
            
            player.sendMessage(
                Text.literal("Дагестанская братва призвана! Големы: " + skillLevel)
                    .formatted(Formatting.DARK_PURPLE), 
                false
            );
            
            Origins.LOGGER.info("Воин {} призвал дагестанскую братву", player.getName().getString());
        }
    }
}