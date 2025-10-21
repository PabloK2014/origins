package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.client.SkillKeybinds;
import io.github.apace100.origins.skill.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Обработчик управления навыками на сервере
 */
public class SkillActivationHandler {
    
    // Примечание: Используем PlayerSkillComponent для хранения активного навыка
    
    public static void register() {
        // Обработчик активации глобального навыка происхождения (G)
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ACTIVATE_GLOBAL_SKILL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                try {
                    activateGlobalSkill(player);
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при активации глобального навыка: " + e.getMessage(), e);
                }
            });
        });
        
        // Обработчик активации активного навыка
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ACTIVATE_ACTIVE_SKILL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                try {
                    activateActiveSkill(player);
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при активации активного навыка: " + e.getMessage(), e);
                }
            });
        });
        
        // Обработчик установки активного навыка через GUI
        ServerPlayNetworking.registerGlobalReceiver(new Identifier("origins", "set_active_skill"), (server, player, handler, buf, responseSender) -> {
            String skillId = buf.readString();
            server.execute(() -> {
                try {
                    setActiveSkillFromGUI(player, skillId);
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при установке активного навыка: " + e.getMessage(), e);
                }
            });
        });
    }
    
    /**
     * Активирует глобальный навык происхождения
     */
    private static void activateGlobalSkill(ServerPlayerEntity player) {
        try {
            // Получаем происхождение игрока
            String originId = getCurrentOriginId(player);
            
            if (originId == null) {
                player.sendMessage(
                    Text.literal("Не удалось определить происхождение")
                        .formatted(Formatting.RED), 
                    true
                );
                return;
            }
            
            // Активируем глобальный навык в зависимости от происхождения
            switch (originId) {
                case "origins:cook":
                    activateCookGlobalSkill(player);
                    break;
                case "origins:miner":
                    activateMinerGlobalSkill(player);
                    break;
                case "origins:blacksmith":
                    activateBlacksmithGlobalSkill(player);
                    break;
                case "origins:warrior":
                    activateWarriorGlobalSkill(player);
                    break;
                case "origins:courier":
                    activateCourierGlobalSkill(player);
                    break;
                case "origins:brewer":
                    activateBrewerGlobalSkill(player);
                    break;
                default:
                    player.sendMessage(
                        Text.literal("У происхождения " + originId + " нет глобального навыка")
                            .formatted(Formatting.GRAY), 
                        true
                    );
                    break;
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при активации глобального навыка: " + e.getMessage(), e);
            player.sendMessage(
                Text.literal("Ошибка при активации глобального навыка")
                    .formatted(Formatting.RED), 
                true
            );
        }
    }
    
    /**
     * Активирует глобальный навык повара - костная мука
     */
    private static void activateCookGlobalSkill(ServerPlayerEntity player) {
        // Используем существующий обработчик из CookActiveSkillHandler
        io.github.apace100.origins.skill.CookActiveSkillHandler.activateCookSkill(player);
    }
    
    /**
     * Активирует глобальный навык шахтера - ночное зрение
     */
    private static void activateMinerGlobalSkill(ServerPlayerEntity player) {
        // Активируем/деактивируем ночное зрение
        boolean hasNightVision = player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
        
        if (hasNightVision) {
            // Убираем ночное зрение
            player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
            player.sendMessage(
                Text.literal("Ночное зрение отключено")
                    .formatted(Formatting.GRAY), 
                true
            );
        } else {
            // Даем ночное зрение на 10 минут
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 
                12000, // 10 минут (12000 тиков)
                0, // уровень 1
                false, // не ambient
                false  // не показывать частицы
            ));
            player.sendMessage(
                Text.literal("Ночное зрение активировано на 10 минут")
                    .formatted(Formatting.GREEN), 
                true
            );
        }
    }
    
    /**
     * Получает ID текущего происхождения игрока
     */
    private static String getCurrentOriginId(ServerPlayerEntity player) {
        try {
            io.github.apace100.origins.component.OriginComponent originComponent = 
                io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            
            if (originComponent == null) {
                return null;
            }
            
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(Origins.identifier("origin")));
            if (origin == null) {
                return null;
            }
            
            return origin.getIdentifier().toString();
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении ID происхождения: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Устанавливает активный навык через GUI
     */
    private static void setActiveSkillFromGUI(ServerPlayerEntity player, String skillId) {
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent == null) {
            player.sendMessage(
                Text.literal("Ошибка: компонент навыков не найден")
                    .formatted(Formatting.RED), 
                false
            );
            return;
        }
        
        // Проверяем, что навык изучен
        int skillLevel = skillComponent.getSkillLevel(skillId);
        if (skillLevel <= 0) {
            player.sendMessage(
                Text.literal("Навык '" + skillId + "' не изучен!")
                    .formatted(Formatting.RED), 
                false
            );
            return;
        }
        
        // Устанавливаем активный навык
        skillComponent.setActiveSkill(skillId);
        
        String skillName = getSkillDisplayName(skillId);
        player.sendMessage(
            Text.literal("Активный навык установлен: " + skillName + " (Уровень " + skillLevel + ")")
                .formatted(Formatting.GREEN), 
            false
        );
        
        player.sendMessage(
            Text.literal("Используйте клавишу K для активации навыка")
                .formatted(Formatting.GRAY), 
            false
        );
        
        Origins.LOGGER.info("Игрок {} установил активный навык: {}", 
            player.getName().getString(), skillId);
    }
    
    /**
     * Активирует текущий выбранный активный навык игрока
     */
    private static void activateActiveSkill(ServerPlayerEntity player) {
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent == null) {
            return;
        }
        
        // Получаем текущий выбранный активный навык игрока
        String activeSkill = getPlayerActiveSkill(player);
        
        if (activeSkill == null || activeSkill.isEmpty()) {
            player.sendMessage(
                Text.literal("Активный навык не выбран! Используйте клавишу L для выбора.")
                    .formatted(Formatting.YELLOW), 
                true
            );
            return;
        }
        
        // Проверяем, что навык изучен
        int skillLevel = skillComponent.getSkillLevel(activeSkill);
        if (skillLevel <= 0) {
            player.sendMessage(
                Text.literal("Навык '" + activeSkill + "' не изучен!")
                    .formatted(Formatting.RED), 
                true
            );
            return;
        }
        
        // Активируем навык
        SkillInfo skillInfo = new SkillInfo(activeSkill, getSkillDisplayName(activeSkill), skillLevel);
        activateSkillByInfo(player, skillInfo);
    }
    
    /**
     * Открывает меню выбора активного навыка
     */
    private static void openSkillSelectionMenu(ServerPlayerEntity player) {
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent == null) {
            return;
        }
        
        // Получаем список доступных активных навыков
        List<SkillInfo> availableSkills = getAvailableSkills(player, skillComponent);
        
        if (availableSkills.isEmpty()) {
            player.sendMessage(
                Text.literal("У вас нет доступных активных навыков")
                    .formatted(Formatting.GRAY), 
                true
            );
            return;
        }
        
        // Отображаем список доступных навыков
        player.sendMessage(
            Text.literal("=== Доступные активные навыки ===")
                .formatted(Formatting.GOLD), 
            false
        );
        
        String currentActiveSkill = getPlayerActiveSkill(player);
        
        for (int i = 0; i < availableSkills.size(); i++) {
            SkillInfo skill = availableSkills.get(i);
            String prefix = skill.id.equals(currentActiveSkill) ? "► " : "  ";
            Formatting color = skill.id.equals(currentActiveSkill) ? Formatting.GREEN : Formatting.WHITE;
            
            player.sendMessage(
                Text.literal(prefix + (i + 1) + ". " + skill.name + " (Уровень " + skill.level + ")")
                    .formatted(color), 
                false
            );
        }
        
        player.sendMessage(
            Text.literal("Введите /setactiveskill <номер> для выбора навыка")
                .formatted(Formatting.GRAY), 
            false
        );
    }
    
    /**
     * Получает текущий выбранный активный навык игрока
     */
    private static String getPlayerActiveSkill(ServerPlayerEntity player) {
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent != null) {
            return skillComponent.getActiveSkill();
        }
        return null;
    }
    
    /**
     * Получает отображаемое имя навыка
     */
    private static String getSkillDisplayName(String skillId) {
        return switch (skillId) {
            // Навыки кузнеца
            case "hot_strike" -> "Раскалённый удар";
            case "instant_repair" -> "Мгновенный ремонт";
            
            // Навыки воина
            case "mad_boost" -> "Безумный рывок";
            case "indestructibility" -> "Несокрушимость";
            case "dagestan" -> "Дагестанская братва";
            
            // Навыки курьера
            case "sprint_boost" -> "Рывок";
            case "speed_surge" -> "Всплеск скорости";
            case "carry_capacity_basic" -> "Ловушка";
            case "carry_surge" -> "Граната с перцем";
            
            // Навыки шахтера
            case "ore_highlight" -> "Подсветка руды";
            case "vein_miner" -> "Жилокопатель";
            
            // Навыки пивовара
            case "master_brewer" -> "Мастер-пивовар";
            case "bottle_throw" -> "Метание бутылок";
            case "berserker_drink" -> "Напиток берсерка";
            case "healing_ale" -> "Лечебный эль";
            case "party_time" -> "Время вечеринки";
            
            // Навыки повара
            case "smoke_screen" -> "Дымовая завеса";
            case "banquet" -> "Банкет";
            
            default -> skillId;
        };
    }
    
    /**
     * Получает стоимость энергии для навыка
     */
    private static int getSkillEnergyCost(String skillId) {
        return switch (skillId) {
            // Навыки кузнеца
            case "hot_strike" -> 3;
            case "instant_repair" -> 5;
            
            // Навыки воина
            case "mad_boost" -> 4;
            case "indestructibility" -> 6;
            case "dagestan" -> 10;
            
            // Навыки курьера
            case "sprint_boost" -> 2;
            case "speed_surge" -> 5;
            case "carry_capacity_basic" -> 3;
            case "carry_surge" -> 3;
            
            // Навыки шахтера
            case "ore_highlight" -> 4;
            case "vein_miner" -> 6;
            
            // Навыки пивовара
            case "master_brewer" -> 3;
            case "bottle_throw" -> 15; // 15 энергии для метания бутылок
            case "berserker_drink" -> 8;
            case "healing_ale" -> 5;
            case "party_time" -> 10;
            
            // Навыки повара
            case "smoke_screen" -> 3;
            case "banquet" -> 8;
            
            default -> 2; // Стоимость по умолчанию
        };
    }
    
    /**
     * Получает кулдаун навыка в тиках
     */
    private static int getSkillCooldown(String skillId) {
        return switch (skillId) {
            // Навыки кузнеца
            case "hot_strike" -> 600; // 30 секунд
            case "instant_repair" -> 6000; // 300 секунд (5 минут)
            
            // Навыки воина
            case "mad_boost" -> 100; // 5 секунд
            case "indestructibility" -> 1200; // 60 секунд
            case "dagestan" -> 2400; // 120 секунд
            
            // Навыки курьера
            case "sprint_boost" -> 600; // 30 секунд
            case "speed_surge" -> 1200; // 60 секунд
            case "carry_capacity_basic" -> 600; // 30 секунд
            case "carry_surge" -> 600; // 30 секунд
            
            // Навыки шахтера
            case "ore_highlight" -> 600; // 30 секунд
            case "vein_miner" -> 1200; // 60 секунд
            
            // Навыки пивовара
            case "master_brewer" -> 300; // 15 секунд
            case "bottle_throw" -> 300; // 15 секунд
            case "berserker_drink" -> 1200; // 60 секунд
            case "healing_ale" -> 400; // 20 секунд
            case "party_time" -> 1800; // 90 секунд
            
            // Навыки повара
            case "smoke_screen" -> 300; // 15 секунд
            case "banquet" -> 1800; // 90 секунд
            
            default -> 1200; // 60 секунд по умолчанию
        };
    }
    
    // Удалены неиспользуемые методы selectNextSkill и activateSelectedSkill
    // Теперь используется GUI для выбора навыков
    
    /**
     * Активирует конкретный навык
     */
    private static void activateSkillByInfo(ServerPlayerEntity player, SkillInfo skill) {
        try {
            // Проверяем систему энергии
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
            if (skillComponent == null) {
                player.sendMessage(
                    Text.literal("Ошибка: компонент навыков не найден")
                        .formatted(Formatting.RED), 
                    true
                );
                return;
            }
            
            // Получаем стоимость энергии для навыка
            int energyCost = getSkillEnergyCost(skill.id);
            
            Origins.LOGGER.info("Проверка активации навыка {} для игрока {}", skill.id, player.getName().getString());
            Origins.LOGGER.info("Стоимость энергии: {}", energyCost);
            Origins.LOGGER.info("Текущая энергия игрока: {}", skillComponent.getCurrentEnergy());
            Origins.LOGGER.info("Достаточно энергии: {}", skillComponent.hasEnoughEnergy(energyCost));
            Origins.LOGGER.info("Скилл в кулдауне: {}", skillComponent.isSkillOnCooldown(skill.id));
            
            // Проверяем, хватает ли энергии
            if (!skillComponent.canUseSkill(skill.id, energyCost)) {
                if (!skillComponent.hasEnoughEnergy(energyCost)) {
                    Origins.LOGGER.info("Недостаточно энергии для активации навыка {}", skill.id);
                    player.sendMessage(
                        Text.literal("Недостаточно энергии! Требуется: " + energyCost + ", у вас: " + skillComponent.getCurrentEnergy())
                            .formatted(Formatting.RED), 
                        true
                    );
                } else if (skillComponent.isSkillOnCooldown(skill.id)) {
                    long cooldownRemaining = skillComponent.getSkillCooldownRemaining(skill.id);
                    Origins.LOGGER.info("Скилл {} находится в кулдауне, осталось {} тиков", skill.id, cooldownRemaining);
                    player.sendMessage(
                        Text.literal("Навык перезарядится через " + (cooldownRemaining / 20) + " сек")
                            .formatted(Formatting.GRAY), 
                        true
                    );
                }
                return;
            }
            
            // Тратим энергию
            if (!skillComponent.consumeEnergy(energyCost)) {
                player.sendMessage(
                    Text.literal("Не удалось потратить энергию")
                        .formatted(Formatting.RED), 
                    true
                );
                return;
            }
            
            // Получаем кулдаун навыка
            int cooldownTicks = getSkillCooldown(skill.id);
            
            // Устанавливаем кулдаун только для скиллов, кроме bottle_throw
            if (!"bottle_throw".equals(skill.id)) {
                skillComponent.setSkillCooldown(skill.id, cooldownTicks);
            }
            
            switch (skill.id) {
                // Навыки кузнеца
                case "hot_strike":
                    BlacksmithSkillHandler.handleHotStrike(player, skill.level);
                    break;
                case "instant_repair":
                    BlacksmithSkillHandler.handleInstantRepair(player, skill.level);
                    break;
                    
                // Навыки воина
                case "mad_boost":
                    WarriorSkillHandler.handleMadBoost(player, skill.level);
                    break;
                case "indestructibility":
                    WarriorSkillHandler.handleIndestructibility(player, skill.level);
                    break;
                case "dagestan":
                    WarriorSkillHandler.handleDagestanskayaBratva(player, skill.level);
                    break;
                    
                // Навыки курьера
                case "sprint_boost":
                    CourierSkillHandler.handleSprintBoost(player, skill.level);
                    break;
                case "speed_surge":
                    CourierSkillHandler.handleSpeedSurge(player, skill.level);
                    break;
                case "carry_capacity_basic":
                    CourierSkillHandler.handleTrap(player, skill.level);
                    break;
                case "carry_surge":
                    CourierSkillHandler.handleCarrySurge(player, skill.level);
                    break;
                    
                // Навыки шахтера
                case "ore_highlight":
                    handleMinerOreHighlight(player, skill.level);
                    break;
                case "vein_miner":
                    handleMinerVeinMiner(player, skill.level);
                    break;
                    
                // Навыки пивовара
                case "master_brewer":
                    handleBrewerMasterBrewer(player, skill.level);
                    break;
                case "bottle_throw":
                    Origins.LOGGER.info("Вызов BrewerSkillHandler.handleBottleThrow для игрока {}", player.getName().getString());
                    // Используем наш обработчик, который восполняет жажду другим игрокам
                    BrewerSkillHandler.handleBottleThrow(player, skill.level);
                    break;
                case "berserker_drink":
                    Origins.LOGGER.info("Вызов handleBrewerBerserkerDrink для игрока {}", player.getName().getString());
                    handleBrewerBerserkerDrink(player, skill.level);
                    break;
                case "healing_ale":
                    Origins.LOGGER.info("Вызов handleBrewerHealingAle для игрока {}", player.getName().getString());
                    handleBrewerHealingAle(player, skill.level);
                    break;
                case "party_time":
                    Origins.LOGGER.info("Вызов handleBrewerPartyTime для игрока {}", player.getName().getString());
                    handleBrewerPartyTime(player, skill.level);
                    break;
                    
                // Навыки повара
                case "smoke_screen":
                    handleCookSmokeScreen(player, skill.level);
                    break;
                case "banquet":
                    Origins.LOGGER.info("Вызов handleCookBanquet для игрока {}", player.getName().getString());
                    handleCookBanquet(player, skill.level);
                    break;
                    
                default:
                    player.sendMessage(
                        Text.literal("Неизвестный навык: " + skill.id)
                            .formatted(Formatting.RED), 
                        true
                    );
                    break;
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при активации навыка: " + e.getMessage(), e);
            player.sendMessage(
                Text.literal("Ошибка при активации навыка: " + e.getMessage())
                    .formatted(Formatting.RED), 
                true
            );
        }
    }
    
    /**
     * Получает список доступных активных навыков для игрока
     */
    private static List<SkillInfo> getAvailableSkills(ServerPlayerEntity player, PlayerSkillComponent skillComponent) {
        List<SkillInfo> availableSkills = new java.util.ArrayList<>();
        
        // Определяем класс игрока и добавляем соответствующие навыки
        if (BlacksmithSkillHandler.isBlacksmith(player)) {
            // Навыки кузнеца
            int hotStrikeLevel = skillComponent.getSkillLevel("hot_strike");
            if (hotStrikeLevel > 0) {
                availableSkills.add(new SkillInfo("hot_strike", "Раскалённый удар", hotStrikeLevel));
            }
            
            int instantRepairLevel = skillComponent.getSkillLevel("instant_repair");
            if (instantRepairLevel > 0) {
                availableSkills.add(new SkillInfo("instant_repair", "Мгновенный ремонт", instantRepairLevel));
            }
        } 
        else if (WarriorSkillHandler.isWarrior(player)) {
            // Навыки воина
            int madBoostLevel = skillComponent.getSkillLevel("mad_boost");
            if (madBoostLevel > 0) {
                availableSkills.add(new SkillInfo("mad_boost", "Безумный рывок", madBoostLevel));
            }
            
            int indestructibilityLevel = skillComponent.getSkillLevel("indestructibility");
            if (indestructibilityLevel > 0) {
                availableSkills.add(new SkillInfo("indestructibility", "Несокрушимость", indestructibilityLevel));
            }
            
            int dagestanskayaBratvaLevel = skillComponent.getSkillLevel("dagestan");
            if (dagestanskayaBratvaLevel > 0) {
                availableSkills.add(new SkillInfo("dagestan", "Дагестанская братва", dagestanskayaBratvaLevel));
            }
        } 
        else if (CourierSkillHandler.isCourier(player)) {
            // Навыки курьера
            int sprintBoostLevel = skillComponent.getSkillLevel("sprint_boost");
            if (sprintBoostLevel > 0) {
                availableSkills.add(new SkillInfo("sprint_boost", "Рывок", sprintBoostLevel));
            }
            
            int speedSurgeLevel = skillComponent.getSkillLevel("speed_surge");
            if (speedSurgeLevel > 0) {
                availableSkills.add(new SkillInfo("speed_surge", "Всплеск скорости", speedSurgeLevel));
            }
            
            int trapLevel = skillComponent.getSkillLevel("carry_capacity_basic");
            if (trapLevel > 0) {
                availableSkills.add(new SkillInfo("carry_capacity_basic", "Ловушка", trapLevel));
            }
            
            int carrySurgeLevel = skillComponent.getSkillLevel("carry_surge");
            if (carrySurgeLevel > 0) {
                availableSkills.add(new SkillInfo("carry_surge", "Граната с перцем", carrySurgeLevel));
            }
        }
        else {
            // Для других классов получаем навыки из SkillTreeHandler
            String originId = getCurrentOriginId(player);
            if (originId != null) {
                SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(originId);
                if (skillTree != null) {
                    for (SkillTreeHandler.Skill skill : skillTree.getAllSkills()) {
                        // Проверяем активные навыки (ACTIVE и GLOBAL могут быть активированы)
                        if (skill.getType() == SkillTreeHandler.SkillType.ACTIVE || 
                            skill.getType() == SkillTreeHandler.SkillType.GLOBAL) {
                            int skillLevel = skillComponent.getSkillLevel(skill.getId());
                            if (skillLevel > 0) {
                                availableSkills.add(new SkillInfo(skill.getId(), skill.getName(), skillLevel));
                            }
                        }
                    }
                }
            }
        }
        
        return availableSkills;
    }
    
    /**
     * Класс для хранения информации о навыке
     */
    private static class SkillInfo {
        public final String id;
        public final String name;
        public final int level;
        
        public SkillInfo(String id, String name, int level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }
    }
    
    // Методы активации глобальных навыков для остальных классов
    
    /**
     * Активирует глобальный навык кузнеца - нет глобального навыка
     */
    private static void activateBlacksmithGlobalSkill(ServerPlayerEntity player) {
        player.sendMessage(
            Text.literal("У кузнеца нет глобального навыка")
                .formatted(Formatting.GRAY), 
            true
        );
            }
    
    /**
     * Активирует глобальный навык воина - боевая ярость
     */
    private static void activateWarriorGlobalSkill(ServerPlayerEntity player) {
        player.sendMessage(
            Text.literal("Боевая ярость активирована!")
                .formatted(Formatting.RED), 
            true
        );
        
        // Даем эффект силы на 30 секунд
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.STRENGTH, 
            600, // 30 секунд (600 тиков)
            0, // уровень 1
            false, // не ambient
            false  // не показывать частицы
        ));
        
            }
    
    /**
     * Активирует глобальный навык курьера - быстрая доставка
     */
    private static void activateCourierGlobalSkill(ServerPlayerEntity player) {
        player.sendMessage(
            Text.literal("Быстрая доставка активирована!")
                .formatted(Formatting.BLUE), 
            true
        );
        
        // Даем эффект скорости на 60 секунд
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SPEED, 
            1200, // 60 секунд (1200 тиков)
            1, // уровень 2
            false, // не ambient
            false  // не показывать частицы
        ));
        
            }
    
    /**
     * Активирует глобальный навык пивовара - очищающий эликсир
     */
    private static void activateBrewerGlobalSkill(ServerPlayerEntity player) {
        player.sendMessage(
            Text.literal("Очищающий эликсир активирован!")
                .formatted(Formatting.LIGHT_PURPLE), 
            true
        );
        
        // Убираем все негативные эффекты
        player.clearStatusEffects();
        
        // Даем регенерацию на 10 секунд
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.REGENERATION, 
            200, // 10 секунд (200 тиков)
            0, // уровень 1
            false, // не ambient
            false  // не показывать частицы
        ));
        
            }
    
    // Методы активации навыков шахтера
    private static void handleMinerOreHighlight(ServerPlayerEntity player, int level) {
        player.sendMessage(
            Text.literal("Подсветка руды активирована на " + (30 + level * 10) + " секунд!")
                .formatted(Formatting.YELLOW), 
            true
        );
        
        // Даем эффект ночного зрения для подсветки руды
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 
            (30 + level * 10) * 20, // время в тиках
            0, 
            false, 
            false
        ));
        
        // Даем светящийся эффект для лучшей видимости
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.GLOWING, 
            (30 + level * 10) * 20,
            0, 
            false, 
            false
        ));
    }
    
    private static void handleMinerVeinMiner(ServerPlayerEntity player, int level) {
        player.sendMessage(
            Text.literal("Жилокопатель активирован! Следующие " + (3 + level) + " блоков руды будут добыты автоматически")
                .formatted(Formatting.GOLD), 
            true
        );
        
        // Даем эффект спешки для быстрой добычи
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.HASTE, 
            600, // 30 секунд
            level - 1, 
            false, 
            false
        ));
    }
    
    // Методы активации навыков пивовара
    private static void handleBrewerMasterBrewer(ServerPlayerEntity player, int level) {
        player.sendMessage(
            Text.literal("Мастер-пивовар активирован! Создание уникальных напитков доступно")
                .formatted(Formatting.LIGHT_PURPLE), 
            true
        );
        
        // Даем временный эффект удачи для лучших результатов варки
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.LUCK, 
            1200, // 60 секунд
            level - 1, 
            false, 
            false
        ));
    }
    
    // Метод активации навыка "Метание бутылок" теперь использует наш обработчик
    // который восполняет жажду другим игрокам
    
    private static void handleBrewerBerserkerDrink(ServerPlayerEntity player, int level) {
        Origins.LOGGER.info("Начало обработки навыка 'Напиток берсерка' для игрока {}", player.getName().getString());
        Origins.LOGGER.info("Уровень навыка: {}", level);
        
        player.sendMessage(
            Text.literal("Напиток берсерка! Временная неуязвимость и ярость!")
                .formatted(Formatting.DARK_RED), 
            false // Отправляем в чат, а не в action bar
        );
        
        // Даем мощные эффекты
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.STRENGTH, 
            400, // 20 секунд
            1, // уровень 2
            false, 
            false
        ));
        
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.RESISTANCE, 
            400, // 20 секунд
            0, 
            false, 
            false
        ));
        
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SPEED, 
            400, // 20 секунд
            0, 
            false, 
            false
        ));
        
        Origins.LOGGER.info("Завершение обработки навыка 'Напиток берсерка'");
    }
    
    private static void handleBrewerHealingAle(ServerPlayerEntity player, int level) {
        Origins.LOGGER.info("Начало обработки навыка 'Лечебный эль' для игрока {}", player.getName().getString());
        Origins.LOGGER.info("Уровень навыка: {}", level);
        
        player.sendMessage(
            Text.literal("Лечебный эль! Восстановление здоровья союзников")
                .formatted(Formatting.GREEN), 
            false // Отправляем в чат, а не в action bar
        );
        
        // Лечим игрока
        player.heal(2.0f * level);
        Origins.LOGGER.info("Игрок {} вылечен на {} HP", player.getName().getString(), 2.0f * level);
        
        // Даем регенерацию
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.REGENERATION, 
            200, // 10 секунд
            level - 1, 
            false, 
            false
        ));
        Origins.LOGGER.info("Игроку {} добавлен эффект регенерации", player.getName().getString());
        
        // Лечим ближайших союзников (включая самого игрока)
        List<ServerPlayerEntity> playersInRange = player.getWorld().getEntitiesByClass(
            ServerPlayerEntity.class, 
            player.getBoundingBox().expand(10.0),
            p -> p.isAlive()
        );
        
        Origins.LOGGER.info("Найдено {} игроков в радиусе для лечебного эля", playersInRange.size());
        
        for (ServerPlayerEntity ally : playersInRange) {
            Origins.LOGGER.info("Лечение союзника: {}", ally.getName().getString());
            ally.heal(1.0f * level);
            ally.sendMessage(
                Text.literal("Вы получили лечение от лечебного эля!")
                    .formatted(Formatting.GREEN), 
                false // Отправляем в чат, а не в action bar
            );
        }
        
        Origins.LOGGER.info("Завершение обработки навыка 'Лечебный эль'");
    }
    
    private static void handleBrewerPartyTime(ServerPlayerEntity player, int level) {
        Origins.LOGGER.info("Начало обработки навыка 'Время вечеринки' для игрока {}", player.getName().getString());
        Origins.LOGGER.info("Уровень навыка: {}", level);
        
        player.sendMessage(
            Text.literal("Время вечеринки! Массовые баффы для всей команды!")
                .formatted(Formatting.GOLD), 
            false // Отправляем в чат, а не в action bar
        );
        
        // Даем баффы всем игрокам в радиусе 20 блоков (включая самого игрока)
        List<ServerPlayerEntity> playersInRange = player.getWorld().getEntitiesByClass(
            ServerPlayerEntity.class, 
            player.getBoundingBox().expand(20.0),
            p -> p.isAlive()
        );
        
        Origins.LOGGER.info("Найдено {} игроков в радиусе", playersInRange.size());
        
        for (ServerPlayerEntity ally : playersInRange) {
            Origins.LOGGER.info("Применение эффектов к союзнику: {}", ally.getName().getString());
            
            // Даем различные положительные эффекты
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.STRENGTH, 600, 0, false, false));
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED, 600, 0, false, false));
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.REGENERATION, 200, 0, false, false));
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.LUCK, 600, 0, false, false));
            
            ally.sendMessage(
                Text.literal("Вечеринка началась! Получены баффы!")
                    .formatted(Formatting.GOLD), 
                false // Отправляем в чат, а не в action bar
            );
        }
        
        Origins.LOGGER.info("Завершение обработки навыка 'Время вечеринки'");
    }
    
    // Методы активации навыков повара
    private static void handleCookSmokeScreen(ServerPlayerEntity player, int level) {
        player.sendMessage(
            Text.literal("Дымовая завеса! Невидимость активирована")
                .formatted(Formatting.GRAY), 
            false // Отправляем в чат, а не в action bar
        );
        
        // Даем невидимость
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 
            60 + (level * 20), // 3 + level секунд
            0, 
            false, 
            false
        ));
        
        // Даем скорость для быстрого отступления
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SPEED, 
            60 + (level * 20),
            0, 
            false, 
            false
        ));
        
        // Даем эффект медленного падения для более плавного перемещения
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SLOW_FALLING, 
            60 + (level * 20),
            0, 
            false, 
            false
        ));
    }
    
    private static void handleCookBanquet(ServerPlayerEntity player, int level) {
        Origins.LOGGER.info("Начало обработки навыка 'Банкет' для игрока {}", player.getName().getString());
        Origins.LOGGER.info("Уровень навыка: {}", level);
        
        player.sendMessage(
            Text.literal("Банкет! Все союзники получают регенерацию и сопротивление")
                .formatted(Formatting.GOLD), 
            false // Отправляем в чат, а не в action bar
        );
        
        // Даем мощные эффекты всем игрокам в радиусе 15 блоков (включая самого игрока)
        List<ServerPlayerEntity> playersInRange = player.getWorld().getEntitiesByClass(
            ServerPlayerEntity.class, 
            player.getBoundingBox().expand(15.0),
            p -> p.isAlive()
        );
        
        Origins.LOGGER.info("Найдено {} игроков в радиусе для банкета", playersInRange.size());
        
        for (ServerPlayerEntity ally : playersInRange) {
            Origins.LOGGER.info("Применение эффектов банкета к союзнику: {}", ally.getName().getString());
            
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.REGENERATION, 
                400, // 20 секунд
                level - 1, 
                false, 
                false
            ));
            
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.RESISTANCE, 
                400, // 20 секунд
                0, 
                false, 
                false
            ));
            
            ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SATURATION, 
                200, // 10 секунд
                level - 1, 
                false, 
                false
            ));
            
            // Восстанавливаем здоровье и голод
            ally.heal(4.0f);
            ally.getHungerManager().add(6, 0.6f);
            
            ally.sendMessage(
                Text.literal("Банкет! Вы получили мощные баффы и восстановление!")
                    .formatted(Formatting.GOLD), 
                false // Отправляем в чат, а не в action bar
            );
        }
        
        Origins.LOGGER.info("Завершение обработки навыка 'Банкет'");
    }

}