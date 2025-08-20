package io.github.apace100.origins.skill;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Обработчик навыков пивовара
 */
public class BrewerSkillHandler {
    
    private static final java.util.Random RANDOM = new java.util.Random();
    
    /**
     * Проверяет, является ли игрок пивоваром
     */
    public static boolean isBrewer(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(serverPlayer);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                return origin != null && "origins:brewer".equals(origin.getIdentifier().toString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке пивовара: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Обрабатывает навык "Метание бутылок" (bottle_throw)
     * Восполняет жажду игрокам в радиусе, включая самого пивовара
     */
    public static void handleBottleThrow(ServerPlayerEntity player, int skillLevel) {
        Origins.LOGGER.info("Начало обработки навыка 'Метание бутылок' для игрока {}", player.getName().getString());
        
        if (skillLevel <= 0) {
            Origins.LOGGER.info("Уровень навыка <= 0, завершение обработки");
            return;
        }

        // Получаем компонент навыков для управления кулдауном и энергией
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent == null) {
            Origins.LOGGER.warn("PlayerSkillComponent не найден для игрока {}", player.getName().getString());
            return;
        }
        
        String skillId = "bottle_throw";
        
        // --- Проверка и применение энергии ---
        // Определим стоимость энергии и кулдаун в зависимости от уровня
        int baseEnergyCost = 15; // Базовая стоимость
        int energyCost = Math.max(5, baseEnergyCost - (skillLevel - 1) * 2); // Снижается на 2 за уровень, минимум 5
        
        int baseCooldownTicks = 300; // 15 секунд (300 тиков)
        int cooldownTicks = Math.max(100, baseCooldownTicks - (skillLevel - 1) * 30); // Снижается на 1.5 сек за уровень, минимум 5 сек (100 тиков)

        Origins.LOGGER.info("Параметры навыка: уровень={}, стоимость энергии={}, кулдаун={} тиков", skillLevel, energyCost, cooldownTicks);
        Origins.LOGGER.info("Текущая энергия игрока: {}, достаточно энергии: {}", skillComponent.getCurrentEnergy(), skillComponent.hasEnoughEnergy(energyCost));
        
        // Проверяем кулдаун ДО проверки энергии
        boolean onCooldown = skillComponent.isSkillOnCooldown(skillId);
        Origins.LOGGER.info("Навык на кулдауне: {}", onCooldown);
        
        if (onCooldown) {
            long remainingTicks = skillComponent.getSkillCooldownRemaining(skillId);
            long remainingSeconds = remainingTicks / 20;
            
            player.sendMessage(
                Text.literal("Метание бутылок перезарядится через " + remainingSeconds + " сек")
                    .formatted(Formatting.GRAY),
                true // action bar
            );
            Origins.LOGGER.info("Навык на кулдауне, осталось {} секунд", remainingSeconds);
            return;
        }
        
        // Проверяем энергию
        if (!skillComponent.hasEnoughEnergy(energyCost)) {
            player.sendMessage(
                Text.literal("Недостаточно энергии! Требуется: " + energyCost + ", у вас: " + skillComponent.getCurrentEnergy())
                    .formatted(Formatting.RED),
                true // action bar
            );
            Origins.LOGGER.info("Недостаточно энергии для активации навыка");
            return;
        }

        // --- Логика навыка ---
        ServerWorld world = (ServerWorld) player.getWorld();
        double radius = 10.0;
        int thirstAmount = 5; // Фиксированное количество восполнения жажды
        
        // Определяем область действия
        Box box = player.getBoundingBox().expand(radius);
        
        // Находим всех игроков в области, включая самого пивовара
        List<ServerPlayerEntity> playersInRange = world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> p.isAlive());

        int affectedPlayers = 0;
        Origins.LOGGER.info("Найдено {} игроков в радиусе {} блоков у пивовара {}", playersInRange.size(), radius, player.getName().getString());
        
        // Восполняем жажду каждому найденному игроку (если мод Dehydration установлен)
        for (ServerPlayerEntity targetPlayer : playersInRange) {
            Origins.LOGGER.info("Попытка восполнить жажду игроку {}", targetPlayer.getName().getString());
            if (addThirst(targetPlayer, thirstAmount)) {
                affectedPlayers++;
                Origins.LOGGER.info("Жажда успешно восполнена игроку {}", targetPlayer.getName().getString());
                // Можно добавить сообщение цели (не обязательно)
                // targetPlayer.sendMessage(Text.literal("Вас освежил пивовар!").formatted(Formatting.AQUA), true);
            } else {
                Origins.LOGGER.info("Не удалось восполнить жажду игроку {}", targetPlayer.getName().getString());
            }
        }

        // --- Завершение: тратим энергию, устанавливаем кулдаун, эффекты ---
        if (!skillComponent.consumeEnergy(energyCost)) {
            Origins.LOGGER.warn("Не удалось потратить энергию для навыка {} у игрока {}", skillId, player.getName().getString());
            // Даже если энергия не списалась, продолжаем (например, из-за гонки условий)
        }
        
        // Устанавливаем кулдаун
        skillComponent.setSkillCooldown(skillId, cooldownTicks);
        Origins.LOGGER.info("Установлен кулдаун для навыка {} на {} тиков", skillId, cooldownTicks);

        // Воспроизводим звук активации скилла
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        
        // Создаем эффект частиц (можно настроить)
        world.spawnParticles(
            ParticleTypes.SPLASH,
            player.getX(), player.getY() + 1.5, player.getZ(), // Позиция частиц (чуть выше игрока)
            50, // Количество частиц
            2.0, 2.0, 2.0, // Разброс по осям
            0.5 // Скорость частиц
        );

        // Сообщение пивовару
        player.sendMessage(
            Text.literal("Метание бутылок! Освежено игроков: " + affectedPlayers + " (потрачено " + energyCost + " энергии)")
                .formatted(Formatting.AQUA),
            false
        );
        
        Origins.LOGGER.info("Пивовар {} использовал 'Метание бутылок', освежено {} игроков, потрачено {} энергии",
            player.getName().getString(), affectedPlayers, energyCost);
        Origins.LOGGER.info("Завершение обработки навыка 'Метание бутылок'");
    }
    
    /**
     * Добавляет уровень жажды игроку, если мод Dehydration установлен
     * @param player Игрок
     * @param amount Количество единиц жажды для добавления
     * @return true, если мод Dehydration установлен и операция выполнена, false в противном случае
     */
    private static boolean addThirst(ServerPlayerEntity player, int amount) {
        try {
            Origins.LOGGER.info("Попытка восполнить жажду для игрока {}: {} единиц", player.getName().getString(), amount);
            
            // Проверяем, установлен ли мод Dehydration через доступ к ThirstManagerAccess
            Class<?> thirstManagerAccessClass = Class.forName("net.dehydration.access.ThirstManagerAccess");
            Origins.LOGGER.info("Класс ThirstManagerAccess найден: {}", thirstManagerAccessClass != null);
            
            // Проверяем, что игрок реализует интерфейс ThirstManagerAccess
            if (thirstManagerAccessClass.isInstance(player)) {
                Origins.LOGGER.info("Игрок {} реализует интерфейс ThirstManagerAccess", player.getName().getString());
                
                // Получаем ThirstManager через метод getThirstManager()
                Object thirstManager = thirstManagerAccessClass.getMethod("getThirstManager").invoke(player);
                Origins.LOGGER.info("ThirstManager получен: {}", thirstManager != null);
                
                if (thirstManager != null) {
                    // Вызываем метод add у ThirstManager
                    Method addMethod = thirstManager.getClass().getMethod("add", int.class);
                    Origins.LOGGER.info("Метод add найден: {}", addMethod != null);
                    
                    addMethod.invoke(thirstManager, amount);
                    Origins.LOGGER.info("Жажда успешно восполнена игроку {} на {} единиц", player.getName().getString(), amount);
                    return true;
                } else {
                    Origins.LOGGER.warn("ThirstManager равен null для игрока {}", player.getName().getString());
                }
            } else {
                Origins.LOGGER.debug("Игрок {} не реализует ThirstManagerAccess", player.getName().getString());
            }
        } catch (ClassNotFoundException e) {
            // Мод Dehydration не установлен
            Origins.LOGGER.debug("Мод Dehydration не найден, восполнение жажды пропущено для игрока {}: {}", player.getName().getString(), e.getMessage());
        } catch (Exception e) {
            // Другая ошибка
            Origins.LOGGER.error("Ошибка при попытке восполнить жажду игроку {}: {}", player.getName().getString(), e.getMessage(), e);
        }
        
        return false;
    }
    
    // Другие методы для пассивных навыков пивовара могут быть добавлены здесь позже
    // Например, handleAlcoholResistance, handleDrunkStrength, handleGroupBuff и т.д.
}