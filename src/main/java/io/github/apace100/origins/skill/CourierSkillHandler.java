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
    public static float handleSpeedBasic(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 0.0f;
        
        return skillLevel * 0.05f; // 5% за уровень
    }
    
    /**
     * Обрабатывает навык "Базовая скорость" (устаревший метод для совместимости)
     */
    public static float handleSpeedBoost(ServerPlayerEntity player, int skillLevel) {
        return handleSpeedBasic(player, skillLevel);
    }
    
    /**
     * Обрабатывает навык "Снижение голода"
     */
    public static float handleHungerReduction(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return 1.0f;
        
        return 1.0f - (skillLevel * 0.1f); // 10% снижение за уровень
    }
    
    /**
     * Обрабатывает улучшенный навык "Снижение голода" с учетом активных эффектов
     */
    public static void handleAdvancedHungerReduction(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Применяем базовое снижение голода
        float reduction = handleHungerReduction(player, skillLevel);
        
        // Дополнительно уменьшаем расход энергии при движении
        if (player.isSprinting()) {
            // При спринте снижаем расход энергии на 5% за уровень
            // Это будет применяться в миксинах при расходе энергии
        }
        
        if (player.isSwimming()) {
            // При плавании снижаем расход энергии на 3% за уровень
            // Это будет применяться в миксинах при расходе энергии
        }
    }
    
    /**
     * Обрабатывает навык "Граната с перцем" (теперь выдает перцовый баллончик)
     */
    public static void handleCarrySurge(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Создаем перцовый баллончик с учетом уровня навыка
        net.minecraft.item.ItemStack pepperSpray = io.github.apace100.origins.item.PepperSprayItem.createPepperSpray(skillLevel);
        
        // Добавляем в инвентарь игрока
        if (player.getInventory().insertStack(pepperSpray)) {
            player.sendMessage(
                Text.literal("Получен перцовый баллончик!")
                    .formatted(Formatting.GREEN), 
                true // action bar
            );
        } else {
            // Если инвентарь полон, выбрасываем предмет
            player.dropItem(pepperSpray, false);
            player.sendMessage(
                Text.literal("Перцовый баллончик выброшен из-за нехватки места!")
                    .formatted(Formatting.YELLOW), 
                true // action bar
            );
        }
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
     * Обрабатывает улучшенный навык "Всплеск скорости" с дополнительными эффектами
     */
    public static void handleAdvancedSpeedSurge(ServerPlayerEntity player, int skillLevel) {
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
            
            // Добавляем_jump boost
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.JUMP_BOOST, 
                600, // 30 секунд
                1 // Уровень II
            ));
            
            // Восстанавливаем голод
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(20.0f);
            
            // При высоком уровне добавляем дополнительные эффекты
            if (skillLevel >= 3) {
                // Добавляем сопротивление
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE, 
                    300, // 15 секунд
                    0 // Уровень I
                ));
            }
            
            if (skillLevel >= 5) {
                // Добавляем удачу
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.LUCK, 
                    300, // 15 секунд
                    0 // Уровень I
                ));
            }
            
            player.sendMessage(
                Text.literal("Всплеск скорости! Голод восстановлен!" + 
                    (skillLevel >= 3 ? " +Сопротивление" : "") + 
                    (skillLevel >= 5 ? " +Удача" : ""))
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
     * Обрабатывает навык "Сумка для еды" (ранее базовые слоты)
     */
    public static void handleExtraSlots(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Выдаем сумку для еды при первом уровне (теперь это скилл "сумка для еды")
        if (skillLevel == 1) {
            net.minecraft.item.ItemStack foodBag = new net.minecraft.item.ItemStack(
                io.github.apace100.origins.registry.ModItems.FOOD_BAG
            );
            
            // Добавляем в инвентарь игрока
            if (player.getInventory().insertStack(foodBag)) {
                player.sendMessage(
                    Text.literal("Получена сумка для еды!")
                        .formatted(Formatting.GREEN), 
                    true // action bar
                );
            } else {
                // Если инвентарь полон, выбрасываем предмет
                player.dropItem(foodBag, false);
                player.sendMessage(
                    Text.literal("Сумка для еды выброшена из-за нехватки места!")
                        .formatted(Formatting.YELLOW), 
                    true // action bar
                );
            }
        }
        
        // Если раньше этот скилл увеличивал слоты, то больше не делаем этого
        // Теперь это скилл "Сумка для еды"
    }
    
    /**
     * Обрабатывает навык "Магнитные карманы"
     */
    public static void handleMagneticPockets(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Увеличиваем радиус подбора предметов
        double radius = 1.5 + (skillLevel * 0.5); // Базовый радиус 1.5 + 0.5 за уровень
        
        // Ищем предметы в радиусе
        var items = player.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class, 
            player.getBoundingBox().expand(radius), 
            item -> !item.cannotPickup() && item.isAlive());
        
        int collectedCount = 0;
        
        for (net.minecraft.entity.ItemEntity item : items) {
            // Проверяем, что предмет находится в пределах радиуса
            double distance = player.getPos().distanceTo(item.getPos());
            if (distance <= radius) {
                // Притягиваем предмет к игроку с учетом уровня
                net.minecraft.util.math.Vec3d direction = player.getPos().subtract(item.getPos()).normalize();
                double pullStrength = 0.05 + (skillLevel * 0.02); // Сила притяжения зависит от уровня
                item.setVelocity(direction.multiply(pullStrength));
                
                // Проверяем возможность подбора предмета
                if (distance < 1.0) {
                    // Предмет достаточно близко для подбора
                    if (player.getInventory().insertStack(item.getStack())) {
                        // Успешно добавили предмет в инвентарь
                        item.discard();
                        collectedCount++;
                    }
                }
            }
        }
        
        // Отправляем сообщение о количестве собранных предметов
        if (collectedCount > 0) {
            player.sendMessage(
                Text.literal("Собрано предметов: " + collectedCount)
                    .formatted(Formatting.GREEN), 
                true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает улучшенный навык "Магнитные карманы" с фильтрацией
     */
    public static void handleAdvancedMagneticPockets(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Увеличиваем радиус подбора предметов
        double radius = 1.5 + (skillLevel * 0.7); // Базовый радиус 1.5 + 0.7 за уровень
        
        // Ищем предметы в радиусе
        var items = player.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class, 
            player.getBoundingBox().expand(radius), 
            item -> !item.cannotPickup() && item.isAlive());
        
        int collectedCount = 0;
        
        for (net.minecraft.entity.ItemEntity item : items) {
            // Проверяем, что предмет находится в пределах радиуса
            double distance = player.getPos().distanceTo(item.getPos());
            if (distance <= radius) {
                // Притягиваем предмет к игроку с учетом уровня
                net.minecraft.util.math.Vec3d direction = player.getPos().subtract(item.getPos()).normalize();
                double pullStrength = 0.08 + (skillLevel * 0.03); // Сила притяжения зависит от уровня
                item.setVelocity(direction.multiply(pullStrength));
                
                // Проверяем возможность подбора предмета
                if (distance < 1.2) {
                    // Предмет достаточно близко для подбора
                    if (player.getInventory().insertStack(item.getStack())) {
                        // Успешно добавили предмет в инвентарь
                        item.discard();
                        collectedCount++;
                    }
                }
            }
        }
        
        // Отправляем сообщение о количестве собранных предметов
        if (collectedCount > 0) {
            player.sendMessage(
                Text.literal("Собрано предметов: " + collectedCount)
                    .formatted(Formatting.GREEN), 
                true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает навык "Сумка для еды" - теперь имеет другое поведение
     */
    public static void handleInventorySurge(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = player.writeNbt(new net.minecraft.nbt.NbtCompound());
        long lastUse = nbt.getLong("InventorySurgeLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 1200) { // 60 секунд
            nbt.putLong("InventorySurgeLastUse", currentTime);
            player.readNbt(nbt);
            
            // Дополнительное хранилище для еды
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.HASTE, 
                600, // 30 секунд
                skillLevel - 1 // Уровень эффекта
            ));
            
            player.sendMessage(
                Text.literal("Временное ускорение работы с едой!")
                    .formatted(Formatting.GOLD), 
                true // action bar
            );
            
        } else {
            long cooldownLeft = 1200 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Сумка для еды перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
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
     * Обрабатывает навык "Карта в голове"
     */
    public static void handleShulkerCarry(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем кулдаун
        var nbt = player.writeNbt(new net.minecraft.nbt.NbtCompound());
        long lastUse = nbt.getLong("ShulkerCarryLastUse");
        long currentTime = player.getWorld().getTime();
        
        if (currentTime - lastUse >= 1800) { // 90 секунд
            nbt.putLong("ShulkerCarryLastUse", currentTime);
            player.readNbt(nbt);
            
            // Создаем "карту" с информацией о ближайших деревнях
            net.minecraft.item.ItemStack mapItem = new net.minecraft.item.ItemStack(
                net.minecraft.item.Items.MAP
            );
            
            // Добавляем NBT данные для идентификации
            var mapNbt = mapItem.getOrCreateNbt();
            mapNbt.putString("MapType", "VillageMap");
            mapNbt.putInt("MapLevel", skillLevel);
            
            // Добавляем описание
            mapItem.setCustomName(
                Text.literal("Карта деревень (Уровень " + skillLevel + ")")
                    .formatted(Formatting.BLUE)
            );
            
            // Добавляем в инвентарь игрока
            if (player.getInventory().insertStack(mapItem)) {
                player.sendMessage(
                    Text.literal("Карта деревень создана!")
                        .formatted(Formatting.GREEN), 
                    true // action bar
                );
            } else {
                // Если инвентарь полон, выбрасываем предмет
                player.dropItem(mapItem, false);
                player.sendMessage(
                    Text.literal("Карта деревень выброшена!")
                        .formatted(Formatting.YELLOW), 
                    true // action bar
                );
            }
            
        } else {
            long cooldownLeft = 1800 - (currentTime - lastUse);
            player.sendMessage(
                Text.literal("Карта деревень перезарядится через " + (cooldownLeft / 20) + " сек")
                    .formatted(Formatting.GRAY), 
                    true // action bar
            );
        }
    }
    
    /**
     * Обрабатывает навык "Ловушка"
     */
    public static void handleTrap(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Устанавливаем ловушку под игроком
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(
            (int) Math.floor(player.getX()), 
            (int) Math.floor(player.getY()) - 1, 
            (int) Math.floor(player.getZ())
        );
        
        // Проверяем, можно ли разместить ловушку
        net.minecraft.block.BlockState blockState = player.getWorld().getBlockState(pos);
        if (blockState.isAir() || blockState.isOf(net.minecraft.block.Blocks.GRASS) || 
            blockState.isOf(net.minecraft.block.Blocks.TALL_GRASS)) {
            
            // Устанавливаем реальный блок ловушки
            player.getWorld().setBlockState(pos, io.github.apace100.origins.block.ModBlocks.TRAP_BLOCK.getDefaultState());
            
            // Звук установки
            player.getWorld().playSound(null, pos, 
                net.minecraft.sound.SoundEvents.BLOCK_STONE_PLACE,
                net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
            
            player.sendMessage(
                Text.literal("Ловушка установлена под вами!")
                    .formatted(Formatting.RED), 
                true // action bar
            );
        } else {
            player.sendMessage(
                Text.literal("Нельзя установить ловушку здесь!")
                    .formatted(Formatting.RED), 
                true // action bar
            );
        }
    }
}