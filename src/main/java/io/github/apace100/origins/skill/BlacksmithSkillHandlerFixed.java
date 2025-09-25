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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;

import java.util.Random;

/**
 * Исправленный обработчик навыков кузнеца
 */
public class BlacksmithSkillHandlerFixed {
    
    private static final Random RANDOM = new Random();
    
    // Константы для навыка "Мгновенный ремонт"
    private static final int INSTANT_REPAIR_BASE_ENERGY_COST = 80;
    private static final int INSTANT_REPAIR_MIN_ENERGY_COST = 40;
    private static final int INSTANT_REPAIR_ENERGY_REDUCTION_PER_LEVEL = 10;
    
    private static final int INSTANT_REPAIR_BASE_COOLDOWN = 6000; // 5 минут в тиках
    private static final int INSTANT_REPAIR_MIN_COOLDOWN = 3000;  // 2.5 минуты в тиках
    private static final int INSTANT_REPAIR_COOLDOWN_REDUCTION_PER_LEVEL = 600; // 30 секунд в тиках
    
    private static final float INSTANT_REPAIR_PERCENTAGE = 0.5f; // 50% восстановления прочности
    
    /**
     * Проверяет, является ли игрок кузнецом
     */
    public static boolean isBlacksmith(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(serverPlayer);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                return origin != null && "origins:blacksmith".equals(origin.getIdentifier().toString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке кузнеца: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Обрабатывает навык "Повышенная прочность"
     */
    public static void handleExtraDurability(ServerPlayerEntity player, ItemStack craftedItem, int skillLevel) {
        if (skillLevel <= 0) return;
        
        int chance = skillLevel * 2; // 2% за уровень
        if (RANDOM.nextInt(100) < chance) {
            // Увеличиваем прочность на 10%
            int maxDamage = craftedItem.getMaxDamage();
            if (maxDamage > 0) {
                int bonusDurability = (int) (maxDamage * 0.1f);
                // В Fabric нет прямого способа изменить максимальную прочность
                // Поэтому добавляем NBT тег для отслеживания
                craftedItem.getOrCreateNbt().putInt("ExtraDurability", bonusDurability);
                
                player.sendMessage(
                    Text.literal("Предмет создан с повышенной прочностью!")
                        .formatted(Formatting.GREEN), 
                    false
                );
                
                Origins.LOGGER.info("Кузнец {} создал предмет с повышенной прочностью", 
                    player.getName().getString());
            }
        }
    }
    
    /**
     * Обрабатывает навык "Ресурсная экономия"
     */
    public static boolean handleResourceEfficiency(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return false;
        
        int chance = skillLevel * 2; // 2% за уровень
        if (RANDOM.nextInt(100) < chance) {
            player.sendMessage(
                Text.literal("Ресурсы не потрачены благодаря мастерству!")
                    .formatted(Formatting.YELLOW), 
                false
            );
            
            Origins.LOGGER.info("Кузнец {} сэкономил ресурсы при крафте", 
                player.getName().getString());
            return true; // Не тратить ресурсы
        }
        
        return false;
    }
    
    /**
     * Обрабатывает навык "Удвоение слитков"
     */
    public static ItemStack handleDoubleIngot(ServerPlayerEntity player, ItemStack smeltedItem, int skillLevel) {
        if (skillLevel <= 0) return smeltedItem;
        
        // Проверяем, что это слиток
        String itemName = smeltedItem.getItem().toString().toLowerCase();
        if (!itemName.contains("ingot")) {
            return smeltedItem;
        }
        
        int chance = skillLevel * 5; // 5% за уровень
        if (RANDOM.nextInt(100) < chance) {
            ItemStack doubledItem = smeltedItem.copy();
            doubledItem.setCount(smeltedItem.getCount() * 2);
            
            player.sendMessage(
                Text.literal("Слитки удвоены мастерством кузнеца!")
                    .formatted(Formatting.GOLD), 
                false
            );
            
            Origins.LOGGER.info("Кузнец {} получил удвоенные слитки", 
                player.getName().getString());
            
            return doubledItem;
        }
        
        return smeltedItem;
    }
    
    /**
     * Обрабатывает навык "Авторемонт"
     */
    public static void handleAutoRepair(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        // Проверяем каждые 30 секунд (600 тиков)
        if (player.age % 600 != 0) return;
        
        // Ищем поврежденные предметы в инвентаре
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isDamaged()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamage();
                int repairAmount = (int) (maxDamage * 0.05f * skillLevel); // 5% за уровень
                
                int newDamage = Math.max(0, currentDamage - repairAmount);
                stack.setDamage(newDamage);
                
                if (i == 0) { // Показываем сообщение только для первого отремонтированного предмета
                    player.sendMessage(
                        Text.literal("Предметы автоматически отремонтированы")
                            .formatted(Formatting.AQUA), 
                        true // action bar
                    );
                }
                
                break; // Ремонтируем только один предмет за раз
            }
        }
    }
    
    /**
     * ИСПРАВЛЕННЫЙ: Обрабатывает навык "Мгновенный ремонт"
     */
    public static boolean handleInstantRepair(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return false;
        
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent == null) {
            Origins.LOGGER.warn("PlayerSkillComponent не найден для игрока {}", player.getName().getString());
            return false;
        }
        
        String skillId = "instant_repair";
        // Стоимость энергии зависит от уровня навыка (уменьшается с ростом уровня)
        int energyCost = Math.max(INSTANT_REPAIR_MIN_ENERGY_COST, 
            INSTANT_REPAIR_BASE_ENERGY_COST - (skillLevel - 1) * INSTANT_REPAIR_ENERGY_REDUCTION_PER_LEVEL);
        // Кулдаун также уменьшается с уровнем навыка
        int cooldownTicks = Math.max(INSTANT_REPAIR_MIN_COOLDOWN, 
            INSTANT_REPAIR_BASE_COOLDOWN - (skillLevel - 1) * INSTANT_REPAIR_COOLDOWN_REDUCTION_PER_LEVEL);
        
        // Проверяем кулдаун
        if (skillComponent.isSkillOnCooldown(skillId)) {
            long remainingTicks = skillComponent.getSkillCooldownRemaining(skillId);
            long remainingSeconds = remainingTicks / 20;
            
            player.sendMessage(
                Text.literal("Мгновенный ремонт перезарядится через " + remainingSeconds + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
            return false;
        }
        
        // Проверяем энергию
        if (!skillComponent.hasEnoughEnergy(energyCost)) {
            player.sendMessage(
                Text.literal("Недостаточно энергии! Требуется: " + energyCost + ", у вас: " + skillComponent.getCurrentEnergy())
                    .formatted(Formatting.RED), 
                true // action bar
            );
            return false;
        }
        
        // Тратим энергию
        if (!skillComponent.consumeEnergy(energyCost)) {
            return false; // Не удалось потратить энергию
        }
        
        // Устанавливаем кулдаун
        skillComponent.setSkillCooldown(skillId, cooldownTicks);
        
        int repairedCount = 0;
        
        // Ремонтируем все предметы в инвентаре
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isDamaged()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamage();
                int repairAmount = (int) (maxDamage * INSTANT_REPAIR_PERCENTAGE);
                
                int newDamage = Math.max(0, currentDamage - repairAmount);
                stack.setDamage(newDamage);
                repairedCount++;
            }
        }
        
        if (repairedCount > 0) {
            player.sendMessage(
                Text.literal("Отремонтировано предметов: " + repairedCount + " (потрачено " + energyCost + " энергии)")
                    .formatted(Formatting.GREEN), 
                false
            );
            
            Origins.LOGGER.info("Кузнец {} мгновенно отремонтировал {} предметов за {} энергии", 
                player.getName().getString(), repairedCount, energyCost);
            return true;
        } else {
            // Если нечего было ремонтировать, возвращаем энергию и сбрасываем кулдаун
            skillComponent.restoreEnergy(energyCost);
            skillComponent.setSkillCooldown(skillId, 0);
            
            player.sendMessage(
                Text.literal("Нет поврежденных предметов для ремонта")
                    .formatted(Formatting.YELLOW), 
                true // action bar
            );
            return false;
        }
    }
    
    /**
     * Обрабатывает навык "Огненный иммунитет"
     */
    public static boolean handleFireImmunity(ServerPlayerEntity player, int skillLevel) {
        return skillLevel > 0; // Полный иммунитет при любом уровне
    }
    
    /**
     * ИСПРАВЛЕННЫЙ: Обрабатывает навык "Раскалённый удар"
     */
    public static boolean handleHotStrike(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return false;
        
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent == null) return false;
        
        String skillId = "hot_strike";
        
        // Проверяем кулдаун (30 секунд = 600 тиков)
        if (skillComponent.isSkillOnCooldown(skillId)) {
            long remainingTicks = skillComponent.getSkillCooldownRemaining(skillId);
            long remainingSeconds = remainingTicks / 20;
            
            player.sendMessage(
                Text.literal("Раскалённый удар перезарядится через " + remainingSeconds + " сек")
                    .formatted(Formatting.GRAY), 
                true // action bar
            );
            return false;
        }
        
        // Устанавливаем кулдаун и готовность навыка
        skillComponent.setSkillCooldown(skillId, 600); // 30 секунд
        skillComponent.setSkillState(skillId, true); // Готов к использованию
        
        player.sendMessage(
            Text.literal("Следующая атака будет раскалённой!")
                .formatted(Formatting.RED), 
            true // action bar
        );
        
        Origins.LOGGER.info("Кузнец {} активировал раскалённый удар", 
            player.getName().getString());
        return true;
    }
    
    /**
     * Применяет эффект раскалённого удара к цели
     */
    public static void applyHotStrike(ServerPlayerEntity attacker, net.minecraft.entity.Entity target) {
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(attacker);
        if (skillComponent == null) return;
        
        String skillId = "hot_strike";
        
        // Проверяем, готов ли навык к использованию
        if (skillComponent.getSkillState(skillId)) {
            // Сбрасываем состояние готовности
            skillComponent.setSkillState(skillId, false);
            
            // Поджигаем цель на 5 секунд
            target.setOnFireFor(5);
            
            // Добавляем дополнительный урон в зависимости от уровня навыка
            int skillLevel = skillComponent.getSkillLevel(skillId);
            if (target instanceof net.minecraft.entity.LivingEntity livingTarget) {
                float bonusDamage = skillLevel * 2.0f; // 2 урона за уровень
                livingTarget.damage(attacker.getDamageSources().onFire(), bonusDamage);
            }
            
            attacker.sendMessage(
                Text.literal("Раскалённый удар!")
                    .formatted(Formatting.RED), 
                true // action bar
            );
            
            Origins.LOGGER.info("Кузнец {} применил раскалённый удар к {}", 
                attacker.getName().getString(), target.getName().getString());
        }
    }
    
    /**
     * Обрабатывает навык "Мастер горна"
     */
    public static void handleForgeMaster(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return;
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Ускоряем печи в радиусе 10 блоков
        for (int x = -10; x <= 10; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    
                    if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
                        // Добавляем NBT тег для ускорения
                        var nbt = furnace.createNbt();
                        nbt.putBoolean("ForgeMasterBoost", true);
                        nbt.putInt("ForgeMasterLevel", skillLevel);
                    }
                }
            }
        }
        
        // Показываем сообщение каждые 10 секунд
        if (player.age % 200 == 0) {
            player.sendMessage(
                Text.literal("Мастерство горна активно")
                    .formatted(Formatting.GOLD), 
                true // action bar
            );
        }
    }
    
    /**
     * Улучшает шанс успеха зачарований
     */
    public static boolean improveEnchantmentSuccess(ServerPlayerEntity player, int skillLevel) {
        if (skillLevel <= 0) return false;
        
        // Удваиваем шанс успеха зачарований
        int bonusChance = skillLevel * 20; // 20% за уровень
        return RANDOM.nextInt(100) < bonusChance;
    }
}