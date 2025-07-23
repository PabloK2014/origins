package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.client.SkillKeybinds;
import io.github.apace100.origins.skill.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
            case "hot_strike" -> "Раскалённый удар";
            case "instant_repair" -> "Мгновенный ремонт";
            case "mad_boost" -> "Безумный рывок";
            case "indestructibility" -> "Несокрушимость";
            case "dagestan" -> "Дагестанская братва";
            case "sprint_boost" -> "Рывок";
            case "speed_surge" -> "Всплеск скорости";
            case "carry_capacity_basic" -> "Ловушка";
            default -> skillId;
        };
    }
    
    // Удалены неиспользуемые методы selectNextSkill и activateSelectedSkill
    // Теперь используется GUI для выбора навыков
    
    /**
     * Активирует конкретный навык
     */
    private static void activateSkillByInfo(ServerPlayerEntity player, SkillInfo skill) {
        try {
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
        Origins.LOGGER.info("Попытка активации глобального навыка кузнеца для игрока {}", player.getName().getString());
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
        
        Origins.LOGGER.info("Активирован глобальный навык воина для игрока {}", player.getName().getString());
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
        
        Origins.LOGGER.info("Активирован глобальный навык курьера для игрока {}", player.getName().getString());
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
        
        Origins.LOGGER.info("Активирован глобальный навык пивовара для игрока {}", player.getName().getString());
    }

}